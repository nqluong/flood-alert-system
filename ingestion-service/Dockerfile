FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY pom.xml .

COPY common-service/pom.xml common-service/
COPY api-gateway/pom.xml api-gateway/
COPY notification-service/pom.xml notification-service/
COPY floodcore-service/pom.xml floodcore-service/
COPY floodprocessor-service/pom.xml floodprocessor-service/
COPY auth-service/pom.xml auth-service/

COPY ingestion-service/pom.xml ingestion-service/
COPY ingestion-service/src ingestion-service/src

RUN mvn clean package -pl ingestion-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/ingestion-service/target/*.jar app.jar

# Expose the port
EXPOSE 8082

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
