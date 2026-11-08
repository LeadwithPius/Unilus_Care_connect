# Build the Spring Boot application with Java 21.
# Render uses the repository root as the Docker build context.
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY backend/pom.xml ./
RUN mvn --batch-mode -DskipTests dependency:go-offline

COPY backend/src ./src
RUN mvn --batch-mode -DskipTests clean package

# Run the application on a small Java 21 runtime image.
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN useradd --system --uid 10001 --create-home appuser

COPY --from=build /workspace/target/*.jar /app/app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:InitialRAMPercentage=20.0", "-XX:MaxRAMPercentage=70.0", "-jar", "/app/app.jar"]
