FROM eclipse-temurin:26-jdk AS build
WORKDIR /workspace

# Copy only what's needed to resolve dependencies first, so this layer stays
# cached across builds as long as build.gradle.kts/settings.gradle.kts don't change.
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:26-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
