# Build Stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn clean package -pl gateway -am -DskipTests

# Run Stage
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /build/gateway/target/*.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
