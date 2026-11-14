# ADR-005 — Contrato de eventos `triaje.eventos` y vista de urgencias en Redis

**Estado:** aceptada · **Fecha:** 2026-11-14

## Contexto

Los servicios se integran únicamente por Kafka. El asistente debe reconstruir "pacientes activos por nivel"
consumiendo el topic (sin llamar a la API de triage) y persistir la vista en Redis o NoSQL. Kafka entrega
*al menos una vez*, y el orden solo está garantizado dentro de una partición.

## Decisión

### Formato de mensaje: JSON versionado, clave = id del paciente

- **Topic:** `triaje.eventos`, 3 particiones. **Clave:** `pacienteId` → todos los eventos de un paciente
  caen en la misma partición y conservan su orden relativo.
- **Formato:** JSON UTF-8 (legible, depurable con cualquier herramienta, sin registro de esquemas). Se
  descartó Avro/Schema Registry por añadir infraestructura que a esta escala no aporta; el campo `version`
  permite evolucionar el contrato y los consumidores aplican *lector tolerante* (ignoran campos desconocidos).

```json
{
  "eventoId":  "6f1a…",                 // UUID único del evento (idempotencia en consumidores)
  "tipo":      "paciente_registrado",   // paciente_registrado | paciente_en_consulta | paciente_alta | paciente_escalado
  "version":   1,
  "pacienteId":"0b7c…",
  "nivel":     "CRITICO",
  "estado":    "REGISTRADO",            // estado clínico tras el evento
  "escalado":  false,
  "ocurridoEn":"2026-11-14T15:04:05Z"
}
```

- El productor usa `acks=all` + `enable.idempotence=true` y publica **después** de confirmar la transacción
  de base de datos. Se asume consciente la ventana "commit OK / publicación fallida" (sin *outbox*); el
  error queda en logs y el impacto es una vista de urgencias desactualizada, no una pérdida clínica. Un
  patrón *outbox* es la evolución natural si el negocio lo exige.

### Vista en Redis, idempotente mediante script Lua

- `urgencias:activos` → hash `nivel → activos`; `urgencias:paciente:{id}` → hash `{nivel, estado}` con TTL 7 días.
- Cada evento se aplica con **un script Lua atómico** que lee el último estado conocido del paciente y
  decide si suma, resta o ignora. Reglas:
  - Primer evento de un paciente activo → `+1` en su nivel (sin importar si llega primero `registrado` o `en_consulta`).
  - Transición a `ALTA` de un paciente activo → `−1`; `ALTA` es terminal: eventos posteriores o repetidos se ignoran.
  - Mismo estado repetido (duplicado de Kafka) → sin cambio.
- Consumo con reactor-kafka: `concatMap` (un registro a la vez, backpressure real) y *acknowledge*
  manual tras aplicar el evento. Un mensaje malformado se registra y se descarta para no bloquear la partición.

## Consecuencias

- (+) Reentregas y reinicios del consumidor no alteran los conteos; probado en `AsistenteApiIT`.
- (+) `GET /asistente/estado-urgencias` es O(1): un `HGETALL` sobre cinco claves.
- (−) Si un evento se pierde en origen (ver outbox), la vista diverge hasta el siguiente evento del paciente.
  Mitigación futura: reconstrucción desde el principio del topic (los consumidores arrancan con `earliest`).
