package br.com.clinicahumaniza.patient_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.clinicahumaniza.patient_service.dto.AgendamentoRecorrenteRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.AgendamentoRecorrenteResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.AgendamentoResponseDTO;
import br.com.clinicahumaniza.patient_service.model.AgendamentoRecorrente;
import br.com.clinicahumaniza.patient_service.model.Assinatura;
import br.com.clinicahumaniza.patient_service.model.FrequenciaRecorrencia;
import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.model.Servico;
import br.com.clinicahumaniza.patient_service.model.StatusAssinatura;
import br.com.clinicahumaniza.patient_service.repository.AgendamentoRecorrenteRepository;
import br.com.clinicahumaniza.patient_service.repository.AssinaturaRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssinaturaRenovacaoService")
class AssinaturaRenovacaoServiceTest {

    @Mock
    private AssinaturaRepository assinaturaRepository;

    @Mock
    private AgendamentoRecorrenteRepository recorrenteRepository;

    @Mock
    private AgendamentoRecorrenteService agendamentoRecorrenteService;

    @InjectMocks
    private AssinaturaRenovacaoService renovacaoService;

    private Assinatura assinatura;

    @BeforeEach
    void setUp() {
        Patient paciente = new Patient();
        paciente.setId(UUID.randomUUID());
        paciente.setNomeCompleto("Maria Santos");

        assinatura = new Assinatura();
        assinatura.setId(UUID.randomUUID());
        assinatura.setPaciente(paciente);
        assinatura.setStatus(StatusAssinatura.ATIVO);
        assinatura.setDataVencimento(LocalDate.now().plusDays(1));
        assinatura.setSessoesContratadas(8);
        assinatura.setSessoesRealizadas(0);
    }

    @Test
    @DisplayName("Deve retornar 0 quando não há assinaturas próximas do vencimento")
    void renovar_NenhumaAssinatura() {
        when(assinaturaRepository.findByRenovacaoAutomaticaTrueAndStatusInAndDataVencimentoLessThanEqual(
                        any(), any(LocalDate.class)))
                .thenReturn(List.of());

        int renovadas = renovacaoService.renovarAssinaturasProximasDoVencimento();

        assertThat(renovadas).isZero();
    }

    @Test
    @DisplayName("Deve pular assinatura sem agendamentos recorrentes vinculados")
    void renovar_AssinaturaSemTemplates() {
        when(assinaturaRepository.findByRenovacaoAutomaticaTrueAndStatusInAndDataVencimentoLessThanEqual(
                        any(), any(LocalDate.class)))
                .thenReturn(List.of(assinatura));
        when(recorrenteRepository.findByAssinaturaIdAndAtivoTrue(assinatura.getId()))
                .thenReturn(List.of());

        int renovadas = renovacaoService.renovarAssinaturasProximasDoVencimento();

        // Sem templates a renovação nao cria nada, mas tambem nao lanca erro
        assertThat(renovadas).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve capturar erro de renovação e anotar nas observações")
    void renovar_ErroNaRenovacao() {
        when(assinaturaRepository.findByRenovacaoAutomaticaTrueAndStatusInAndDataVencimentoLessThanEqual(
                        any(), any(LocalDate.class)))
                .thenReturn(List.of(assinatura));
        // Template presente mas servico null -> NPE dentro de renovarAssinatura,
        // capturada pelo loop externo
        when(recorrenteRepository.findByAssinaturaIdAndAtivoTrue(assinatura.getId()))
                .thenReturn(List.of(new AgendamentoRecorrente()));

        int renovadas = renovacaoService.renovarAssinaturasProximasDoVencimento();

        assertThat(renovadas).isZero();
        // A falha deve ter sido anotada e a assinatura salva
        verify(assinaturaRepository).save(assinatura);
        assertThat(assinatura.getObservacoes()).contains("Falha na renovacao automatica");
    }

    // --- Regressão: bug de duplicação na renovação (templates repetidos) ---

    private AgendamentoRecorrente template(String diasSemana, LocalTime hora) {
        AgendamentoRecorrente t = new AgendamentoRecorrente();
        t.setId(UUID.randomUUID());
        t.setAtivo(true);
        t.setPaciente(assinatura.getPaciente());
        Servico servico = new Servico();
        servico.setId(UUID.randomUUID());
        t.setServico(servico);
        t.setAssinatura(assinatura);
        t.setFrequencia(FrequenciaRecorrencia.SEMANAL);
        t.setDiasSemana(diasSemana);
        t.setHoraInicio(hora);
        t.setDuracaoMinutos(50);
        return t;
    }

    private AgendamentoRecorrenteResponseDTO respostaComUmCriado() {
        AgendamentoRecorrenteResponseDTO resp = new AgendamentoRecorrenteResponseDTO();
        resp.setAgendamentosCriados(List.of(new AgendamentoResponseDTO()));
        return resp;
    }

    @Test
    @DisplayName("Regressão: renovação com templates duplicados gera 1 recorrência por slot distinto")
    void renovar_NaoDuplicaComTemplatesRepetidos() {
        // Cenário do bug: renovações anteriores deixaram 2 templates ativos do MESMO slot
        AgendamentoRecorrente terca1 = template("TUESDAY", LocalTime.of(10, 0));
        AgendamentoRecorrente terca2 = template("TUESDAY", LocalTime.of(10, 0));
        AgendamentoRecorrente quinta = template("THURSDAY", LocalTime.of(8, 0));

        when(assinaturaRepository.findByRenovacaoAutomaticaTrueAndStatusInAndDataVencimentoLessThanEqual(
                        any(), any(LocalDate.class)))
                .thenReturn(List.of(assinatura));
        when(recorrenteRepository.findByAssinaturaIdAndAtivoTrue(assinatura.getId()))
                .thenReturn(List.of(terca1, terca2, quinta));
        when(agendamentoRecorrenteService.createRecorrente(any())).thenReturn(respostaComUmCriado());

        renovacaoService.renovarAssinaturasProximasDoVencimento();

        // 2 slots distintos (Ter 10h, Qui 8h) -> exatamente 2 recorrências, não 3
        ArgumentCaptor<AgendamentoRecorrenteRequestDTO> captor =
                ArgumentCaptor.forClass(AgendamentoRecorrenteRequestDTO.class);
        verify(agendamentoRecorrenteService, times(2)).createRecorrente(captor.capture());
        List<String> slots = captor.getAllValues().stream()
                .map(d -> d.getDiasSemana().get(0) + "|" + d.getHoraInicio())
                .collect(Collectors.toList());
        assertThat(slots).containsExactlyInAnyOrder("TUESDAY|10:00", "THURSDAY|08:00");

        // Todos os templates do ciclo anterior devem ser desativados (senão a próxima renovação duplica)
        assertThat(terca1.isAtivo()).isFalse();
        assertThat(terca2.isAtivo()).isFalse();
        assertThat(quinta.isAtivo()).isFalse();
    }

    @Test
    @DisplayName("Regressão: vencimento avança por mês de calendário, não +30 dias fixos")
    void renovar_AvancaVencimentoPorMesCalendario() {
        LocalDate vencimentoAtual = assinatura.getDataVencimento();

        when(assinaturaRepository.findByRenovacaoAutomaticaTrueAndStatusInAndDataVencimentoLessThanEqual(
                        any(), any(LocalDate.class)))
                .thenReturn(List.of(assinatura));
        when(recorrenteRepository.findByAssinaturaIdAndAtivoTrue(assinatura.getId()))
                .thenReturn(List.of(template("TUESDAY", LocalTime.of(10, 0))));
        when(agendamentoRecorrenteService.createRecorrente(any())).thenReturn(respostaComUmCriado());

        renovacaoService.renovarAssinaturasProximasDoVencimento();

        // Novo ciclo: [venc+1 .. venc+1 + 1 mês - 1] — mantém o dia do mês entre renovações
        LocalDate esperado = vencimentoAtual.plusDays(1).plusMonths(1).minusDays(1);
        assertThat(assinatura.getDataVencimento()).isEqualTo(esperado);
    }
}
