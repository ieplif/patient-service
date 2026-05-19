package br.com.clinicahumaniza.patient_service.repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import br.com.clinicahumaniza.patient_service.model.HorarioDisponivel;

@Repository
public interface HorarioDisponivelRepository extends JpaRepository<HorarioDisponivel, UUID> {

    List<HorarioDisponivel> findByProfissionalId(UUID profissionalId);

    List<HorarioDisponivel> findByProfissionalIdAndDiaSemana(UUID profissionalId, DayOfWeek diaSemana);

    @Query(value = "SELECT * FROM horarios_disponiveis", nativeQuery = true)
    List<HorarioDisponivel> findAllIncludingInactive();
}
