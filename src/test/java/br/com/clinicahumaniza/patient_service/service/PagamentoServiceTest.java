package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.PagamentoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.PagamentoStatusDTO;
import br.com.clinicahumaniza.patient_service.dto.PagamentoUpdateDTO;
import br.com.clinicahumaniza.patient_service.dto.ParcelaStatusDTO;
import br.com.clinicahumaniza.patient_service.exception.BusinessException;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.PagamentoMapper;
import br.com.clinicahumaniza.patient_service.model.*;
import br.com.clinicahumaniza.patient_service.repository.AgendamentoRepository;
import br.com.clinicahumaniza.patient_service.repository.AssinaturaRepository;
import br.com.clinicahumaniza.patient_service.repository.PagamentoRepository;
import br.com.clinicahumaniza.patient_service.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AssinaturaRepository assinaturaRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private PagamentoMapper pagamentoMapper;

    @InjectMocks
    private PagamentoService pagamentoService;

    private Patient paciente;
    private Assinatura assinatura;
    private Agendamento agendamento;
    private Pagamento pagamento;
    private PagamentoRequestDTO requestDTO;
    private UUID pagamentoId;
    private UUID pacienteId;
    private UUID assinaturaId;
    private UUID agendamentoId;

    @BeforeEach
    void setUp() {
        pagamentoId = UUID.randomUUID();
        pacienteId = UUID.randomUUID();
        assinaturaId = UUID.randomUUID();
        agendamentoId = UUID.randomUUID();

        paciente = new Patient();
        paciente.setId(pacienteId);
        paciente.setNomeCompleto("João Silva");

        Atividade atividade = new Atividade();
        atividade.setId(UUID.randomUUID());
        atividade.setNome("Pilates");

        Plano plano = new Plano();
        plano.setId(UUID.randomUUID());
        plano.setNome("Mensal");

        Servico servico = new Servico();
        servico.setId(UUID.randomUUID());
        servico.setAtividade(atividade);
        servico.setPlano(plano);

        assinatura = new Assinatura();
        assinatura.setId(assinaturaId);
        assinatura.setPaciente(paciente);
        assinatura.setServico(servico);
        assinatura.setStatus(StatusAssinatura.ATIVO);

        Profissional profissional = new Profissional();
        profissional.setId(UUID.randomUUID());
        profissional.setNome("Dra. Maria");

        agendamento = new Agendamento();
        agendamento.setId(agendamentoId);
        agendamento.setPaciente(paciente);
        agendamento.setProfissional(profissional);
        agendamento.setServico(servico);
        agendamento.setDataHora(LocalDateTime.of(2025, 2, 15, 10, 0));

        pagamento = new Pagamento();
        pagamento.setId(pagamentoId);
        pagamento.setPaciente(paciente);
        pagamento.setAssinatura(assinatura);
        pagamento.setValor(new BigDecimal("300.00"));
        pagamento.setFormaPagamento(FormaPagamento.PIX);
        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamento.setNumeroParcelas(1);
        pagamento.setDataVencimento(LocalDate.of(2025, 2, 15));
        pagamento.setAtivo(true);
        pagamento.setParcelas(new ArrayList<>());

        requestDTO = new PagamentoRequestDTO();
        requestDTO.setPacienteId(pacienteId);
        requestDTO.setAssinaturaId(assinaturaId);
        requestDTO.setValor(new BigDecimal("300.00"));
        requestDTO.setFormaPagamento(FormaPagamento.PIX);
        requestDTO.setNumeroParcelas(1);
        requestDTO.setDataVencimento(LocalDate.of(2025, 2, 15));
    }

    // ===== Criação de Pagamento =====

    @Test
    @DisplayName("Deve criar pagamento com assinatura com sucesso")
    void createPagamento_ComAssinatura_Success() {
        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));
        when(pagamentoMapper.toEntity(requestDTO, paciente, assinatura, null)).thenReturn(pagamento);
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Pagamento result = pagamentoService.createPagamento(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(result.getParcelas()).hasSize(1);
        verify(pagamentoRepository).save(any(Pagamento.class));
    }

    @Test
    @DisplayName("Deve criar pagamento com agendamento avulso com sucesso")
    void createPagamento_ComAgendamento_Success() {
        requestDTO.setAssinaturaId(null);
        requestDTO.setAgendamentoId(agendamentoId);

        Pagamento pagamentoAgendamento = new Pagamento();
        pagamentoAgendamento.setId(pagamentoId);
        pagamentoAgendamento.setPaciente(paciente);
        pagamentoAgendamento.setAgendamento(agendamento);
        pagamentoAgendamento.setValor(new BigDecimal("300.00"));
        pagamentoAgendamento.setFormaPagamento(FormaPagamento.PIX);
        pagamentoAgendamento.setNumeroParcelas(1);
        pagamentoAgendamento.setDataVencimento(LocalDate.of(2025, 2, 15));
        pagamentoAgendamento.setParcelas(new ArrayList<>());

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
        when(pagamentoMapper.toEntity(requestDTO, paciente, null, agendamento)).thenReturn(pagamentoAgendamento);
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Pagamento result = pagamentoService.createPagamento(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getAgendamento()).isNotNull();
        assertThat(result.getParcelas()).hasSize(1);
    }

    @Test
    @DisplayName("Deve lançar exceção quando nem assinatura nem agendamento informados")
    void createPagamento_SemAssinaturaNemAgendamento_ThrowsException() {
        requestDTO.setAssinaturaId(null);
        requestDTO.setAgendamentoId(null);

        assertThatThrownBy(() -> pagamentoService.createPagamento(requestDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("assinatura ou um agendamento");

        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando paciente não encontrado")
    void createPagamento_PacienteNotFound() {
        when(patientRepository.findById(pacienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagamentoService.createPagamento(requestDTO))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando assinatura não encontrada")
    void createPagamento_AssinaturaNotFound() {
        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagamentoService.createPagamento(requestDTO))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando agendamento não encontrado")
    void createPagamento_AgendamentoNotFound() {
        requestDTO.setAssinaturaId(null);
        requestDTO.setAgendamentoId(agendamentoId);

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagamentoService.createPagamento(requestDTO))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(pagamentoRepository, never()).save(any());
    }

    // ===== Geração de Parcelas =====

    @Test
    @DisplayName("Deve gerar 1 parcela para pagamento à vista")
    void createPagamento_PagamentoAVista_1Parcela() {
        requestDTO.setNumeroParcelas(1);
        requestDTO.setValor(new BigDecimal("300.00"));

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));
        when(pagamentoMapper.toEntity(any(), any(), any(), any())).thenReturn(pagamento);
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Pagamento result = pagamentoService.createPagamento(requestDTO);

        assertThat(result.getParcelas()).hasSize(1);
        assertThat(result.getParcelas().get(0).getValor()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(result.getParcelas().get(0).getNumero()).isEqualTo(1);
        assertThat(result.getParcelas().get(0).getDataVencimento()).isEqualTo(LocalDate.of(2025, 2, 15));
    }

    @Test
    @DisplayName("Deve gerar 3 parcelas com valores corretos")
    void createPagamento_3Parcelas_ValoresCorretos() {
        requestDTO.setNumeroParcelas(3);
        requestDTO.setValor(new BigDecimal("300.00"));

        pagamento.setNumeroParcelas(3);

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));
        when(pagamentoMapper.toEntity(any(), any(), any(), any())).thenReturn(pagamento);
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Pagamento result = pagamentoService.createPagamento(requestDTO);

        assertThat(result.getParcelas()).hasSize(3);
        assertThat(result.getParcelas().get(0).getValor()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.getParcelas().get(1).getValor()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.getParcelas().get(2).getValor()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Deve gerar 3 parcelas com datas mensais corretas")
    void createPagamento_3Parcelas_DatasMensais() {
        requestDTO.setNumeroParcelas(3);
        pagamento.setNumeroParcelas(3);

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));
        when(pagamentoMapper.toEntity(any(), any(), any(), any())).thenReturn(pagamento);
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Pagamento result = pagamentoService.createPagamento(requestDTO);

        assertThat(result.getParcelas().get(0).getDataVencimento()).isEqualTo(LocalDate.of(2025, 2, 15));
        assertThat(result.getParcelas().get(1).getDataVencimento()).isEqualTo(LocalDate.of(2025, 3, 15));
        assertThat(result.getParcelas().get(2).getDataVencimento()).isEqualTo(LocalDate.of(2025, 4, 15));
    }

    @Test
    @DisplayName("Deve gerar 6 parcelas com arredondamento na última")
    void createPagamento_6Parcelas_ArredondamentoUltimaParcela() {
        requestDTO.setNumeroParcelas(6);
        requestDTO.setValor(new BigDecimal("100.00"));

        pagamento.setNumeroParcelas(6);
        pagamento.setValor(new BigDecimal("100.00"));

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(assinaturaRepository.findById(assinaturaId)).thenReturn(Optional.of(assinatura));
        when(pagamentoMapper.toEntity(any(), any(), any(), any())).thenReturn(pagamento);
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Pagamento result = pagamentoService.createPagamento(requestDTO);

        assertThat(result.getParcelas()).hasSize(6);
        // 100.00 / 6 = 16.66 (DOWN), last = 100 - (16.66 * 5) = 100 - 83.30 = 16.70
        BigDecimal somaTotal = result.getParcelas().stream()
                .map(Parcela::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(somaTotal).isEqualByComparingTo(new BigDecimal("100.00"));

        // Última parcela deve absorver a diferença
        Parcela ultimaParcela = result.getParcelas().get(5);
        assertThat(ultimaParcela.getValor()).isEqualByComparingTo(new BigDecimal("16.70"));
    }

    // ===== Consultas =====

    @Test
    @DisplayName("Deve buscar pagamento por ID com sucesso")
    void getPagamentoById_Success() {
        when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));

        Pagamento result = pagamentoService.getPagamentoById(pagamentoId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(pagamentoId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando pagamento não encontrado")
    void getPagamentoById_NotFound() {
        when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagamentoService.getPagamentoById(pagamentoId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve listar todos os pagamentos")
    void getAllPagamentos_Success() {
        when(pagamentoRepository.findAll()).thenReturn(List.of(pagamento));

        List<Pagamento> result = pagamentoService.getAllPagamentos();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Deve listar pagamentos por paciente")
    void getPagamentosByPaciente_Success() {
        when(pagamentoRepository.findByPacienteId(pacienteId)).thenReturn(List.of(pagamento));

        List<Pagamento> result = pagamentoService.getPagamentosByPaciente(pacienteId);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Deve listar pagamentos por assinatura")
    void getPagamentosByAssinatura_Success() {
        when(pagamentoRepository.findByAssinaturaId(assinaturaId)).thenReturn(List.of(pagamento));

        List<Pagamento> result = pagamentoService.getPagamentosByAssinatura(assinaturaId);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Deve listar pagamentos por agendamento")
    void getPagamentosByAgendamento_Success() {
        when(pagamentoRepository.findByAgendamentoId(agendamentoId)).thenReturn(List.of(pagamento));

        List<Pagamento> result = pagamentoService.getPagamentosByAgendamento(agendamentoId);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Deve listar pagamentos por período")
    void getPagamentosByPeriodo_Success() {
        LocalDate inicio = LocalDate.of(2025, 2, 1);
        LocalDate fim = LocalDate.of(2025, 2, 28);

        when(pagamentoRepository.findByDataVencimentoBetween(inicio, fim)).thenReturn(List.of(pagamento));

        List<Pagamento> result = pagamentoService.getPagamentosByPeriodo(inicio, fim);

        assertThat(result).hasSize(1);
    }

    // ===== Atualização =====

    @Test
    @DisplayName("Deve atualizar pagamento com sucesso")
    void updatePagamento_Success() {
        PagamentoUpdateDTO updateDTO = new PagamentoUpdateDTO();
        updateDTO.setFormaPagamento(FormaPagamento.CARTAO_CREDITO);
        updateDTO.setObservacoes("Atualizado");

        when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));
        doNothing().when(pagamentoMapper).updateEntityFromDto(updateDTO, pagamento);
        when(pagamentoRepository.save(pagamento)).thenReturn(pagamento);

        Pagamento result = pagamentoService.updatePagamento(pagamentoId, updateDTO);

        assertThat(result).isNotNull();
        verify(pagamentoRepository).save(pagamento);
    }

    // ===== Transições de Status =====

    @Test
    @DisplayName("Deve alterar status de PENDENTE para PAGO")
    void updateStatus_PendenteParaPago_Success() {
        PagamentoStatusDTO statusDTO = new PagamentoStatusDTO(StatusPagamento.PAGO);

        when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));
        when(pagamentoRepository.save(pagamento)).thenReturn(pagamento);

        Pagamento result = pagamentoService.updateStatus(pagamentoId, statusDTO);

        assertThat(result.getStatus()).isEqualTo(StatusPagamento.PAGO);
        assertThat(result.getDataPagamento()).isNotNull();
    }

    @Test
    @DisplayName("Deve alterar status de PENDENTE para CANCELADO")
    void updateStatus_PendenteParaCancelado_Success() {
        PagamentoStatusDTO statusDTO = new PagamentoStatusDTO(StatusPagamento.CANCELADO);

        when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));
        when(pagamentoRepository.save(pagamento)).thenReturn(pagamento);

        Pagamento result = pagamentoService.updateStatus(pagamentoId, statusDTO);

        assertThat(result.getStatus()).isEqualTo(StatusPagamento.CANCELADO);
    }

    @Test
    @DisplayName("Deve alterar status de PAGO para REEMBOLSADO")
    void updateStatus_PagoParaReembolsado_Success() {
        pagamento.setStatus(StatusPagamento.PAGO);
        PagamentoStatusDTO statusDTO = new PagamentoStatusDTO(StatusPagamento.REEMBOLSADO);

        when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));
        when(pagamentoRepository.save(pagamento)).thenReturn(pagamento);

        Pagamento result = pagamentoService.updateStatus(pagamentoId, statusDTO);

        assertThat(result.getStatus()).isEqualTo(StatusPagamento.REEMBOLSADO);
    }

    @Test
    @DisplayName("Deve lançar exceção para transição de status inválida CANCELADO -> PAGO")
    void updateStatus_TransicaoInvalida_CanceladoParaPago() {
        pagamento.setStatus(StatusPagamento.CANCELADO);
        PagamentoStatusDTO statusDTO = new PagamentoStatusDTO(StatusPagamento.PAGO);

        when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));

        assertThatThrownBy(() -> pagamentoService.updateStatus(pagamentoId, statusDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição de status inválida");

        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção para transição de status inválida REEMBOLSADO -> PAGO")
    void updateStatus_TransicaoInvalida_ReembolsadoParaPago() {
        pagamento.setStatus(StatusPagamento.REEMBOLSADO);
        PagamentoStatusDTO statusDTO = new PagamentoStatusDTO(StatusPagamento.PAGO);

        when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));

        assertThatThrownBy(() -> pagamentoService.updateStatus(pagamentoId, statusDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição de status inválida");
    }

    // ===== Pagamento de Parcelas =====

    @Test
    @DisplayName("Deve pagar parcela e atualizar status para PARCIALMENTE_PAGO")
    void updateParcelaStatus_PagaPrimeiraParcela_ParcialmentePago() {
        UUID parcelaId1 = UUID.randomUUID();
        UUID parcelaId2 = UUID.randomUUID();

        Parcela parcela1 = new Parcela();
        parcela1.setId(parcelaId1);
        parcela1.setNumero(1);
        parcela1.setValor(new BigDecimal("150.00"));
        parcela1.setStatus(StatusParcela.PENDENTE);
        parcela1.setPagamento(pagamento);

        Parcela parcela2 = new Parcela();
        parcela2.setId(parcelaId2);
        parcela2.setNumero(2);
        parcela2.setValor(new BigDecimal("150.00"));
        parcela2.setStatus(StatusParcela.PENDENTE);
        parcela2.setPagamento(pagamento);

        pagamento.setParcelas(new ArrayList<>(List.of(parcela1, parcela2)));

        ParcelaStatusDTO parcelaStatusDTO = new ParcelaStatusDTO(StatusParcela.PAGO, null);

        when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Pagamento result = pagamentoService.updateParcelaStatus(pagamentoId, parcelaId1, parcelaStatusDTO);

        assertThat(parcela1.getStatus()).isEqualTo(StatusParcela.PAGO);
        assertThat(parcela1.getDataPagamento()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusPagamento.PARCIALMENTE_PAGO);
    }

    @Test
    @DisplayName("Deve pagar todas as parcelas e atualizar status para PAGO")
    void updateParcelaStatus_PagaTodasParcelas_Pago() {
        UUID parcelaId1 = UUID.randomUUID();
        UUID parcelaId2 = UUID.randomUUID();

        Parcela parcela1 = new Parcela();
        parcela1.setId(parcelaId1);
        parcela1.setNumero(1);
        parcela1.setValor(new BigDecimal("150.00"));
        parcela1.setStatus(StatusParcela.PAGO);
        parcela1.setDataPagamento(LocalDateTime.now());
        parcela1.setPagamento(pagamento);

        Parcela parcela2 = new Parcela();
        parcela2.setId(parcelaId2);
        parcela2.setNumero(2);
        parcela2.setValor(new BigDecimal("150.00"));
        parcela2.setStatus(StatusParcela.PENDENTE);
        parcela2.setPagamento(pagamento);

        pagamento.setParcelas(new ArrayList<>(List.of(parcela1, parcela2)));

        ParcelaStatusDTO parcelaStatusDTO = new ParcelaStatusDTO(StatusParcela.PAGO, null);

        when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        Pagamento result = pagamentoService.updateParcelaStatus(pagamentoId, parcelaId2, parcelaStatusDTO);

        assertThat(parcela2.getStatus()).isEqualTo(StatusParcela.PAGO);
        assertThat(result.getStatus()).isEqualTo(StatusPagamento.PAGO);
        assertThat(result.getDataPagamento()).isNotNull();
    }

    @Test
    @DisplayName("Deve lançar exceção quando parcela não encontrada")
    void updateParcelaStatus_ParcelaNotFound() {
        UUID parcelaIdInexistente = UUID.randomUUID();
        pagamento.setParcelas(new ArrayList<>());

        ParcelaStatusDTO parcelaStatusDTO = new ParcelaStatusDTO(StatusParcela.PAGO, null);

        when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));

        assertThatThrownBy(() -> pagamentoService.updateParcelaStatus(pagamentoId, parcelaIdInexistente, parcelaStatusDTO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve pagar parcela com data personalizada")
    void updateParcelaStatus_ComDataPersonalizada() {
        UUID parcelaId1 = UUID.randomUUID();
        LocalDateTime dataPgto = LocalDateTime.of(2025, 2, 20, 14, 30);

        Parcela parcela1 = new Parcela();
        parcela1.setId(parcelaId1);
        parcela1.setNumero(1);
        parcela1.setValor(new BigDecimal("300.00"));
        parcela1.setStatus(StatusParcela.PENDENTE);
        parcela1.setPagamento(pagamento);

        pagamento.setParcelas(new ArrayList<>(List.of(parcela1)));

        ParcelaStatusDTO parcelaStatusDTO = new ParcelaStatusDTO(StatusParcela.PAGO, dataPgto);

        when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        pagamentoService.updateParcelaStatus(pagamentoId, parcelaId1, parcelaStatusDTO);

        assertThat(parcela1.getDataPagamento()).isEqualTo(dataPgto);
    }

    // ===== Soft Delete =====

    @Test
    @DisplayName("Deve deletar pagamento com sucesso (soft delete)")
    void deletePagamento_Success() {
        when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.of(pagamento));
        when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(pagamento);

        pagamentoService.deletePagamento(pagamentoId);

        assertThat(pagamento.isAtivo()).isFalse();
        verify(pagamentoRepository).save(pagamento);
    }

    @Test
    @DisplayName("Deve lançar exceção ao deletar pagamento inexistente")
    void deletePagamento_NotFound() {
        when(pagamentoRepository.findById(pagamentoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagamentoService.deletePagamento(pagamentoId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
