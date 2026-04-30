-- =============================================================================
-- Migration: tornar campos opcionais em agendamentos e patients
-- =============================================================================
-- Hibernate ddl-auto=update NAO remove restricoes NOT NULL automaticamente.
-- Este script precisa rodar 1 vez no Supabase SQL Editor:
--   https://supabase.com/dashboard/project/rzsqyjeqrizzyazkcezc/sql/new
-- =============================================================================

-- Profissional opcional em agendamentos
ALTER TABLE agendamentos
    ALTER COLUMN profissional_id DROP NOT NULL;

ALTER TABLE agendamentos_recorrentes
    ALTER COLUMN profissional_id DROP NOT NULL;

-- Email, CPF e data de nascimento opcionais em pacientes
-- (UNIQUE em email/cpf continua valido — PostgreSQL aceita multiplos NULLs em colunas UNIQUE)
ALTER TABLE patients
    ALTER COLUMN email DROP NOT NULL;

ALTER TABLE patients
    ALTER COLUMN cpf DROP NOT NULL;

ALTER TABLE patients
    ALTER COLUMN data_nascimento DROP NOT NULL;

-- Verificacao
SELECT table_name, column_name, is_nullable
FROM information_schema.columns
WHERE (table_name IN ('agendamentos', 'agendamentos_recorrentes')
        AND column_name = 'profissional_id')
   OR (table_name = 'patients'
        AND column_name IN ('email', 'cpf', 'data_nascimento'))
ORDER BY table_name, column_name;
