-- =============================================================================
-- Migration: tornar profissional_id opcional em agendamentos e agendamentos_recorrentes
-- =============================================================================
-- Hibernate ddl-auto=update NAO remove restricoes NOT NULL automaticamente.
-- Este script precisa rodar 1 vez no Supabase SQL Editor:
--   https://supabase.com/dashboard/project/rzsqyjeqrizzyazkcezc/sql/new
-- =============================================================================

ALTER TABLE agendamentos
    ALTER COLUMN profissional_id DROP NOT NULL;

ALTER TABLE agendamentos_recorrentes
    ALTER COLUMN profissional_id DROP NOT NULL;

-- Verificacao
SELECT column_name, is_nullable
FROM information_schema.columns
WHERE table_name IN ('agendamentos', 'agendamentos_recorrentes')
  AND column_name = 'profissional_id';
