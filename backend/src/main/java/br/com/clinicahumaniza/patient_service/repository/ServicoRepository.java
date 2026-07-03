package br.com.clinicahumaniza.patient_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.clinicahumaniza.patient_service.model.Servico;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, UUID> {

    List<Servico> findByAtividadeId(UUID atividadeId);

    List<Servico> findByPlanoId(UUID planoId);
}
