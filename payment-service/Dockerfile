# Multi-stage build for Payment Service
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle ./
COPY gradle gradle

# Copy source code
COPY microservice-logging-starter microservice-logging-starter
COPY payment-service payment-service

# Build the application
RUN gradle :payment-service:bootJar --no-daemon

# Runtime stage
FROM openjdk:17-jre-slim

WORKDIR /app

# Create non-root user
RUN groupadd -r paymentservice && useradd -r -g paymentservice paymentservice

# Copy the built jar
COPY --from=builder /app/payment-service/build/libs/payment-service-*.jar app.jar

# Change ownership
RUN chown paymentservice:paymentservice app.jar

# Switch to non-root user
USER paymentservice

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]