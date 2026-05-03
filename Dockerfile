
FROM gradle:8.7-jdk21 AS build
WORKDIR /app
COPY zip/ .          # ← copy contents of zip/ not the root
RUN gradle build -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/app.jar app.jar
EXPOSE 8027
ENTRYPOINT ["java", "-jar", "app.jar"]
