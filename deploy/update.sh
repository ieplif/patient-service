#!/bin/bash
# ============================================================================
# Humaniza RJ — Atualizar sistema em produção
# Execute na instância Lightsail: cd /opt/humaniza/patient-service && ./deploy/update.sh
# ============================================================================

set -e

echo "=== Puxando alterações ==="
git pull origin main

echo "=== Build backend (sequencial para poupar memória) ==="
docker compose -f docker-compose.yml -f docker-compose.prod.yml build backend

echo "=== Build frontend ==="
docker compose -f docker-compose.yml -f docker-compose.prod.yml build frontend

echo "=== Restart dos serviços ==="
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d

echo "=== Aguardando backend iniciar ==="
for i in $(seq 1 30); do
    if curl -sf http://localhost:8080/api/health > /dev/null 2>&1; then
        echo "Backend OK!"
        break
    fi
    echo "  Aguardando... ($i/30)"
    sleep 2
done

echo ""
echo "=== Status ==="
docker compose ps
echo ""
echo "Atualização concluída!"
