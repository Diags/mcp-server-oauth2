FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY mcp-client/pom.xml ./mcp-client/
COPY mcp-client/src ./mcp-client/src
RUN mvn clean package -DskipTests -f pom.xml -pl mcp-client -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/mcp-client/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

