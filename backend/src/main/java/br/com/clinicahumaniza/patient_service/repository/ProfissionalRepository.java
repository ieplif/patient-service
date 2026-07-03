package br.com.clinicahumaniza.patient_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.clinicahumaniza.patient_service.model.Profissional;

@Repository
public interface ProfissionalRepository extends JpaRepository<Profissional, UUID> {

    List<Profissional> findByAtividadesId(UUID atividadeId);
}
