package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.model.AgendamentoRecorrente;
import br.com.clinicahumaniza.patient_service.model.Assinatura;
import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.model.StatusAssinatura;
import br.com.clinicahumaniza.patient_service.repository.AgendamentoRecorrenteRepository;
import br.com.clinicahumaniza.patient_service.repository.AssinaturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(assinaturaRepository.findByRenovacaoAutomaticaTrueAndStatusAndDataVencimentoLessThanEqual(
                eq(StatusAssinatura.ATIVO), any(LocalDate.class)))
                .thenReturn(List.of());

        int renovadas = renovacaoService.renovarAssinaturasProximasDoVencimento();

        assertThat(renovadas).isZero();
    }

    @Test
    @DisplayName("Deve pular assinatura sem agendamentos recorrentes vinculados")
    void renovar_AssinaturaSemTemplates() {
        when(assinaturaRepository.findByRenovacaoAutomaticaTrueAndStatusAndDataVencimentoLessThanEqual(
                eq(StatusAssinatura.ATIVO), any(LocalDate.class)))
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
        when(assinaturaRepository.findByRenovacaoAutomaticaTrueAndStatusAndDataVencimentoLessThanEqual(
                eq(StatusAssinatura.ATIVO), any(LocalDate.class)))
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
}
