#!/bin/bash

# Database Management Script
set -e

COMMAND=${1:-help}
SERVICE=${2:-all}
ENVIRONMENT=${3:-dev}

# Database connection parameters
ORDER_DB_HOST=${ORDER_DB_HOST:-localhost}
ORDER_DB_PORT=${ORDER_DB_PORT:-5432}
ORDER_DB_NAME=${ORDER_DB_NAME:-orderdb}
ORDER_DB_USER=${ORDER_DB_USER:-orderuser}
ORDER_DB_PASSWORD=${ORDER_DB_PASSWORD:-orderpass}

PAYMENT_DB_HOST=${PAYMENT_DB_HOST:-localhost}
PAYMENT_DB_PORT=${PAYMENT_DB_PORT:-5433}
PAYMENT_DB_NAME=${PAYMENT_DB_NAME:-paymentdb}
PAYMENT_DB_USER=${PAYMENT_DB_USER:-paymentuser}
PAYMENT_DB_PASSWORD=${PAYMENT_DB_PASSWORD:-paymentpass}

function show_help() {
    echo "Database Management Script"
    echo ""
    echo "Usage: $0 <command> [service] [environment]"
    echo ""
    echo "Commands:"
    echo "  migrate     - Run database migrations"
    echo "  clean       - Clean database (development only)"
    echo "  seed        - Seed database with sample data"
    echo "  status      - Show migration status"
    echo "  validate    - Validate migrations"
    echo "  repair      - Repair migration metadata"
    echo "  backup      - Create database backup"
    echo "  restore     - Restore database from backup"
    echo ""
    echo "Services:"
    echo "  order       - Order service database only"
    echo "  payment     - Payment service database only"
    echo "  all         - Both databases (default)"
    echo ""
    echo "Environments:"
    echo "  dev         - Development (default)"
    echo "  docker      - Docker environment"
    echo "  sit         - System Integration Test"
    echo "  prod        - Production"
}

function run_flyway_command() {
    local service=$1
    local flyway_command=$2
    local db_host=$3
    local db_port=$4
    local db_name=$5
    local db_user=$6
    local db_password=$7
    
    echo "Running Flyway $flyway_command for $service service..."
    
    # Build the service to ensure migrations are available
    ./gradlew :${service}-service:build -x test
    
    # Run Flyway command
    ./gradlew :${service}-service:flywayMigrate \
        -Pflyway.url="jdbc:postgresql://${db_host}:${db_port}/${db_name}" \
        -Pflyway.user="${db_user}" \
        -Pflyway.password="${db_password}" \
        -Pflyway.locations="classpath:db/migration"
}

function migrate_databases() {
    echo "Running database migrations for environment: $ENVIRONMENT"
    
    if [[ "$SERVICE" == "all" || "$SERVICE" == "order" ]]; then
        run_flyway_command "order" "migrate" "$ORDER_DB_HOST" "$ORDER_DB_PORT" "$ORDER_DB_NAME" "$ORDER_DB_USER" "$ORDER_DB_PASSWORD"
    fi
    
    if [[ "$SERVICE" == "all" || "$SERVICE" == "payment" ]]; then
        run_flyway_command "payment" "migrate" "$PAYMENT_DB_HOST" "$PAYMENT_DB_PORT" "$PAYMENT_DB_NAME" "$PAYMENT_DB_USER" "$PAYMENT_DB_PASSWORD"
    fi
    
    echo "Database migrations completed successfully!"
}

function clean_databases() {
    if [[ "$ENVIRONMENT" == "prod" ]]; then
        echo "ERROR: Clean operation is not allowed in production environment!"
        exit 1
    fi
    
    echo "WARNING: This will delete all data in the databases!"
    read -p "Are you sure you want to continue? (y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Cleaning databases..."
        
        if [[ "$SERVICE" == "all" || "$SERVICE" == "order" ]]; then
            PGPASSWORD=$ORDER_DB_PASSWORD psql -h $ORDER_DB_HOST -p $ORDER_DB_PORT -U $ORDER_DB_USER -d $ORDER_DB_NAME -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
        fi
        
        if [[ "$SERVICE" == "all" || "$SERVICE" == "payment" ]]; then
            PGPASSWORD=$PAYMENT_DB_PASSWORD psql -h $PAYMENT_DB_HOST -p $PAYMENT_DB_PORT -U $PAYMENT_DB_USER -d $PAYMENT_DB_NAME -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
        fi
        
        echo "Databases cleaned successfully!"
        echo "Run 'migrate' command to recreate the schema."
    else
        echo "Clean operation cancelled."
    fi
}

function seed_databases() {
    if [[ "$ENVIRONMENT" == "prod" ]]; then
        echo "ERROR: Seed operation is not allowed in production environment!"
        exit 1
    fi
    
    echo "Seeding databases with sample data..."
    
    if [[ "$SERVICE" == "all" || "$SERVICE" == "order" ]]; then
        PGPASSWORD=$ORDER_DB_PASSWORD psql -h $ORDER_DB_HOST -p $ORDER_DB_PORT -U $ORDER_DB_USER -d $ORDER_DB_NAME -f scripts/init-dev-data.sql
    fi
    
    if [[ "$SERVICE" == "all" || "$SERVICE" == "payment" ]]; then
        PGPASSWORD=$PAYMENT_DB_PASSWORD psql -h $PAYMENT_DB_HOST -p $PAYMENT_DB_PORT -U $PAYMENT_DB_USER -d $PAYMENT_DB_NAME -f scripts/init-dev-data.sql
    fi
    
    echo "Database seeding completed successfully!"
}

function backup_databases() {
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_dir="backups/${ENVIRONMENT}_${timestamp}"
    
    mkdir -p "$backup_dir"
    
    echo "Creating database backups in $backup_dir..."
    
    if [[ "$SERVICE" == "all" || "$SERVICE" == "order" ]]; then
        PGPASSWORD=$ORDER_DB_PASSWORD pg_dump -h $ORDER_DB_HOST -p $ORDER_DB_PORT -U $ORDER_DB_USER -d $ORDER_DB_NAME > "$backup_dir/order_db.sql"
        echo "Order database backup created: $backup_dir/order_db.sql"
    fi
    
    if [[ "$SERVICE" == "all" || "$SERVICE" == "payment" ]]; then
        PGPASSWORD=$PAYMENT_DB_PASSWORD pg_dump -h $PAYMENT_DB_HOST -p $PAYMENT_DB_PORT -U $PAYMENT_DB_USER -d $PAYMENT_DB_NAME > "$backup_dir/payment_db.sql"
        echo "Payment database backup created: $backup_dir/payment_db.sql"
    fi
    
    echo "Database backups completed successfully!"
}

# Main command processing
case $COMMAND in
    migrate)
        migrate_databases
        ;;
    clean)
        clean_databases
        ;;
    seed)
        seed_databases
        ;;
    backup)
        backup_databases
        ;;
    help|*)
        show_help
        ;;
esac