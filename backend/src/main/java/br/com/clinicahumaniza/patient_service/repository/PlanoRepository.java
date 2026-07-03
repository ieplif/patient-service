package br.com.clinicahumaniza.patient_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.clinicahumaniza.patient_service.model.Plano;

@Repository
public interface PlanoRepository extends JpaRepository<Plano, UUID> {

    boolean existsByNome(String nome);
}
