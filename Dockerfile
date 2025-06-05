# Use distroless OpenJDK image
FROM gcr.io/distroless/java-base-debian12:nonroot

# Set the working directory inside the container
WORKDIR /app

# Copy the jar file into the container
COPY target/*.jar app.jar

# Switch to nonroot user
USER nonroot

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
