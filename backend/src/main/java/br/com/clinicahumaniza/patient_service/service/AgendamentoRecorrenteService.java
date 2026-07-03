package br.com.clinicahumaniza.patient_service.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.clinicahumaniza.patient_service.dto.*;
import br.com.clinicahumaniza.patient_service.exception.BusinessException;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.AgendamentoMapper;
import br.com.clinicahumaniza.patient_service.model.*;
import br.com.clinicahumaniza.patient_service.repository.*;

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
    public AgendamentoRecorrenteService(
            AgendamentoService agendamentoService,
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

    public AgendamentoRecorrenteResponseDTO createRecorrente(AgendamentoRecorrenteRequestDTO dto) {
        if (dto.getTotalSessoes() == null && dto.getDataFim() == null) {
            throw new BusinessException("É necessário informar totalSessoes ou dataFim");
        }

        if (dto.getTotalSessoes() != null && dto.getTotalSessoes() > MAX_DATAS) {
            throw new BusinessException("Total de sessões não pode exceder " + MAX_DATAS);
        }

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
            boolean atende = profissional.getAtividades().stream()
                    .anyMatch(a -> a.getId().equals(servico.getAtividade().getId()));
            if (!atende) {
                throw new BusinessException("O profissional " + profissional.getNome() + " não atende a atividade "
                        + servico.getAtividade().getNome());
            }
        }

        Assinatura assinatura = null;
        if (dto.getAssinaturaId() != null) {
            assinatura = assinaturaRepository
                    .findById(dto.getAssinaturaId())
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

        // Determinar data de início referência.
        // Prioridade: dto.dataInicio (explícito — usado pela renovação automática
        // pra forçar geração no próximo período) > assinatura.dataInicio (mantém
        // a referência original ao criar a 1ª vez) > amanhã (fallback).
        LocalDate dataInicioRef = null;
        if (dto.getDataInicio() != null) {
            dataInicioRef = dto.getDataInicio();
        } else if (assinatura != null && assinatura.getDataInicio() != null) {
            dataInicioRef = assinatura.getDataInicio();
        }

        // Gerar datas candidatas
        List<LocalDateTime> datasCandidatas = gerarDatas(
                dto.getFrequencia(),
                dto.getDiasSemana(),
                dto.getHoraInicio(),
                dto.getTotalSessoes(),
                dto.getDataFim(),
                dataInicioRef);

        // Salvar entidade AgendamentoRecorrente
        AgendamentoRecorrente recorrente = new AgendamentoRecorrente();
        recorrente.setPaciente(paciente);
        recorrente.setProfissional(profissional);
        recorrente.setServico(servico);
        recorrente.setAssinatura(assinatura);
        recorrente.setFrequencia(dto.getFrequencia());
        recorrente.setDiasSemana(
                dto.getDiasSemana().stream().map(DayOfWeek::name).collect(Collectors.joining(",")));
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

    List<LocalDateTime> gerarDatas(
            FrequenciaRecorrencia frequencia,
            List<DayOfWeek> diasSemana,
            LocalTime horaInicio,
            Integer totalSessoes,
            LocalDate dataFim,
            LocalDate dataInicioRef) {
        List<LocalDateTime> datas = new ArrayList<>();
        // Default: amanhã. Se o caller informa uma data (mesmo retroativa), respeita —
        // útil para registrar histórico ao popular o sistema com casos do dia a dia.
        LocalDate atual =
                dataInicioRef != null ? dataInicioRef : LocalDate.now().plusDays(1);

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
                // Mensal: usa o mesmo dia do mês, ignora diasSemana
                datas.add(atual.atTime(horaInicio));

                if (totalSessoes != null && datas.size() >= totalSessoes) break;
                if (dataFim != null && atual.plusMonths(1).isAfter(dataFim)) break;

                atual = atual.plusMonths(1);
            } else {
                // Semanal ou Quinzenal
                if (diasSemana.contains(atual.getDayOfWeek())) {
                    datas.add(atual.atTime(horaInicio));

                    if (totalSessoes != null && datas.size() >= totalSessoes) break;

                    // Se é o último dia da semana no ciclo, avança o ciclo
                    DayOfWeek ultimoDiaCiclo =
                            diasSemana.stream().max(DayOfWeek::compareTo).orElse(atual.getDayOfWeek());

                    if (atual.getDayOfWeek() == ultimoDiaCiclo) {
                        int semanas = frequencia == FrequenciaRecorrencia.QUINZENAL ? 2 : 1;
                        // Avança para o início da próxima semana do ciclo
                        DayOfWeek primeiroDiaCiclo =
                                diasSemana.stream().min(DayOfWeek::compareTo).orElse(atual.getDayOfWeek());
                        int diasAteProximoCiclo =
                                (semanas * 7) - (atual.getDayOfWeek().getValue() - primeiroDiaCiclo.getValue());
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
        Agendamento agendamento = agendamentoRepository
                .findById(agendamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento", agendamentoId));

        // Direito a reposição é decisão humana caso a caso (checkbox no cancelamento
        // individual da tela). Cancelamento em lote/administrativo NUNCA concede
        // automaticamente — senão cada sessão futura viraria crédito silencioso.
        if (!cancelarFuturos) {
            agendamentoService.updateStatus(
                    agendamentoId, new AgendamentoStatusDTO(StatusAgendamento.CANCELADO, null, false));
            return List.of(agendamentoMapper.toResponseDTO(agendamento));
        }

        // Cancelar futuros
        AgendamentoRecorrente recorrente = agendamento.getAgendamentoRecorrente();
        if (recorrente == null) {
            throw new BusinessException("Este agendamento não faz parte de uma recorrência");
        }

        List<StatusAgendamento> statusesNaoFinais = List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO);

        List<Agendamento> futuros =
                agendamentoRepository.findByAgendamentoRecorrenteIdAndDataHoraGreaterThanEqualAndStatusIn(
                        recorrente.getId(), agendamento.getDataHora(), statusesNaoFinais);

        List<AgendamentoResponseDTO> cancelados = new ArrayList<>();
        for (Agendamento futuro : futuros) {
            agendamentoService.updateStatus(
                    futuro.getId(), new AgendamentoStatusDTO(StatusAgendamento.CANCELADO, null, false));
            cancelados.add(agendamentoMapper.toResponseDTO(futuro));
        }

        return cancelados;
    }

    /**
     * Regenera os horários fixos de uma assinatura: cancela agendamentos futuros
     * (status AGENDADO/CONFIRMADO) a partir de {@code dataInicioRegeneracao},
     * desativa templates {@link AgendamentoRecorrente} antigos e cria novos
     * agendamentos para os slots informados, até a {@code dataVencimento} da assinatura.
     */
    @Transactional
    public RegenerarHorariosResponseDTO regenerarHorarios(UUID assinaturaId, RegenerarHorariosRequestDTO dto) {
        Assinatura assinatura = assinaturaRepository
                .findById(assinaturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", assinaturaId));

        if (assinatura.getStatus() != StatusAssinatura.ATIVO) {
            throw new BusinessException("Só é possível regenerar horários de assinaturas ativas");
        }
        if (assinatura.getDataVencimento() == null) {
            throw new BusinessException("Assinatura sem data de vencimento — não é possível regenerar");
        }

        LocalDate dataInicio = dto.getDataInicioRegeneracao() != null
                ? dto.getDataInicioRegeneracao()
                : LocalDate.now().plusDays(1);

        // 1. Cancelar agendamentos futuros que ainda estão pendentes
        List<StatusAgendamento> statusesPendentes = List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO);
        List<Agendamento> futurosPendentes =
                agendamentoRepository.findByAssinaturaIdAndDataHoraGreaterThanEqualAndStatusIn(
                        assinaturaId, dataInicio.atStartOfDay(), statusesPendentes);

        int cancelados = 0;
        for (Agendamento ag : futurosPendentes) {
            // gerarReposicao=false: a sessão é recriada no novo horário logo abaixo —
            // conceder reposição aqui daria crédito duplo à paciente.
            agendamentoService.updateStatus(
                    ag.getId(),
                    new AgendamentoStatusDTO(
                            StatusAgendamento.CANCELADO, "Cancelado por regeneração de horários da assinatura", false));
            cancelados++;
        }

        // 2. Desativar templates antigos
        List<AgendamentoRecorrente> antigos = recorrenteRepository.findByAssinaturaIdAndAtivoTrue(assinaturaId);
        for (AgendamentoRecorrente template : antigos) {
            template.setAtivo(false);
            recorrenteRepository.save(template);
        }

        // 3. Criar novos templates + agendamentos para cada slot
        // Distribui as aulas restantes entre os slots informados.
        int slots = dto.getHorariosFixos().size();
        int aulasRestantes =
                Math.max(0, assinatura.getSessoesContratadas() - assinatura.getSessoesRealizadas() + cancelados);

        List<Agendamento> novosAgendamentos = new ArrayList<>();
        List<DataIgnoradaDTO> datasIgnoradas = new ArrayList<>();

        for (int idx = 0; idx < slots; idx++) {
            RegenerarHorariosRequestDTO.HorarioFixoDTO slot =
                    dto.getHorariosFixos().get(idx);
            int slotsRestantes = slots - idx;
            int sessoesEsteSlot = aulasRestantes > 0 ? (int) Math.ceil((double) aulasRestantes / slotsRestantes) : 0;
            if (sessoesEsteSlot == 0) continue;

            AgendamentoRecorrenteRequestDTO recorrenteReq = new AgendamentoRecorrenteRequestDTO();
            recorrenteReq.setPacienteId(assinatura.getPaciente().getId());
            recorrenteReq.setProfissionalId(dto.getProfissionalId());
            recorrenteReq.setServicoId(assinatura.getServico().getId());
            recorrenteReq.setAssinaturaId(assinaturaId);
            recorrenteReq.setFrequencia(FrequenciaRecorrencia.SEMANAL);
            recorrenteReq.setDiasSemana(List.of(slot.getDiaSemana()));
            recorrenteReq.setHoraInicio(slot.getHoraInicio());
            recorrenteReq.setTotalSessoes(sessoesEsteSlot);
            // Gera a partir da data de regeneração (padrão: amanhã), NÃO da dataInicio
            // original da assinatura — sem isto, createRecorrente usaria a data de início
            // original e recriaria agendamentos retroativos no novo horário.
            recorrenteReq.setDataInicio(dataInicio);
            recorrenteReq.setDataFim(assinatura.getDataVencimento());

            try {
                AgendamentoRecorrenteResponseDTO result = createRecorrente(recorrenteReq);
                if (result.getAgendamentosCriados() != null) {
                    int criados = result.getAgendamentosCriados().size();
                    aulasRestantes = Math.max(0, aulasRestantes - criados);
                }
                if (result.getDatasIgnoradas() != null) {
                    datasIgnoradas.addAll(result.getDatasIgnoradas());
                }
            } catch (BusinessException e) {
                datasIgnoradas.add(new DataIgnoradaDTO(dataInicio, e.getMessage()));
            }
        }

        // Buscar todos os novos agendamentos da assinatura criados a partir de dataInicio
        // (mais confiável do que coletar do retorno de createRecorrente, que sai sem o ID persistido)
        List<Agendamento> novos = agendamentoRepository.findByAssinaturaIdAndDataHoraGreaterThanEqualAndStatusIn(
                assinaturaId, dataInicio.atStartOfDay(), List.of(StatusAgendamento.AGENDADO));
        novosAgendamentos.addAll(novos);

        return new RegenerarHorariosResponseDTO(
                cancelados,
                novosAgendamentos.size(),
                novosAgendamentos.stream().map(agendamentoMapper::toResponseDTO).collect(Collectors.toList()),
                datasIgnoradas);
    }

    public AgendamentoRecorrenteResponseDTO getRecorrenciaByAgendamento(UUID agendamentoId) {
        Agendamento agendamento = agendamentoRepository
                .findById(agendamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento", agendamentoId));

        AgendamentoRecorrente recorrente = agendamento.getAgendamentoRecorrente();
        if (recorrente == null) {
            throw new BusinessException("Este agendamento não faz parte de uma recorrência");
        }

        return toResponseDTO(recorrente, List.of(), List.of());
    }

    private AgendamentoRecorrenteResponseDTO toResponseDTO(
            AgendamentoRecorrente recorrente, List<Agendamento> criados, List<DataIgnoradaDTO> ignoradas) {
        AgendamentoRecorrenteResponseDTO response = new AgendamentoRecorrenteResponseDTO();
        response.setId(recorrente.getId());
        response.setPacienteId(recorrente.getPaciente().getId());
        response.setPacienteNome(recorrente.getPaciente().getNomeCompleto());
        if (recorrente.getProfissional() != null) {
            response.setProfissionalId(recorrente.getProfissional().getId());
            response.setProfissionalNome(recorrente.getProfissional().getNome());
        }
        response.setServicoId(recorrente.getServico().getId());
        response.setServicoDescricao(recorrente.getServico().getAtividade().getNome() + " - "
                + recorrente.getServico().getPlano().getNome());
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
        response.setAgendamentosCriados(
                criados.stream().map(agendamentoMapper::toResponseDTO).collect(Collectors.toList()));
        response.setDatasIgnoradas(ignoradas);
        response.setCreatedAt(recorrente.getCreatedAt());
        return response;
    }
}
