#!/bin/bash
# ============================================================================
# Humaniza RJ — Backup do banco (Supabase Postgres)
#
# Faz pg_dump (formato custom, comprimido), guarda uma cópia local na VM e
# envia para um bucket privado do Supabase Storage. Rotaciona ambos por data.
#
# Lê as credenciais do .env na raiz do projeto (mesmo arquivo do backend):
#   DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD  (obrigatórios)
#   SUPABASE_URL, SUPABASE_SERVICE_KEY                  (para upload remoto)
#
# --- Pré-requisitos na Lightsail (uma vez) ---------------------------------
#   # cliente Postgres 15 (versão do servidor no Supabase):
#   sudo sh -c 'echo "deb https://apt.postgresql.org/pub/repos/apt jammy-pgdg main" \
#     > /etc/apt/sources.list.d/pgdg.list'
#   curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc \
#     | sudo gpg --dearmor -o /etc/apt/trusted.gpg.d/postgresql.gpg
#   sudo apt update && sudo apt install -y postgresql-client-15
#
#   # criar bucket PRIVADO "backups" no painel do Supabase (Storage → New bucket)
#
# --- Agendar (cron diário às 03h, antes dos schedulers do app) -------------
#   crontab -e
#   0 3 * * * cd /opt/humaniza/patient-service && ./deploy/backup-db.sh \
#     >> /opt/humaniza/backups/backup.log 2>&1
#
# --- Restaurar (exemplo) ----------------------------------------------------
#   PGSSLMODE=require PGPASSWORD="<senha>" pg_restore \
#     --host=db.<ref>.supabase.co --port=5432 --username=postgres \
#     --dbname=postgres --clean --if-exists --no-owner --no-privileges \
#     humaniza-2026-06-01.dump
# ============================================================================

set -euo pipefail

# --- Configuração (pode sobrescrever via variáveis de ambiente) ------------
BACKUP_DIR="${BACKUP_DIR:-/opt/humaniza/backups}"
BACKUP_BUCKET="${BACKUP_BUCKET:-backups}"
KEEP_DAYS="${KEEP_DAYS:-14}"          # janela de retenção (local e remoto)
REMOTE_PREFIX="${REMOTE_PREFIX:-db}"  # "pasta" dentro do bucket

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env}"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }

# --- Lê uma variável do .env sem executar o arquivo (seguro) ---------------
get_env() {
  local val
  val="$(grep -E "^$1=" "$ENV_FILE" | head -1 | cut -d= -f2- || true)"
  val="${val%\"}"; val="${val#\"}"
  val="${val%\'}"; val="${val#\'}"
  printf '%s' "$val"
}

[ -f "$ENV_FILE" ] || { log "ERRO: .env não encontrado em $ENV_FILE"; exit 1; }

DATABASE_URL="$(get_env DATABASE_URL)"
DB_USER="$(get_env DATABASE_USERNAME)"
DB_PASS="$(get_env DATABASE_PASSWORD)"
SUPABASE_URL="$(get_env SUPABASE_URL)"
SUPABASE_SERVICE_KEY="$(get_env SUPABASE_SERVICE_KEY)"

[ -n "$DATABASE_URL" ] || { log "ERRO: DATABASE_URL vazio no .env"; exit 1; }
[ -n "$DB_PASS" ]      || { log "ERRO: DATABASE_PASSWORD vazio no .env"; exit 1; }

# --- Parse do JDBC URL: jdbc:postgresql://HOST:PORT/DB[?params] -------------
URL="${DATABASE_URL#jdbc:postgresql://}"
HOSTPORT="${URL%%/*}"
DB_NAME="${URL#*/}"; DB_NAME="${DB_NAME%%\?*}"
DB_HOST="${HOSTPORT%%:*}"
DB_PORT="${HOSTPORT##*:}"
[ "$DB_PORT" = "$DB_HOST" ] && DB_PORT=5432
DB_NAME="${DB_NAME:-postgres}"
DB_USER="${DB_USER:-postgres}"

mkdir -p "$BACKUP_DIR"
STAMP="$(date +%F)"                     # YYYY-MM-DD
FILE="humaniza-$STAMP.dump"
LOCAL_PATH="$BACKUP_DIR/$FILE"

# --- Dump (formato custom = comprimido; SSL exigido pelo Supabase) ---------
log "Iniciando pg_dump de $DB_HOST:$DB_PORT/$DB_NAME ..."
PGPASSWORD="$DB_PASS" PGSSLMODE=require pg_dump \
  --host="$DB_HOST" --port="$DB_PORT" --username="$DB_USER" \
  --dbname="$DB_NAME" \
  --no-owner --no-privileges --format=custom \
  --file="$LOCAL_PATH"

SIZE="$(du -h "$LOCAL_PATH" | cut -f1)"
log "Dump local OK: $LOCAL_PATH ($SIZE)"

# --- Upload para o Supabase Storage (best-effort) --------------------------
if [ -n "$SUPABASE_URL" ] && [ -n "$SUPABASE_SERVICE_KEY" ]; then
  REMOTE="$REMOTE_PREFIX/$FILE"
  log "Enviando para Supabase Storage: $BACKUP_BUCKET/$REMOTE ..."
  if curl -sf -X POST \
      "$SUPABASE_URL/storage/v1/object/$BACKUP_BUCKET/$REMOTE" \
      -H "Authorization: Bearer $SUPABASE_SERVICE_KEY" \
      -H "apikey: $SUPABASE_SERVICE_KEY" \
      -H "Content-Type: application/octet-stream" \
      -H "x-upsert: true" \
      --data-binary "@$LOCAL_PATH" > /dev/null; then
    log "Upload OK."
  else
    log "AVISO: upload para o Supabase Storage falhou (cópia local preservada)."
  fi

  # Remove o backup remoto que saiu da janela (1 por dia → janela estável).
  OLD_DATE="$(date -d "-${KEEP_DAYS} day" +%F 2>/dev/null || true)"
  if [ -n "$OLD_DATE" ]; then
    OLD_REMOTE="$REMOTE_PREFIX/humaniza-$OLD_DATE.dump"
    curl -sf -X DELETE \
      "$SUPABASE_URL/storage/v1/object/$BACKUP_BUCKET/$OLD_REMOTE" \
      -H "Authorization: Bearer $SUPABASE_SERVICE_KEY" \
      -H "apikey: $SUPABASE_SERVICE_KEY" > /dev/null 2>&1 || true
  fi
else
  log "AVISO: SUPABASE_URL/SERVICE_KEY ausentes — backup só local."
fi

# --- Rotação local ---------------------------------------------------------
find "$BACKUP_DIR" -name 'humaniza-*.dump' -type f -mtime "+$KEEP_DAYS" -delete
log "Rotação concluída (mantendo ~$KEEP_DAYS dias). Backup finalizado."
