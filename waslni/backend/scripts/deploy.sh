#!/bin/bash
# deploy.sh — Deploy or update the Waselni backend on the production server.
#
# Usage:
#   ./scripts/deploy.sh v1.0.0           # deploy specific version
#   ./scripts/deploy.sh v1.0.0 --migrate  # deploy + run migrations
#
# This script is meant to be run ON the production server.
# CI/CD (Phase 27) will SSH into the server and run this script.

set -euo pipefail

COMPOSE_FILE="docker-compose.prod.yml"
VERSION="${1:?Usage: deploy.sh <version> [--migrate]}"
MIGRATE=false

if [ "${2:-}" = "--migrate" ]; then
    MIGRATE=true
fi

cd /opt/waselni/backend

echo "=== Waselni Deploy v${VERSION} ==="
echo "[$(date)] Starting deployment of version ${VERSION}..."

# Pull the specific versioned image
echo "[$(date)] Pulling Docker image waselni-backend:${VERSION}..."
export WASLNI_VERSION="${VERSION}"
docker compose -f $COMPOSE_FILE pull backend

# Restart backend with the new image
echo "[$(date)] Restarting backend..."
docker compose -f $COMPOSE_FILE up -d --no-deps --force-recreate backend

# Wait for backend to be healthy
echo "[$(date)] Waiting for backend health..."
sleep 10
for i in $(seq 1 30); do
    HEALTH=$(docker inspect --format='{{.State.Health.Status}}' waselni-prod-backend 2>/dev/null || echo "starting")
    if [ "$HEALTH" = "healthy" ]; then
        echo "[$(date)] Backend is healthy."
        break
    fi
    echo "  ...waiting ($i/30) status=$HEALTH"
    sleep 2
done

# Run migrations if requested
if [ "$MIGRATE" = true ]; then
    echo "[$(date)] Running database migrations..."
    docker compose -f $COMPOSE_FILE exec -T backend alembic upgrade head
    echo "[$(date)] Migrations complete."
fi

# Verify
echo "[$(date)] Verifying deployment..."
HEALTH_RESPONSE=$(docker compose -f $COMPOSE_FILE exec -T backend python -c "
import urllib.request, json
resp = urllib.request.urlopen('http://localhost:8000/health')
print(json.loads(resp.read())['status'])
" 2>/dev/null || echo "FAILED")

if [ "$HEALTH_RESPONSE" = "ok" ]; then
    echo "[$(date)] ✅ Deployment successful! Health: OK"
else
    echo "[$(date)] ❌ HEALTH CHECK FAILED: $HEALTH_RESPONSE"
    exit 1
fi

echo ""
echo "Services:"
docker compose -f $COMPOSE_FILE ps
