package br.com.clinicahumaniza.patient_service.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.repository.PatientRepository;
import br.com.clinicahumaniza.patient_service.util.BuscaNome;
import lombok.RequiredArgsConstructor;

/**
 * Preenche {@code nomeNormalizado} dos pacientes já existentes (criados antes da
 * coluna). Idempotente: roda só nos que estão nulos; após o primeiro start, não faz nada.
 */
@Component
@RequiredArgsConstructor
public class NomeNormalizadoBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NomeNormalizadoBackfill.class);

    private final PatientRepository patientRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Patient> pendentes = patientRepository.findByNomeNormalizadoIsNull();
        if (pendentes.isEmpty()) return;
        // Seta explicitamente para marcar a entidade como suja e forçar o UPDATE
        // (o @PreUpdate recalcularia o mesmo valor de qualquer forma).
        pendentes.forEach(p -> p.setNomeNormalizado(BuscaNome.normalizar(p.getNomeCompleto())));
        patientRepository.saveAll(pendentes);
        log.info("Backfill de nomeNormalizado: {} paciente(s) atualizado(s).", pendentes.size());
    }
}
