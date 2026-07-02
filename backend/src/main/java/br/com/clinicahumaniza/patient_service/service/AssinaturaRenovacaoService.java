package br.com.clinicahumaniza.patient_service.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.clinicahumaniza.patient_service.dto.AgendamentoRecorrenteRequestDTO;
import br.com.clinicahumaniza.patient_service.model.*;
import br.com.clinicahumaniza.patient_service.repository.AgendamentoRecorrenteRepository;
import br.com.clinicahumaniza.patient_service.repository.AssinaturaRepository;

@Service
public class AssinaturaRenovacaoService {

    private static final Logger log = LoggerFactory.getLogger(AssinaturaRenovacaoService.class);
    private static final int DIAS_ANTECEDENCIA_RENOVACAO = 3;

    private final AssinaturaRepository assinaturaRepository;
    private final AgendamentoRecorrenteRepository recorrenteRepository;
    private final AgendamentoRecorrenteService agendamentoRecorrenteService;

    public AssinaturaRenovacaoService(
            AssinaturaRepository assinaturaRepository,
            AgendamentoRecorrenteRepository recorrenteRepository,
            AgendamentoRecorrenteService agendamentoRecorrenteService) {
        this.assinaturaRepository = assinaturaRepository;
        this.recorrenteRepository = recorrenteRepository;
        this.agendamentoRecorrenteService = agendamentoRecorrenteService;
    }

    @Transactional
    public int renovarAssinaturasProximasDoVencimento() {
        LocalDate limitDate = LocalDate.now().plusDays(DIAS_ANTECEDENCIA_RENOVACAO);

        // Inclui FINALIZADO: assinaturas recorrentes que completaram todas as sessões viram
        // FINALIZADO e ainda assim devem renovar (são reativadas para ATIVO ao gerar o novo ciclo).
        List<Assinatura> assinaturas =
                assinaturaRepository.findByRenovacaoAutomaticaTrueAndStatusInAndDataVencimentoLessThanEqual(
                        List.of(StatusAssinatura.ATIVO, StatusAssinatura.FINALIZADO), limitDate);

        log.info("Encontradas {} assinaturas para renovacao automatica", assinaturas.size());

        int renovadas = 0;
        for (Assinatura assinatura : assinaturas) {
            try {
                renovarAssinatura(assinatura);
                renovadas++;
            } catch (Exception e) {
                log.error(
                        "Erro ao renovar assinatura {} do paciente {}: {}",
                        assinatura.getId(),
                        assinatura.getPaciente().getNomeCompleto(),
                        e.getMessage());
                appendObservacao(
                        assinatura,
                        "Falha na renovacao automatica em " + formatDate(LocalDate.now()) + ": " + e.getMessage());
                assinaturaRepository.save(assinatura);
            }
        }

        log.info("Renovacao automatica concluida: {}/{} assinaturas renovadas", renovadas, assinaturas.size());
        return renovadas;
    }

    /**
     * Renova manualmente uma única assinatura (botão "Renovar agora"), independente da
     * antecedência do vencimento. Gera os agendamentos do próximo ciclo a partir do
     * vencimento atual e reativa a assinatura caso estivesse FINALIZADA.
     */
    @Transactional
    public Assinatura renovarAssinaturaManual(java.util.UUID id) {
        Assinatura assinatura = assinaturaRepository
                .findById(id)
                .orElseThrow(() -> new br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException(
                        "Assinatura", id));
        renovarAssinatura(assinatura);
        return assinaturaRepository.findById(id).orElse(assinatura);
    }

    private void renovarAssinatura(Assinatura assinatura) {
        List<AgendamentoRecorrente> templatesAtivos =
                recorrenteRepository.findByAssinaturaIdAndAtivoTrue(assinatura.getId());

        if (templatesAtivos.isEmpty()) {
            log.warn(
                    "Assinatura {} nao possui agendamentos recorrentes vinculados, pulando renovacao",
                    assinatura.getId());
            return;
        }

        // Deduplica por slot (dias da semana + hora): renovações anteriores podiam ter
        // deixado templates repetidos ativos. Sem isso, cada ciclo geraria um novo template
        // por template ativo — dobrando os agendamentos a cada renovação.
        Map<String, AgendamentoRecorrente> slotsDistintos = new LinkedHashMap<>();
        for (AgendamentoRecorrente t : templatesAtivos) {
            slotsDistintos.putIfAbsent(t.getDiasSemana() + "|" + t.getHoraInicio(), t);
        }
        List<AgendamentoRecorrente> templates = new ArrayList<>(slotsDistintos.values());

        LocalDate novaDataInicio = assinatura.getDataVencimento().plusDays(1);
        // Avança por mês de calendário (não +30 dias fixos): mantém o vencimento no mesmo
        // dia do mês e evita o deslize acumulado a cada renovação.
        LocalDate novaDataVencimento = novaDataInicio.plusMonths(1).minusDays(1);
        int diasNoPeriodo = (int) (novaDataVencimento.toEpochDay() - novaDataInicio.toEpochDay()) + 1;

        // Pré-calcula quantas sessões o próximo ciclo vai gerar (somando todos os slots).
        int totalNovasSessoesPrevistas = 0;
        for (AgendamentoRecorrente template : templates) {
            List<DayOfWeek> diasSemana = Arrays.stream(template.getDiasSemana().split(","))
                    .map(DayOfWeek::valueOf)
                    .collect(Collectors.toList());
            totalNovasSessoesPrevistas += contarDiasDaSemanaNoPeríodo(novaDataInicio, diasNoPeriodo, diasSemana);
        }
        if (totalNovasSessoesPrevistas == 0) {
            log.warn(
                    "Renovacao da assinatura {} nao gera novas sessoes no periodo {} a {}; pulando",
                    assinatura.getId(),
                    novaDataInicio,
                    novaDataVencimento);
            return;
        }

        // createRecorrente/createAgendamento exigem assinatura ATIVA e COM sessões restantes.
        // Uma assinatura recorrente FINALIZADA (sessões completas) é justamente o caso que
        // queremos renovar: reativamos e já ampliamos as sessões contratadas ANTES do laço
        // (a mesma instância gerenciada é vista pelas validações na transação corrente) para
        // abrir espaço. Reconciliamos com o total real ao final e revertemos se nada for criado.
        StatusAssinatura statusOriginal = assinatura.getStatus();
        int contratadasOriginais = assinatura.getSessoesContratadas();
        assinatura.setStatus(StatusAssinatura.ATIVO);
        assinatura.setSessoesContratadas(contratadasOriginais + totalNovasSessoesPrevistas);
        assinaturaRepository.save(assinatura);

        int totalAgendamentosCriados = 0;

        for (AgendamentoRecorrente template : templates) {
            List<DayOfWeek> diasSemana = Arrays.stream(template.getDiasSemana().split(","))
                    .map(DayOfWeek::valueOf)
                    .collect(Collectors.toList());

            int sessoesParaEsteSlot = contarDiasDaSemanaNoPeríodo(novaDataInicio, diasNoPeriodo, diasSemana);
            if (sessoesParaEsteSlot == 0) continue;

            AgendamentoRecorrenteRequestDTO dto = new AgendamentoRecorrenteRequestDTO();
            dto.setPacienteId(template.getPaciente().getId());
            if (template.getProfissional() != null) {
                dto.setProfissionalId(template.getProfissional().getId());
            }
            dto.setServicoId(template.getServico().getId());
            dto.setAssinaturaId(assinatura.getId());
            dto.setFrequencia(template.getFrequencia());
            dto.setDiasSemana(diasSemana);
            dto.setHoraInicio(template.getHoraInicio());
            dto.setDuracaoMinutos(template.getDuracaoMinutos());
            dto.setTotalSessoes(sessoesParaEsteSlot);
            // FORÇA a geração no próximo período (vencimento+1 .. novoVencimento).
            // Sem isso, createRecorrente usaria assinatura.dataInicio (a data ORIGINAL)
            // e repetiria as mesmas datas do mês anterior.
            dto.setDataInicio(novaDataInicio);
            dto.setDataFim(novaDataVencimento);

            try {
                var result = agendamentoRecorrenteService.createRecorrente(dto);
                int criados = result.getAgendamentosCriados() != null
                        ? result.getAgendamentosCriados().size()
                        : 0;
                totalAgendamentosCriados += criados;

                log.info(
                        "Renovacao assinatura {}: slot {} {} - {} agendamentos criados",
                        assinatura.getId(),
                        template.getHoraInicio(),
                        template.getDiasSemana(),
                        criados);
            } catch (Exception e) {
                log.warn(
                        "Erro ao criar agendamentos para slot {} da assinatura {}: {}",
                        template.getHoraInicio(),
                        assinatura.getId(),
                        e.getMessage());
            }
        }

        if (totalAgendamentosCriados > 0) {
            // Desativa os templates do ciclo anterior — os novos (criados acima) assumem.
            // Impede que a próxima renovação some templates e volte a duplicar.
            for (AgendamentoRecorrente antigo : templatesAtivos) {
                antigo.setAtivo(false);
                recorrenteRepository.save(antigo);
            }

            assinatura.setDataVencimento(novaDataVencimento);
            // Reconcilia as sessões contratadas com o que realmente foi criado — algumas datas
            // podem ter sido ignoradas por conflito de horário (menos que o previsto).
            assinatura.setSessoesContratadas(contratadasOriginais + totalAgendamentosCriados);
            // Reabre o ciclo: uma assinatura que estava FINALIZADA (sessões completas) volta a ATIVO,
            // já que o novo período acrescenta sessões (realizadas < contratadas novamente).
            assinatura.setStatus(StatusAssinatura.ATIVO);
            appendObservacao(
                    assinatura,
                    "Renovado automaticamente em " + formatDate(LocalDate.now()) + " (" + totalAgendamentosCriados
                            + " agendamentos criados)");
            assinaturaRepository.save(assinatura);

            log.info(
                    "Assinatura {} renovada: nova data vencimento={}, +{} agendamentos/sessoes",
                    assinatura.getId(),
                    novaDataVencimento,
                    totalAgendamentosCriados);
        } else {
            // Nada foi criado: reverte a reativação e a ampliação de sessões para não deixar
            // a assinatura num estado inconsistente (ATIVO porém sem novas sessões reais).
            assinatura.setStatus(statusOriginal);
            assinatura.setSessoesContratadas(contratadasOriginais);
            assinaturaRepository.save(assinatura);
            log.warn("Nenhum agendamento criado na renovacao da assinatura {}", assinatura.getId());
        }
    }

    private int contarDiasDaSemanaNoPeríodo(LocalDate inicio, int totalDias, List<DayOfWeek> diasSemana) {
        int count = 0;
        for (int i = 0; i < totalDias; i++) {
            if (diasSemana.contains(inicio.plusDays(i).getDayOfWeek())) {
                count++;
            }
        }
        return count;
    }

    private void appendObservacao(Assinatura assinatura, String texto) {
        String obs = assinatura.getObservacoes();
        if (obs == null || obs.isBlank()) {
            assinatura.setObservacoes(texto);
        } else {
            assinatura.setObservacoes(obs + "\n" + texto);
        }
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
