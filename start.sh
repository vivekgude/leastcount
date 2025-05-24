#!/bin/bash

# Exit on any error
set -e

echo "Building Maven project..."
./mvnw clean package -DskipTests

echo "Building Docker image..."
docker build -t leastcount-backend-image .

echo "Stopping existing container if running..."
docker stop leastcount-container || true
docker rm leastcount-container || true

echo "Starting new container..."
docker run -d \
    --name leastcount-container \
    -p 8080:8080 \
    leastcount-backend-image

echo "Done! Container is running..."