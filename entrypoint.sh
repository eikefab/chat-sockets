#!/bin/sh
set -e

MODE="${APP_MODE:-server}"
HOST="${APP_HOST:-0.0.0.0}"
PORT="${APP_PORT:-8080}"

if [ "$MODE" = "server" ]; then
  MAX_CLIENTS="${APP_MAX_CLIENTS:-50}"
  LOG_LEVEL="${APP_LOG_LEVEL:-INFO}"
  exec java -jar /app/app.jar \
    --server \
    --host "$HOST" \
    --port "$PORT" \
    --max-clients "$MAX_CLIENTS" \
    --log-level "$LOG_LEVEL"
else
  exec java -jar /app/app.jar \
    --client \
    --host "$HOST" \
    --port "$PORT"
fi
