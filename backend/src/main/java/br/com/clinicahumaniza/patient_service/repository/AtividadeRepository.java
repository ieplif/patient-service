package br.com.clinicahumaniza.patient_service.repository;

import br.com.clinicahumaniza.patient_service.model.Atividade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AtividadeRepository extends JpaRepository<Atividade, UUID> {

    Optional<Atividade> findByNome(String nome);

    boolean existsByNome(String nome);

    @Query(value = "SELECT * FROM atividades", nativeQuery = true)
    List<Atividade> findAllIncludingInactive();
}
