# Stage 1: Build JAR
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Production image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN mkdir -p uploads && chmod 777 uploads
COPY --from=builder /app/target/freelanceflow-backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
