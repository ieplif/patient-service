package br.com.clinicahumaniza.patient_service.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.clinicahumaniza.patient_service.model.Parcela;
import br.com.clinicahumaniza.patient_service.model.StatusParcela;

@Repository
public interface ParcelaRepository extends JpaRepository<Parcela, UUID> {

    List<Parcela> findByPagamentoId(UUID pagamentoId);

    List<Parcela> findByStatus(StatusParcela status);

    List<Parcela> findByDataVencimentoBeforeAndStatus(LocalDate data, StatusParcela status);
}
