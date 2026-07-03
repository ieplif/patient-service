package br.com.clinicahumaniza.patient_service.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import br.com.clinicahumaniza.patient_service.model.Assinatura;
import br.com.clinicahumaniza.patient_service.model.StatusAssinatura;

@Repository
public interface AssinaturaRepository extends JpaRepository<Assinatura, UUID>, JpaSpecificationExecutor<Assinatura> {

    List<Assinatura> findByPacienteId(UUID pacienteId);

    List<Assinatura> findByServicoId(UUID servicoId);

    List<Assinatura> findByStatusIn(List<StatusAssinatura> statuses);

    List<Assinatura> findByRenovacaoAutomaticaTrueAndStatusInAndDataVencimentoLessThanEqual(
            List<StatusAssinatura> statuses, LocalDate limitDate);
}
