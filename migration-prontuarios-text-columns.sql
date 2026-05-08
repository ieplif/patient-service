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

-- Garantir DEFAULT true em "ativo" — sem isso o INSERT do Hibernate falha
-- quando a entidade Java nao envia o campo
ALTER TABLE prontuarios
    ALTER COLUMN ativo SET DEFAULT true;

-- Atualiza linhas que possam ter ficado com NULL (improvavel mas seguro)
UPDATE prontuarios SET ativo = true WHERE ativo IS NULL;

-- Verificacao
SELECT column_name, data_type, character_maximum_length, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'prontuarios'
  AND column_name IN ('storage_url', 'storage_path', 'nome_arquivo', 'ativo')
ORDER BY column_name;
