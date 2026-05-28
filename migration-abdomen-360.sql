-- migration-abdomen-360.sql
-- Adiciona a atividade "Abdômen 360°" e seus 2 serviços (avulso e pacote 12 sessões).
-- Seguro rodar múltiplas vezes (idempotente via ON CONFLICT).

-- 1. Nova atividade
INSERT INTO atividades (id, nome, descricao, duracao_padrao, ativo, created_at, updated_at)
VALUES (
    'a1000000-0000-0000-0000-000000000011',
    'Abdômen 360°',
    'Protocolo de reestruturação abdominal integrado: radiofrequência, eletroestimulação, bioestimuladores e exercícios para core. 12 sessões em 3 fases progressivas (preparação, colágeno, sustentação).',
    60,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- 2. Serviço 1 — sessão avulsa (R$ 284)
--    Como servicos não tem natural-key única (id é gen_random_uuid()), usamos
--    um WHERE NOT EXISTS para tornar idempotente baseado em atividade+plano.
INSERT INTO servicos (id, atividade_id, plano_id, tipo_atendimento, quantidade, unidade_servico, modalidade_local, valor, ativo, created_at, updated_at)
SELECT
    gen_random_uuid(),
    'a1000000-0000-0000-0000-000000000011',
    'b1000000-0000-0000-0000-000000000007',  -- plano "Sessão"
    'Individual', 1, 'sessão', 'Clínica',
    284.00, true,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM servicos
    WHERE atividade_id = 'a1000000-0000-0000-0000-000000000011'
      AND plano_id = 'b1000000-0000-0000-0000-000000000007'
);

-- 3. Serviço 2 — pacote 12 sessões (R$ 2590)
INSERT INTO servicos (id, atividade_id, plano_id, tipo_atendimento, quantidade, unidade_servico, modalidade_local, valor, ativo, created_at, updated_at)
SELECT
    gen_random_uuid(),
    'a1000000-0000-0000-0000-000000000011',
    'b1000000-0000-0000-0000-000000000008',  -- plano "Pacote" (90 dias de validade)
    'Individual', 12, 'sessão', 'Clínica',
    2590.00, true,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM servicos
    WHERE atividade_id = 'a1000000-0000-0000-0000-000000000011'
      AND plano_id = 'b1000000-0000-0000-0000-000000000008'
);

-- Conferência (rode após para validar)
-- SELECT a.nome, p.nome AS plano, s.quantidade, s.valor
-- FROM servicos s
-- JOIN atividades a ON a.id = s.atividade_id
-- JOIN planos p ON p.id = s.plano_id
-- WHERE a.id = 'a1000000-0000-0000-0000-000000000011';
