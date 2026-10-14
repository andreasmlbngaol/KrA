# syntax=docker/dockerfile:1

# ---- Build: compile the Spring Boot server into a runnable jar ----
# Base images are pinned to the "-noble" (Ubuntu) variant: the plain
# eclipse-temurin:25-jre tag has flipped between Alpine and Ubuntu, which
# breaks the compose healthcheck (Alpine has no bash, Ubuntu has no wget).
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /src

# Wrapper + build scripts first for better layer caching.
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src ./src

# The cache mount persists Gradle's downloaded dependencies and build cache
# across builds, so repeat builds skip re-downloading. bootJar also emits a
# *-plain.jar, so copy only the executable one to a fixed name.
RUN --mount=type=cache,id=gradle-server,target=/root/.gradle \
    chmod +x gradlew && ./gradlew bootJar --no-daemon && cp build/libs/*-SNAPSHOT.jar /src/app.jar

# ---- Runtime: minimal JRE that runs the jar ----
FROM eclipse-temurin:25-jre-noble AS runtime
WORKDIR /app

RUN groupadd -r appgroup && useradd -r -g appgroup appuser
COPY --from=build /src/app.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser

ENV PORT=8083
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8083
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
