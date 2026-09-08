#!/bin/bash
# deploy.sh — Deploy or update the Waselni backend on the production server.
#
# Usage:
#   ./scripts/deploy.sh              # pull latest + restart
#   ./scripts/deploy.sh --migrate    # pull latest + run migrations + restart
#   ./scripts/deploy.sh --build      # rebuild Docker images + restart
#
# This script is meant to be run ON the production server.
# CI/CD (Phase 27) will SSH into the server and run this script.

set -euo pipefail

COMPOSE_FILE="docker-compose.prod.yml"
MIGRATE=false
BUILD=false

for arg in "$@"; do
    case $arg in
        --migrate) MIGRATE=true ;;
        --build)   BUILD=true ;;
    esac
done

cd /opt/waselni

echo "=== Waselni Deploy ==="
echo "[$(date)] Starting deployment..."

# Pull latest code
echo "[$(date)] Pulling latest code..."
git pull origin main

# Rebuild Docker images if requested
if [ "$BUILD" = true ]; then
    echo "[$(date)] Building Docker images..."
    docker compose -f $COMPOSE_FILE build --no-cache backend
fi

# Restart services
echo "[$(date)] Restarting services..."
docker compose -f $COMPOSE_FILE up -d --remove-orphans

# Wait for backend to be healthy
echo "[$(date)] Waiting for backend health..."
sleep 10
for i in $(seq 1 30); do
    if curl -fsS http://localhost:8000/health > /dev/null 2>&1; then
        echo "[$(date)] Backend is healthy."
        break
    fi
    echo "  ...waiting ($i/30)"
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
RESPONSE=$(curl -fsS http://localhost:8000/health 2>/dev/null || echo "FAILED")
echo "  Health check: $RESPONSE"

echo "[$(date)] Deployment complete!"
echo ""
echo "Services:"
docker compose -f $COMPOSE_FILE ps
