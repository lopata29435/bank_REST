# Stage 1: сборка
FROM openjdk:21-slim AS build

RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn dependency:go-offline -B
RUN mvn clean package -DskipTests

# Stage 2: runtime
FROM openjdk:21-slim

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
WORKDIR /app

RUN mkdir -p /app/logs && chown -R appuser:appgroup /app/logs

# Копируем собранный jar
COPY --from=build /app/target/*.jar app.jar
RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080
ENV JAVA_OPTS="-Xmx512m -Xms256m"

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]