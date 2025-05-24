# Stage 1: Build
FROM gradle:8.10-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle buildFatJar --no-daemon

# Stage 2: Run
FROM openjdk:17-jdk-slim
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/ktor-sample-all.jar /app/ktor-sample.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/ktor-sample.jar"]