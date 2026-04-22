# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY common/pom.xml common/
COPY server/pom.xml server/

RUN mvn dependency:go-offline -B

COPY server/src server/src
COPY common/src common/src

RUN mvn clean package -pl server -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/server/target/server-1.0-SNAPSHOT.jar server.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "server.jar"]