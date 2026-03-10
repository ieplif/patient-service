package br.com.clinicahumaniza.patient_service.repository;

import br.com.clinicahumaniza.patient_service.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, UUID> {

    List<Servico> findByAtividadeId(UUID atividadeId);

    List<Servico> findByPlanoId(UUID planoId);

    List<Servico> findByAtividadeIdAndPlanoId(UUID atividadeId, UUID planoId);

    @Query(value = "SELECT * FROM servicos", nativeQuery = true)
    List<Servico> findAllIncludingInactive();
}
