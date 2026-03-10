-- =============================================================================
-- ATIVIDADES
-- =============================================================================
INSERT INTO atividades (id, nome, descricao, duracao_padrao, ativo, created_at, updated_at) VALUES
('a1000000-0000-0000-0000-000000000001', 'Pilates Clássico', 'Método clássico de Pilates com foco em fortalecimento e flexibilidade', 60, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a1000000-0000-0000-0000-000000000002', 'Pilates Funcional', 'Pilates com exercícios funcionais integrados', 60, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a1000000-0000-0000-0000-000000000003', 'Fisioterapia Pélvica', 'Tratamento especializado em disfunções do assoalho pélvico', 60, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a1000000-0000-0000-0000-000000000004', 'Fisioterapia Traumato', 'Tratamento e reabilitação de lesões traumato-ortopédicas', 60, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a1000000-0000-0000-0000-000000000005', 'Drenagem', 'Drenagem linfática para estimular o sistema linfático', 60, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a1000000-0000-0000-0000-000000000006', 'Obstetrícia', 'Acompanhamento fisioterapêutico obstétrico', 60, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a1000000-0000-0000-0000-000000000007', 'Massagem', 'Massoterapia para alívio de dores e tensões', 60, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a1000000-0000-0000-0000-000000000008', 'Pacote Pilates, Fisio e Drenagem', 'Combo de Pilates, Fisioterapia e Drenagem', 60, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- PLANOS
-- =============================================================================
INSERT INTO planos (id, nome, descricao, tipo_plano, validade_dias, sessoes_incluidas, permite_transferencia, ativo, created_at, updated_at) VALUES
('b1000000-0000-0000-0000-000000000001', 'Experimental', 'Aula teste', 'experimental', 7, 1, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000002', 'Avulso', 'Pagamento por sessão', 'avulso', 30, 1, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000003', 'Mensalidade', 'Sessões fixas por mês', 'mensal', 30, NULL, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000004', 'Trimestral', 'Sessões fixas por trimestre', 'trimestral', 90, NULL, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000005', 'Semestral', 'Sessões fixas por semestre', 'semestral', 180, NULL, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000006', 'Avaliação', 'Primeira consulta', 'avaliacao', 30, 1, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000007', 'Sessão', 'Sessão avulsa', 'sessao', 30, 1, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000008', 'Pacote', 'Plano com desconto para sessões', 'pacote', 90, NULL, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('b1000000-0000-0000-0000-000000000009', 'Pacote Combo', 'Plano 4 meses', 'pacote_combo', 120, NULL, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- SERVIÇOS — Pilates Clássico (atividade 1)
-- =============================================================================
INSERT INTO servicos (id, atividade_id, plano_id, tipo_atendimento, quantidade, unidade_servico, modalidade_local, valor, ativo, created_at, updated_at) VALUES
-- Experimental e Avulso
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001', 'Coletivo', 1, 'aula experimental', 'Clínica', 55.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000002', 'Coletivo', 1, 'aula avulsa', 'Clínica', 60.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Mensalidade 1x, 2x, 3x/semana
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000003', 'Coletivo', 1, 'frequência/semana', 'Clínica', 225.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000003', 'Coletivo', 2, 'frequência/semana', 'Clínica', 302.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000003', 'Coletivo', 3, 'frequência/semana', 'Clínica', 384.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Trimestral 1x, 2x, 3x/semana
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000004', 'Coletivo', 1, 'frequência/semana', 'Clínica', 630.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000004', 'Coletivo', 2, 'frequência/semana', 'Clínica', 845.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000004', 'Coletivo', 3, 'frequência/semana', 'Clínica', 1075.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Semestral 1x, 2x, 3x/semana
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000005', 'Coletivo', 1, 'frequência/semana', 'Clínica', 1305.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000005', 'Coletivo', 2, 'frequência/semana', 'Clínica', 1750.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000005', 'Coletivo', 3, 'frequência/semana', 'Clínica', 2227.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Individual
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001', 'Individual', 2, 'frequência/semana', 'Clínica', 604.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- SERVIÇOS — Pilates Funcional (atividade 2)
-- =============================================================================
INSERT INTO servicos (id, atividade_id, plano_id, tipo_atendimento, quantidade, unidade_servico, modalidade_local, valor, ativo, created_at, updated_at) VALUES
-- Experimental e Avulso
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000001', 'Coletivo', 1, 'aula experimental', 'Clínica', 55.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000002', 'Coletivo', 1, 'aula avulsa', 'Clínica', 60.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Mensalidade 1x, 2x, 3x/semana
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000003', 'Coletivo', 1, 'frequência/semana', 'Clínica', 225.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000003', 'Coletivo', 2, 'frequência/semana', 'Clínica', 302.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000003', 'Coletivo', 3, 'frequência/semana', 'Clínica', 384.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Trimestral 1x, 2x, 3x/semana
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000004', 'Coletivo', 1, 'frequência/semana', 'Clínica', 630.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000004', 'Coletivo', 2, 'frequência/semana', 'Clínica', 845.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000004', 'Coletivo', 3, 'frequência/semana', 'Clínica', 1075.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Semestral 1x, 2x, 3x/semana
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000005', 'Coletivo', 1, 'frequência/semana', 'Clínica', 1305.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000005', 'Coletivo', 2, 'frequência/semana', 'Clínica', 1750.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000005', 'Coletivo', 3, 'frequência/semana', 'Clínica', 2227.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Individual
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000001', 'Individual', 2, 'frequência/semana', 'Clínica', 604.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- SERVIÇOS — Fisioterapia Pélvica (atividade 3)
-- =============================================================================
INSERT INTO servicos (id, atividade_id, plano_id, tipo_atendimento, quantidade, unidade_servico, modalidade_local, valor, ativo, created_at, updated_at) VALUES
-- Clínica
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000006', 'Individual', 1, 'avaliação', 'Clínica', 330.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000007', 'Individual', 1, 'sessão', 'Clínica', 264.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000008', 'Individual', 5, 'sessão', 'Clínica', 1100.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000008', 'Individual', 10, 'sessão', 'Clínica', 1980.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Domiciliar
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000007', 'Individual', 1, 'sessão', 'Domiciliar', 550.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000008', 'Individual', 5, 'sessão', 'Domiciliar', 2630.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000008', 'Individual', 10, 'sessão', 'Domiciliar', 5000.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- SERVIÇOS — Fisioterapia Traumato (atividade 4)
-- =============================================================================
INSERT INTO servicos (id, atividade_id, plano_id, tipo_atendimento, quantidade, unidade_servico, modalidade_local, valor, ativo, created_at, updated_at) VALUES
-- Clínica
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000006', 'Individual', 1, 'avaliação', 'Clínica', 198.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000007', 'Individual', 1, 'sessão', 'Clínica', 154.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000008', 'Individual', 5, 'sessão', 'Clínica', 660.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000008', 'Individual', 10, 'sessão', 'Clínica', 1100.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Domiciliar
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000007', 'Individual', 1, 'sessão', 'Domiciliar', 220.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000008', 'Individual', 5, 'sessão', 'Domiciliar', 880.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000008', 'Individual', 10, 'sessão', 'Domiciliar', 1540.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- SERVIÇOS — Drenagem (atividade 5)
-- =============================================================================
INSERT INTO servicos (id, atividade_id, plano_id, tipo_atendimento, quantidade, unidade_servico, modalidade_local, valor, ativo, created_at, updated_at) VALUES
-- Clínica
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000007', 'Individual', 1, 'sessão', 'Clínica', 154.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000008', 'Individual', 5, 'sessão', 'Clínica', 660.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000008', 'Individual', 10, 'sessão', 'Clínica', 1100.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Domiciliar
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000007', 'Individual', 1, 'sessão', 'Domiciliar', 220.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000008', 'Individual', 5, 'sessão', 'Domiciliar', 880.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000008', 'Individual', 10, 'sessão', 'Domiciliar', 1540.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- SERVIÇOS — Obstetrícia (atividade 6)
-- =============================================================================
INSERT INTO servicos (id, atividade_id, plano_id, tipo_atendimento, quantidade, unidade_servico, modalidade_local, valor, ativo, created_at, updated_at) VALUES
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000006', 'Individual', 1, 'avaliação', 'Clínica', 330.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000007', 'Individual', 1, 'sessão', 'Clínica', 264.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000008', 'Individual', 5, 'sessão', 'Clínica', 1100.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000008', 'Individual', 10, 'sessão', 'Clínica', 1980.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- SERVIÇOS — Massagem (atividade 7)
-- =============================================================================
INSERT INTO servicos (id, atividade_id, plano_id, tipo_atendimento, quantidade, unidade_servico, modalidade_local, valor, ativo, created_at, updated_at) VALUES
-- Clínica
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000007', 'Individual', 1, 'sessão', 'Clínica', 154.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000008', 'Individual', 5, 'sessão', 'Clínica', 660.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000008', 'Individual', 10, 'sessão', 'Clínica', 1100.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Domiciliar
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000007', 'Individual', 1, 'sessão', 'Domiciliar', 220.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000008', 'Individual', 5, 'sessão', 'Domiciliar', 880.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000008', 'Individual', 10, 'sessão', 'Domiciliar', 1540.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- SERVIÇOS — Pacote Pilates, Fisio e Drenagem (atividade 8)
-- =============================================================================
INSERT INTO servicos (id, atividade_id, plano_id, tipo_atendimento, quantidade, unidade_servico, modalidade_local, valor, ativo, created_at, updated_at) VALUES
(RANDOM_UUID(), 'a1000000-0000-0000-0000-000000000008', 'b1000000-0000-0000-0000-000000000009', 'Individual', 10, 'sessão', 'Clínica', 4000.00, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
