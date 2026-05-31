package br.com.clinicahumaniza.patient_service.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import br.com.clinicahumaniza.patient_service.model.Pagamento;
import br.com.clinicahumaniza.patient_service.model.StatusPagamento;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, UUID>, JpaSpecificationExecutor<Pagamento> {

    List<Pagamento> findByPacienteId(UUID pacienteId);

    List<Pagamento> findByAssinaturasId(UUID assinaturaId);

    List<Pagamento> findByAgendamentoId(UUID agendamentoId);

    List<Pagamento> findByStatus(StatusPagamento status);

    List<Pagamento> findByDataVencimentoBetween(LocalDate inicio, LocalDate fim);

    /**
     * Idempotência da cobrança recorrente: indica se já existe um pagamento (não cancelado)
     * vinculado a esta assinatura com o vencimento informado. Evita gerar a mesma mensalidade
     * mais de uma vez quando o scheduler roda.
     */
    @Query("SELECT COUNT(p) > 0 FROM Pagamento p JOIN p.assinaturas a "
            + "WHERE a.id = :assinaturaId AND p.dataVencimento = :dataVencimento "
            + "AND p.status <> br.com.clinicahumaniza.patient_service.model.StatusPagamento.CANCELADO")
    boolean existsByAssinaturaAndVencimento(UUID assinaturaId, LocalDate dataVencimento);

    @Query(value = "SELECT * FROM pagamentos", nativeQuery = true)
    List<Pagamento> findAllIncludingInactive();
}
