# Imagen del Servicio de Asistente Clínico. Contexto de build: la raíz del repositorio.
#   docker build -f asistente-service/Dockerfile -t vidacruz/asistente-service .

# ---- Etapa 1: compilación (cachea dependencias por separado del código) ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY triage-service/pom.xml triage-service/
COPY asistente-service/pom.xml asistente-service/
RUN mvn -q -B -pl asistente-service -am dependency:go-offline
COPY asistente-service/src asistente-service/src
RUN mvn -q -B -pl asistente-service -am package -DskipTests

# ---- Etapa 2: imagen de ejecución mínima, usuario sin privilegios ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/asistente-service/target/asistente-service-*.jar app.jar
USER app
EXPOSE 8082
ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
