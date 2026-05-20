FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY common-service/pom.xml common-service/
COPY auth-service/pom.xml auth-service/
COPY notification-service/pom.xml notification-service/
COPY ingestion-service/pom.xml ingestion-service/
COPY floodcore-service/pom.xml floodcore-service/
COPY floodprocessor-service/pom.xml floodprocessor-service/

RUN mvn dependency:go-offline -pl api-gateway || true

COPY api-gateway/src api-gateway/src

RUN mvn clean package -pl api-gateway -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache wget
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/api-gateway/target/*.jar app.jar
RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
