# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-21 as builder

WORKDIR /build

# Copy pom.xml and download dependencies (this layer will be cached)
COPY pom.xml .
RUN mvn dependency:resolve

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Create a non-root user for security
RUN addgroup -g 1000 appuser && adduser -D -u 1000 -G appuser appuser

# Copy the compiled JAR from the builder stage
COPY --from=builder --chown=appuser:appuser /build/target/Automated-Invoice-System-0.0.1-SNAPSHOT.jar app.jar

# Switch to non-root user
USER appuser

# Expose the port that Spring Boot runs on
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
