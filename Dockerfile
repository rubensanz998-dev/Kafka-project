# syntax=docker/dockerfile:1
# Un único Dockerfile para los 4 servicios: docker compose pasa el módulo con el build-arg SERVICE.
FROM maven:3.9-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /src
COPY . .
# La caché de ~/.m2 evita descargar las dependencias 4 veces.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -pl ${SERVICE} -am package -DskipTests

FROM eclipse-temurin:21-jre
ARG SERVICE
WORKDIR /app
COPY --from=build /src/${SERVICE}/target/${SERVICE}.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
