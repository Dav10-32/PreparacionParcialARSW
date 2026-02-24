# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/stock-facade-1.0.0.jar app.jar

ENV ALPHAVANTAGE_API_KEY=demo
ENV CORS_ORIGINS=http://localhost:3000

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
