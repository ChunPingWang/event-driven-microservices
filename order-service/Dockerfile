# Multi-stage build for Order Service
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle ./
COPY gradle gradle

# Copy source code
COPY microservice-logging-starter microservice-logging-starter
COPY order-service order-service

# Build the application
RUN gradle :order-service:bootJar --no-daemon

# Runtime stage
FROM openjdk:17-jre-slim

WORKDIR /app

# Create non-root user
RUN groupadd -r orderservice && useradd -r -g orderservice orderservice

# Copy the built jar
COPY --from=builder /app/order-service/build/libs/order-service-*.jar app.jar

# Change ownership
RUN chown orderservice:orderservice app.jar

# Switch to non-root user
USER orderservice

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]