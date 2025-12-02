FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY mcp-server/pom.xml ./mcp-server/
COPY mcp-server/src ./mcp-server/src
RUN mvn clean package -DskipTests -f pom.xml -pl mcp-server -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/mcp-server/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

