FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY gateway-server/pom.xml gateway-server/
COPY mcp-server/pom.xml mcp-server/
COPY mcp-client/pom.xml mcp-client/
RUN mvn dependency:go-offline -B
COPY gateway-server/src gateway-server/src
RUN mvn clean package -pl gateway-server -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/gateway-server/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]

