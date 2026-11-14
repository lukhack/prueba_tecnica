# ADR-002 — RAG con Spring AI y vector store en memoria

**Estado:** aceptada · **Fecha:** 2026-11-14

## Contexto

El asistente debe responder en lenguaje natural **solo** con contenido del corpus oficial (RN-05), citando la
fuente. El corpus es pequeño (unas decenas de secciones), se versiona con el código y cambia con despliegues,
no en caliente. Se puede elegir Spring AI o LangChain4j y cualquier vector store.

## Decisión

- **Spring AI 1.1** con el starter OpenAI: cubre chat y embeddings con una sola configuración y es compatible
  con cualquier proveedor que exponga la API de OpenAI (OpenAI, Azure OpenAI, Groq, **Ollama local**). El
  proveedor y la clave se inyectan por variables de entorno (`LLM_BASE_URL`, `LLM_API_KEY`, `LLM_CHAT_MODEL`,
  `LLM_EMBEDDING_MODEL`).
- **Vector store en memoria (`SimpleVectorStore`)**, indexado al arrancar desde `classpath:protocolos/*.md`
  (un fragmento por sección `##`). Razones:
  - El corpus cabe sobradamente en memoria y es idéntico en todas las réplicas (es parte del artefacto).
  - Cero infraestructura adicional y arranque reproducible; pgvector/Redis añadirían operación sin beneficio
    a esta escala.
  - El puerto `BuscadorProtocolos` aísla la decisión: migrar a pgvector o Azure AI Search es cambiar un adaptador.
- **Recuperación con umbral**: se recuperan top-k=4 fragmentos y se descartan los que no superen
  `asistente.rag.umbral-similitud` (0.30 por defecto, configurable porque cada modelo de embeddings tiene su escala).
  Si no queda ninguno, **no se llama al LLM** y se responde "no existe protocolo disponible".
- **Prompt estricto + salida JSON** (`respuesta`, `fuentes[]`, `sinProtocolo`) y **validación posterior**: solo
  sobreviven las citas que existen entre los fragmentos entregados; sin citas válidas la respuesta se descarta.
- La indexación corre en segundo plano con reintentos y se refleja en el probe de **readiness** (`corpus`):
  un proveedor caído al arrancar no tumba el pod ni recibe tráfico antes de tiempo.

## Consecuencias

- (+) Anti-alucinación en tres capas (umbral, decisión del modelo, verificación de citas), probada en tests.
- (+) El LLM se mockea completamente en tests (`ModelosIaFalsos`): CI sin red ni API key.
- (−) Re-indexación por réplica al arrancar (segundos con un corpus pequeño; aceptable).
- (−) El umbral debe recalibrarse si cambia el modelo de embeddings; está documentado en `.env.example`.
