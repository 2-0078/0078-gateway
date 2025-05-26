FROM openjdk:17
WORKDIR /app
COPY gateway.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]