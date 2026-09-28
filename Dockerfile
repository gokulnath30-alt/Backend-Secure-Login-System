# Stage 1: Build the Spring Boot application using Maven and JDK 21
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy dependency definition and source code
COPY pom.xml .
COPY src ./src

# Build the executable jar without running tests to speed up deployment
RUN mvn clean package -DskipTests

# Stage 2: Create runtime container with slim JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy built jar from builder stage
COPY --from=build /app/target/*.jar app.jar

# Render assigns a dynamic port via the PORT environment variable (typically 10000)
ENV PORT=10000
EXPOSE 10000

# Start Spring Boot application listening on $PORT
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
