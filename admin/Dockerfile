# Build Stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn clean package -pl admin -am -DskipTests

# Run Stage
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /build/admin/target/*.jar app.jar
EXPOSE 8002
ENV DATABASE_ENV=docker
ENTRYPOINT ["java", "-jar", "app.jar"]
