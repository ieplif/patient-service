-- migration-pagamento-assinaturas.sql
-- Permite que um pagamento seja vinculado a múltiplas assinaturas.
-- Seguro rodar múltiplas vezes (idempotente).

-- 1. Cria a tabela de junção (Hibernate ddl-auto=update também faz isso,
--    mas o INSERT abaixo precisa que ela já exista antes do restart da app)
CREATE TABLE IF NOT EXISTS pagamento_assinaturas (
    pagamento_id  UUID NOT NULL,
    assinatura_id UUID NOT NULL,
    PRIMARY KEY (pagamento_id, assinatura_id),
    CONSTRAINT fk_pa_pagamento  FOREIGN KEY (pagamento_id)  REFERENCES pagamentos(id),
    CONSTRAINT fk_pa_assinatura FOREIGN KEY (assinatura_id) REFERENCES assinaturas(id)
);

-- 2. Migra dados existentes: cada pagamento que tinha assinatura_id vira uma linha na join table
INSERT INTO pagamento_assinaturas (pagamento_id, assinatura_id)
SELECT id, assinatura_id
FROM   pagamentos
WHERE  assinatura_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- 3. (Opcional) Remove a coluna legada após confirmar que a migração funcionou.
--    Hibernate ddl-auto=update nunca remove colunas, então esta coluna ficará
--    orphaned no banco — inofensivo, mas pode ser limpo manualmente:
-- ALTER TABLE pagamentos DROP COLUMN IF EXISTS assinatura_id;
