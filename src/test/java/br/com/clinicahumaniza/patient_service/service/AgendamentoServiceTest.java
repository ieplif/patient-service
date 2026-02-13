package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.AgendamentoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.AgendamentoStatusDTO;
import br.com.clinicahumaniza.patient_service.dto.AgendamentoUpdateDTO;
import br.com.clinicahumaniza.patient_service.exception.BusinessException;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.AgendamentoMapper;
import br.com.clinicahumaniza.patient_service.model.*;
import br.com.clinicahumaniza.patient_service.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ProfissionalRepository profissionalRepository;

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private AssinaturaRepository assinaturaRepository;

    @Mock
    private HorarioDisponivelRepository horarioDisponivelRepository;

    @Mock
    private AgendamentoMapper agendamentoMapper;

    @Mock
    private AssinaturaService assinaturaService;

    private AgendamentoService agendamentoService;

    private Agendamento agendamento;
    private Patient paciente;
    private Profissional profissional;
    private Servico servico;
    private Atividade atividade;
    private Plano plano;
    private Assinatura assinatura;
    private HorarioDisponivel horarioDisponivel;
    private AgendamentoRequestDTO requestDTO;
    private UUID agendamentoId;
    private UUID pacienteId;
    private UUID profissionalId;
    private UUID servicoId;
    private UUID assinaturaId;

    @BeforeEach
    void setUp() {
        agendamentoService = new AgendamentoService(
                agendamentoRepository, patientRepository, profissionalRepository,
                servicoRepository, assinaturaRepository, horarioDisponivelRepository,
                agendamentoMapper, assinaturaService, Optional.empty());

        agendamentoId = UUID.randomUUID();
        pacienteId = UUID.randomUUID();
        profissionalId = UUID.randomUUID();
        servicoId = UUID.randomUUID();
        assinaturaId = UUID.randomUUID();

        atividade = new Atividade();
        atividade.setId(UUID.randomUUID());
        atividade.setNome("Pilates");
        atividade.setDuracaoPadrao(50);
        atividade.setCapacidadeMaxima(1);

        plano = new Plano();
        plano.setId(UUID.randomUUID());
        plano.setNome("Mensal");

        paciente = new Patient();
        paciente.setId(pacienteId);
        paciente.setNomeCompleto("Maria Santos");

        profissional = new Profissional();
        profissional.setId(profissionalId);
        profissional.setNome("Dr. Ana");
        profissional.setAtividades(new HashSet<>(Set.of(atividade)));

        servico = new Servico();
        servico.setId(servicoId);
        servico.setAtividade(atividade);
        servico.setPlano(plano);
        servico.setValor(new BigDecimal("350.00"));

        assinatura = new Assinatura();
        assinatura.setId(assinaturaId);
        assinatura.setPaciente(paciente);
        assinatura.setServico(servico);
        assinatura.setStatus(StatusAssinatura.ATIVO);
        assinatura.setSessoesContratadas(4);
        assinatura.setSessoesRealizadas(0);

        // Monday 10:00
        LocalDateTime dataHora = LocalDateTime.of(2025, 6, 2, 10, 0);

        horarioDisponivel = new HorarioDisponivel();
        horarioDisponivel.setId(UUID.randomUUID());
        horarioDisponivel.setProfissional(profissional);
        horarioDisponivel.setDiaSemana(DayOfWeek.MONDAY);
        horarioDisponivel.setHoraInicio(LocalTime.of(8, 0));
        horarioDisponivel.setHoraFim(LocalTime.of(18, 0));
        horarioDisponivel.setAtivo(true);

        agendamento = new Agendamento();
        agendamento.setId(agendamentoId);
        agendamento.setPaciente(paciente);
        agendamento.setProfissional(profissional);
        agendamento.setServico(servico);
        agendamento.setDataHora(dataHora);
        agendamento.setDuracaoMinutos(50);
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        agendamento.setAtivo(true);

        requestDTO = new AgendamentoRequestDTO();
        requestDTO.setPacienteId(pacienteId);
        requestDTO.setProfissionalId(profissionalId);
        requestDTO.setServicoId(servicoId);
        requestDTO.setDataHora(dataHora);
        requestDTO.setDuracaoMinutos(50);
    }

    // --- Testes de criação ---

    @Test
    @DisplayName("Deve criar agendamento com sucesso")
    void createAgendamento_Success() {
        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
        when(horarioDisponivelRepository.findByProfissionalIdAndDiaSemana(profissionalId, DayOfWeek.MONDAY))
                .thenReturn(List.of(horarioDisponivel));
        when(agendamentoRepository.findByProfissionalIdAndStatusInAndDataHoraBetween(
                eq(profissionalId), any(), any(), any())).thenReturn(List.of());
        when(agendamentoMapper.toEntity(any(), any(), any(), any(), any())).thenReturn(agendamento);
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);

        Agendamento result = agendamentoService.createAgendamento(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
        verify(agendamentoRepository).save(agendamento);
    }

    @Test
    @DisplayName("Deve criar agendamento com duração padrão da atividade")
    void createAgendamento_DefaultDuracao() {
        requestDTO.setDuracaoMinutos(null);

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
        when(horarioDisponivelRepository.findByProfissionalIdAndDiaSemana(profissionalId, DayOfWeek.MONDAY))
                .thenReturn(List.of(horarioDisponivel));
        when(agendamentoRepository.findByProfissionalIdAndStatusInAndDataHoraBetween(
                eq(profissionalId), any(), any(), any())).thenReturn(List.of());
        when(agendamentoMapper.toEntity(any(), any(), any(), any(), any())).thenReturn(agendamento);
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);

        agendamentoService.createAgendamento(requestDTO);

        assertThat(requestDTO.getDuracaoMinutos()).isEqualTo(50);
    }

    @Test
    @DisplayName("Deve criar agendamento com assinatura vinculada")
    void createAgendamento_WithAssinatura() {
        requestDTO.setAssinaturaId(assinaturaId);

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));
        when(horarioDisponivelRepository.findByProfissionalIdAndDiaSemana(profissionalId, DayOfWeek.MONDAY))
                .thenReturn(List.of(horarioDisponivel));
        when(agendamentoRepository.findByProfissionalIdAndStatusInAndDataHoraBetween(
                eq(profissionalId), any(), any(), any())).thenReturn(List.of());
        when(agendamentoMapper.toEntity(any(), any(), any(), any(), eq(assinatura))).thenReturn(agendamento);
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);

        Agendamento result = agendamentoService.createAgendamento(requestDTO);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar agendamento com paciente inexistente")
    void createAgendamento_PacienteNotFound() {
        when(patientRepository.findById(pacienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agendamentoService.createAgendamento(requestDTO))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar agendamento com profissional inexistente")
    void createAgendamento_ProfissionalNotFound() {
        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agendamentoService.createAgendamento(requestDTO))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar agendamento com serviço inexistente")
    void createAgendamento_ServicoNotFound() {
        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agendamentoService.createAgendamento(requestDTO))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando profissional não atende atividade do serviço")
    void createAgendamento_ProfissionalNaoAtendeAtividade() {
        Atividade outraAtividade = new Atividade();
        outraAtividade.setId(UUID.randomUUID());
        outraAtividade.setNome("Yoga");
        profissional.setAtividades(new HashSet<>(Set.of(outraAtividade)));

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));

        assertThatThrownBy(() -> agendamentoService.createAgendamento(requestDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não atende a atividade");

        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando assinatura não está ativa")
    void createAgendamento_AssinaturaNaoAtiva() {
        requestDTO.setAssinaturaId(assinaturaId);
        assinatura.setStatus(StatusAssinatura.CANCELADO);

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));

        assertThatThrownBy(() -> agendamentoService.createAgendamento(requestDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não está ativa");

        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando assinatura sem sessões restantes")
    void createAgendamento_AssinaturaSemSessoes() {
        requestDTO.setAssinaturaId(assinaturaId);
        assinatura.setSessoesRealizadas(4);
        assinatura.setSessoesContratadas(4);

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));

        assertThatThrownBy(() -> agendamentoService.createAgendamento(requestDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sessões restantes");

        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando horário fora da disponibilidade")
    void createAgendamento_ForaDoHorarioDisponivel() {
        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
        when(horarioDisponivelRepository.findByProfissionalIdAndDiaSemana(profissionalId, DayOfWeek.MONDAY))
                .thenReturn(List.of());

        assertThatThrownBy(() -> agendamentoService.createAgendamento(requestDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("fora da disponibilidade");

        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando há conflito de horário")
    void createAgendamento_ConflitoHorario() {
        Agendamento existente = new Agendamento();
        existente.setId(UUID.randomUUID());
        existente.setDataHora(requestDTO.getDataHora());
        existente.setDuracaoMinutos(50);

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
        when(horarioDisponivelRepository.findByProfissionalIdAndDiaSemana(profissionalId, DayOfWeek.MONDAY))
                .thenReturn(List.of(horarioDisponivel));
        when(agendamentoRepository.findByProfissionalIdAndStatusInAndDataHoraBetween(
                eq(profissionalId), any(), any(), any())).thenReturn(List.of(existente));

        assertThatThrownBy(() -> agendamentoService.createAgendamento(requestDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("capacidade máxima");

        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve detectar conflito de sobreposição parcial")
    void createAgendamento_SobreposicaoParcial() {
        Agendamento existente = new Agendamento();
        existente.setId(UUID.randomUUID());
        existente.setDataHora(requestDTO.getDataHora().minusMinutes(30));
        existente.setDuracaoMinutos(50);

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
        when(horarioDisponivelRepository.findByProfissionalIdAndDiaSemana(profissionalId, DayOfWeek.MONDAY))
                .thenReturn(List.of(horarioDisponivel));
        when(agendamentoRepository.findByProfissionalIdAndStatusInAndDataHoraBetween(
                eq(profissionalId), any(), any(), any())).thenReturn(List.of(existente));

        assertThatThrownBy(() -> agendamentoService.createAgendamento(requestDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("capacidade máxima");
    }

    @Test
    @DisplayName("Deve permitir múltiplos agendamentos no mesmo horário quando capacidade > 1")
    void createAgendamento_CapacidadeMaiorQue1() {
        atividade.setCapacidadeMaxima(4); // Pilates em grupo: até 4 pacientes

        Agendamento existente = new Agendamento();
        existente.setId(UUID.randomUUID());
        existente.setDataHora(requestDTO.getDataHora());
        existente.setDuracaoMinutos(50);

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
        when(horarioDisponivelRepository.findByProfissionalIdAndDiaSemana(profissionalId, DayOfWeek.MONDAY))
                .thenReturn(List.of(horarioDisponivel));
        when(agendamentoRepository.findByProfissionalIdAndStatusInAndDataHoraBetween(
                eq(profissionalId), any(), any(), any())).thenReturn(List.of(existente));
        when(agendamentoMapper.toEntity(any(), any(), any(), any(), any())).thenReturn(agendamento);
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);

        Agendamento result = agendamentoService.createAgendamento(requestDTO);

        assertThat(result).isNotNull();
        verify(agendamentoRepository).save(agendamento);
    }

    @Test
    @DisplayName("Deve bloquear quando capacidade máxima atingida (4 existentes, capacidade 4)")
    void createAgendamento_CapacidadeMaximaAtingida() {
        atividade.setCapacidadeMaxima(4);

        List<Agendamento> existentes = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Agendamento a = new Agendamento();
            a.setId(UUID.randomUUID());
            a.setDataHora(requestDTO.getDataHora());
            a.setDuracaoMinutos(50);
            existentes.add(a);
        }

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
        when(horarioDisponivelRepository.findByProfissionalIdAndDiaSemana(profissionalId, DayOfWeek.MONDAY))
                .thenReturn(List.of(horarioDisponivel));
        when(agendamentoRepository.findByProfissionalIdAndStatusInAndDataHoraBetween(
                eq(profissionalId), any(), any(), any())).thenReturn(existentes);

        assertThatThrownBy(() -> agendamentoService.createAgendamento(requestDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("capacidade máxima (4)");
    }

    @Test
    @DisplayName("Deve lançar exceção quando duração não informada e atividade sem duração padrão")
    void createAgendamento_SemDuracaoPadrao() {
        requestDTO.setDuracaoMinutos(null);
        atividade.setDuracaoPadrao(null);

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));

        assertThatThrownBy(() -> agendamentoService.createAgendamento(requestDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Duração não informada");
    }

    // --- Testes de busca ---

    @Test
    @DisplayName("Deve buscar agendamento por ID com sucesso")
    void getAgendamentoById_Success() {
        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));

        Agendamento result = agendamentoService.getAgendamentoById(agendamentoId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(agendamentoId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando agendamento não encontrado")
    void getAgendamentoById_NotFound() {
        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agendamentoService.getAgendamentoById(agendamentoId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve listar todos os agendamentos")
    void getAllAgendamentos_Success() {
        when(agendamentoRepository.findAll()).thenReturn(List.of(agendamento));

        List<Agendamento> result = agendamentoService.getAllAgendamentos();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Deve listar agendamentos por paciente")
    void getAgendamentosByPaciente_Success() {
        when(agendamentoRepository.findByPacienteId(pacienteId)).thenReturn(List.of(agendamento));

        List<Agendamento> result = agendamentoService.getAgendamentosByPaciente(pacienteId);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Deve listar agendamentos por profissional")
    void getAgendamentosByProfissional_Success() {
        when(agendamentoRepository.findByProfissionalId(profissionalId)).thenReturn(List.of(agendamento));

        List<Agendamento> result = agendamentoService.getAgendamentosByProfissional(profissionalId);

        assertThat(result).hasSize(1);
    }

    // --- Testes de atualização ---

    @Test
    @DisplayName("Deve atualizar agendamento com sucesso")
    void updateAgendamento_Success() {
        AgendamentoUpdateDTO updateDTO = new AgendamentoUpdateDTO();
        updateDTO.setObservacoes("Paciente pediu para remarcar");

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
        doNothing().when(agendamentoMapper).updateEntityFromDto(updateDTO, agendamento);
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);

        Agendamento result = agendamentoService.updateAgendamento(agendamentoId, updateDTO);

        assertThat(result).isNotNull();
        verify(agendamentoRepository).save(agendamento);
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar agendamento com status final")
    void updateAgendamento_StatusFinal() {
        agendamento.setStatus(StatusAgendamento.REALIZADO);
        AgendamentoUpdateDTO updateDTO = new AgendamentoUpdateDTO();
        updateDTO.setObservacoes("Teste");

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> agendamentoService.updateAgendamento(agendamentoId, updateDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("REALIZADO");
    }

    // --- Testes de transição de status ---

    @Test
    @DisplayName("Deve transicionar AGENDADO → CONFIRMADO")
    void updateStatus_AgendadoParaConfirmado() {
        AgendamentoStatusDTO statusDTO = new AgendamentoStatusDTO(StatusAgendamento.CONFIRMADO);

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);

        Agendamento result = agendamentoService.updateStatus(agendamentoId, statusDTO);

        assertThat(result.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    @Test
    @DisplayName("Deve transicionar AGENDADO → CANCELADO")
    void updateStatus_AgendadoParaCancelado() {
        AgendamentoStatusDTO statusDTO = new AgendamentoStatusDTO(StatusAgendamento.CANCELADO);

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);

        Agendamento result = agendamentoService.updateStatus(agendamentoId, statusDTO);

        assertThat(result.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
    }

    @Test
    @DisplayName("Deve transicionar CONFIRMADO → REALIZADO")
    void updateStatus_ConfirmadoParaRealizado() {
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        AgendamentoStatusDTO statusDTO = new AgendamentoStatusDTO(StatusAgendamento.REALIZADO);

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);

        Agendamento result = agendamentoService.updateStatus(agendamentoId, statusDTO);

        assertThat(result.getStatus()).isEqualTo(StatusAgendamento.REALIZADO);
    }

    @Test
    @DisplayName("Deve transicionar CONFIRMADO → NAO_COMPARECEU")
    void updateStatus_ConfirmadoParaNaoCompareceu() {
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        AgendamentoStatusDTO statusDTO = new AgendamentoStatusDTO(StatusAgendamento.NAO_COMPARECEU);

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);

        Agendamento result = agendamentoService.updateStatus(agendamentoId, statusDTO);

        assertThat(result.getStatus()).isEqualTo(StatusAgendamento.NAO_COMPARECEU);
    }

    @Test
    @DisplayName("Deve registrar sessão na assinatura ao marcar REALIZADO")
    void updateStatus_RealizadoComAssinatura() {
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        agendamento.setAssinatura(assinatura);
        AgendamentoStatusDTO statusDTO = new AgendamentoStatusDTO(StatusAgendamento.REALIZADO);

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);
        when(assinaturaService.registrarSessao(assinaturaId)).thenReturn(assinatura);

        agendamentoService.updateStatus(agendamentoId, statusDTO);

        verify(assinaturaService).registrarSessao(assinaturaId);
    }

    @Test
    @DisplayName("Não deve registrar sessão quando REALIZADO sem assinatura")
    void updateStatus_RealizadoSemAssinatura() {
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        agendamento.setAssinatura(null);
        AgendamentoStatusDTO statusDTO = new AgendamentoStatusDTO(StatusAgendamento.REALIZADO);

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);

        agendamentoService.updateStatus(agendamentoId, statusDTO);

        verify(assinaturaService, never()).registrarSessao(any());
    }

    @Test
    @DisplayName("Deve lançar exceção para transição inválida AGENDADO → REALIZADO")
    void updateStatus_TransicaoInvalida_AgendadoParaRealizado() {
        AgendamentoStatusDTO statusDTO = new AgendamentoStatusDTO(StatusAgendamento.REALIZADO);

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> agendamentoService.updateStatus(agendamentoId, statusDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição de status inválida");
    }

    @Test
    @DisplayName("Deve lançar exceção ao alterar status final CANCELADO")
    void updateStatus_StatusFinalCancelado() {
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        AgendamentoStatusDTO statusDTO = new AgendamentoStatusDTO(StatusAgendamento.AGENDADO);

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> agendamentoService.updateStatus(agendamentoId, statusDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CANCELADO");
    }

    @Test
    @DisplayName("Deve lançar exceção ao alterar status final REALIZADO")
    void updateStatus_StatusFinalRealizado() {
        agendamento.setStatus(StatusAgendamento.REALIZADO);
        AgendamentoStatusDTO statusDTO = new AgendamentoStatusDTO(StatusAgendamento.AGENDADO);

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> agendamentoService.updateStatus(agendamentoId, statusDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("REALIZADO");
    }

    // --- Testes de deleção ---

    @Test
    @DisplayName("Deve deletar agendamento com sucesso (soft delete)")
    void deleteAgendamento_Success() {
        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any(Agendamento.class))).thenReturn(agendamento);

        agendamentoService.deleteAgendamento(agendamentoId);

        assertThat(agendamento.isAtivo()).isFalse();
        verify(agendamentoRepository).save(agendamento);
    }

    // --- Testes de slots disponíveis ---

    @Test
    @DisplayName("Deve retornar slots disponíveis sem agendamentos existentes")
    void getAvailableSlots_NoExisting() {
        LocalDate data = LocalDate.of(2025, 6, 2); // Monday
        HorarioDisponivel h = new HorarioDisponivel();
        h.setHoraInicio(LocalTime.of(8, 0));
        h.setHoraFim(LocalTime.of(10, 0));

        when(horarioDisponivelRepository.findByProfissionalIdAndDiaSemana(profissionalId, DayOfWeek.MONDAY))
                .thenReturn(List.of(h));
        when(agendamentoRepository.findByProfissionalIdAndStatusInAndDataHoraBetween(
                eq(profissionalId), any(), any(), any())).thenReturn(List.of());

        List<LocalDateTime> slots = agendamentoService.getAvailableSlots(profissionalId, data, 50, 1);

        // 08:00-08:50, 08:50-09:40 = 2 slots (09:40+50=10:30 > 10:00 so 3rd doesn't fit)
        assertThat(slots).hasSize(2);
        assertThat(slots.get(0)).isEqualTo(data.atTime(8, 0));
        assertThat(slots.get(1)).isEqualTo(data.atTime(8, 50));
    }

    @Test
    @DisplayName("Deve filtrar slots ocupados")
    void getAvailableSlots_WithExistingAppointments() {
        LocalDate data = LocalDate.of(2025, 6, 2);
        HorarioDisponivel h = new HorarioDisponivel();
        h.setHoraInicio(LocalTime.of(8, 0));
        h.setHoraFim(LocalTime.of(10, 0));

        Agendamento existente = new Agendamento();
        existente.setDataHora(data.atTime(8, 0));
        existente.setDuracaoMinutos(50);

        when(horarioDisponivelRepository.findByProfissionalIdAndDiaSemana(profissionalId, DayOfWeek.MONDAY))
                .thenReturn(List.of(h));
        when(agendamentoRepository.findByProfissionalIdAndStatusInAndDataHoraBetween(
                eq(profissionalId), any(), any(), any())).thenReturn(List.of(existente));

        List<LocalDateTime> slots = agendamentoService.getAvailableSlots(profissionalId, data, 50, 1);

        // Only 08:50-09:40 available (08:00 is occupied)
        assertThat(slots).hasSize(1);
        assertThat(slots.get(0)).isEqualTo(data.atTime(8, 50));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando profissional sem disponibilidade no dia")
    void getAvailableSlots_NoDisponibilidade() {
        LocalDate data = LocalDate.of(2025, 6, 2);

        when(horarioDisponivelRepository.findByProfissionalIdAndDiaSemana(profissionalId, DayOfWeek.MONDAY))
                .thenReturn(List.of());

        List<LocalDateTime> slots = agendamentoService.getAvailableSlots(profissionalId, data, 50, 1);

        assertThat(slots).isEmpty();
    }
}
