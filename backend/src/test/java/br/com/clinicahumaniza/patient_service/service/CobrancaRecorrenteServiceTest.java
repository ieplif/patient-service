package br.com.clinicahumaniza.patient_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.clinicahumaniza.patient_service.model.Assinatura;
import br.com.clinicahumaniza.patient_service.model.Atividade;
import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.model.Plano;
import br.com.clinicahumaniza.patient_service.model.Servico;
import br.com.clinicahumaniza.patient_service.model.StatusAssinatura;
import br.com.clinicahumaniza.patient_service.repository.AssinaturaRepository;
import br.com.clinicahumaniza.patient_service.repository.PagamentoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CobrancaRecorrenteService")
class CobrancaRecorrenteServiceTest {

    @Mock
    private AssinaturaRepository assinaturaRepository;

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private PagamentoService pagamentoService;

    @InjectMocks
    private CobrancaRecorrenteService cobrancaService;

    private Assinatura pilatesMensal(LocalDate vencimento) {
        return assinatura("Pilates Clássico", "mensal", vencimento, true, new BigDecimal("300.00"));
    }

    private Assinatura assinatura(
            String atividadeNome,
            String tipoPlano,
            LocalDate vencimento,
            boolean renovacaoAutomatica,
            BigDecimal valorServico) {
        Patient paciente = new Patient();
        paciente.setId(UUID.randomUUID());
        paciente.setNomeCompleto("Maria Santos");

        Atividade atividade = new Atividade();
        atividade.setNome(atividadeNome);

        Plano plano = new Plano();
        plano.setTipoPlano(tipoPlano);

        Servico servico = new Servico();
        servico.setAtividade(atividade);
        servico.setPlano(plano);
        servico.setValor(valorServico);

        Assinatura assinatura = new Assinatura();
        assinatura.setId(UUID.randomUUID());
        assinatura.setPaciente(paciente);
        assinatura.setServico(servico);
        assinatura.setStatus(StatusAssinatura.ATIVO);
        assinatura.setDataVencimento(vencimento);
        assinatura.setRenovacaoAutomatica(renovacaoAutomatica);
        assinatura.setValor(new BigDecimal("250.00"));
        return assinatura;
    }

    @Test
    @DisplayName("Deve gerar cobrança para Pilates mensal com vencimento no mês corrente")
    void gera_PilatesMensalNoMes() {
        Assinatura assinatura = pilatesMensal(LocalDate.now().withDayOfMonth(15));
        when(assinaturaRepository.findByStatusIn(any())).thenReturn(List.of(assinatura));
        when(pagamentoRepository.existsByAssinaturaAndVencimento(eq(assinatura.getId()), any(LocalDate.class)))
                .thenReturn(false);

        int geradas = cobrancaService.gerarCobrancasPilatesDoMes();

        assertThat(geradas).isEqualTo(1);
        verify(pagamentoService)
                .gerarCobrancaPendente(
                        eq(assinatura), eq(new BigDecimal("300.00")), eq(assinatura.getDataVencimento()));
    }

    @Test
    @DisplayName("Não deve duplicar cobrança quando já existe pagamento para o vencimento")
    void naoDuplica_QuandoJaExiste() {
        Assinatura assinatura = pilatesMensal(LocalDate.now().withDayOfMonth(15));
        when(assinaturaRepository.findByStatusIn(any())).thenReturn(List.of(assinatura));
        when(pagamentoRepository.existsByAssinaturaAndVencimento(eq(assinatura.getId()), any(LocalDate.class)))
                .thenReturn(true);

        int geradas = cobrancaService.gerarCobrancasPilatesDoMes();

        assertThat(geradas).isZero();
        verify(pagamentoService, never()).gerarCobrancaPendente(any(), any(), any());
    }

    @Test
    @DisplayName("Deve ignorar assinatura que não é de Pilates")
    void ignora_NaoPilates() {
        Assinatura fisio = assinatura(
                "Fisioterapia Pélvica", "mensal", LocalDate.now().withDayOfMonth(15), true, new BigDecimal("400.00"));
        when(assinaturaRepository.findByStatusIn(any())).thenReturn(List.of(fisio));

        int geradas = cobrancaService.gerarCobrancasPilatesDoMes();

        assertThat(geradas).isZero();
        verify(pagamentoService, never()).gerarCobrancaPendente(any(), any(), any());
    }

    @Test
    @DisplayName("Deve ignorar Pilates sem renovação automática")
    void ignora_SemRenovacaoAutomatica() {
        Assinatura avulso = assinatura(
                "Pilates Clássico", "avulso", LocalDate.now().withDayOfMonth(15), false, new BigDecimal("80.00"));
        when(assinaturaRepository.findByStatusIn(any())).thenReturn(List.of(avulso));

        int geradas = cobrancaService.gerarCobrancasPilatesDoMes();

        assertThat(geradas).isZero();
        verify(pagamentoService, never()).gerarCobrancaPendente(any(), any(), any());
    }

    @Test
    @DisplayName("Deve ignorar trimestral cujo vencimento está fora da janela do mês")
    void ignora_VencimentoForaDaJanela() {
        Assinatura trimestral = assinatura(
                "Pilates Funcional", "trimestral", LocalDate.now().plusMonths(2), true, new BigDecimal("800.00"));
        when(assinaturaRepository.findByStatusIn(any())).thenReturn(List.of(trimestral));

        int geradas = cobrancaService.gerarCobrancasPilatesDoMes();

        assertThat(geradas).isZero();
        verify(pagamentoService, never()).gerarCobrancaPendente(any(), any(), any());
    }

    @Test
    @DisplayName("Deve usar valor da assinatura quando serviço não tem preço")
    void usaValorAssinatura_QuandoServicoSemPreco() {
        Assinatura assinatura = pilatesMensal(LocalDate.now().withDayOfMonth(10));
        assinatura.getServico().setValor(null);
        when(assinaturaRepository.findByStatusIn(any())).thenReturn(List.of(assinatura));
        when(pagamentoRepository.existsByAssinaturaAndVencimento(eq(assinatura.getId()), any(LocalDate.class)))
                .thenReturn(false);

        int geradas = cobrancaService.gerarCobrancasPilatesDoMes();

        assertThat(geradas).isEqualTo(1);
        verify(pagamentoService, times(1))
                .gerarCobrancaPendente(
                        eq(assinatura), eq(new BigDecimal("250.00")), eq(assinatura.getDataVencimento()));
    }
}
