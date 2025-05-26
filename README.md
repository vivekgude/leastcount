# Least Count Backend

A Spring Boot-based backend application for Least Count Game.

<!-- A Spring Boot-based backend application that provides a robust API infrastructure with features like WebSocket support, JWT authentication, and database integration. -->

<!-- ## 🚀 Features

- Spring Boot 3.4.4
- Java 21
- WebSocket Support
- JWT Authentication
- MySQL Database Integration
- Redis Caching
- Quartz Scheduler
- Spring Security
- JPA for Database Operations -->

## 📋 Prerequisites

- Java 21 JDK
- Maven
- Docker
- MySQL
- Redis

## 🛠️ Installation

1. Clone the repository:
```bash
git clone https://github.com/vivekgude/leastcount.git
cd leastcount
```

2. Build the project:
```bash
./mvnw clean package
```

## 🐳 Docker Deployment

The project includes a Docker setup for easy deployment. Use the provided `start.sh` script to build and run the application:

```bash
./start.sh
```

This script will:
1. Build the Maven project
2. Create a Docker image
3. Run the container on port 8080

## 🔧 Configuration

The application requires the following services to be running:
- MySQL database
- Redis server

Make sure to configure the following environment variables or update the application properties:
- Database connection details
- Redis connection details
- JWT secret key

## 🏗️ Project Structure

```
leastcount/
├── src/                    # Source code
├── target/                 # Compiled output
├── pom.xml                 # Maven configuration
├── Dockerfile              # Docker configuration
├── start.sh                # Deployment script
└── mvnw                    # Maven wrapper
```

## 🔐 Security

The application uses Spring Security with JWT authentication for secure API access. Make sure to:
1. Configure proper JWT secret keys
2. Set up appropriate security roles
<!-- 3. Use HTTPS in production -->

<!-- ## 📝 License

[Add your license information here]

## 👥 Contributing

[Add contribution guidelines here]

## 📞 Support

[Add support information here]  -->