FROM ubuntu:latest
LABEL authors="santo"

ENTRYPOINT ["top", "-b"]


FROM openjdk:22-jdk
WORKDIR /app
COPY target/Jewellerydocker.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
EXPOSE 8080