package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.*;
import br.com.clinicahumaniza.patient_service.exception.BusinessException;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.AgendamentoMapper;
import br.com.clinicahumaniza.patient_service.model.*;
import br.com.clinicahumaniza.patient_service.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AgendamentoRecorrenteService {

    private static final int MAX_DATAS = 52;

    private final AgendamentoService agendamentoService;
    private final AgendamentoRepository agendamentoRepository;
    private final AgendamentoRecorrenteRepository recorrenteRepository;
    private final PatientRepository patientRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ServicoRepository servicoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final AgendamentoMapper agendamentoMapper;

    @Autowired
    public AgendamentoRecorrenteService(AgendamentoService agendamentoService,
                                         AgendamentoRepository agendamentoRepository,
                                         AgendamentoRecorrenteRepository recorrenteRepository,
                                         PatientRepository patientRepository,
                                         ProfissionalRepository profissionalRepository,
                                         ServicoRepository servicoRepository,
                                         AssinaturaRepository assinaturaRepository,
                                         AgendamentoMapper agendamentoMapper) {
        this.agendamentoService = agendamentoService;
        this.agendamentoRepository = agendamentoRepository;
        this.recorrenteRepository = recorrenteRepository;
        this.patientRepository = patientRepository;
        this.profissionalRepository = profissionalRepository;
        this.servicoRepository = servicoRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.agendamentoMapper = agendamentoMapper;
    }

    @Transactional
    public AgendamentoRecorrenteResponseDTO createRecorrente(AgendamentoRecorrenteRequestDTO dto) {
        if (dto.getTotalSessoes() == null && dto.getDataFim() == null) {
            throw new BusinessException("É necessário informar totalSessoes ou dataFim");
        }

        Patient paciente = patientRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", dto.getPacienteId()));

        Profissional profissional = profissionalRepository.findById(dto.getProfissionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Profissional", dto.getProfissionalId()));

        Servico servico = servicoRepository.findById(dto.getServicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Serviço", dto.getServicoId()));

        // Validar que o profissional atende a atividade do serviço
        boolean atende = profissional.getAtividades().stream()
                .anyMatch(a -> a.getId().equals(servico.getAtividade().getId()));
        if (!atende) {
            throw new BusinessException(
                    "O profissional " + profissional.getNome() +
                    " não atende a atividade " + servico.getAtividade().getNome()
            );
        }

        Assinatura assinatura = null;
        if (dto.getAssinaturaId() != null) {
            assinatura = assinaturaRepository.findById(dto.getAssinaturaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assinatura", dto.getAssinaturaId()));
            if (assinatura.getStatus() != StatusAssinatura.ATIVO) {
                throw new BusinessException("Assinatura não está ativa (status: " + assinatura.getStatus() + ")");
            }
        }

        // Resolver duração
        Integer duracaoMinutos = dto.getDuracaoMinutos();
        if (duracaoMinutos == null) {
            duracaoMinutos = servico.getAtividade().getDuracaoPadrao();
            if (duracaoMinutos == null) {
                throw new BusinessException("Duração não informada e a atividade não possui duração padrão");
            }
        }

        // Gerar datas candidatas
        List<LocalDateTime> datasCandidatas = gerarDatas(
                dto.getFrequencia(), dto.getDiasSemana(), dto.getHoraInicio(),
                dto.getTotalSessoes(), dto.getDataFim());

        // Salvar entidade AgendamentoRecorrente
        AgendamentoRecorrente recorrente = new AgendamentoRecorrente();
        recorrente.setPaciente(paciente);
        recorrente.setProfissional(profissional);
        recorrente.setServico(servico);
        recorrente.setAssinatura(assinatura);
        recorrente.setFrequencia(dto.getFrequencia());
        recorrente.setDiasSemana(dto.getDiasSemana().stream()
                .map(DayOfWeek::name)
                .collect(Collectors.joining(",")));
        recorrente.setHoraInicio(dto.getHoraInicio());
        recorrente.setDuracaoMinutos(duracaoMinutos);
        recorrente.setTotalSessoes(dto.getTotalSessoes());
        recorrente.setDataFim(dto.getDataFim());
        recorrente.setObservacoes(dto.getObservacoes());
        recorrente = recorrenteRepository.save(recorrente);

        // Tentar criar cada agendamento
        List<Agendamento> criados = new ArrayList<>();
        List<DataIgnoradaDTO> ignoradas = new ArrayList<>();

        for (LocalDateTime dataHora : datasCandidatas) {
            AgendamentoRequestDTO agendamentoDTO = new AgendamentoRequestDTO();
            agendamentoDTO.setPacienteId(dto.getPacienteId());
            agendamentoDTO.setProfissionalId(dto.getProfissionalId());
            agendamentoDTO.setServicoId(dto.getServicoId());
            agendamentoDTO.setAssinaturaId(dto.getAssinaturaId());
            agendamentoDTO.setDataHora(dataHora);
            agendamentoDTO.setDuracaoMinutos(duracaoMinutos);
            agendamentoDTO.setObservacoes(dto.getObservacoes());

            try {
                Agendamento agendamento = agendamentoService.createAgendamento(agendamentoDTO);
                agendamento.setAgendamentoRecorrente(recorrente);
                agendamentoRepository.save(agendamento);
                criados.add(agendamento);
            } catch (BusinessException e) {
                ignoradas.add(new DataIgnoradaDTO(dataHora.toLocalDate(), e.getMessage()));
            }
        }

        // Montar response
        return toResponseDTO(recorrente, criados, ignoradas);
    }

    List<LocalDateTime> gerarDatas(FrequenciaRecorrencia frequencia, List<DayOfWeek> diasSemana,
                                    LocalTime horaInicio, Integer totalSessoes, LocalDate dataFim) {
        List<LocalDateTime> datas = new ArrayList<>();
        LocalDate atual = LocalDate.now().plusDays(1); // começa amanhã

        while (datas.size() < MAX_DATAS) {
            // Verifica limite por data
            if (dataFim != null && atual.isAfter(dataFim)) {
                break;
            }

            // Verifica limite por quantidade
            if (totalSessoes != null && datas.size() >= totalSessoes) {
                break;
            }

            if (frequencia == FrequenciaRecorrencia.MENSAL) {
                // Mensal: usa o mesmo dia do mês
                if (diasSemana.contains(atual.getDayOfWeek()) || datas.isEmpty()) {
                    // Para mensal, ignora diasSemana - usa a data diretamente
                    datas.add(atual.atTime(horaInicio));

                    if (totalSessoes != null && datas.size() >= totalSessoes) break;
                    if (dataFim != null && atual.plusMonths(1).isAfter(dataFim)) break;

                    atual = atual.plusMonths(1);
                    continue;
                }
                atual = atual.plusDays(1);
            } else {
                // Semanal ou Quinzenal
                if (diasSemana.contains(atual.getDayOfWeek())) {
                    datas.add(atual.atTime(horaInicio));

                    if (totalSessoes != null && datas.size() >= totalSessoes) break;

                    // Se é o último dia da semana no ciclo, avança o ciclo
                    DayOfWeek ultimoDiaCiclo = diasSemana.stream()
                            .max(DayOfWeek::compareTo).orElse(atual.getDayOfWeek());

                    if (atual.getDayOfWeek() == ultimoDiaCiclo) {
                        int semanas = frequencia == FrequenciaRecorrencia.QUINZENAL ? 2 : 1;
                        // Avança para o início da próxima semana do ciclo
                        DayOfWeek primeiroDiaCiclo = diasSemana.stream()
                                .min(DayOfWeek::compareTo).orElse(atual.getDayOfWeek());
                        int diasAteProximoCiclo = (semanas * 7) - (atual.getDayOfWeek().getValue() - primeiroDiaCiclo.getValue());
                        atual = atual.plusDays(diasAteProximoCiclo);
                    } else {
                        atual = atual.plusDays(1);
                    }
                } else {
                    atual = atual.plusDays(1);
                }
            }
        }

        return datas;
    }

    @Transactional
    public List<AgendamentoResponseDTO> cancelarRecorrencia(UUID agendamentoId, boolean cancelarFuturos) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento", agendamentoId));

        if (!cancelarFuturos) {
            agendamentoService.updateStatus(agendamentoId, new AgendamentoStatusDTO(StatusAgendamento.CANCELADO));
            return List.of(agendamentoMapper.toResponseDTO(agendamento));
        }

        // Cancelar futuros
        AgendamentoRecorrente recorrente = agendamento.getAgendamentoRecorrente();
        if (recorrente == null) {
            throw new BusinessException("Este agendamento não faz parte de uma recorrência");
        }

        List<StatusAgendamento> statusesNaoFinais = List.of(
                StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO);

        List<Agendamento> futuros = agendamentoRepository
                .findByAgendamentoRecorrenteIdAndDataHoraGreaterThanEqualAndStatusIn(
                        recorrente.getId(), agendamento.getDataHora(), statusesNaoFinais);

        List<AgendamentoResponseDTO> cancelados = new ArrayList<>();
        for (Agendamento futuro : futuros) {
            agendamentoService.updateStatus(futuro.getId(), new AgendamentoStatusDTO(StatusAgendamento.CANCELADO));
            cancelados.add(agendamentoMapper.toResponseDTO(futuro));
        }

        return cancelados;
    }

    public AgendamentoRecorrenteResponseDTO getRecorrenciaByAgendamento(UUID agendamentoId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento", agendamentoId));

        AgendamentoRecorrente recorrente = agendamento.getAgendamentoRecorrente();
        if (recorrente == null) {
            throw new BusinessException("Este agendamento não faz parte de uma recorrência");
        }

        return toResponseDTO(recorrente, List.of(), List.of());
    }

    private AgendamentoRecorrenteResponseDTO toResponseDTO(AgendamentoRecorrente recorrente,
                                                             List<Agendamento> criados,
                                                             List<DataIgnoradaDTO> ignoradas) {
        AgendamentoRecorrenteResponseDTO response = new AgendamentoRecorrenteResponseDTO();
        response.setId(recorrente.getId());
        response.setPacienteId(recorrente.getPaciente().getId());
        response.setPacienteNome(recorrente.getPaciente().getNomeCompleto());
        response.setProfissionalId(recorrente.getProfissional().getId());
        response.setProfissionalNome(recorrente.getProfissional().getNome());
        response.setServicoId(recorrente.getServico().getId());
        response.setServicoDescricao(
                recorrente.getServico().getAtividade().getNome() + " - " +
                recorrente.getServico().getPlano().getNome());
        if (recorrente.getAssinatura() != null) {
            response.setAssinaturaId(recorrente.getAssinatura().getId());
        }
        response.setFrequencia(recorrente.getFrequencia());
        response.setDiasSemana(Arrays.stream(recorrente.getDiasSemana().split(","))
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toList()));
        response.setHoraInicio(recorrente.getHoraInicio());
        response.setDuracaoMinutos(recorrente.getDuracaoMinutos());
        response.setTotalSessoes(recorrente.getTotalSessoes());
        response.setDataFim(recorrente.getDataFim());
        response.setObservacoes(recorrente.getObservacoes());
        response.setAgendamentosCriados(criados.stream()
                .map(agendamentoMapper::toResponseDTO)
                .collect(Collectors.toList()));
        response.setDatasIgnoradas(ignoradas);
        response.setCreatedAt(recorrente.getCreatedAt());
        return response;
    }
}
