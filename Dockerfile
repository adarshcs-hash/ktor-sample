# Build stage
FROM gradle:8.10-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle shadowJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built application
COPY --from=build /app/build/libs/ktor-sample-all.jar .

# Set default environment variables
ENV PORT=8080
EXPOSE $PORT

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:$PORT/ || exit 1

ENTRYPOINT ["java", "-jar", "ktor-sample-all.jar"]