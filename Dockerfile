# Use distroless Java image
FROM gcr.io/distroless/java:21-debian12

# Set the working directory inside the container
WORKDIR /app

# Copy the jar file into the container
COPY target/*.jar app.jar

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
