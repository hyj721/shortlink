# Build Stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn clean package -s settings.xml -pl admin -am -Dmaven.test.skip=true

# Run Stage
FROM eclipse-temurin:17-jre

# 设置时区为中国标准时间
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app
COPY --from=build /build/admin/target/*.jar app.jar
EXPOSE 8002
ENV DATABASE_ENV=docker
ENTRYPOINT ["java", "-jar", "app.jar"]
