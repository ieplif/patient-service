package br.com.clinicahumaniza.patient_service.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.clinicahumaniza.patient_service.service.CobrancaRecorrenteService;

@Component
public class CobrancaRecorrenteScheduler {

    private static final Logger log = LoggerFactory.getLogger(CobrancaRecorrenteScheduler.class);

    private final CobrancaRecorrenteService cobrancaService;

    public CobrancaRecorrenteScheduler(CobrancaRecorrenteService cobrancaService) {
        this.cobrancaService = cobrancaService;
    }

    /**
     * Roda no dia 1 de cada mês às 05h — uma hora antes da renovação automática (06h), para
     * avaliar o vencimento do ciclo vigente antes de ele ser adiantado pela renovação.
     */
    @Scheduled(cron = "0 0 5 1 * *")
    public void gerarCobrancasMensais() {
        log.info("Iniciando geracao automatica de cobrancas mensais (Pilates)");
        try {
            int geradas = cobrancaService.gerarCobrancasPilatesDoMes();
            log.info("Geracao automatica de cobrancas concluida: {} cobrancas geradas", geradas);
        } catch (Exception e) {
            log.error("Erro na geracao automatica de cobrancas mensais: {}", e.getMessage(), e);
        }
    }
}
