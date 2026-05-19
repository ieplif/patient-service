package br.com.clinicahumaniza.patient_service.repository;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.clinicahumaniza.patient_service.model.Feriado;

@Repository
public interface FeriadoRepository extends JpaRepository<Feriado, UUID> {

    @Query(
            value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM feriados f " + "WHERE f.data = :data "
                    + "OR (f.recorrente = true AND EXTRACT(MONTH FROM f.data) = EXTRACT(MONTH FROM CAST(:data AS date)) "
                    + "AND EXTRACT(DAY FROM f.data) = EXTRACT(DAY FROM CAST(:data AS date)))",
            nativeQuery = true)
    boolean isFeriado(@Param("data") LocalDate data);
}
