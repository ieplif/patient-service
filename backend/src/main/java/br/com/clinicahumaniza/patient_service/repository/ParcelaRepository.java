package br.com.clinicahumaniza.patient_service.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import br.com.clinicahumaniza.patient_service.model.Parcela;
import br.com.clinicahumaniza.patient_service.model.StatusParcela;

@Repository
public interface ParcelaRepository extends JpaRepository<Parcela, UUID> {

    List<Parcela> findByPagamentoId(UUID pagamentoId);

    List<Parcela> findByStatus(StatusParcela status);

    List<Parcela> findByDataVencimentoBeforeAndStatus(LocalDate data, StatusParcela status);

    /**
     * Soma do valor de todas as parcelas PAGAS cuja dataPagamento cai no período.
     * Usado para o card "Receita do mês" do dashboard — reflete o dinheiro
     * efetivamente recebido (parcial ou totalmente) no mês.
     * Retorna 0 se não houver parcelas no período (graças ao COALESCE).
     */
    @Query("SELECT COALESCE(SUM(p.valor), 0) FROM Parcela p "
            + "WHERE p.status = 'PAGO' "
            + "AND p.dataPagamento BETWEEN :inicio AND :fim")
    BigDecimal sumParcelasPagasBetween(LocalDateTime inicio, LocalDateTime fim);
}
