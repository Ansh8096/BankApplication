# -------------------------------
#       BUILD STAGE
# -------------------------------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom first (for caching dependencies)
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Copy source code
COPY src ./src

# Build jar
RUN mvn clean package -DskipTests

# -------------------------------
#       RUN STAGE
# -------------------------------
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy Kafka SSL certificate into container
COPY src/main/resources/kafka/ca.pem /app/ca.pem

# Render provides PORT dynamically
ENV PORT=8080

# Expose port
EXPOSE 8080

# Run app
ENTRYPOINT ["java","-jar","app.jar"]
