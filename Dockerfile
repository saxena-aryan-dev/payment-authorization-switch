# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Cache dependencies first for faster rebuilds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Compile and run the test suite
COPY src ./src
RUN mvn -B clean package

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -ms /bin/bash appuser
USER appuser
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
