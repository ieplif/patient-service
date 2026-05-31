package br.com.clinicahumaniza.patient_service.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.clinicahumaniza.patient_service.model.Assinatura;
import br.com.clinicahumaniza.patient_service.model.Servico;
import br.com.clinicahumaniza.patient_service.model.StatusAssinatura;
import br.com.clinicahumaniza.patient_service.repository.AssinaturaRepository;
import br.com.clinicahumaniza.patient_service.repository.PagamentoRepository;

/**
 * Gera automaticamente as cobranças (pagamentos PENDENTES) das assinaturas de Pilates
 * recorrentes na virada do mês, seguindo o ciclo do plano:
 *
 * <ul>
 *   <li>mensal → todo mês</li>
 *   <li>trimestral → a cada 3 meses (mês em que cai o vencimento)</li>
 *   <li>semestral → a cada 6 meses</li>
 * </ul>
 *
 * <p>Idempotência: para cada (assinatura, dataVencimento) é gerado no máximo um pagamento
 * não cancelado — então rodar o scheduler mais de uma vez nunca duplica a cobrança.
 *
 * <p>Roda 1h antes da renovação automática (que adianta o vencimento em até 3 dias), por isso
 * a janela inclui uma pequena margem além do fim do mês para cobrir vencimentos já adiantados.
 */
@Service
public class CobrancaRecorrenteService {

    private static final Logger log = LoggerFactory.getLogger(CobrancaRecorrenteService.class);

    /** Tipos de plano de Pilates que geram mensalidade recorrente. */
    private static final Set<String> TIPOS_PLANO_RECORRENTE = Set.of("mensal", "trimestral", "semestral");

    /** Margem além do fim do mês para cobrir vencimentos já adiantados pela renovação. */
    private static final int DIAS_MARGEM = 5;

    private final AssinaturaRepository assinaturaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final PagamentoService pagamentoService;

    public CobrancaRecorrenteService(
            AssinaturaRepository assinaturaRepository,
            PagamentoRepository pagamentoRepository,
            PagamentoService pagamentoService) {
        this.assinaturaRepository = assinaturaRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.pagamentoService = pagamentoService;
    }

    @Transactional
    public int gerarCobrancasPilatesDoMes() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate limite = hoje.with(TemporalAdjusters.lastDayOfMonth()).plusDays(DIAS_MARGEM);

        // ATIVO + FINALIZADO: recorrentes que completaram as sessões também devem ser cobradas
        // (a renovação as reativa, mas a idempotência por vencimento evita duplicar).
        List<Assinatura> ativas =
                assinaturaRepository.findByStatusIn(List.of(StatusAssinatura.ATIVO, StatusAssinatura.FINALIZADO));
        log.info("Geracao de cobrancas Pilates: {} assinaturas candidatas para avaliar", ativas.size());

        int geradas = 0;
        for (Assinatura assinatura : ativas) {
            try {
                if (gerarCobrancaSeAplicavel(assinatura, inicioMes, limite)) {
                    geradas++;
                }
            } catch (Exception e) {
                log.error(
                        "Erro ao gerar cobranca da assinatura {} (paciente {}): {}",
                        assinatura.getId(),
                        assinatura.getPaciente().getNomeCompleto(),
                        e.getMessage());
            }
        }

        log.info("Geracao de cobrancas Pilates concluida: {} cobrancas geradas", geradas);
        return geradas;
    }

    private boolean gerarCobrancaSeAplicavel(Assinatura assinatura, LocalDate inicioMes, LocalDate limite) {
        if (Boolean.FALSE.equals(assinatura.getRenovacaoAutomatica())) {
            return false;
        }
        if (!isPilatesRecorrente(assinatura)) {
            return false;
        }

        LocalDate venc = assinatura.getDataVencimento();
        if (venc == null || venc.isBefore(inicioMes) || venc.isAfter(limite)) {
            return false;
        }

        if (pagamentoRepository.existsByAssinaturaAndVencimento(assinatura.getId(), venc)) {
            return false;
        }

        BigDecimal valor = resolverValor(assinatura);
        if (valor == null || valor.signum() <= 0) {
            log.warn("Assinatura {} sem valor definido, pulando geracao de cobranca", assinatura.getId());
            return false;
        }

        pagamentoService.gerarCobrancaPendente(assinatura, valor, venc);
        log.info(
                "Cobranca gerada: assinatura={}, paciente={}, vencimento={}, valor={}",
                assinatura.getId(),
                assinatura.getPaciente().getNomeCompleto(),
                venc,
                valor);
        return true;
    }

    private boolean isPilatesRecorrente(Assinatura assinatura) {
        Servico servico = assinatura.getServico();
        if (servico == null || servico.getAtividade() == null || servico.getPlano() == null) {
            return false;
        }
        String atividade = servico.getAtividade().getNome();
        boolean pilates = atividade != null && atividade.toLowerCase().startsWith("pilates");

        String tipoPlano = servico.getPlano().getTipoPlano();
        boolean tipoRecorrente = tipoPlano != null && TIPOS_PLANO_RECORRENTE.contains(tipoPlano.toLowerCase());

        return pilates && tipoRecorrente;
    }

    /** Preço vigente do serviço; cai para o valor da assinatura se o serviço não tiver preço. */
    private BigDecimal resolverValor(Assinatura assinatura) {
        Servico servico = assinatura.getServico();
        if (servico != null && servico.getValor() != null && servico.getValor().signum() > 0) {
            return servico.getValor();
        }
        return assinatura.getValor();
    }
}
