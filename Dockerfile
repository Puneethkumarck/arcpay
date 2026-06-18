# syntax=docker/dockerfile:1
# Parameterized multi-stage build for any ArcPay service.
# SERVICE is the module's filesystem path, e.g. "identity/identity" (gradle path :identity:identity).
# Build a specific service:
#   docker build --build-arg SERVICE=identity/identity --build-arg PORT=8080 -t arcpay/identity .

FROM eclipse-temurin:25.0.3_9-jdk AS build
WORKDIR /workspace
COPY . .
ARG SERVICE
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon ":$(echo "$SERVICE" | tr '/' ':'):bootJar" \
 && cp "$(ls "${SERVICE}"/build/libs/*.jar | grep -v plain | head -n1)" /workspace/app.jar

FROM eclipse-temurin:25.0.3_9-jre AS runtime
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd -r -u 1001 appuser
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
ARG PORT=8080
ENV SERVER_PORT=${PORT}
EXPOSE ${PORT}
USER appuser
# Java 25 is container-aware; tune via JDK_JAVA_OPTIONS if needed.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
