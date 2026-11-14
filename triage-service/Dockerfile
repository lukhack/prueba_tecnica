# Imagen del Servicio de Triage. Contexto de build: la raíz del repositorio.
#   docker build -f triage-service/Dockerfile -t vidacruz/triage-service .

# ---- Etapa 1: compilación (cachea dependencias por separado del código) ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY triage-service/pom.xml triage-service/
COPY asistente-service/pom.xml asistente-service/
RUN mvn -q -B -pl triage-service -am dependency:go-offline
COPY triage-service/src triage-service/src
RUN mvn -q -B -pl triage-service -am package -DskipTests

# ---- Etapa 2: imagen de ejecución mínima, usuario sin privilegios ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/triage-service/target/triage-service-*.jar app.jar
USER app
EXPOSE 8081
ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
