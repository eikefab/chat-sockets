FROM amazoncorretto:17-alpine AS builder

WORKDIR /app

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/

RUN ./gradlew --no-daemon dependencies

COPY src/ src/

RUN ./gradlew jar --no-daemon -q

FROM amazoncorretto:17-alpine

RUN addgroup -S chat && adduser -S chat -G chat

WORKDIR /app

ENV APP_MODE=server \
    APP_HOST=0.0.0.0 \
    APP_PORT=8080 \
    APP_MAX_CLIENTS=50 \
    APP_LOG_LEVEL=INFO

COPY --from=builder /app/build/libs/chat-sockets-*.jar app.jar
COPY entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh && \
    chown -R chat:chat /app /entrypoint.sh

USER chat

ENTRYPOINT ["/entrypoint.sh"]
