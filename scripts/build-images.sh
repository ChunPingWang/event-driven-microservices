#!/bin/bash

# Build Docker Images Script
set -e

VERSION=${1:-latest}
REGISTRY=${2:-""}

echo "Building microservices Docker images..."
echo "Version: $VERSION"
echo "Registry: $REGISTRY"

# Build Order Service
echo "Building Order Service..."
docker build -f order-service/Dockerfile -t order-service:$VERSION .

# Build Payment Service
echo "Building Payment Service..."
docker build -f payment-service/Dockerfile -t payment-service:$VERSION .

# Tag images for registry if provided
if [ ! -z "$REGISTRY" ]; then
    echo "Tagging images for registry: $REGISTRY"
    
    docker tag order-service:$VERSION $REGISTRY/order-service:$VERSION
    docker tag payment-service:$VERSION $REGISTRY/payment-service:$VERSION
    
    echo "Pushing images to registry..."
    docker push $REGISTRY/order-service:$VERSION
    docker push $REGISTRY/payment-service:$VERSION
fi

echo "Docker images built successfully!"
echo "Order Service: order-service:$VERSION"
echo "Payment Service: payment-service:$VERSION"

# List built images
echo ""
echo "Built images:"
docker images | grep -E "(order-service|payment-service)" | grep $VERSION