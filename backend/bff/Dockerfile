# Use the pre-built fat JAR produced by: ./gradlew shadowJar
# This avoids re-downloading all Gradle dependencies on every docker build.
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl

WORKDIR /app
COPY build/libs/openui-bff-all.jar app.jar

# Persistent volume for the SQLite database file
RUN mkdir -p /data
VOLUME ["/data"]

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
