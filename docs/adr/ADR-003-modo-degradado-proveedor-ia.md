# ADR-003 — Resiliencia y modo degradado ante el proveedor de IA

**Estado:** aceptada · **Fecha:** 2026-11-14

## Contexto

El proveedor de LLM es una dependencia externa, lenta y con fallos ocasionales. El requisito técnico pide
circuit breaker con fallback explícito (503 con mensaje claro) y que el triage nunca dependa del asistente.
Además, las APIs de Spring AI son bloqueantes en su mayoría y no pueden ejecutarse en el event loop.

## Decisión

- **Un único circuit breaker `proveedorIa`** (Resilience4j) protege las dos llamadas al proveedor:
  embeddings de la pregunta y generación. Ventana de 10 llamadas, abre al 50 % de fallos, 30 s en abierto,
  2 llamadas de prueba en semiabierto. Se usa el **operador reactivo** (`CircuitBreakerOperator`) en lugar de
  anotaciones AOP: explícito, sin proxies y visible en la cadena.
- **Timeout** configurable (`asistente.rag.timeout-llm`, 20 s) sobre la generación.
- **Fallback explícito**: cualquier error (timeout, HTTP 5xx, `CallNotPermittedException`, corpus sin indexar)
  se traduce a `ExcepcionProveedorIaNoDisponible` → **HTTP 503** con mensaje legible. No se devuelve una
  "respuesta inventada" en modo degradado: en un contexto clínico es preferible fallar de forma honesta.
- **Sin bloqueo del event loop**:
  - Generación: se usa `ChatModel.stream(...)` (WebClient por debajo) y se concatenan los trozos → 100 % reactivo.
  - Embeddings / búsqueda vectorial: APIs bloqueantes envueltas en `Mono.fromCallable(...).subscribeOn(boundedElastic())`.
- **Independencia del triage**: el servicio de Triage no conoce al asistente; la única relación es Kafka,
  asíncrona y de un solo sentido. Si el asistente cae, el triage sigue registrando y escalando.
- Métricas: `asistente.llm.latencia` (timer con p50/p95), `asistente.consultas{resultado}` y el health
  indicator del circuit breaker (`register-health-indicator: true`).

## Consecuencias

- (+) Con el proveedor caído el servicio responde en milisegundos (circuito abierto) con 503 y no acumula hilos.
- (+) `GET /asistente/estado-urgencias` no pasa por el LLM: sigue funcionando en modo degradado.
- (−) El cliente debe manejar 503 y reintentar; se documenta en el README.
