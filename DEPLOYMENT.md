# Deployment Guide

This guide covers the deployment and configuration of the Event-Driven Microservices system.

## Prerequisites

- Docker and Docker Compose
- Java 17
- Gradle 8.5+
- PostgreSQL 15+ (for production)
- RabbitMQ 3.12+ (for production)

## Quick Start (Development)

1. **Clone and build the project:**
   ```bash
   git clone <repository-url>
   cd event-driven-microservices
   ./gradlew build
   ```

2. **Start the development environment:**
   ```bash
   docker-compose up -d
   ```

3. **Verify services are running:**
   ```bash
   # Check service health
   curl http://localhost:8080/actuator/health
   curl http://localhost:8081/actuator/health
   
   # Check RabbitMQ management UI
   open http://localhost:15672 (guest/guest)
   
   # Check Grafana dashboard
   open http://localhost:3000 (admin/admin)
   ```

## Environment Configurations

### Development Environment

- **Profile:** `dev`
- **Database:** H2 in-memory
- **Message Broker:** Embedded or Docker RabbitMQ
- **Logging:** Console + File
- **Monitoring:** Basic Actuator endpoints

```bash
# Run services locally
./gradlew :order-service:bootRun --args='--spring.profiles.active=dev'
./gradlew :payment-service:bootRun --args='--spring.profiles.active=dev'
```

### Docker Environment

- **Profile:** `docker`
- **Database:** PostgreSQL containers
- **Message Broker:** RabbitMQ container
- **Logging:** Structured logging
- **Monitoring:** Prometheus + Grafana

```bash
# Start all services with Docker Compose
docker-compose up -d

# View logs
docker-compose logs -f order-service
docker-compose logs -f payment-service

# Scale services
docker-compose up -d --scale order-service=2 --scale payment-service=2
```

### Production Environment

- **Profile:** `prod`
- **Database:** External PostgreSQL
- **Message Broker:** External RabbitMQ cluster
- **Logging:** JSON structured logging
- **Monitoring:** Full observability stack

```bash
# Build production images
./scripts/build-images.sh latest your-registry.com

# Deploy with production compose
docker-compose -f docker-compose.prod.yml up -d
```

## Database Management

### Migration Commands

```bash
# Run migrations for all services
./scripts/manage-db.sh migrate all dev

# Run migrations for specific service
./scripts/manage-db.sh migrate order docker

# Check migration status
./scripts/manage-db.sh status all prod

# Seed development data
./scripts/manage-db.sh seed all dev

# Create backup
./scripts/manage-db.sh backup all prod
```

### Manual Migration

```bash
# Order Service
./gradlew :order-service:flywayMigrate \
  -Pflyway.url="jdbc:postgresql://localhost:5432/orderdb" \
  -Pflyway.user="orderuser" \
  -Pflyway.password="orderpass"

# Payment Service
./gradlew :payment-service:flywayMigrate \
  -Pflyway.url="jdbc:postgresql://localhost:5433/paymentdb" \
  -Pflyway.user="paymentuser" \
  -Pflyway.password="paymentpass"
```

## Monitoring and Observability

### Health Checks

- **Order Service:** http://localhost:8080/actuator/health
- **Payment Service:** http://localhost:8081/actuator/health

### Metrics

- **Prometheus:** http://localhost:9090
- **Grafana:** http://localhost:3000
- **Order Service Metrics:** http://localhost:8080/actuator/prometheus
- **Payment Service Metrics:** http://localhost:8081/actuator/prometheus

### Logging

#### Log Locations
- **Development:** `logs/order-service.log`, `logs/payment-service.log`
- **Docker:** Container logs via `docker-compose logs`
- **Production:** JSON logs for centralized logging systems

#### Log Levels
```yaml
# application.yml
logging:
  level:
    com.example.orderservice: INFO
    com.example.paymentservice: INFO
    org.springframework.amqp: INFO
    org.hibernate.SQL: WARN
```

## Configuration Management

### Environment Variables

#### Order Service
```bash
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://db-host:5432/orderdb
SPRING_DATASOURCE_USERNAME=orderuser
SPRING_DATASOURCE_PASSWORD=orderpass
SPRING_RABBITMQ_HOST=rabbitmq-host
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest
```

#### Payment Service
```bash
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://db-host:5432/paymentdb
SPRING_DATASOURCE_USERNAME=paymentuser
SPRING_DATASOURCE_PASSWORD=paymentpass
SPRING_RABBITMQ_HOST=rabbitmq-host
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest
```

### Application Properties

#### Microservice Logging Configuration
```yaml
microservice:
  logging:
    enabled: true
    log-request-payload: true
    log-response-payload: true
    log-message-payload: true
    max-payload-length: 5000
```

#### Outbox Publisher Configuration
```yaml
outbox:
  publisher:
    enabled: true
    batch-size: 100
    polling-interval: 5000
```

## Security Considerations

### Database Security
- Use strong passwords for database users
- Enable SSL/TLS for database connections in production
- Implement database connection pooling limits
- Regular security updates for PostgreSQL

### Message Broker Security
- Configure RabbitMQ authentication and authorization
- Use SSL/TLS for RabbitMQ connections in production
- Implement message encryption for sensitive data
- Regular security updates for RabbitMQ

### Application Security
- Enable Spring Security for production endpoints
- Implement API authentication and authorization
- Use HTTPS for all external communications
- Regular security updates for dependencies

## Performance Tuning

### JVM Settings
```bash
# Production JVM settings
JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### Database Connection Pool
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### RabbitMQ Settings
```yaml
spring:
  rabbitmq:
    listener:
      simple:
        concurrency: 5
        max-concurrency: 10
        prefetch: 10
```

## Troubleshooting

### Common Issues

1. **Database Connection Issues**
   ```bash
   # Check database connectivity
   pg_isready -h localhost -p 5432 -U orderuser
   
   # Check migration status
   ./scripts/manage-db.sh status all docker
   ```

2. **RabbitMQ Connection Issues**
   ```bash
   # Check RabbitMQ status
   docker-compose exec rabbitmq rabbitmq-diagnostics ping
   
   # Check queue status
   curl -u guest:guest http://localhost:15672/api/queues
   ```

3. **Service Health Issues**
   ```bash
   # Check service logs
   docker-compose logs -f order-service
   
   # Check health endpoints
   curl http://localhost:8080/actuator/health
   ```

### Log Analysis

```bash
# Search for errors in logs
grep -i error logs/order-service.log

# Monitor real-time logs
tail -f logs/order-service.log | grep -i payment

# Analyze performance metrics
curl http://localhost:8080/actuator/metrics/http.server.requests
```

## Backup and Recovery

### Database Backup
```bash
# Create backup
./scripts/manage-db.sh backup all prod

# Manual backup
pg_dump -h localhost -p 5432 -U orderuser orderdb > backup.sql
```

### Configuration Backup
- Backup all configuration files
- Version control all deployment scripts
- Document environment-specific settings

## Scaling

### Horizontal Scaling
```bash
# Scale services with Docker Compose
docker-compose up -d --scale order-service=3 --scale payment-service=2

# Scale with Docker Swarm
docker service scale microservices_order-service=3
```

### Vertical Scaling
- Increase JVM heap size
- Increase database connection pool size
- Increase RabbitMQ consumer concurrency

## Maintenance

### Regular Tasks
- Monitor disk space and log rotation
- Update dependencies and security patches
- Review and optimize database queries
- Monitor and tune JVM garbage collection
- Review and update monitoring alerts

### Health Checks
- Implement automated health checks
- Set up alerting for service failures
- Monitor key business metrics
- Regular disaster recovery testing