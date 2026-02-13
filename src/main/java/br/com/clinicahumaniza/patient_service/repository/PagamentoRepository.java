package br.com.clinicahumaniza.patient_service.repository;

import br.com.clinicahumaniza.patient_service.model.Pagamento;
import br.com.clinicahumaniza.patient_service.model.StatusPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {

    List<Pagamento> findByPacienteId(UUID pacienteId);

    List<Pagamento> findByAssinaturaId(UUID assinaturaId);

    List<Pagamento> findByAgendamentoId(UUID agendamentoId);

    List<Pagamento> findByStatus(StatusPagamento status);

    List<Pagamento> findByDataVencimentoBetween(LocalDate inicio, LocalDate fim);

    @Query(value = "SELECT * FROM pagamentos", nativeQuery = true)
    List<Pagamento> findAllIncludingInactive();
}
