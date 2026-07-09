# ── Etapa 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Descargar dependencias primero (cache de Docker)
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Etapa 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/aetheris-backend-1.0.0.jar app.jar

# Variables de entorno requeridas (se pasan en la plataforma de despliegue)
ENV MYSQL_HOST=mysql-roberth.alwaysdata.net
ENV MYSQL_DATABASE=roberth_is
ENV MYSQL_USER=roberth
# MYSQL_PASSWORD  → configurar como secret en la plataforma
# JWT_SECRET      → configurar como secret en la plataforma

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dserver.port=${PORT:-8080}", "app.jar"]
