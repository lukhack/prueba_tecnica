# ADR-004 — Alertas de escalamiento al equipo médico (RF-06)

**Estado:** aceptada · **Fecha:** 2026-11-14

## Contexto

RF-06 pide alertar al equipo médico cuando un paciente es escalado, sin fijar canal, formato ni tecnología.
Supuestos sobre el contexto de VidaCruz (12 salas de urgencias):

- El equipo médico ya trabaja con un canal de mensajería corporativa (Teams/Slack) o un sistema de
  guardias que acepta webhooks; es el medio más rápido para una alerta operativa de "paciente esperando de más".
- Una alerta no debe depender de que el servicio de notificaciones esté arriba: lo crítico es que el
  escalamiento quede **persistido, auditado y publicado**; la alerta es la "última milla".
- Puede haber varios consumidores interesados (tableros, SMS, buscapersonas) que no deben acoplarse al triage.

## Decisión

1. **Puerto de dominio `NotificadorEquipoMedico`** invocado por el caso de uso de escalamiento, *después* de
   confirmar la transacción (flag + auditoría) y de publicar `paciente_escalado`.
2. **Adaptador webhook HTTP** (`AdaptadorNotificadorWebhook`): `POST` JSON con un campo `text` legible
   (compatible con *incoming webhooks* de Slack y Microsoft Teams) más campos estructurados (`pacienteId`,
   `nivel`, `minutosEsperando`, `fechaRegistro`). URL por variable `ALERTAS_WEBHOOK_URL`.
3. **Best-effort con protección**: timeout 3 s, 2 reintentos con backoff y circuit breaker `alertasWebhook`.
   Si el webhook falla, el error se registra y **no** afecta al escalamiento ni al ciclo del monitor.
4. **Siempre se emite un log de alerta** (logger `com.vidacruz.triage.ALERTAS`, nivel WARN, campos
   estructurados). Sin webhook configurado, este log es el canal: cualquier plataforma de observabilidad
   (Azure Monitor, Grafana Loki, Datadog) puede enrutarlo a guardias.
5. **El evento `paciente_escalado` en Kafka es el canal de integración** para cualquier otro consumidor
   (p. ej. un futuro servicio de SMS); el triage no necesita conocerlos.

## Alternativas descartadas

- **Correo electrónico**: demasiado lento para un paciente crítico que lleva más de 5 min esperando.
- **Servicio de notificaciones dedicado**: justificado solo cuando haya varios canales con plantillas y
  preferencias por usuario; hoy sería sobre-ingeniería (ver ADR-001).
- **Alertar desde el asistente al consumir el evento**: mezclaría responsabilidades y haría la alerta
  dependiente de un servicio que puede estar en modo degradado.

## Consecuencias

- (+) Implementación pequeña, probada (fallo del webhook no impide escalar) y reemplazable por puerto.
- (+) Sin credenciales en código; el webhook es un secreto de despliegue.
- (−) No hay confirmación de lectura ni escalado a un segundo nivel si nadie atiende; queda como evolución
  (por ejemplo, un consumidor de `paciente_escalado` que re-alerta a los N minutos).
