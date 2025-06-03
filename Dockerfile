FROM openjdk:21-jdk-slim
COPY build/libs/*.jar app.jar
EXPOSE 8280
ENTRYPOINT ["java", "-jar", "/app.jar"]