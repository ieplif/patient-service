-- =============================================================================
-- SEED TEST DATA — Dados para desenvolvimento e testes de UX
-- IMPORTANTE: Execute seed-postgres.sql ANTES deste arquivo.
-- Execute UMA VEZ no SQL Editor do Supabase.
-- É seguro re-executar (ON CONFLICT DO NOTHING).
-- =============================================================================

-- =============================================================================
-- USUÁRIOS DOS PROFISSIONAIS (ROLE_PROFISSIONAL)
-- Senha armazenada: não é necessária para testes (profissionais não fazem login).
-- Hash abaixo é BCrypt válido sintaticamente — pode ser redefinido via admin.
-- =============================================================================
INSERT INTO users (id, nome, email, senha, role) VALUES
('a2000000-0000-0000-0000-000000000001', 'Ana Paula Rodrigues', 'ana.paula@humaniza.com',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LPVrSQep7Uq', 'ROLE_PROFISSIONAL'),
('a2000000-0000-0000-0000-000000000002', 'Carlos Eduardo Souza', 'carlos.eduardo@humaniza.com',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LPVrSQep7Uq', 'ROLE_PROFISSIONAL')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- PROFISSIONAIS (2 profissionais vinculados às atividades)
-- =============================================================================
INSERT INTO profissionais (id, nome, telefone, user_id, ativo, created_at, updated_at) VALUES
('d1000000-0000-0000-0000-000000000001', 'Ana Paula Rodrigues', '21987654301', 'a2000000-0000-0000-0000-000000000001', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('d1000000-0000-0000-0000-000000000002', 'Carlos Eduardo Souza', '21987654302', 'a2000000-0000-0000-0000-000000000002', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Vincular profissionais às atividades
INSERT INTO profissional_atividades (profissional_id, atividade_id) VALUES
('d1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001'), -- Ana → Pilates Clássico
('d1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000002'), -- Ana → Pilates Funcional
('d1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000003'), -- Ana → Fisioterapia Pélvica
('d1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000006'), -- Ana → Obstetrícia
('d1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000004'), -- Carlos → Fisioterapia Traumato
('d1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000005'), -- Carlos → Drenagem
('d1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000007'), -- Carlos → Massagem
('d1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000008')  -- Carlos → Pacote Combo
ON CONFLICT DO NOTHING;

-- =============================================================================
-- PACIENTES (15 pacientes — 13 ativos, 2 inativos)
-- =============================================================================
INSERT INTO patients (id, nome_completo, email, cpf, data_nascimento, telefone, endereco, profissao, estado_civil, status_ativo, consentimento_lgpd, created_at, updated_at) VALUES
('c1000000-0000-0000-0000-000000000001', 'Beatriz Santos Ferreira',   'beatriz.santos@email.com',   '10000000001', '1988-03-12', '21987650001', 'Rua das Flores, 123 - Copacabana',         'Professora',      'Casada',     true,  true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000002', 'Fernanda Lima Oliveira',     'fernanda.lima@email.com',    '10000000002', '1992-07-24', '21987650002', 'Av. Atlântica, 500 - Copacabana',          'Advogada',        'Solteira',   true,  true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000003', 'Juliana Moreira Costa',      'juliana.moreira@email.com',  '10000000003', '1985-11-05', '21987650003', 'Rua Visconde de Pirajá, 200 - Ipanema',    'Médica',          'Divorciada', true,  true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000004', 'Larissa Pereira Alves',      'larissa.pereira@email.com',  '10000000004', '1995-02-18', '21987650004', 'Rua Prudente de Morais, 100 - Ipanema',    'Designer',        'Solteira',   true,  false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000005', 'Mariana Carvalho Nunes',     'mariana.carvalho@email.com', '10000000005', '1990-09-30', '21987650005', 'Rua Barão da Torre, 300 - Ipanema',        'Enfermeira',      'Casada',     true,  true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000006', 'Patricia Gomes Ribeiro',     'patricia.gomes@email.com',   '10000000006', '1983-06-14', '21987650006', 'Rua Farme de Amoedo, 50 - Ipanema',        'Empresária',      'Casada',     true,  true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000007', 'Rafaela Mendes Sousa',       'rafaela.mendes@email.com',   '10000000007', '1997-04-22', '21987650007', 'Av. Epitácio Pessoa, 1000 - Lagoa',        'Estudante',       'Solteira',   true,  true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000008', 'Sandra Teixeira Barbosa',    'sandra.teixeira@email.com',  '10000000008', '1978-12-08', '21987650008', 'Rua Voluntários da Pátria, 400 - Botafogo','Psicóloga',       'Viúva',      true,  true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000009', 'Tatiane Rocha Nascimento',   'tatiane.rocha@email.com',    '10000000009', '1993-08-17', '21987650009', 'Rua Real Grandeza, 200 - Botafogo',        'Nutricionista',   'Casada',     true,  false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000010', 'Vanessa Cruz Pinto',         'vanessa.cruz@email.com',     '10000000010', '1987-01-25', '21987650010', 'Rua Bambina, 150 - Botafogo',              'Arquiteta',       'Solteira',   true,  true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000011', 'Camila Araújo Dias',         'camila.araujo@email.com',    '10000000011', '1991-05-03', '21987650011', 'Rua das Laranjeiras, 600 - Laranjeiras',   'Fisioterapeuta',  'Casada',     true,  true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000012', 'Daniela Freitas Melo',       'daniela.freitas@email.com',  '10000000012', '1986-10-19', '21987650012', 'Rua Pinheiro Machado, 100 - Laranjeiras',  'Dentista',        'Divorciada', true,  true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000013', 'Gabriela Lopes Cardoso',     'gabriela.lopes@email.com',   '10000000013', '1994-03-28', '21987650013', 'Rua Senador Vergueiro, 200 - Flamengo',    'Jornalista',      'Solteira',   true,  true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Pacientes inativos (aparecem no banco mas não na listagem padrão)
('c1000000-0000-0000-0000-000000000014', 'Helena Martins Correia',     'helena.martins@email.com',   '10000000014', '1980-07-11', '21987650014', 'Rua do Catete, 300 - Catete',              'Professora',      'Casada',     false, true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c1000000-0000-0000-0000-000000000015', 'Isabela Fernandes Queiroz',  'isabela.fernandes@email.com','10000000015', '1996-12-31', '21987650015', 'Rua Dois de Dezembro, 100 - Flamengo',     'Estudante',       'Solteira',   false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- ASSINATURAS (5 assinaturas em estados variados)
-- =============================================================================

-- Beatriz: Pilates Clássico Mensalidade 3x/semana — ATIVO
INSERT INTO assinaturas (id, paciente_id, servico_id, data_inicio, data_vencimento, sessoes_contratadas, sessoes_realizadas, status, valor, observacoes, ativo, created_at, updated_at)
SELECT 'e1000000-0000-0000-0000-000000000001',
       'c1000000-0000-0000-0000-000000000001', s.id,
       '2026-03-01', '2026-03-31',
       12, 6, 'ATIVO', s.valor, 'Mensalidade Março — 3x/semana', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM servicos s
WHERE s.atividade_id = 'a1000000-0000-0000-0000-000000000001'
  AND s.plano_id    = 'b1000000-0000-0000-0000-000000000003'
  AND s.quantidade  = 3 AND s.tipo_atendimento = 'Coletivo'
LIMIT 1
ON CONFLICT (id) DO NOTHING;

-- Fernanda: Fisioterapia Pélvica Pacote 5 sessões — ATIVO
INSERT INTO assinaturas (id, paciente_id, servico_id, data_inicio, data_vencimento, sessoes_contratadas, sessoes_realizadas, status, valor, observacoes, ativo, created_at, updated_at)
SELECT 'e1000000-0000-0000-0000-000000000002',
       'c1000000-0000-0000-0000-000000000002', s.id,
       '2026-02-10', '2026-05-10',
       5, 3, 'ATIVO', s.valor, 'Pós-parto', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM servicos s
WHERE s.atividade_id     = 'a1000000-0000-0000-0000-000000000003'
  AND s.plano_id         = 'b1000000-0000-0000-0000-000000000008'
  AND s.quantidade       = 5
  AND s.modalidade_local = 'Clínica'
LIMIT 1
ON CONFLICT (id) DO NOTHING;

-- Mariana: Pilates Trimestral 2x/semana — FINALIZADO
INSERT INTO assinaturas (id, paciente_id, servico_id, data_inicio, data_vencimento, sessoes_contratadas, sessoes_realizadas, status, valor, observacoes, ativo, created_at, updated_at)
SELECT 'e1000000-0000-0000-0000-000000000003',
       'c1000000-0000-0000-0000-000000000005', s.id,
       '2025-10-01', '2025-12-31',
       24, 24, 'FINALIZADO', s.valor, 'Trimestre encerrado', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM servicos s
WHERE s.atividade_id = 'a1000000-0000-0000-0000-000000000001'
  AND s.plano_id    = 'b1000000-0000-0000-0000-000000000004'
  AND s.quantidade  = 2 AND s.tipo_atendimento = 'Coletivo'
LIMIT 1
ON CONFLICT (id) DO NOTHING;

-- Patricia: Drenagem Pacote 10 sessões — ATIVO
INSERT INTO assinaturas (id, paciente_id, servico_id, data_inicio, data_vencimento, sessoes_contratadas, sessoes_realizadas, status, valor, observacoes, ativo, created_at, updated_at)
SELECT 'e1000000-0000-0000-0000-000000000004',
       'c1000000-0000-0000-0000-000000000006', s.id,
       '2026-03-10', '2026-06-10',
       10, 1, 'ATIVO', s.valor, NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM servicos s
WHERE s.atividade_id     = 'a1000000-0000-0000-0000-000000000005'
  AND s.plano_id         = 'b1000000-0000-0000-0000-000000000008'
  AND s.quantidade       = 10
  AND s.modalidade_local = 'Clínica'
LIMIT 1
ON CONFLICT (id) DO NOTHING;

-- Vanessa: Fisioterapia Traumato Pacote 10 sessões — VENCIDO
INSERT INTO assinaturas (id, paciente_id, servico_id, data_inicio, data_vencimento, sessoes_contratadas, sessoes_realizadas, status, valor, observacoes, ativo, created_at, updated_at)
SELECT 'e1000000-0000-0000-0000-000000000005',
       'c1000000-0000-0000-0000-000000000010', s.id,
       '2025-11-01', '2026-01-31',
       10, 7, 'VENCIDO', s.valor, 'Recuperação pós-cirurgia', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM servicos s
WHERE s.atividade_id     = 'a1000000-0000-0000-0000-000000000004'
  AND s.plano_id         = 'b1000000-0000-0000-0000-000000000008'
  AND s.quantidade       = 10
  AND s.modalidade_local = 'Clínica'
LIMIT 1
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- AGENDAMENTOS (10 agendamentos — vários status e datas)
-- =============================================================================

-- Beatriz com Ana — hoje (AGENDADO)
INSERT INTO agendamentos (id, paciente_id, profissional_id, servico_id, assinatura_id, data_hora, duracao_minutos, status, observacoes, ativo, created_at, updated_at)
SELECT 'f1000000-0000-0000-0000-000000000001',
       'c1000000-0000-0000-0000-000000000001', 'd1000000-0000-0000-0000-000000000001', s.id,
       'e1000000-0000-0000-0000-000000000001',
       CURRENT_DATE + TIME '09:00:00', 60, 'AGENDADO', NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM servicos s
WHERE s.atividade_id = 'a1000000-0000-0000-0000-000000000001'
  AND s.plano_id = 'b1000000-0000-0000-0000-000000000003' AND s.quantidade = 3 AND s.tipo_atendimento = 'Coletivo'
LIMIT 1
ON CONFLICT (id) DO NOTHING;

-- Fernanda com Ana — hoje (CONFIRMADO)
INSERT INTO agendamentos (id, paciente_id, profissional_id, servico_id, assinatura_id, data_hora, duracao_minutos, status, observacoes, ativo, created_at, updated_at)
SELECT 'f1000000-0000-0000-0000-000000000002',
       'c1000000-0000-0000-0000-000000000002', 'd1000000-0000-0000-0000-000000000001', s.id,
       'e1000000-0000-0000-0000-000000000002',
       CURRENT_DATE + TIME '10:00:00', 60, 'CONFIRMADO', 'Levar laudo médico', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM servicos s
WHERE s.atividade_id = 'a1000000-0000-0000-0000-000000000003'
  AND s.plano_id = 'b1000000-0000-0000-0000-000000000008' AND s.quantidade = 5 AND s.modalidade_local = 'Clínica'
LIMIT 1
ON CONFLICT (id) DO NOTHING;

-- Juliana com Ana — amanhã (AGENDADO)
INSERT INTO agendamentos (id, paciente_id, profissional_id, servico_id, assinatura_id, data_hora, duracao_minutos, status, observacoes, ativo, created_at, updated_at)
SELECT 'f1000000-0000-0000-0000-000000000003',
       'c1000000-0000-0000-0000-000000000003', 'd1000000-0000-0000-0000-000000000001', s.id, NULL,
       (CURRENT_DATE + INTERVAL '1 day') + TIME '09:00:00', 60, 'AGENDADO', NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM servicos s
WHERE s.atividade_id = 'a1000000-0000-0000-0000-000000000001'
  AND s.plano_id = 'b1000000-0000-0000-0000-000000000002' AND s.tipo_atendimento = 'Coletivo'
LIMIT 1
ON CONFLICT (id) DO NOTHING;

-- Patricia com Carlos — hoje (AGENDADO)
INSERT INTO agendamentos (id, paciente_id, profissional_id, servico_id, assinatura_id, data_hora, duracao_minutos, status, observacoes, ativo, created_at, updated_at)
SELECT 'f1000000-0000-0000-0000-000000000004',
       'c1000000-0000-0000-0000-000000000006', 'd1000000-0000-0000-0000-000000000002', s.id,
       'e1000000-0000-0000-0000-000000000004',
       CURRENT_DATE + TIME '14:00:00', 60, 'AGENDADO', NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM servicos s
WHERE s.atividade_id = 'a1000000-0000-0000-0000-000000000005'
  AND s.plano_id = 'b1000000-0000-0000-0000-000000000008' AND s.quantidade = 10 AND s.modalidade_local = 'Clínica'
LIMIT 1
ON CONFLICT (id) DO NOTHING;

-- Sandra com Carlos — amanhã (AGENDADO)
INSERT INTO agendamentos (id, paciente_id, profissional_id, servico_id, assinatura_id, data_hora, duracao_minutos, status, observacoes, ativo, created_at, updated_at)
SELECT 'f1000000-0000-0000-0000-000000000005',
       'c1000000-0000-0000-0000-000000000008', 'd1000000-0000-0000-0000-000000000002', s.id, NULL,
       (CURRENT_DATE + INTERVAL '1 day') + TIME '15:00:00', 60, 'AGENDADO', 'Primeira sessão', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM servicos s
WHERE s.atividade_id = 'a1000000-0000-0000-0000-000000000007'
  AND s.plano_id = 'b1000000-0000-0000-0000-000000000007' AND s.modalidade_local = 'Clínica'
LIMIT 1
ON CONFLICT (id) DO NOTHING;

-- Beatriz com Ana — semana passada (REALIZADO)
INSERT INTO agendamentos (id, paciente_id, profissional_id, servico_id, assinatura_id, data_hora, duracao_minutos, status, observacoes, ativo, created_at, updated_at)
SELECT 'f1000000-0000-0000-0000-000000000006',
       'c1000000-0000-0000-0000-000000000001', 'd1000000-0000-0000-0000-000000000001', s.id,
       'e1000000-0000-0000-0000-000000000001',
       (CURRENT_DATE - INTERVAL '7 days') + TIME '09:00:00', 60, 'REALIZADO', NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM servicos s
WHERE s.atividade_id = 'a1000000-0000-0000-0000-000000000001'
  AND s.plano_id = 'b1000000-0000-0000-0000-000000000003' AND s.quantidade = 3 AND s.tipo_atendimento = 'Coletivo'
LIMIT 1
ON CONFLICT (id) DO NOTHING;

-- Larissa — CANCELADO
INSERT INTO agendamentos (id, paciente_id, profissional_id, servico_id, assinatura_id, data_hora, duracao_minutos, status, observacoes, ativo, created_at, updated_at)
SELECT 'f1000000-0000-0000-0000-000000000007',
       'c1000000-0000-0000-0000-000000000004', 'd1000000-0000-0000-0000-000000000001', s.id, NULL,
       (CURRENT_DATE - INTERVAL '3 days') + TIME '11:00:00', 60, 'CANCELADO', 'Paciente cancelou', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM servicos s
WHERE s.atividade_id = 'a1000000-0000-0000-0000-000000000001'
  AND s.plano_id = 'b1000000-0000-0000-0000-000000000001' AND s.tipo_atendimento = 'Coletivo'
LIMIT 1
ON CONFLICT (id) DO NOTHING;

-- Mariana — NAO_COMPARECEU
INSERT INTO agendamentos (id, paciente_id, profissional_id, servico_id, assinatura_id, data_hora, duracao_minutos, status, observacoes, ativo, created_at, updated_at)
SELECT 'f1000000-0000-0000-0000-000000000008',
       'c1000000-0000-0000-0000-000000000005', 'd1000000-0000-0000-0000-000000000001', s.id, NULL,
       (CURRENT_DATE - INTERVAL '5 days') + TIME '10:00:00', 60, 'NAO_COMPARECEU', NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM servicos s
WHERE s.atividade_id = 'a1000000-0000-0000-0000-000000000001'
  AND s.plano_id = 'b1000000-0000-0000-0000-000000000002' AND s.tipo_atendimento = 'Coletivo'
LIMIT 1
ON CONFLICT (id) DO NOTHING;

-- Camila com Carlos — próxima semana (AGENDADO)
INSERT INTO agendamentos (id, paciente_id, profissional_id, servico_id, assinatura_id, data_hora, duracao_minutos, status, observacoes, ativo, created_at, updated_at)
SELECT 'f1000000-0000-0000-0000-000000000009',
       'c1000000-0000-0000-0000-000000000011', 'd1000000-0000-0000-0000-000000000002', s.id, NULL,
       (CURRENT_DATE + INTERVAL '5 days') + TIME '08:00:00', 60, 'AGENDADO', NULL, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM servicos s
WHERE s.atividade_id = 'a1000000-0000-0000-0000-000000000004'
  AND s.plano_id = 'b1000000-0000-0000-0000-000000000006' AND s.modalidade_local = 'Clínica'
LIMIT 1
ON CONFLICT (id) DO NOTHING;

-- Daniela com Ana — próxima semana (AGENDADO)
INSERT INTO agendamentos (id, paciente_id, profissional_id, servico_id, assinatura_id, data_hora, duracao_minutos, status, observacoes, ativo, created_at, updated_at)
SELECT 'f1000000-0000-0000-0000-000000000010',
       'c1000000-0000-0000-0000-000000000012', 'd1000000-0000-0000-0000-000000000001', s.id, NULL,
       (CURRENT_DATE + INTERVAL '6 days') + TIME '11:00:00', 60, 'AGENDADO', 'Avaliação inicial', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM servicos s
WHERE s.atividade_id = 'a1000000-0000-0000-0000-000000000003'
  AND s.plano_id = 'b1000000-0000-0000-0000-000000000006' AND s.modalidade_local = 'Clínica'
LIMIT 1
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- HORÁRIOS DISPONÍVEIS DOS PROFISSIONAIS
-- Ana Paula: segunda a sexta 08:00-18:00
-- Carlos Eduardo: segunda a sábado 08:00-17:00 (sábado até 13:00)
-- =============================================================================
INSERT INTO horarios_disponiveis (id, profissional_id, dia_semana, hora_inicio, hora_fim, ativo, created_at, updated_at) VALUES
  (gen_random_uuid(), 'd1000000-0000-0000-0000-000000000001', 'MONDAY',    '08:00', '18:00', true, NOW(), NOW()),
  (gen_random_uuid(), 'd1000000-0000-0000-0000-000000000001', 'TUESDAY',   '08:00', '18:00', true, NOW(), NOW()),
  (gen_random_uuid(), 'd1000000-0000-0000-0000-000000000001', 'WEDNESDAY', '08:00', '18:00', true, NOW(), NOW()),
  (gen_random_uuid(), 'd1000000-0000-0000-0000-000000000001', 'THURSDAY',  '08:00', '18:00', true, NOW(), NOW()),
  (gen_random_uuid(), 'd1000000-0000-0000-0000-000000000001', 'FRIDAY',    '08:00', '18:00', true, NOW(), NOW()),
  (gen_random_uuid(), 'd1000000-0000-0000-0000-000000000002', 'MONDAY',    '08:00', '17:00', true, NOW(), NOW()),
  (gen_random_uuid(), 'd1000000-0000-0000-0000-000000000002', 'TUESDAY',   '08:00', '17:00', true, NOW(), NOW()),
  (gen_random_uuid(), 'd1000000-0000-0000-0000-000000000002', 'WEDNESDAY', '08:00', '17:00', true, NOW(), NOW()),
  (gen_random_uuid(), 'd1000000-0000-0000-0000-000000000002', 'THURSDAY',  '08:00', '17:00', true, NOW(), NOW()),
  (gen_random_uuid(), 'd1000000-0000-0000-0000-000000000002', 'FRIDAY',    '08:00', '17:00', true, NOW(), NOW()),
  (gen_random_uuid(), 'd1000000-0000-0000-0000-000000000002', 'SATURDAY',  '08:00', '13:00', true, NOW(), NOW());
