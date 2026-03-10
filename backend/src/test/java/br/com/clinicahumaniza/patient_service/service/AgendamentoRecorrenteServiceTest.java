package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.*;
import br.com.clinicahumaniza.patient_service.exception.BusinessException;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.AgendamentoMapper;
import br.com.clinicahumaniza.patient_service.model.*;
import br.com.clinicahumaniza.patient_service.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendamentoRecorrenteServiceTest {

    @Mock
    private AgendamentoService agendamentoService;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private AgendamentoRecorrenteRepository recorrenteRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ProfissionalRepository profissionalRepository;

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private AssinaturaRepository assinaturaRepository;

    @Mock
    private AgendamentoMapper agendamentoMapper;

    private AgendamentoRecorrenteService service;

    private Patient paciente;
    private Profissional profissional;
    private Servico servico;
    private Atividade atividade;
    private Plano plano;
    private Assinatura assinatura;
    private UUID pacienteId;
    private UUID profissionalId;
    private UUID servicoId;
    private UUID assinaturaId;

    @BeforeEach
    void setUp() {
        service = new AgendamentoRecorrenteService(
                agendamentoService, agendamentoRepository, recorrenteRepository,
                patientRepository, profissionalRepository, servicoRepository,
                assinaturaRepository, agendamentoMapper);

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
        assinatura.setSessoesContratadas(8);
        assinatura.setSessoesRealizadas(0);
    }

    private AgendamentoRecorrenteRequestDTO buildRequestDTO(List<DayOfWeek> dias, Integer totalSessoes, LocalDate dataFim) {
        AgendamentoRecorrenteRequestDTO dto = new AgendamentoRecorrenteRequestDTO();
        dto.setPacienteId(pacienteId);
        dto.setProfissionalId(profissionalId);
        dto.setServicoId(servicoId);
        dto.setFrequencia(FrequenciaRecorrencia.SEMANAL);
        dto.setDiasSemana(dias);
        dto.setHoraInicio(LocalTime.of(10, 0));
        dto.setDuracaoMinutos(50);
        dto.setTotalSessoes(totalSessoes);
        dto.setDataFim(dataFim);
        return dto;
    }

    private void mockBasicLookups() {
        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.of(servico));
    }

    private Agendamento createMockAgendamento(UUID id) {
        Agendamento ag = new Agendamento();
        ag.setId(id);
        ag.setPaciente(paciente);
        ag.setProfissional(profissional);
        ag.setServico(servico);
        ag.setDuracaoMinutos(50);
        ag.setStatus(StatusAgendamento.AGENDADO);
        ag.setAtivo(true);
        return ag;
    }

    private void mockRecorrenteRepositorySave() {
        when(recorrenteRepository.save(any(AgendamentoRecorrente.class))).thenAnswer(invocation -> {
            AgendamentoRecorrente r = invocation.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
    }

    private void mockAgendamentoCreation(int count) {
        when(agendamentoService.createAgendamento(any(AgendamentoRequestDTO.class))).thenAnswer(invocation -> {
            Agendamento ag = createMockAgendamento(UUID.randomUUID());
            AgendamentoRequestDTO req = invocation.getArgument(0);
            ag.setDataHora(req.getDataHora());
            return ag;
        });
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agendamentoMapper.toResponseDTO(any(Agendamento.class))).thenReturn(new AgendamentoResponseDTO());
    }

    // --- Testes de geração de datas ---

    @Test
    @DisplayName("Deve gerar 4 datas semanais com 1 dia (segunda) por 4 sessões")
    void gerarDatas_Semanal_1Dia_4Sessoes() {
        List<LocalDateTime> datas = service.gerarDatas(
                FrequenciaRecorrencia.SEMANAL,
                List.of(DayOfWeek.MONDAY),
                LocalTime.of(10, 0),
                4, null);

        assertThat(datas).hasSize(4);
        datas.forEach(d -> assertThat(d.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY));
        assertThat(datas.get(0).toLocalTime()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("Deve gerar 4 datas semanais com 2 dias (seg, qua) por 4 sessões")
    void gerarDatas_Semanal_2Dias_4Sessoes() {
        List<LocalDateTime> datas = service.gerarDatas(
                FrequenciaRecorrencia.SEMANAL,
                List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                LocalTime.of(10, 0),
                4, null);

        assertThat(datas).hasSize(4);
        datas.forEach(d ->
                assertThat(d.getDayOfWeek()).isIn(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
    }

    @Test
    @DisplayName("Deve gerar datas quinzenais por 6 sessões")
    void gerarDatas_Quinzenal_6Sessoes() {
        List<LocalDateTime> datas = service.gerarDatas(
                FrequenciaRecorrencia.QUINZENAL,
                List.of(DayOfWeek.TUESDAY),
                LocalTime.of(14, 0),
                6, null);

        assertThat(datas).hasSize(6);
        datas.forEach(d -> assertThat(d.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY));

        // Verifica intervalo de 2 semanas entre sessões
        for (int i = 1; i < datas.size(); i++) {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                    datas.get(i - 1).toLocalDate(), datas.get(i).toLocalDate());
            assertThat(daysBetween).isEqualTo(14);
        }
    }

    @Test
    @DisplayName("Deve gerar datas mensais por 3 sessões")
    void gerarDatas_Mensal_3Sessoes() {
        List<LocalDateTime> datas = service.gerarDatas(
                FrequenciaRecorrencia.MENSAL,
                List.of(DayOfWeek.MONDAY),
                LocalTime.of(10, 0),
                3, null);

        assertThat(datas).hasSize(3);
    }

    @Test
    @DisplayName("Deve respeitar dataFim como limite")
    void gerarDatas_ComDataFim() {
        LocalDate dataFim = LocalDate.now().plusWeeks(3);

        List<LocalDateTime> datas = service.gerarDatas(
                FrequenciaRecorrencia.SEMANAL,
                List.of(DayOfWeek.MONDAY),
                LocalTime.of(10, 0),
                null, dataFim);

        datas.forEach(d ->
                assertThat(d.toLocalDate()).isBeforeOrEqualTo(dataFim));
    }

    @Test
    @DisplayName("Deve respeitar totalSessoes E dataFim (o que vier primeiro)")
    void gerarDatas_TotalSessoesEDataFim() {
        LocalDate dataFimDistante = LocalDate.now().plusYears(1);

        List<LocalDateTime> datas = service.gerarDatas(
                FrequenciaRecorrencia.SEMANAL,
                List.of(DayOfWeek.MONDAY),
                LocalTime.of(10, 0),
                3, dataFimDistante);

        assertThat(datas).hasSize(3);
    }

    @Test
    @DisplayName("Deve respeitar limite de segurança de 52 datas")
    void gerarDatas_LimiteSeguranca() {
        LocalDate dataFimDistante = LocalDate.now().plusYears(5);

        List<LocalDateTime> datas = service.gerarDatas(
                FrequenciaRecorrencia.SEMANAL,
                List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                LocalTime.of(10, 0),
                null, dataFimDistante);

        assertThat(datas).hasSizeLessThanOrEqualTo(52);
    }

    // --- Testes de criação de recorrência ---

    @Test
    @DisplayName("Deve criar recorrência semanal com sucesso")
    void createRecorrente_Semanal_Success() {
        AgendamentoRecorrenteRequestDTO dto = buildRequestDTO(
                List.of(DayOfWeek.MONDAY), 4, null);

        mockBasicLookups();
        mockRecorrenteRepositorySave();
        mockAgendamentoCreation(4);

        AgendamentoRecorrenteResponseDTO response = service.createRecorrente(dto);

        assertThat(response).isNotNull();
        assertThat(response.getAgendamentosCriados()).hasSize(4);
        assertThat(response.getDatasIgnoradas()).isEmpty();
        verify(agendamentoService, times(4)).createAgendamento(any());
    }

    @Test
    @DisplayName("Deve criar recorrência com 2 dias por semana")
    void createRecorrente_2DiasPorSemana() {
        AgendamentoRecorrenteRequestDTO dto = buildRequestDTO(
                List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), 4, null);

        mockBasicLookups();
        mockRecorrenteRepositorySave();
        mockAgendamentoCreation(4);

        AgendamentoRecorrenteResponseDTO response = service.createRecorrente(dto);

        assertThat(response.getAgendamentosCriados()).hasSize(4);
    }

    @Test
    @DisplayName("Deve lidar com conflito em 1 data - 3 criados + 1 ignorada")
    void createRecorrente_ComConflito() {
        AgendamentoRecorrenteRequestDTO dto = buildRequestDTO(
                List.of(DayOfWeek.MONDAY), 4, null);

        mockBasicLookups();
        mockRecorrenteRepositorySave();

        // Primeiro 3 chamadas ok, quarta com conflito
        when(agendamentoService.createAgendamento(any(AgendamentoRequestDTO.class)))
                .thenAnswer(invocation -> {
                    Agendamento ag = createMockAgendamento(UUID.randomUUID());
                    AgendamentoRequestDTO req = invocation.getArgument(0);
                    ag.setDataHora(req.getDataHora());
                    return ag;
                })
                .thenAnswer(invocation -> {
                    Agendamento ag = createMockAgendamento(UUID.randomUUID());
                    AgendamentoRequestDTO req = invocation.getArgument(0);
                    ag.setDataHora(req.getDataHora());
                    return ag;
                })
                .thenAnswer(invocation -> {
                    Agendamento ag = createMockAgendamento(UUID.randomUUID());
                    AgendamentoRequestDTO req = invocation.getArgument(0);
                    ag.setDataHora(req.getDataHora());
                    return ag;
                })
                .thenThrow(new BusinessException("Conflito de horário"));

        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agendamentoMapper.toResponseDTO(any(Agendamento.class))).thenReturn(new AgendamentoResponseDTO());

        AgendamentoRecorrenteResponseDTO response = service.createRecorrente(dto);

        assertThat(response.getAgendamentosCriados()).hasSize(3);
        assertThat(response.getDatasIgnoradas()).hasSize(1);
        assertThat(response.getDatasIgnoradas().get(0).getMotivo()).contains("Conflito");
    }

    @Test
    @DisplayName("Deve retornar 0 criados quando todas as datas têm conflito")
    void createRecorrente_TodosConflitos() {
        AgendamentoRecorrenteRequestDTO dto = buildRequestDTO(
                List.of(DayOfWeek.MONDAY), 3, null);

        mockBasicLookups();
        mockRecorrenteRepositorySave();

        when(agendamentoService.createAgendamento(any(AgendamentoRequestDTO.class)))
                .thenThrow(new BusinessException("Conflito de horário"));

        AgendamentoRecorrenteResponseDTO response = service.createRecorrente(dto);

        assertThat(response.getAgendamentosCriados()).isEmpty();
        assertThat(response.getDatasIgnoradas()).hasSize(3);
    }

    @Test
    @DisplayName("Deve lançar exceção sem totalSessoes nem dataFim")
    void createRecorrente_SemLimite() {
        AgendamentoRecorrenteRequestDTO dto = buildRequestDTO(
                List.of(DayOfWeek.MONDAY), null, null);

        assertThatThrownBy(() -> service.createRecorrente(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("totalSessoes ou dataFim");
    }

    @Test
    @DisplayName("Deve lançar exceção com paciente inexistente")
    void createRecorrente_PacienteNotFound() {
        AgendamentoRecorrenteRequestDTO dto = buildRequestDTO(
                List.of(DayOfWeek.MONDAY), 4, null);

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRecorrente(dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve lançar exceção com profissional inexistente")
    void createRecorrente_ProfissionalNotFound() {
        AgendamentoRecorrenteRequestDTO dto = buildRequestDTO(
                List.of(DayOfWeek.MONDAY), 4, null);

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRecorrente(dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve lançar exceção com serviço inexistente")
    void createRecorrente_ServicoNotFound() {
        AgendamentoRecorrenteRequestDTO dto = buildRequestDTO(
                List.of(DayOfWeek.MONDAY), 4, null);

        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));
        when(servicoRepository.findById(servicoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRecorrente(dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve lançar exceção quando profissional não atende atividade")
    void createRecorrente_ProfissionalNaoAtendeAtividade() {
        AgendamentoRecorrenteRequestDTO dto = buildRequestDTO(
                List.of(DayOfWeek.MONDAY), 4, null);

        Atividade outraAtividade = new Atividade();
        outraAtividade.setId(UUID.randomUUID());
        outraAtividade.setNome("Yoga");
        profissional.setAtividades(new HashSet<>(Set.of(outraAtividade)));

        mockBasicLookups();

        assertThatThrownBy(() -> service.createRecorrente(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não atende a atividade");
    }

    @Test
    @DisplayName("Deve usar duração padrão da atividade quando não informada")
    void createRecorrente_DuracaoPadrao() {
        AgendamentoRecorrenteRequestDTO dto = buildRequestDTO(
                List.of(DayOfWeek.MONDAY), 2, null);
        dto.setDuracaoMinutos(null);

        mockBasicLookups();
        mockRecorrenteRepositorySave();
        mockAgendamentoCreation(2);

        AgendamentoRecorrenteResponseDTO response = service.createRecorrente(dto);

        assertThat(response).isNotNull();
        assertThat(response.getDuracaoMinutos()).isEqualTo(50); // padrão da atividade
    }

    // --- Testes de cancelamento ---

    @Test
    @DisplayName("Deve cancelar somente um agendamento (cancelarFuturos=false)")
    void cancelarRecorrencia_SomenteUm() {
        UUID agendamentoId = UUID.randomUUID();
        Agendamento agendamento = createMockAgendamento(agendamentoId);
        agendamento.setDataHora(LocalDateTime.of(2025, 6, 2, 10, 0));

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
        when(agendamentoService.updateStatus(eq(agendamentoId), any(AgendamentoStatusDTO.class)))
                .thenReturn(agendamento);
        when(agendamentoMapper.toResponseDTO(agendamento)).thenReturn(new AgendamentoResponseDTO());

        List<AgendamentoResponseDTO> result = service.cancelarRecorrencia(agendamentoId, false);

        assertThat(result).hasSize(1);
        verify(agendamentoService).updateStatus(eq(agendamentoId), any());
    }

    @Test
    @DisplayName("Deve cancelar esse e futuros (cancelarFuturos=true)")
    void cancelarRecorrencia_EsseFuturos() {
        UUID agendamentoId = UUID.randomUUID();
        UUID recorrenteId = UUID.randomUUID();

        AgendamentoRecorrente recorrente = new AgendamentoRecorrente();
        recorrente.setId(recorrenteId);

        Agendamento agendamento = createMockAgendamento(agendamentoId);
        agendamento.setDataHora(LocalDateTime.of(2025, 6, 9, 10, 0));
        agendamento.setAgendamentoRecorrente(recorrente);

        // 3 agendamentos futuros (incluindo o atual)
        Agendamento futuro1 = createMockAgendamento(UUID.randomUUID());
        futuro1.setDataHora(LocalDateTime.of(2025, 6, 9, 10, 0));
        Agendamento futuro2 = createMockAgendamento(UUID.randomUUID());
        futuro2.setDataHora(LocalDateTime.of(2025, 6, 16, 10, 0));
        Agendamento futuro3 = createMockAgendamento(UUID.randomUUID());
        futuro3.setDataHora(LocalDateTime.of(2025, 6, 23, 10, 0));

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.findByAgendamentoRecorrenteIdAndDataHoraGreaterThanEqualAndStatusIn(
                eq(recorrenteId), any(), any()))
                .thenReturn(List.of(futuro1, futuro2, futuro3));
        when(agendamentoService.updateStatus(any(UUID.class), any(AgendamentoStatusDTO.class)))
                .thenAnswer(invocation -> {
                    Agendamento a = createMockAgendamento(invocation.getArgument(0));
                    a.setStatus(StatusAgendamento.CANCELADO);
                    return a;
                });
        when(agendamentoMapper.toResponseDTO(any(Agendamento.class))).thenReturn(new AgendamentoResponseDTO());

        List<AgendamentoResponseDTO> result = service.cancelarRecorrencia(agendamentoId, true);

        assertThat(result).hasSize(3);
        verify(agendamentoService, times(3)).updateStatus(any(UUID.class), any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao cancelar futuros sem recorrência")
    void cancelarRecorrencia_SemRecorrencia() {
        UUID agendamentoId = UUID.randomUUID();
        Agendamento agendamento = createMockAgendamento(agendamentoId);
        agendamento.setAgendamentoRecorrente(null);

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> service.cancelarRecorrencia(agendamentoId, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não faz parte de uma recorrência");
    }

    // --- Teste de consulta ---

    @Test
    @DisplayName("Deve retornar recorrência por agendamento")
    void getRecorrenciaByAgendamento_Success() {
        UUID agendamentoId = UUID.randomUUID();

        AgendamentoRecorrente recorrente = new AgendamentoRecorrente();
        recorrente.setId(UUID.randomUUID());
        recorrente.setPaciente(paciente);
        recorrente.setProfissional(profissional);
        recorrente.setServico(servico);
        recorrente.setFrequencia(FrequenciaRecorrencia.SEMANAL);
        recorrente.setDiasSemana("MONDAY");
        recorrente.setHoraInicio(LocalTime.of(10, 0));
        recorrente.setDuracaoMinutos(50);
        recorrente.setTotalSessoes(4);

        Agendamento agendamento = createMockAgendamento(agendamentoId);
        agendamento.setAgendamentoRecorrente(recorrente);

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));

        AgendamentoRecorrenteResponseDTO response = service.getRecorrenciaByAgendamento(agendamentoId);

        assertThat(response).isNotNull();
        assertThat(response.getFrequencia()).isEqualTo(FrequenciaRecorrencia.SEMANAL);
        assertThat(response.getDiasSemana()).containsExactly(DayOfWeek.MONDAY);
    }

    @Test
    @DisplayName("Deve lançar exceção ao consultar recorrência de agendamento sem recorrência")
    void getRecorrenciaByAgendamento_SemRecorrencia() {
        UUID agendamentoId = UUID.randomUUID();
        Agendamento agendamento = createMockAgendamento(agendamentoId);
        agendamento.setAgendamentoRecorrente(null);

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> service.getRecorrenciaByAgendamento(agendamentoId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não faz parte de uma recorrência");
    }
}
