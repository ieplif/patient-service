package br.com.clinicahumaniza.patient_service.scheduler;

import br.com.clinicahumaniza.patient_service.service.AssinaturaRenovacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AssinaturaRenovacaoScheduler {

    private static final Logger log = LoggerFactory.getLogger(AssinaturaRenovacaoScheduler.class);

    private final AssinaturaRenovacaoService renovacaoService;

    public AssinaturaRenovacaoScheduler(AssinaturaRenovacaoService renovacaoService) {
        this.renovacaoService = renovacaoService;
    }

    @Scheduled(cron = "0 0 6 * * *")
    public void verificarRenovacoes() {
        log.info("Iniciando verificacao de renovacao automatica de assinaturas");
        try {
            int renovadas = renovacaoService.renovarAssinaturasProximasDoVencimento();
            log.info("Verificacao de renovacao concluida: {} assinaturas renovadas", renovadas);
        } catch (Exception e) {
            log.error("Erro na verificacao de renovacao automatica: {}", e.getMessage(), e);
        }
    }
}
