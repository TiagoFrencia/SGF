FROM gradle:8.8.0-jdk21 AS build
WORKDIR /workspace
COPY . .
WORKDIR /workspace/apps/api
RUN gradle :sgf-app:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/apps/api/sgf-app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

