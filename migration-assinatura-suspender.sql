-- =============================================================================
-- Migration: campos de suspensao em assinaturas
-- =============================================================================
-- Hibernate ddl-auto=update adiciona colunas novas automaticamente, mas e
-- mais seguro rodar manualmente em producao + garantir que enum aceite o
-- novo valor SUSPENSO (o status e VARCHAR/ENUM string, sem CHECK na pratica
-- com EnumType.STRING + sem constraint explicita, mas confirmamos abaixo).
--
-- Rode 1 vez no Supabase SQL Editor antes do deploy:
--   https://supabase.com/dashboard/project/rzsqyjeqrizzyazkcezc/sql/new
-- =============================================================================

-- 1. Adicionar colunas de suspensao (idempotente — IF NOT EXISTS)
ALTER TABLE assinaturas
    ADD COLUMN IF NOT EXISTS data_suspensao DATE;

ALTER TABLE assinaturas
    ADD COLUMN IF NOT EXISTS motivo_suspensao TEXT;

ALTER TABLE assinaturas
    ADD COLUMN IF NOT EXISTS data_prevista_retomada DATE;

-- 2. CHECK constraint do campo status — precisa aceitar o novo valor SUSPENSO.
--    Hibernate criou um CHECK quando a tabela foi gerada e ele NAO atualiza
--    automaticamente com novos valores de enum em ddl-auto=update.
ALTER TABLE assinaturas DROP CONSTRAINT IF EXISTS assinaturas_status_check;
ALTER TABLE assinaturas ADD CONSTRAINT assinaturas_status_check
    CHECK (status IN ('ATIVO','SUSPENSO','CANCELADO','VENCIDO','FINALIZADO'));

-- Verificacao
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'assinaturas'
  AND column_name IN ('data_suspensao', 'motivo_suspensao', 'data_prevista_retomada', 'status')
ORDER BY column_name;
