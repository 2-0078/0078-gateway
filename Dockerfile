FROM openjdk:17-jdk-alpine

ARG JAR_FILE=board.jar
COPY ${JAR_FILE} app.jar

COPY .env .env
RUN export $(cat .env | xargs)

ENTRYPOINT ["java", "-jar", "/app.jar"]