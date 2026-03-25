package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.AgendamentoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.AgendamentoStatusDTO;
import br.com.clinicahumaniza.patient_service.dto.AgendamentoUpdateDTO;
import br.com.clinicahumaniza.patient_service.dto.ReposicaoInfoDTO;
import br.com.clinicahumaniza.patient_service.dto.ReposicaoRequestDTO;
import br.com.clinicahumaniza.patient_service.exception.BusinessException;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.AgendamentoMapper;
import br.com.clinicahumaniza.patient_service.model.*;
import br.com.clinicahumaniza.patient_service.repository.*;
import br.com.clinicahumaniza.patient_service.spec.AgendamentoSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AgendamentoService {

    private static final int LIMITE_REPOSICOES_MES = 2;
    private static final int DIAS_VALIDADE_REPOSICAO = 20;
    private static final int HORAS_ANTECEDENCIA_CANCELAMENTO = 3;

    private final AgendamentoRepository agendamentoRepository;
    private final PatientRepository patientRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ServicoRepository servicoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final HorarioDisponivelRepository horarioDisponivelRepository;
    private final FeriadoRepository feriadoRepository;
    private final AgendamentoMapper agendamentoMapper;
    private final AssinaturaService assinaturaService;
    private final Optional<GoogleCalendarService> googleCalendarService;

    @Autowired
    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                               PatientRepository patientRepository,
                               ProfissionalRepository profissionalRepository,
                               ServicoRepository servicoRepository,
                               AssinaturaRepository assinaturaRepository,
                               HorarioDisponivelRepository horarioDisponivelRepository,
                               FeriadoRepository feriadoRepository,
                               AgendamentoMapper agendamentoMapper,
                               AssinaturaService assinaturaService,
                               Optional<GoogleCalendarService> googleCalendarService) {
        this.agendamentoRepository = agendamentoRepository;
        this.patientRepository = patientRepository;
        this.profissionalRepository = profissionalRepository;
        this.servicoRepository = servicoRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.horarioDisponivelRepository = horarioDisponivelRepository;
        this.feriadoRepository = feriadoRepository;
        this.agendamentoMapper = agendamentoMapper;
        this.assinaturaService = assinaturaService;
        this.googleCalendarService = googleCalendarService;
    }

    @Transactional
    public Agendamento createAgendamento(AgendamentoRequestDTO dto) {
        Patient paciente = patientRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", dto.getPacienteId()));

        Profissional profissional = profissionalRepository.findById(dto.getProfissionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Profissional", dto.getProfissionalId()));

        Servico servico = servicoRepository.findById(dto.getServicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Serviço", dto.getServicoId()));

        // Validar que o profissional atende a atividade do serviço
        validarProfissionalAtendeAtividade(profissional, servico);

        // Validar assinatura se fornecida
        Assinatura assinatura = null;
        if (dto.getAssinaturaId() != null) {
            assinatura = assinaturaRepository.findById(dto.getAssinaturaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assinatura", dto.getAssinaturaId()));
            validarAssinatura(assinatura);
        }

        // Default duração da atividade
        if (dto.getDuracaoMinutos() == null) {
            Integer duracaoPadrao = servico.getAtividade().getDuracaoPadrao();
            if (duracaoPadrao != null) {
                dto.setDuracaoMinutos(duracaoPadrao);
            } else {
                throw new BusinessException("Duração não informada e a atividade não possui duração padrão");
            }
        }

        // Validar dentro do horário disponível do profissional
        validarDentroDoHorarioDisponivel(profissional.getId(), dto.getDataHora(), dto.getDuracaoMinutos());

        // Detectar conflitos de horário (respeitando capacidade máxima da atividade)
        int capacidade = servico.getAtividade().getCapacidadeMaxima() != null
                ? servico.getAtividade().getCapacidadeMaxima() : 1;
        validarConflitoHorario(profissional.getId(), dto.getDataHora(), dto.getDuracaoMinutos(), capacidade);

        Agendamento agendamento = agendamentoMapper.toEntity(dto, paciente, profissional, servico, assinatura);
        agendamento.setStatus(StatusAgendamento.AGENDADO);

        Agendamento saved = agendamentoRepository.save(agendamento);
        googleCalendarService.ifPresent(g -> g.createEvent(saved));
        return saved;
    }

    public Agendamento getAgendamentoById(UUID id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento", id));
    }

    public Page<Agendamento> getAllAgendamentos(StatusAgendamento status, UUID pacienteId,
                                                UUID profissionalId, LocalDateTime dataInicio,
                                                LocalDateTime dataFim, Pageable pageable) {
        Specification<Agendamento> spec = Specification
                .where(AgendamentoSpecification.hasStatus(status))
                .and(AgendamentoSpecification.hasPaciente(pacienteId))
                .and(AgendamentoSpecification.hasProfissional(profissionalId))
                .and(AgendamentoSpecification.betweenDatas(dataInicio, dataFim));
        return agendamentoRepository.findAll(spec, pageable);
    }

    public byte[] exportCsv(LocalDate inicio, LocalDate fim, StatusAgendamento status) {
        LocalDateTime dtInicio = inicio != null ? inicio.atStartOfDay() : null;
        LocalDateTime dtFim = fim != null ? fim.atTime(LocalTime.MAX) : null;
        Specification<Agendamento> spec = Specification
                .where(AgendamentoSpecification.hasStatus(status))
                .and(AgendamentoSpecification.betweenDatas(dtInicio, dtFim));
        List<Agendamento> agendamentos = agendamentoRepository.findAll(spec);
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Paciente,Profissional,Servico,DataHora,Status,DuracaoMinutos\n");
        for (Agendamento a : agendamentos) {
            csv.append(String.join(",",
                    a.getId().toString(),
                    escapeCsv(a.getPaciente().getNomeCompleto()),
                    escapeCsv(a.getProfissional().getNome()),
                    escapeCsv(a.getServico().getAtividade().getNome() + " - " + a.getServico().getPlano().getNome()),
                    a.getDataHora() != null ? a.getDataHora().toString() : "",
                    a.getStatus().name(),
                    a.getDuracaoMinutos() != null ? a.getDuracaoMinutos().toString() : ""
            )).append("\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public List<Agendamento> getAgendamentosByPaciente(UUID pacienteId) {
        return agendamentoRepository.findByPacienteId(pacienteId);
    }

    public List<Agendamento> getAgendamentosByProfissional(UUID profissionalId) {
        return agendamentoRepository.findByProfissionalId(profissionalId);
    }

    public List<Agendamento> getAgendamentosByPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return agendamentoRepository.findByDataHoraBetween(inicio, fim);
    }

    @Transactional
    public Agendamento updateAgendamento(UUID id, AgendamentoUpdateDTO dto) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento", id));

        if (isStatusFinal(agendamento.getStatus())) {
            throw new BusinessException("Não é possível alterar um agendamento com status " + agendamento.getStatus());
        }

        // Se mudou dataHora ou duração, revalidar conflitos e disponibilidade
        LocalDateTime novaDataHora = dto.getDataHora() != null ? dto.getDataHora() : agendamento.getDataHora();
        Integer novaDuracao = dto.getDuracaoMinutos() != null ? dto.getDuracaoMinutos() : agendamento.getDuracaoMinutos();

        if (dto.getDataHora() != null || dto.getDuracaoMinutos() != null) {
            validarDentroDoHorarioDisponivel(agendamento.getProfissional().getId(), novaDataHora, novaDuracao);
            int capacidade = agendamento.getServico().getAtividade().getCapacidadeMaxima() != null
                    ? agendamento.getServico().getAtividade().getCapacidadeMaxima() : 1;
            validarConflitoHorarioExcluindo(agendamento.getProfissional().getId(), novaDataHora, novaDuracao,
                    agendamento.getId(), capacidade);
        }

        agendamentoMapper.updateEntityFromDto(dto, agendamento);
        Agendamento saved = agendamentoRepository.save(agendamento);
        googleCalendarService.ifPresent(g -> g.updateEvent(saved));
        return saved;
    }

    @Transactional
    public Agendamento updateStatus(UUID id, AgendamentoStatusDTO dto) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento", id));

        validarTransicaoStatus(agendamento.getStatus(), dto.getStatus());

        agendamento.setStatus(dto.getStatus());

        // Salvar motivo de cancelamento se fornecido
        if (dto.getMotivoCancelamento() != null) {
            agendamento.setMotivoCancelamento(dto.getMotivoCancelamento());
        }

        // Se marcou como REALIZADO e tem assinatura vinculada, registrar sessão
        if (dto.getStatus() == StatusAgendamento.REALIZADO && agendamento.getAssinatura() != null) {
            assinaturaService.registrarSessao(agendamento.getAssinatura().getId());
        }

        // Avaliar direito a reposição ao cancelar
        if (dto.getStatus() == StatusAgendamento.CANCELADO) {
            avaliarDireitoReposicao(agendamento);
        }

        Agendamento saved = agendamentoRepository.save(agendamento);

        // Se cancelado, remover evento do Google Calendar
        if (dto.getStatus() == StatusAgendamento.CANCELADO) {
            googleCalendarService.ifPresent(g -> g.deleteEvent(saved));
        }

        return saved;
    }

    @Transactional
    public void deleteAgendamento(UUID id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento", id));
        googleCalendarService.ifPresent(g -> g.deleteEvent(agendamento));
        agendamento.setAtivo(false);
        agendamentoRepository.save(agendamento);
    }

    public List<LocalDateTime> getAvailableSlots(UUID profissionalId, LocalDate data,
                                                    Integer duracaoMinutos, int capacidadeMaxima) {
        DayOfWeek diaSemana = data.getDayOfWeek();
        List<HorarioDisponivel> horarios = horarioDisponivelRepository
                .findByProfissionalIdAndDiaSemana(profissionalId, diaSemana);

        if (horarios.isEmpty()) {
            return List.of();
        }

        // Buscar agendamentos existentes do dia (AGENDADO ou CONFIRMADO)
        LocalDateTime startOfDay = data.atStartOfDay();
        LocalDateTime endOfDay = data.atTime(LocalTime.MAX);
        List<Agendamento> existingAppointments = agendamentoRepository
                .findByProfissionalIdAndStatusInAndDataHoraBetween(
                        profissionalId,
                        List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO),
                        startOfDay, endOfDay
                );

        // Gerar slots disponíveis
        List<LocalDateTime> slots = new ArrayList<>();
        for (HorarioDisponivel horario : horarios) {
            LocalTime current = horario.getHoraInicio();
            while (!current.plusMinutes(duracaoMinutos).isAfter(horario.getHoraFim())) {
                LocalDateTime slotDateTime = data.atTime(current);
                LocalDateTime slotEnd = slotDateTime.plusMinutes(duracaoMinutos);

                long overlappingCount = existingAppointments.stream().filter(a -> {
                    LocalDateTime existingStart = a.getDataHora();
                    LocalDateTime existingEnd = existingStart.plusMinutes(a.getDuracaoMinutos());
                    return slotDateTime.isBefore(existingEnd) && slotEnd.isAfter(existingStart);
                }).count();

                if (overlappingCount < capacidadeMaxima) {
                    slots.add(slotDateTime);
                }

                current = current.plusMinutes(duracaoMinutos);
            }
        }

        return slots;
    }

    @Transactional
    public Agendamento criarReposicao(ReposicaoRequestDTO dto) {
        Agendamento origem = agendamentoRepository.findById(dto.getAgendamentoOrigemId())
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento de origem", dto.getAgendamentoOrigemId()));

        // Validar que o agendamento de origem está cancelado e tem direito a reposição
        if (origem.getStatus() != StatusAgendamento.CANCELADO) {
            throw new BusinessException("O agendamento de origem não está cancelado");
        }

        if (origem.getDireitoReposicao() == null || !origem.getDireitoReposicao()) {
            throw new BusinessException("O agendamento de origem não possui direito a reposição");
        }

        // Validar que o agendamento de origem não é uma reposição
        if (origem.getTipoAgendamento() == TipoAgendamento.REPOSICAO) {
            throw new BusinessException("Não é possível criar reposição de uma reposição");
        }

        // Validar que não expirou (20 dias)
        if (origem.getDataLimiteReposicao() != null && LocalDateTime.now().isAfter(origem.getDataLimiteReposicao())) {
            throw new BusinessException("O prazo para reposição expirou em " + origem.getDataLimiteReposicao());
        }

        // Validar que não existe reposição já criada para esta origem
        boolean existeReposicao = agendamentoRepository.existsByReposicaoOrigemIdAndStatusIn(
                origem.getId(),
                List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO, StatusAgendamento.REALIZADO)
        );
        if (existeReposicao) {
            throw new BusinessException("Já existe uma reposição para este agendamento de origem");
        }

        // Validar limite de 2 reposições por mês
        LocalDateTime inicioMes = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fimMes = inicioMes.plusMonths(1).minusNanos(1);
        long reposicoesNoMes = agendamentoRepository.countReposicoesNoMes(
                origem.getPaciente().getId(), inicioMes, fimMes);
        if (reposicoesNoMes >= LIMITE_REPOSICOES_MES) {
            throw new BusinessException("Limite de " + LIMITE_REPOSICOES_MES + " reposições por mês atingido");
        }

        // Buscar profissional
        Profissional profissional = profissionalRepository.findById(dto.getProfissionalId())
                .orElseThrow(() -> new ResourceNotFoundException("Profissional", dto.getProfissionalId()));

        // Validar que o profissional atende a atividade do serviço
        validarProfissionalAtendeAtividade(profissional, origem.getServico());

        // Default duração
        Integer duracao = dto.getDuracaoMinutos() != null ? dto.getDuracaoMinutos() : origem.getDuracaoMinutos();

        // Validar horário disponível e conflitos
        validarDentroDoHorarioDisponivel(profissional.getId(), dto.getDataHora(), duracao);
        int capacidade = origem.getServico().getAtividade().getCapacidadeMaxima() != null
                ? origem.getServico().getAtividade().getCapacidadeMaxima() : 1;
        validarConflitoHorario(profissional.getId(), dto.getDataHora(), duracao, capacidade);

        // Criar o agendamento de reposição
        Agendamento reposicao = new Agendamento();
        reposicao.setPaciente(origem.getPaciente());
        reposicao.setProfissional(profissional);
        reposicao.setServico(origem.getServico());
        reposicao.setAssinatura(origem.getAssinatura());
        reposicao.setDataHora(dto.getDataHora());
        reposicao.setDuracaoMinutos(duracao);
        reposicao.setObservacoes(dto.getObservacoes());
        reposicao.setTipoAgendamento(TipoAgendamento.REPOSICAO);
        reposicao.setReposicaoOrigemId(origem.getId());
        reposicao.setStatus(StatusAgendamento.AGENDADO);

        Agendamento saved = agendamentoRepository.save(reposicao);
        googleCalendarService.ifPresent(g -> g.createEvent(saved));
        return saved;
    }

    public ReposicaoInfoDTO getReposicoesInfo(UUID pacienteId) {
        // Verificar que o paciente existe
        patientRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", pacienteId));

        // Contar reposições usadas no mês corrente
        LocalDateTime inicioMes = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fimMes = inicioMes.plusMonths(1).minusNanos(1);
        long reposicoesUsadas = agendamentoRepository.countReposicoesNoMes(pacienteId, inicioMes, fimMes);

        // Buscar agendamentos com direito a reposição ainda válidos
        List<UUID> agendamentosComDireito = agendamentoRepository
                .findByPacienteIdAndDireitoReposicaoTrueAndDataLimiteReposicaoAfter(pacienteId, LocalDateTime.now())
                .stream()
                .filter(a -> {
                    // Filtrar os que não têm reposição já criada
                    return !agendamentoRepository.existsByReposicaoOrigemIdAndStatusIn(
                            a.getId(),
                            List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO, StatusAgendamento.REALIZADO)
                    );
                })
                .map(Agendamento::getId)
                .collect(Collectors.toList());

        return new ReposicaoInfoDTO(reposicoesUsadas, LIMITE_REPOSICOES_MES, agendamentosComDireito);
    }

    // --- Validações privadas ---

    private void validarProfissionalAtendeAtividade(Profissional profissional, Servico servico) {
        Set<Atividade> atividades = profissional.getAtividades();
        boolean atende = atividades.stream()
                .anyMatch(a -> a.getId().equals(servico.getAtividade().getId()));
        if (!atende) {
            throw new BusinessException(
                    "O profissional " + profissional.getNome() +
                    " não atende a atividade " + servico.getAtividade().getNome()
            );
        }
    }

    private void validarAssinatura(Assinatura assinatura) {
        if (assinatura.getStatus() != StatusAssinatura.ATIVO) {
            throw new BusinessException("Assinatura não está ativa (status: " + assinatura.getStatus() + ")");
        }
        if (assinatura.getSessoesRealizadas() >= assinatura.getSessoesContratadas()) {
            throw new BusinessException("Assinatura não possui sessões restantes");
        }
    }

    private void validarDentroDoHorarioDisponivel(UUID profissionalId, LocalDateTime dataHora, Integer duracaoMinutos) {
        DayOfWeek diaSemana = dataHora.getDayOfWeek();
        LocalTime horaInicio = dataHora.toLocalTime();
        LocalTime horaFim = horaInicio.plusMinutes(duracaoMinutos);

        List<HorarioDisponivel> horarios = horarioDisponivelRepository
                .findByProfissionalIdAndDiaSemana(profissionalId, diaSemana);

        boolean dentroDoHorario = horarios.stream().anyMatch(h ->
                !horaInicio.isBefore(h.getHoraInicio()) && !horaFim.isAfter(h.getHoraFim())
        );

        if (!dentroDoHorario) {
            throw new BusinessException("O horário solicitado está fora da disponibilidade do profissional");
        }
    }

    private void validarConflitoHorario(UUID profissionalId, LocalDateTime dataHora,
                                          Integer duracaoMinutos, int capacidadeMaxima) {
        LocalDateTime fim = dataHora.plusMinutes(duracaoMinutos);
        LocalDateTime startOfDay = dataHora.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = dataHora.toLocalDate().atTime(LocalTime.MAX);

        List<Agendamento> existing = agendamentoRepository
                .findByProfissionalIdAndStatusInAndDataHoraBetween(
                        profissionalId,
                        List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO),
                        startOfDay, endOfDay
                );

        long overlappingCount = existing.stream().filter(a -> {
            LocalDateTime existingStart = a.getDataHora();
            LocalDateTime existingEnd = existingStart.plusMinutes(a.getDuracaoMinutos());
            return dataHora.isBefore(existingEnd) && fim.isAfter(existingStart);
        }).count();

        if (overlappingCount >= capacidadeMaxima) {
            throw new BusinessException("Conflito de horário: capacidade máxima (" + capacidadeMaxima +
                    ") atingida neste período para o profissional");
        }
    }

    private void validarConflitoHorarioExcluindo(UUID profissionalId, LocalDateTime dataHora,
                                                   Integer duracaoMinutos, UUID agendamentoIdExcluir,
                                                   int capacidadeMaxima) {
        LocalDateTime fim = dataHora.plusMinutes(duracaoMinutos);
        LocalDateTime startOfDay = dataHora.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = dataHora.toLocalDate().atTime(LocalTime.MAX);

        List<Agendamento> existing = agendamentoRepository
                .findByProfissionalIdAndStatusInAndDataHoraBetween(
                        profissionalId,
                        List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO),
                        startOfDay, endOfDay
                );

        long overlappingCount = existing.stream()
                .filter(a -> !a.getId().equals(agendamentoIdExcluir))
                .filter(a -> {
                    LocalDateTime existingStart = a.getDataHora();
                    LocalDateTime existingEnd = existingStart.plusMinutes(a.getDuracaoMinutos());
                    return dataHora.isBefore(existingEnd) && fim.isAfter(existingStart);
                }).count();

        if (overlappingCount >= capacidadeMaxima) {
            throw new BusinessException("Conflito de horário: capacidade máxima (" + capacidadeMaxima +
                    ") atingida neste período para o profissional");
        }
    }

    private void validarTransicaoStatus(StatusAgendamento atual, StatusAgendamento novo) {
        if (isStatusFinal(atual)) {
            throw new BusinessException("Não é possível alterar um agendamento com status " + atual);
        }

        boolean transicaoValida = switch (atual) {
            case AGENDADO -> novo == StatusAgendamento.CONFIRMADO || novo == StatusAgendamento.CANCELADO || novo == StatusAgendamento.NAO_COMPARECEU;
            case CONFIRMADO -> novo == StatusAgendamento.REALIZADO ||
                               novo == StatusAgendamento.CANCELADO ||
                               novo == StatusAgendamento.NAO_COMPARECEU;
            default -> false;
        };

        if (!transicaoValida) {
            throw new BusinessException(
                    "Transição de status inválida: " + atual + " → " + novo
            );
        }
    }

    private boolean isStatusFinal(StatusAgendamento status) {
        return status == StatusAgendamento.REALIZADO ||
               status == StatusAgendamento.CANCELADO ||
               status == StatusAgendamento.NAO_COMPARECEU;
    }

    private void avaliarDireitoReposicao(Agendamento agendamento) {
        // Verificar se é Pilates (nome da atividade contém "Pilates")
        String nomeAtividade = agendamento.getServico().getAtividade().getNome();
        boolean isPilates = nomeAtividade != null && nomeAtividade.toLowerCase().contains("pilates");

        if (!isPilates) {
            agendamento.setDireitoReposicao(false);
            return;
        }

        // Não gerar reposição de uma reposição
        if (agendamento.getTipoAgendamento() == TipoAgendamento.REPOSICAO) {
            agendamento.setDireitoReposicao(false);
            return;
        }

        // Verificar regra de 3 horas de antecedência
        boolean cancelouComAntecedencia = LocalDateTime.now().isBefore(
                agendamento.getDataHora().minusHours(HORAS_ANTECEDENCIA_CANCELAMENTO));

        if (!cancelouComAntecedencia) {
            agendamento.setDireitoReposicao(false);
            return;
        }

        // Verificar se o dia do agendamento é feriado
        boolean isFeriado = feriadoRepository.isFeriado(agendamento.getDataHora().toLocalDate());
        if (isFeriado) {
            agendamento.setDireitoReposicao(false);
            return;
        }

        // Conceder direito a reposição
        agendamento.setDireitoReposicao(true);
        agendamento.setDataLimiteReposicao(LocalDateTime.now().plusDays(DIAS_VALIDADE_REPOSICAO));
    }
}
