# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY target/*.jar app.jar

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/app.jar .

# Create a non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
