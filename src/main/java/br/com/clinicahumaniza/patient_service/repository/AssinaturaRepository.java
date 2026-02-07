package br.com.clinicahumaniza.patient_service.repository;

import br.com.clinicahumaniza.patient_service.model.Assinatura;
import br.com.clinicahumaniza.patient_service.model.StatusAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssinaturaRepository extends JpaRepository<Assinatura, UUID> {

    List<Assinatura> findByPacienteId(UUID pacienteId);

    List<Assinatura> findByServicoId(UUID servicoId);

    List<Assinatura> findByStatus(StatusAssinatura status);

    List<Assinatura> findByPacienteIdAndStatus(UUID pacienteId, StatusAssinatura status);

    @Query(value = "SELECT * FROM assinaturas", nativeQuery = true)
    List<Assinatura> findAllIncludingInactive();
}
