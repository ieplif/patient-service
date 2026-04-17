#!/bin/bash
# ============================================================================
# Humaniza RJ — Setup para AWS Lightsail (Ubuntu 22.04)
# Execute como root ou com sudo na instância Lightsail
# ============================================================================

set -e

echo "=== 1/5 Atualizando sistema ==="
apt-get update && apt-get upgrade -y

echo "=== 2/5 Instalando Docker ==="
apt-get install -y ca-certificates curl gnupg
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" > /etc/apt/sources.list.d/docker.list
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

echo "=== 3/5 Configurando Docker ==="
systemctl enable docker
systemctl start docker
usermod -aG docker ubuntu

echo "=== 4/5 Instalando Certbot (SSL) ==="
apt-get install -y certbot

echo "=== 5/5 Criando diretórios ==="
mkdir -p /opt/humaniza
chown ubuntu:ubuntu /opt/humaniza

echo ""
echo "============================================"
echo "  Setup concluído!"
echo "============================================"
echo ""
echo "Próximos passos:"
echo "  1. Saia e entre novamente (para grupo docker):"
echo "     exit && ssh ubuntu@<IP>"
echo ""
echo "  2. Clone o repositório:"
echo "     cd /opt/humaniza"
echo "     git clone https://github.com/ieplif/patient-service.git"
echo "     cd patient-service"
echo ""
echo "  3. Configure o .env:"
echo "     cp .env.example .env"
echo "     nano .env"
echo ""
echo "  4. Obtenha certificado SSL (substitua pelo seu domínio):"
echo "     sudo certbot certonly --standalone -d app.humanizarj.com.br"
echo ""
echo "  5. Inicie o sistema:"
echo "     docker compose up -d --build"
echo ""
echo "  6. Verifique:"
echo "     docker compose ps"
echo "     curl http://localhost:8080/api/health"
echo "============================================"
