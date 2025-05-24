#!/bin/bash

# Exit on any error
set -e

echo "Building Maven project..."
./mvnw clean package -DskipTests

echo "Stopping and removing existing container if running..."
docker stop leastcount-backend || true
docker rm leastcount-backend || true

echo "Removing old Docker image if exists..."
docker rmi leastcount-backend-image || true

echo "Building Docker image..."
docker build -t leastcount-backend-image .

echo "Starting new container..."
docker run -d \
    --name leastcount-backend \
    -p 8080:8080 \
    leastcount-backend-image

echo "Done! Container is running..."