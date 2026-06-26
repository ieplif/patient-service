package br.com.clinicahumaniza.patient_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import br.com.clinicahumaniza.patient_service.dto.AssinaturaRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.AssinaturaStatusDTO;
import br.com.clinicahumaniza.patient_service.dto.AssinaturaUpdateDTO;
import br.com.clinicahumaniza.patient_service.dto.ReativarAssinaturaRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.SuspenderAssinaturaRequestDTO;
import br.com.clinicahumaniza.patient_service.exception.BusinessException;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.AssinaturaMapper;
import br.com.clinicahumaniza.patient_service.model.*;
import br.com.clinicahumaniza.patient_service.repository.AgendamentoRecorrenteRepository;
import br.com.clinicahumaniza.patient_service.repository.AgendamentoRepository;
import br.com.clinicahumaniza.patient_service.repository.AssinaturaRepository;
import br.com.clinicahumaniza.patient_service.repository.PatientRepository;
import br.com.clinicahumaniza.patient_service.repository.ServicoRepository;

@ExtendWith(MockitoExtension.class)
class AssinaturaServiceTest {

    @Mock
    private AssinaturaRepository assinaturaRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private AssinaturaMapper assinaturaMapper;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private AgendamentoRecorrenteRepository recorrenteRepository;

    private AssinaturaService assinaturaService;

    private Assinatura assinatura;
    private Patient paciente;
    private Servico servico;
    private Atividade atividade;
    private Plano plano;
    private AssinaturaRequestDTO requestDTO;
    private UUID assinaturaId;
    private UUID pacienteId;
    private UUID servicoId;

    @BeforeEach
    void setUp() {
        assinaturaService = new AssinaturaService(
                assinaturaRepository,
                patientRepository,
                servicoRepository,
                assinaturaMapper,
                agendamentoRepository,
                recorrenteRepository,
                Optional.empty());

        assinaturaId = UUID.randomUUID();
        pacienteId = UUID.randomUUID();
        servicoId = UUID.randomUUID();

        atividade = new Atividade();
        atividade.setId(UUID.randomUUID());
        atividade.setNome("Pilates");

        plano = new Plano();
        plano.setId(UUID.randomUUID());
        plano.setNome("Mensal");
        plano.setValidadeDias(30);
        plano.setSessoesIncluidas(4);

        paciente = new Patient();
        paciente.setId(pacienteId);
        paciente.setNomeCompleto("João Silva");

        servico = new Servico();
        servico.setId(servicoId);
        servico.setAtividade(atividade);
        servico.setPlano(plano);
        servico.setValor(new BigDecimal("350.00"));

        assinatura = new Assinatura();
        assinatura.setId(assinaturaId);
        assinatura.setPaciente(paciente);
        assinatura.setServico(servico);
        assinatura.setDataInicio(LocalDate.of(2025, 1, 1));
        assinatura.setDataVencimento(LocalDate.of(2025, 1, 31));
        assinatura.setSessoesContratadas(4);
        assinatura.setSessoesRealizadas(0);
        assinatura.setStatus(StatusAssinatura.ATIVO);
        assinatura.setValor(new BigDecimal("350.00"));
        assinatura.setAtivo(true);

        requestDTO = new AssinaturaRequestDTO();
        requestDTO.setPacienteId(pacienteId);
        requestDTO.setServicoId(servicoId);
        requestDTO.setDataInicio(LocalDate.of(2025, 1, 1));
        requestDTO.setDataVencimento(LocalDate.of(2025, 1, 31));
        requestDTO.setSessoesContratadas(4);
        requestDTO.setValor(new BigDecimal("350.00"));
    }

    @Test
    @DisplayName("Deve criar assinatura com sucesso")
    void createAssinatura_Success() {
        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
        when(assinaturaMapper.toEntity(requestDTO, paciente, servico)).thenReturn(assinatura);
        when(assinaturaRepository.save(assinatura)).thenReturn(assinatura);

        Assinatura result = assinaturaService.createAssinatura(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusAssinatura.ATIVO);
        assertThat(result.getSessoesRealizadas()).isEqualTo(0);
        verify(assinaturaRepository).save(assinatura);
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar assinatura com paciente inexistente")
    void createAssinatura_PacienteNotFound() {
        when(patientRepository.findById(pacienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assinaturaService.createAssinatura(requestDTO))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(assinaturaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar assinatura com serviço inexistente")
    void createAssinatura_ServicoNotFound() {
        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assinaturaService.createAssinatura(requestDTO))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(assinaturaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve calcular dataVencimento automaticamente quando não informada")
    void createAssinatura_AutoCalculateDataVencimento() {
        requestDTO.setDataVencimento(null);

        Assinatura assinaturaSemVencimento = new Assinatura();
        assinaturaSemVencimento.setId(assinaturaId);
        assinaturaSemVencimento.setPaciente(paciente);
        assinaturaSemVencimento.setServico(servico);
        assinaturaSemVencimento.setDataInicio(LocalDate.of(2025, 1, 1));
        assinaturaSemVencimento.setDataVencimento(null);
        assinaturaSemVencimento.setSessoesContratadas(4);
        assinaturaSemVencimento.setValor(new BigDecimal("350.00"));

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
        when(assinaturaMapper.toEntity(requestDTO, paciente, servico)).thenReturn(assinaturaSemVencimento);
        when(assinaturaRepository.save(any(Assinatura.class))).thenAnswer(inv -> inv.getArgument(0));

        Assinatura result = assinaturaService.createAssinatura(requestDTO);

        assertThat(result.getDataVencimento()).isEqualTo(LocalDate.of(2025, 1, 31));
    }

    @Test
    @DisplayName("Deve buscar assinatura por ID com sucesso")
    void getAssinaturaById_Success() {
        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));

        Assinatura result = assinaturaService.getAssinaturaById(assinaturaId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(assinaturaId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando assinatura não encontrada")
    void getAssinaturaById_NotFound() {
        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assinaturaService.getAssinaturaById(assinaturaId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve listar todas as assinaturas")
    void getAllAssinaturas_Success() {
        Pageable pageable = PageRequest.of(0, 20);
        when(assinaturaRepository.findAll(
                        org.mockito.ArgumentMatchers.<Specification<Assinatura>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(assinatura)));

        Page<Assinatura> result = assinaturaService.getAllAssinaturas(null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Deve listar assinaturas por paciente")
    void getAssinaturasByPaciente_Success() {
        when(assinaturaRepository.findByPacienteId(pacienteId)).thenReturn(List.of(assinatura));

        List<Assinatura> result = assinaturaService.getAssinaturasByPaciente(pacienteId);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Deve listar assinaturas por serviço")
    void getAssinaturasByServico_Success() {
        when(assinaturaRepository.findByServicoId(servicoId)).thenReturn(List.of(assinatura));

        List<Assinatura> result = assinaturaService.getAssinaturasByServico(servicoId);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Deve atualizar assinatura com sucesso")
    void updateAssinatura_Success() {
        AssinaturaUpdateDTO updateDTO = new AssinaturaUpdateDTO();
        updateDTO.setValor(new BigDecimal("400.00"));

        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));
        doNothing().when(assinaturaMapper).updateEntityFromDto(updateDTO, assinatura);
        when(assinaturaRepository.save(assinatura)).thenReturn(assinatura);

        Assinatura result = assinaturaService.updateAssinatura(assinaturaId, updateDTO);

        assertThat(result).isNotNull();
        verify(assinaturaRepository).save(assinatura);
    }

    @Test
    @DisplayName("Deve alterar status com sucesso")
    void updateStatus_Success() {
        AssinaturaStatusDTO statusDTO = new AssinaturaStatusDTO(StatusAssinatura.CANCELADO);

        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));
        when(assinaturaRepository.save(assinatura)).thenReturn(assinatura);

        Assinatura result = assinaturaService.updateStatus(assinaturaId, statusDTO);

        assertThat(result.getStatus()).isEqualTo(StatusAssinatura.CANCELADO);
    }

    @Test
    @DisplayName("Cancelar assinatura também cancela os agendamentos futuros pendentes")
    void updateStatus_Cancelar_CancelaAgendamentosFuturos() {
        Agendamento futuro = new Agendamento();
        futuro.setId(UUID.randomUUID());
        futuro.setStatus(StatusAgendamento.AGENDADO);

        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));
        when(assinaturaRepository.save(any(Assinatura.class))).thenAnswer(inv -> inv.getArgument(0));
        when(agendamentoRepository.findByAssinaturaIdAndDataHoraGreaterThanEqualAndStatusIn(any(), any(), any()))
                .thenReturn(List.of(futuro));
        when(recorrenteRepository.findByAssinaturaIdAndAtivoTrue(assinaturaId)).thenReturn(List.of());

        assinaturaService.updateStatus(assinaturaId, new AssinaturaStatusDTO(StatusAssinatura.CANCELADO));

        assertThat(futuro.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        assertThat(futuro.getDireitoReposicao()).isFalse();
        verify(agendamentoRepository).save(futuro);
    }

    @Test
    @DisplayName("Deve registrar sessão com sucesso")
    void registrarSessao_Success() {
        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));
        when(assinaturaRepository.save(assinatura)).thenReturn(assinatura);

        Assinatura result = assinaturaService.registrarSessao(assinaturaId);

        assertThat(result.getSessoesRealizadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve finalizar automaticamente ao atingir limite de sessões")
    void registrarSessao_FinalizaAutomaticamente() {
        assinatura.setSessoesRealizadas(3);
        assinatura.setSessoesContratadas(4);

        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));
        when(assinaturaRepository.save(assinatura)).thenReturn(assinatura);

        Assinatura result = assinaturaService.registrarSessao(assinaturaId);

        assertThat(result.getSessoesRealizadas()).isEqualTo(4);
        assertThat(result.getStatus()).isEqualTo(StatusAssinatura.FINALIZADO);
    }

    @Test
    @DisplayName("Deve lançar exceção ao registrar sessão em assinatura não ativa")
    void registrarSessao_AssinaturaNaoAtiva() {
        assinatura.setStatus(StatusAssinatura.CANCELADO);

        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));

        assertThatThrownBy(() -> assinaturaService.registrarSessao(assinaturaId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CANCELADO");

        verify(assinaturaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve deletar assinatura com sucesso (soft delete)")
    void deleteAssinatura_Success() {
        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));
        when(assinaturaRepository.save(any(Assinatura.class))).thenReturn(assinatura);

        assinaturaService.deleteAssinatura(assinaturaId);

        assertThat(assinatura.getAtivo()).isFalse();
        verify(assinaturaRepository).save(assinatura);
    }

    // --- Suspensao ---

    @Test
    @DisplayName("Deve suspender assinatura ATIVO")
    void suspender_Success() {
        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));
        when(assinaturaRepository.save(any(Assinatura.class))).thenAnswer(inv -> inv.getArgument(0));
        when(agendamentoRepository.findByAssinaturaIdAndDataHoraGreaterThanEqualAndStatusIn(any(), any(), any()))
                .thenReturn(List.of());
        when(recorrenteRepository.findByAssinaturaIdAndAtivoTrue(assinaturaId)).thenReturn(List.of());

        SuspenderAssinaturaRequestDTO dto = new SuspenderAssinaturaRequestDTO("Gravidez", null);
        Assinatura result = assinaturaService.suspender(assinaturaId, dto);

        assertThat(result.getStatus()).isEqualTo(StatusAssinatura.SUSPENSO);
        assertThat(result.getMotivoSuspensao()).isEqualTo("Gravidez");
        assertThat(result.getDataSuspensao()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Deve cancelar agendamentos futuros pendentes ao suspender")
    void suspender_CancelaAgendamentosFuturos() {
        Agendamento futuro = new Agendamento();
        futuro.setId(UUID.randomUUID());
        futuro.setStatus(StatusAgendamento.AGENDADO);

        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));
        when(assinaturaRepository.save(any(Assinatura.class))).thenAnswer(inv -> inv.getArgument(0));
        when(agendamentoRepository.findByAssinaturaIdAndDataHoraGreaterThanEqualAndStatusIn(any(), any(), any()))
                .thenReturn(List.of(futuro));
        when(recorrenteRepository.findByAssinaturaIdAndAtivoTrue(assinaturaId)).thenReturn(List.of());

        assinaturaService.suspender(assinaturaId, new SuspenderAssinaturaRequestDTO("Lesão", null));

        assertThat(futuro.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        assertThat(futuro.getDireitoReposicao()).isFalse();
        verify(agendamentoRepository).save(futuro);
    }

    @Test
    @DisplayName("Não deve suspender assinatura que não está ATIVO")
    void suspender_StatusInvalido() {
        assinatura.setStatus(StatusAssinatura.CANCELADO);
        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));

        assertThatThrownBy(() ->
                        assinaturaService.suspender(assinaturaId, new SuspenderAssinaturaRequestDTO("Motivo", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ATIVO");
    }

    // --- Reativacao ---

    @Test
    @DisplayName("Deve reativar assinatura SUSPENSO e recalcular vencimento")
    void reativar_Success() {
        assinatura.setStatus(StatusAssinatura.SUSPENSO);
        assinatura.setMotivoSuspensao("Gravidez");
        assinatura.setDataSuspensao(LocalDate.now().minusDays(60));
        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));
        when(assinaturaRepository.save(any(Assinatura.class))).thenAnswer(inv -> inv.getArgument(0));

        Assinatura result = assinaturaService.reativar(assinaturaId, new ReativarAssinaturaRequestDTO());

        assertThat(result.getStatus()).isEqualTo(StatusAssinatura.ATIVO);
        assertThat(result.getDataInicio()).isEqualTo(LocalDate.now());
        // plano.validadeDias = 30 -> vencimento = hoje + 29
        assertThat(result.getDataVencimento()).isEqualTo(LocalDate.now().plusDays(29));
        assertThat(result.getMotivoSuspensao()).isNull();
        assertThat(result.getDataSuspensao()).isNull();
    }

    @Test
    @DisplayName("Não deve reativar assinatura que não está SUSPENSO")
    void reativar_StatusInvalido() {
        assinatura.setStatus(StatusAssinatura.ATIVO);
        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));

        assertThatThrownBy(() -> assinaturaService.reativar(assinaturaId, new ReativarAssinaturaRequestDTO()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SUSPENSO");
    }
}
