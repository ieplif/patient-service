package br.com.clinicahumaniza.patient_service.repository;

import br.com.clinicahumaniza.patient_service.model.Profissional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfissionalRepository extends JpaRepository<Profissional, UUID> {

    Optional<Profissional> findByUserId(UUID userId);

    List<Profissional> findByAtividadesId(UUID atividadeId);

    @Query(value = "SELECT * FROM profissionais", nativeQuery = true)
    List<Profissional> findAllIncludingInactive();
}
