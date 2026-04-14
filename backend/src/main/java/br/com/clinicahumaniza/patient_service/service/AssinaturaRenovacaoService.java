package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.AgendamentoRecorrenteRequestDTO;
import br.com.clinicahumaniza.patient_service.model.*;
import br.com.clinicahumaniza.patient_service.repository.AgendamentoRecorrenteRepository;
import br.com.clinicahumaniza.patient_service.repository.AssinaturaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssinaturaRenovacaoService {

    private static final Logger log = LoggerFactory.getLogger(AssinaturaRenovacaoService.class);
    private static final int DIAS_ANTECEDENCIA_RENOVACAO = 3;

    private final AssinaturaRepository assinaturaRepository;
    private final AgendamentoRecorrenteRepository recorrenteRepository;
    private final AgendamentoRecorrenteService agendamentoRecorrenteService;

    public AssinaturaRenovacaoService(AssinaturaRepository assinaturaRepository,
                                       AgendamentoRecorrenteRepository recorrenteRepository,
                                       AgendamentoRecorrenteService agendamentoRecorrenteService) {
        this.assinaturaRepository = assinaturaRepository;
        this.recorrenteRepository = recorrenteRepository;
        this.agendamentoRecorrenteService = agendamentoRecorrenteService;
    }

    @Transactional
    public int renovarAssinaturasProximasDoVencimento() {
        LocalDate limitDate = LocalDate.now().plusDays(DIAS_ANTECEDENCIA_RENOVACAO);

        List<Assinatura> assinaturas = assinaturaRepository
                .findByRenovacaoAutomaticaTrueAndStatusAndDataVencimentoLessThanEqual(
                        StatusAssinatura.ATIVO, limitDate);

        log.info("Encontradas {} assinaturas para renovacao automatica", assinaturas.size());

        int renovadas = 0;
        for (Assinatura assinatura : assinaturas) {
            try {
                renovarAssinatura(assinatura);
                renovadas++;
            } catch (Exception e) {
                log.error("Erro ao renovar assinatura {} do paciente {}: {}",
                        assinatura.getId(),
                        assinatura.getPaciente().getNomeCompleto(),
                        e.getMessage());
                appendObservacao(assinatura,
                        "Falha na renovacao automatica em " + formatDate(LocalDate.now()) + ": " + e.getMessage());
                assinaturaRepository.save(assinatura);
            }
        }

        log.info("Renovacao automatica concluida: {}/{} assinaturas renovadas", renovadas, assinaturas.size());
        return renovadas;
    }

    private void renovarAssinatura(Assinatura assinatura) {
        List<AgendamentoRecorrente> templates = recorrenteRepository.findByAssinaturaId(assinatura.getId());

        if (templates.isEmpty()) {
            log.warn("Assinatura {} nao possui agendamentos recorrentes vinculados, pulando renovacao",
                    assinatura.getId());
            return;
        }

        Servico servico = assinatura.getServico();
        Plano plano = servico.getPlano();
        int validadeDias = plano.getValidadeDias() != null ? plano.getValidadeDias() : 30;

        LocalDate novaDataInicio = assinatura.getDataVencimento().plusDays(1);
        LocalDate novaDataVencimento = novaDataInicio.plusDays(validadeDias - 1);

        int totalNovasSessoes = 0;
        int totalAgendamentosCriados = 0;

        for (AgendamentoRecorrente template : templates) {
            List<DayOfWeek> diasSemana = Arrays.stream(template.getDiasSemana().split(","))
                    .map(DayOfWeek::valueOf)
                    .collect(Collectors.toList());

            int sessoesParaEsteSlot = contarDiasDaSemanaNoPeríodo(novaDataInicio, validadeDias, diasSemana);
            if (sessoesParaEsteSlot == 0) continue;

            AgendamentoRecorrenteRequestDTO dto = new AgendamentoRecorrenteRequestDTO();
            dto.setPacienteId(template.getPaciente().getId());
            dto.setProfissionalId(template.getProfissional().getId());
            dto.setServicoId(template.getServico().getId());
            dto.setAssinaturaId(assinatura.getId());
            dto.setFrequencia(template.getFrequencia());
            dto.setDiasSemana(diasSemana);
            dto.setHoraInicio(template.getHoraInicio());
            dto.setDuracaoMinutos(template.getDuracaoMinutos());
            dto.setTotalSessoes(sessoesParaEsteSlot);
            dto.setDataFim(novaDataVencimento);

            try {
                var result = agendamentoRecorrenteService.createRecorrente(dto);
                int criados = result.getAgendamentosCriados() != null ? result.getAgendamentosCriados().size() : 0;
                totalAgendamentosCriados += criados;
                totalNovasSessoes += sessoesParaEsteSlot;

                log.info("Renovacao assinatura {}: slot {} {} - {} agendamentos criados",
                        assinatura.getId(), template.getHoraInicio(), template.getDiasSemana(), criados);
            } catch (Exception e) {
                log.warn("Erro ao criar agendamentos para slot {} da assinatura {}: {}",
                        template.getHoraInicio(), assinatura.getId(), e.getMessage());
            }
        }

        if (totalAgendamentosCriados > 0) {
            assinatura.setDataVencimento(novaDataVencimento);
            assinatura.setSessoesContratadas(assinatura.getSessoesContratadas() + totalNovasSessoes);
            appendObservacao(assinatura,
                    "Renovado automaticamente em " + formatDate(LocalDate.now()) +
                    " (" + totalAgendamentosCriados + " agendamentos criados)");
            assinaturaRepository.save(assinatura);

            log.info("Assinatura {} renovada: nova data vencimento={}, +{} sessoes, {} agendamentos",
                    assinatura.getId(), novaDataVencimento, totalNovasSessoes, totalAgendamentosCriados);
        } else {
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
