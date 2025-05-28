FROM openjdk:17-jdk-alpine

COPY build/libs/*.jar app.jar

COPY .env .env

ENTRYPOINT ["java", "-jar", "/app.jar"]