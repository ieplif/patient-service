-- Atividades
INSERT INTO atividades (id, nome, descricao, duracao_padrao, ativo, created_at, updated_at) VALUES
(RANDOM_UUID(), 'Pilates', 'Método de exercícios físicos que visa fortalecimento muscular e flexibilidade', 50, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'Fisioterapia', 'Tratamento e reabilitação de lesões e disfunções do movimento', 50, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'Estética Corporal', 'Tratamentos estéticos para o corpo', 60, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'Estética Facial', 'Tratamentos estéticos para o rosto', 45, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'RPG', 'Reeducação Postural Global', 50, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'Drenagem Linfática', 'Técnica de massagem para estimular o sistema linfático', 60, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'Massoterapia', 'Terapia através de massagens para alívio de dores e tensões', 50, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Planos
INSERT INTO planos (id, nome, descricao, tipo_plano, validade_dias, sessoes_incluidas, permite_transferencia, ativo, created_at, updated_at) VALUES
(RANDOM_UUID(), 'Experimental', 'Sessão experimental para novos pacientes', 'experimental', 7, 1, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'Avulso', 'Sessão avulsa sem compromisso', 'avulso', 30, 1, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'Mensal', 'Plano mensal com sessões semanais', 'mensal', 30, 4, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'Trimestral', 'Plano trimestral com desconto', 'trimestral', 90, 12, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'Semestral', 'Plano semestral com desconto especial', 'semestral', 180, 24, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'Anual', 'Plano anual com maior desconto', 'anual', 365, 48, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(RANDOM_UUID(), 'Pacote Promocional', 'Pacote com sessões promocionais', 'pacote', 60, 10, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
