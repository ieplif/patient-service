package br.com.clinicahumaniza.patient_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.clinicahumaniza.patient_service.model.Atividade;

@Repository
public interface AtividadeRepository extends JpaRepository<Atividade, UUID> {

    boolean existsByNome(String nome);
}
