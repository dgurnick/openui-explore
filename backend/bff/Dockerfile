# Stage 1: build the fat JAR from source
FROM gradle:8.14-jdk21-alpine AS builder
WORKDIR /build
COPY . .
RUN gradle buildFatJar --no-daemon

# Stage 2: minimal JRE runtime image
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl

WORKDIR /app
COPY --from=builder /build/build/libs/openui-bff-all.jar app.jar

# Persistent volume for the SQLite database file
RUN mkdir -p /data
VOLUME ["/data"]

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
