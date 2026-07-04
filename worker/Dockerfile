# Estágio 1: build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :worker:bootJar --no-daemon

# Estágio 2: runtime (inclui ffmpeg)
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache ffmpeg
WORKDIR /app
COPY --from=build /workspace/worker/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
