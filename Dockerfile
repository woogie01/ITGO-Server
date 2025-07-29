FROM openjdk:17-jdk-slim
COPY ./build/libs/ITGO-0.0.1-SNAPSHOT.jar /app/itgo-backend.jar
ENTRYPOINT ["java", "-jar", "/app/itgo-backend.jar"]