package br.com.clinicahumaniza.patient_service.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.clinicahumaniza.patient_service.model.Prontuario;
import br.com.clinicahumaniza.patient_service.model.TipoDocumento;

@Repository
public interface ProntuarioRepository extends JpaRepository<Prontuario, UUID> {
    Page<Prontuario> findByPacienteId(UUID pacienteId, Pageable pageable);

    Page<Prontuario> findByPacienteIdAndTipo(UUID pacienteId, TipoDocumento tipo, Pageable pageable);

    long countByPacienteId(UUID pacienteId);
}
