-- =============================================================================
-- Migration: alterar colunas da tabela prontuarios para TEXT
-- =============================================================================
-- Os campos storage_url, storage_path e nome_arquivo foram criados como
-- VARCHAR(255) (default do JPA quando nao se especifica length). A URL
-- assinada gerada pelo Supabase Storage contem um JWT longo e estoura
-- facilmente esse limite, provocando "value too long for type character
-- varying(255)" ao tentar salvar um Prontuario.
--
-- Hibernate ddl-auto=update NAO altera o tipo de colunas existentes — entao
-- precisa rodar este script 1 vez no Supabase SQL Editor:
--   https://supabase.com/dashboard/project/rzsqyjeqrizzyazkcezc/sql/new
-- =============================================================================

ALTER TABLE prontuarios
    ALTER COLUMN storage_url TYPE TEXT;

ALTER TABLE prontuarios
    ALTER COLUMN storage_path TYPE TEXT;

ALTER TABLE prontuarios
    ALTER COLUMN nome_arquivo TYPE TEXT;

-- Verificacao
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'prontuarios'
  AND column_name IN ('storage_url', 'storage_path', 'nome_arquivo')
ORDER BY column_name;
