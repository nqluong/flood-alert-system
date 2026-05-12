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

RUN mvn dependency:go-offline -pl auth-service -am || true

COPY common-service/src common-service/src
COPY auth-service/src auth-service/src

RUN mvn clean package -pl auth-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN apk add --no-cache wget
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/auth-service/target/*.jar app.jar
RUN chown -R spring:spring /app
USER spring:spring
EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
