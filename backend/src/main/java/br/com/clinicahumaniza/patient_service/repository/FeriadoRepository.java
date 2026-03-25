package br.com.clinicahumaniza.patient_service.repository;

import br.com.clinicahumaniza.patient_service.model.Feriado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface FeriadoRepository extends JpaRepository<Feriado, UUID> {

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Feriado f " +
           "WHERE f.data = :data " +
           "OR (f.recorrente = true AND FUNCTION('MONTH', f.data) = FUNCTION('MONTH', :data) " +
           "AND FUNCTION('DAY', f.data) = FUNCTION('DAY', :data))")
    boolean isFeriado(LocalDate data);
}
