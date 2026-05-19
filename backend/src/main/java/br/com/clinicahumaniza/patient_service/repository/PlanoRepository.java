package br.com.clinicahumaniza.patient_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import br.com.clinicahumaniza.patient_service.model.Plano;

@Repository
public interface PlanoRepository extends JpaRepository<Plano, UUID> {

    Optional<Plano> findByNome(String nome);

    boolean existsByNome(String nome);

    List<Plano> findByTipoPlano(String tipoPlano);

    @Query(value = "SELECT * FROM planos", nativeQuery = true)
    List<Plano> findAllIncludingInactive();
}
