# ADR-001 — Descomposición en dos microservicios y arquitectura hexagonal

**Estado:** aceptada · **Fecha:** 2026-11-14

## Contexto

El enunciado exige mínimo dos microservicios reactivos (Triage y Asistente Clínico) y deja la
arquitectura interna a criterio del candidato. Se busca un diseño que un equipo pueda entender y
extender rápidamente, con el dominio clínico aislado de la tecnología.

## Decisión

1. **Exactamente dos servicios**, alineados con los dos contextos del negocio:
   - `triage-service`: fuente de verdad del paciente (registro, clasificación, estados, SLA, auditoría, reportes).
   - `asistente-service`: consumidor de eventos (vista de urgencias) y RAG sobre protocolos.

   No se añadió un tercer servicio (p. ej. "notificaciones") porque RF-06 cabe como adaptador de
   salida del triage y una descomposición extra solo añadiría latencia y operación sin un dominio propio.

2. **Arquitectura hexagonal (puertos y adaptadores)** en cada servicio, con tres paquetes y nombres en español
   (el lenguaje del negocio):

   | Paquete           | Contenido                                                                                        | Depende de |
   |-------------------|--------------------------------------------------------------------------------------------------|------------|
   | `dominio`         | `modelo` (agregados y objetos de valor), `servicio` (reglas RN-xx), `puerto` (interfaces), `excepcion` | nada       |
   | `aplicacion`      | `manejador` (un caso de uso por clase), `comando`, `respuesta`                                   | dominio    |
   | `infraestructura` | `entrada` (REST, Kafka consumer, scheduler) y `salida` (R2DBC, Kafka producer, Redis, IA, webhook), `configuracion` | ambos      |

   El dominio y los manejadores **no tienen anotaciones de Spring**; se ensamblan en
   `ConfiguracionCasosDeUso`. Las reglas de negocio viven en el dominio (`Paciente`, `ClasificadorTriaje`,
   `PoliticaAntiAlucinacion`) y se prueban sin contenedor.

3. **Sin módulo compartido entre servicios.** Cada uno tiene su propio `NivelTriaje` y su propia lectura del
   evento. El contrato entre ellos es el mensaje JSON documentado en ADR-005, no una clase Java.

## Consecuencias

- (+) Cada servicio se despliega, versiona y escala de forma independiente; el triage nunca depende del asistente.
- (+) Los tests unitarios de reglas (E1–E6, transiciones, idempotencia, RN-05) corren en milisegundos.
- (+) Cambiar PostgreSQL, Kafka, Redis o el proveedor de IA implica tocar solo un adaptador.
- (−) Hay duplicación pequeña y consciente (filtro de API key, enum de niveles, formato de error).
- (−) Más clases que un CRUD por capas; se compensa con nombres explícitos y un caso de uso por clase.
