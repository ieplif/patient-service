package br.com.clinicahumaniza.patient_service.service;

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class AgendamentoService {

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
    public AgendamentoService(
            AgendamentoRepository agendamentoRepository,
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

    // noRollbackFor BusinessException: todas as BusinessException aqui (duração ausente,
    // conflito de horário, assinatura inválida) são lançadas ANTES de qualquer escrita no
    // banco — não há nada para desfazer. Sem isto, quando este método participa de uma
    // transação maior (ex.: renovação/recorrência criando vários agendamentos em lote) e uma
    // data conflita, o proxy marcaria a transação inteira como rollback-only, fazendo o lote
    // todo falhar com "Transaction silently rolled back" mesmo capturando a exceção no laço.
    @Transactional(noRollbackFor = BusinessException.class)
    public Agendamento createAgendamento(AgendamentoRequestDTO dto) {
        Patient paciente = patientRepository
                .findById(dto.getPacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", dto.getPacienteId()));

        // Carrega serviço primeiro (sempre obrigatório); profissional só se for informado
        Servico servico = servicoRepository
                .findById(dto.getServicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Serviço", dto.getServicoId()));

        Profissional profissional = null;
        if (dto.getProfissionalId() != null) {
            profissional = profissionalRepository
                    .findById(dto.getProfissionalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Profissional", dto.getProfissionalId()));
            // Validar que o profissional atende a atividade do serviço
            validarProfissionalAtendeAtividade(profissional, servico);
        }

        // Validar assinatura se fornecida
        Assinatura assinatura = null;
        if (dto.getAssinaturaId() != null) {
            assinatura = assinaturaRepository
                    .findById(dto.getAssinaturaId())
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

        // Validações que dependem de profissional só rodam se ele foi informado
        if (profissional != null) {
            // Para registros retroativos (dataHora no passado), pula a validação de
            // HorarioDisponivel — o profissional pode ter mudado de turnos desde então.
            // A validação de capacidade da turma continua valendo para integridade do histórico.
            boolean ehRetroativo =
                    dto.getDataHora() != null && dto.getDataHora().isBefore(LocalDateTime.now());
            if (!ehRetroativo) {
                validarDentroDoHorarioDisponivel(profissional.getId(), dto.getDataHora(), dto.getDuracaoMinutos());
            }
            int capacidade = servico.getAtividade().getCapacidadeMaxima() != null
                    ? servico.getAtividade().getCapacidadeMaxima()
                    : 1;
            validarConflitoHorario(profissional.getId(), dto.getDataHora(), dto.getDuracaoMinutos(), capacidade);
        }

        Agendamento agendamento = agendamentoMapper.toEntity(dto, paciente, profissional, servico, assinatura);
        agendamento.setStatus(StatusAgendamento.AGENDADO);

        Agendamento saved = agendamentoRepository.save(agendamento);
        if (saved.getProfissional() != null) {
            googleCalendarService.ifPresent(g -> g.createEvent(saved));
        }
        return saved;
    }

    public Agendamento getAgendamentoById(UUID id) {
        return agendamentoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Agendamento", id));
    }

    public Page<Agendamento> getAllAgendamentos(
            StatusAgendamento status,
            UUID pacienteId,
            String pacienteNome,
            UUID profissionalId,
            UUID assinaturaId,
            LocalDateTime dataInicio,
            LocalDateTime dataFim,
            Pageable pageable) {
        Specification<Agendamento> spec = Specification.allOf(
                AgendamentoSpecification.hasStatus(status),
                AgendamentoSpecification.hasPaciente(pacienteId),
                AgendamentoSpecification.hasPacienteNome(pacienteNome),
                AgendamentoSpecification.hasProfissional(profissionalId),
                AgendamentoSpecification.hasAssinatura(assinaturaId),
                AgendamentoSpecification.betweenDatas(dataInicio, dataFim));
        return agendamentoRepository.findAll(spec, pageable);
    }

    public byte[] exportCsv(LocalDate inicio, LocalDate fim, StatusAgendamento status) {
        LocalDateTime dtInicio = inicio != null ? inicio.atStartOfDay() : null;
        LocalDateTime dtFim = fim != null ? fim.atTime(LocalTime.MAX) : null;
        Specification<Agendamento> spec = Specification.allOf(
                AgendamentoSpecification.hasStatus(status), AgendamentoSpecification.betweenDatas(dtInicio, dtFim));
        List<Agendamento> agendamentos = agendamentoRepository.findAll(spec);
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Paciente,Profissional,Servico,DataHora,Status,DuracaoMinutos\n");
        for (Agendamento a : agendamentos) {
            String nomeProfissional =
                    a.getProfissional() != null ? a.getProfissional().getNome() : "Sem profissional";
            csv.append(String.join(
                            ",",
                            a.getId().toString(),
                            escapeCsv(a.getPaciente().getNomeCompleto()),
                            escapeCsv(nomeProfissional),
                            escapeCsv(a.getServico().getAtividade().getNome() + " - "
                                    + a.getServico().getPlano().getNome()),
                            a.getDataHora() != null ? a.getDataHora().toString() : "",
                            a.getStatus().name(),
                            a.getDuracaoMinutos() != null
                                    ? a.getDuracaoMinutos().toString()
                                    : ""))
                    .append("\n");
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
        Agendamento agendamento =
                agendamentoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Agendamento", id));

        if (isStatusFinal(agendamento.getStatus())) {
            throw new BusinessException("Não é possível alterar um agendamento com status " + agendamento.getStatus());
        }

        // Resolver o profissional efetivo da sessão.
        // alterarProfissional=true aplica o novo (pode ser null = "Sem profissional");
        // caso contrário mantém o atual.
        Profissional profissionalEfetivo = agendamento.getProfissional();
        boolean trocouProfissional = Boolean.TRUE.equals(dto.getAlterarProfissional());
        if (trocouProfissional) {
            if (dto.getProfissionalId() != null) {
                profissionalEfetivo = profissionalRepository
                        .findById(dto.getProfissionalId())
                        .orElseThrow(() -> new ResourceNotFoundException("Profissional", dto.getProfissionalId()));
                validarProfissionalAtendeAtividade(profissionalEfetivo, agendamento.getServico());
            } else {
                profissionalEfetivo = null;
            }
        }

        // Se mudou dataHora, duração ou profissional, revalidar conflitos e disponibilidade
        LocalDateTime novaDataHora = dto.getDataHora() != null ? dto.getDataHora() : agendamento.getDataHora();
        Integer novaDuracao =
                dto.getDuracaoMinutos() != null ? dto.getDuracaoMinutos() : agendamento.getDuracaoMinutos();

        boolean precisaRevalidar = dto.getDataHora() != null || dto.getDuracaoMinutos() != null || trocouProfissional;
        if (precisaRevalidar && profissionalEfetivo != null) {
            // Pula validação de HorarioDisponivel para sessões retroativas
            boolean ehRetroativo = novaDataHora != null && novaDataHora.isBefore(LocalDateTime.now());
            if (!ehRetroativo) {
                validarDentroDoHorarioDisponivel(profissionalEfetivo.getId(), novaDataHora, novaDuracao);
            }
            int capacidade = agendamento.getServico().getAtividade().getCapacidadeMaxima() != null
                    ? agendamento.getServico().getAtividade().getCapacidadeMaxima()
                    : 1;
            validarConflitoHorarioExcluindo(
                    profissionalEfetivo.getId(), novaDataHora, novaDuracao, agendamento.getId(), capacidade);
        }

        if (trocouProfissional) {
            agendamento.setProfissional(profissionalEfetivo);
        }
        agendamentoMapper.updateEntityFromDto(dto, agendamento);
        Agendamento saved = agendamentoRepository.save(agendamento);
        if (saved.getProfissional() != null) {
            googleCalendarService.ifPresent(g -> g.updateEvent(saved));
        }
        return saved;
    }

    @Transactional
    public Agendamento updateStatus(UUID id, AgendamentoStatusDTO dto) {
        Agendamento agendamento =
                agendamentoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Agendamento", id));

        StatusAgendamento statusAnterior = agendamento.getStatus();
        StatusAgendamento statusNovo = dto.getStatus();

        validarTransicaoStatus(statusAnterior, statusNovo);

        agendamento.setStatus(statusNovo);

        // Salvar motivo de cancelamento se fornecido
        if (dto.getMotivoCancelamento() != null) {
            agendamento.setMotivoCancelamento(dto.getMotivoCancelamento());
        }

        // === Efeitos colaterais nas sessões da assinatura ===
        // Marcou como REALIZADO agora (não era antes) → registrar sessão
        if (statusNovo == StatusAgendamento.REALIZADO
                && statusAnterior != StatusAgendamento.REALIZADO
                && agendamento.getAssinatura() != null) {
            assinaturaService.registrarSessao(agendamento.getAssinatura().getId());
        }
        // Saiu de REALIZADO para outro status (correção de engano) → reverter sessão
        if (statusAnterior == StatusAgendamento.REALIZADO
                && statusNovo != StatusAgendamento.REALIZADO
                && agendamento.getAssinatura() != null) {
            assinaturaService.reverterSessao(agendamento.getAssinatura().getId());
        }

        // === Direito a reposição ===
        if (statusNovo == StatusAgendamento.CANCELADO) {
            avaliarDireitoReposicao(agendamento, dto.getGerarReposicao());
        }
        // Saiu de CANCELADO → limpar direito a reposição (não faz mais sentido)
        if (statusAnterior == StatusAgendamento.CANCELADO && statusNovo != StatusAgendamento.CANCELADO) {
            agendamento.setDireitoReposicao(false);
            agendamento.setMotivoCancelamento(null);
        }

        Agendamento saved = agendamentoRepository.save(agendamento);

        // === Google Calendar ===
        // Entrou em CANCELADO agora → remover evento
        if (statusNovo == StatusAgendamento.CANCELADO && statusAnterior != StatusAgendamento.CANCELADO) {
            googleCalendarService.ifPresent(g -> g.deleteEvent(saved));
        }
        // Saiu de CANCELADO → recriar evento (se tem profissional)
        if (statusAnterior == StatusAgendamento.CANCELADO
                && statusNovo != StatusAgendamento.CANCELADO
                && saved.getProfissional() != null) {
            googleCalendarService.ifPresent(g -> g.createEvent(saved));
        }

        return saved;
    }

    @Transactional
    public void deleteAgendamento(UUID id) {
        Agendamento agendamento =
                agendamentoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Agendamento", id));
        if (agendamento.getProfissional() != null) {
            googleCalendarService.ifPresent(g -> g.deleteEvent(agendamento));
        }
        agendamento.setAtivo(false);
        agendamentoRepository.save(agendamento);
    }

    public List<LocalDateTime> getAvailableSlots(
            UUID profissionalId, LocalDate data, Integer duracaoMinutos, int capacidadeMaxima) {
        DayOfWeek diaSemana = data.getDayOfWeek();
        List<HorarioDisponivel> horarios =
                horarioDisponivelRepository.findByProfissionalIdAndDiaSemana(profissionalId, diaSemana);

        if (horarios.isEmpty()) {
            return List.of();
        }

        // Buscar agendamentos existentes do dia (AGENDADO ou CONFIRMADO)
        LocalDateTime startOfDay = data.atStartOfDay();
        LocalDateTime endOfDay = data.atTime(LocalTime.MAX);
        List<Agendamento> existingAppointments =
                agendamentoRepository.findByProfissionalIdAndStatusInAndDataHoraBetween(
                        profissionalId,
                        List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO),
                        startOfDay,
                        endOfDay);

        // Gerar slots disponíveis
        List<LocalDateTime> slots = new ArrayList<>();
        for (HorarioDisponivel horario : horarios) {
            LocalTime current = horario.getHoraInicio();
            while (!current.plusMinutes(duracaoMinutos).isAfter(horario.getHoraFim())) {
                LocalDateTime slotDateTime = data.atTime(current);
                LocalDateTime slotEnd = slotDateTime.plusMinutes(duracaoMinutos);

                long overlappingCount = existingAppointments.stream()
                        .filter(a -> {
                            LocalDateTime existingStart = a.getDataHora();
                            LocalDateTime existingEnd = existingStart.plusMinutes(a.getDuracaoMinutos());
                            return slotDateTime.isBefore(existingEnd) && slotEnd.isAfter(existingStart);
                        })
                        .count();

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
        Agendamento origem = agendamentoRepository
                .findById(dto.getAgendamentoOrigemId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Agendamento de origem", dto.getAgendamentoOrigemId()));

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

        // Validar que não existe reposição já criada para esta origem
        boolean existeReposicao = agendamentoRepository.existsByReposicaoOrigemIdAndStatusIn(
                origem.getId(),
                List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO, StatusAgendamento.REALIZADO));
        if (existeReposicao) {
            throw new BusinessException("Já existe uma reposição para este agendamento de origem");
        }

        // Buscar profissional (opcional — pode ficar nulo, ex.: Pilates onde varia por dia)
        Profissional profissional = null;
        if (dto.getProfissionalId() != null) {
            profissional = profissionalRepository
                    .findById(dto.getProfissionalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Profissional", dto.getProfissionalId()));
            validarProfissionalAtendeAtividade(profissional, origem.getServico());
        }

        // Default duração
        Integer duracao = dto.getDuracaoMinutos() != null ? dto.getDuracaoMinutos() : origem.getDuracaoMinutos();

        // Validar horário disponível e conflitos só se houver profissional
        if (profissional != null) {
            validarDentroDoHorarioDisponivel(profissional.getId(), dto.getDataHora(), duracao);
            int capacidade = origem.getServico().getAtividade().getCapacidadeMaxima() != null
                    ? origem.getServico().getAtividade().getCapacidadeMaxima()
                    : 1;
            validarConflitoHorario(profissional.getId(), dto.getDataHora(), duracao, capacidade);
        }

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
        if (saved.getProfissional() != null) {
            googleCalendarService.ifPresent(g -> g.createEvent(saved));
        }
        return saved;
    }

    public ReposicaoInfoDTO getReposicoesInfo(UUID pacienteId) {
        // Verificar que o paciente existe
        patientRepository.findById(pacienteId).orElseThrow(() -> new ResourceNotFoundException("Paciente", pacienteId));

        // Buscar agendamentos com direito a reposição sem reposição já criada
        List<UUID> agendamentosComDireito =
                agendamentoRepository.findByPacienteIdAndDireitoReposicaoTrue(pacienteId).stream()
                        .filter(a -> !agendamentoRepository.existsByReposicaoOrigemIdAndStatusIn(
                                a.getId(),
                                List.of(
                                        StatusAgendamento.AGENDADO,
                                        StatusAgendamento.CONFIRMADO,
                                        StatusAgendamento.REALIZADO)))
                        .map(Agendamento::getId)
                        .collect(Collectors.toList());

        // Sem limite mensal — retorna 0 para indicar sem restrição
        return new ReposicaoInfoDTO(0L, 0, agendamentosComDireito);
    }

    // --- Validações privadas ---

    private void validarProfissionalAtendeAtividade(Profissional profissional, Servico servico) {
        Set<Atividade> atividades = profissional.getAtividades();
        boolean atende = atividades.stream()
                .anyMatch(a -> a.getId().equals(servico.getAtividade().getId()));
        if (!atende) {
            throw new BusinessException("O profissional " + profissional.getNome() + " não atende a atividade "
                    + servico.getAtividade().getNome());
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

        List<HorarioDisponivel> horarios =
                horarioDisponivelRepository.findByProfissionalIdAndDiaSemana(profissionalId, diaSemana);

        boolean dentroDoHorario = horarios.stream()
                .anyMatch(h -> !horaInicio.isBefore(h.getHoraInicio()) && !horaFim.isAfter(h.getHoraFim()));

        if (!dentroDoHorario) {
            throw new BusinessException("O horário solicitado está fora da disponibilidade do profissional");
        }
    }

    private void validarConflitoHorario(
            UUID profissionalId, LocalDateTime dataHora, Integer duracaoMinutos, int capacidadeMaxima) {
        LocalDateTime fim = dataHora.plusMinutes(duracaoMinutos);
        LocalDateTime startOfDay = dataHora.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = dataHora.toLocalDate().atTime(LocalTime.MAX);

        List<Agendamento> existing = agendamentoRepository.findByProfissionalIdAndStatusInAndDataHoraBetween(
                profissionalId,
                List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO),
                startOfDay,
                endOfDay);

        long overlappingCount = existing.stream()
                .filter(a -> {
                    LocalDateTime existingStart = a.getDataHora();
                    LocalDateTime existingEnd = existingStart.plusMinutes(a.getDuracaoMinutos());
                    return dataHora.isBefore(existingEnd) && fim.isAfter(existingStart);
                })
                .count();

        if (overlappingCount >= capacidadeMaxima) {
            throw new BusinessException("Conflito de horário: capacidade máxima (" + capacidadeMaxima
                    + ") atingida neste período para o profissional");
        }
    }

    private void validarConflitoHorarioExcluindo(
            UUID profissionalId,
            LocalDateTime dataHora,
            Integer duracaoMinutos,
            UUID agendamentoIdExcluir,
            int capacidadeMaxima) {
        LocalDateTime fim = dataHora.plusMinutes(duracaoMinutos);
        LocalDateTime startOfDay = dataHora.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = dataHora.toLocalDate().atTime(LocalTime.MAX);

        List<Agendamento> existing = agendamentoRepository.findByProfissionalIdAndStatusInAndDataHoraBetween(
                profissionalId,
                List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO),
                startOfDay,
                endOfDay);

        long overlappingCount = existing.stream()
                .filter(a -> !a.getId().equals(agendamentoIdExcluir))
                .filter(a -> {
                    LocalDateTime existingStart = a.getDataHora();
                    LocalDateTime existingEnd = existingStart.plusMinutes(a.getDuracaoMinutos());
                    return dataHora.isBefore(existingEnd) && fim.isAfter(existingStart);
                })
                .count();

        if (overlappingCount >= capacidadeMaxima) {
            throw new BusinessException("Conflito de horário: capacidade máxima (" + capacidadeMaxima
                    + ") atingida neste período para o profissional");
        }
    }

    /**
     * Política de transições — qualquer status pode ir para qualquer outro,
     * exceto "no-op" (mesmo status). Os efeitos colaterais (contagem de sessão
     * na assinatura, Google Calendar, motivo de cancelamento) são tratados em
     * updateStatus() comparando statusAnterior x statusNovo.
     *
     * Status "finais" (REALIZADO, CANCELADO, NAO_COMPARECEU) podem ser revertidos
     * para corrigir engano da recepção.
     */
    private void validarTransicaoStatus(StatusAgendamento atual, StatusAgendamento novo) {
        if (atual == novo) {
            throw new BusinessException("O agendamento já está com status " + atual);
        }
    }

    /**
     * Status considerados "fechados" — usados pelo updateAgendamento para bloquear
     * edição de dados sensíveis (data, profissional, serviço) de uma sessão que
     * já encerrou. O status em si ainda pode ser revertido via updateStatus.
     */
    private boolean isStatusFinal(StatusAgendamento status) {
        return status == StatusAgendamento.REALIZADO
                || status == StatusAgendamento.CANCELADO
                || status == StatusAgendamento.NAO_COMPARECEU;
    }

    /**
     * Decide se o cancelamento gera direito a reposição.
     *
     * Quando {@code overrideManual} é informado (não-nulo), respeita a escolha
     * da recepção (caso a caso). Quando é {@code null}, aplica a regra padrão:
     * não-reposição + não-feriado (vale para todos os serviços).
     *
     * Nota: a antiga regra de "3h de antecedência" foi removida — a clínica
     * decide caso a caso pela UI.
     */
    private void avaliarDireitoReposicao(Agendamento agendamento, Boolean overrideManual) {
        // Override manual da recepção tem prioridade (qualquer caso)
        if (overrideManual != null) {
            agendamento.setDireitoReposicao(overrideManual);
            return;
        }

        // Não gerar reposição de uma reposição
        if (agendamento.getTipoAgendamento() == TipoAgendamento.REPOSICAO) {
            agendamento.setDireitoReposicao(false);
            return;
        }

        // Verificar se o dia do agendamento é feriado
        boolean isFeriado =
                feriadoRepository.isFeriado(agendamento.getDataHora().toLocalDate());
        if (isFeriado) {
            agendamento.setDireitoReposicao(false);
            return;
        }

        // Conceder direito a reposição (vale para todos os serviços)
        agendamento.setDireitoReposicao(true);
    }
}
