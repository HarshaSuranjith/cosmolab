#!/bin/bash
# Usage: wait-for-health.sh <url> <timeout_seconds>
URL=$1
TIMEOUT=${2:-60}
ELAPSED=0

echo "Waiting for $URL (timeout: ${TIMEOUT}s)..."
until curl -sf "$URL" > /dev/null 2>&1; do
    sleep 2
    ELAPSED=$((ELAPSED + 2))
    if [ "$ELAPSED" -ge "$TIMEOUT" ]; then
        echo "ERROR: Timed out waiting for $URL after ${TIMEOUT}s"
        exit 1
    fi
    echo "  still waiting... ${ELAPSED}s elapsed"
done
echo "Health check passed: $URL"
