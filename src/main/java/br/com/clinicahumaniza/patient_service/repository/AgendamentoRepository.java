package br.com.clinicahumaniza.patient_service.repository;

import br.com.clinicahumaniza.patient_service.model.Agendamento;
import br.com.clinicahumaniza.patient_service.model.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, UUID> {

    List<Agendamento> findByPacienteId(UUID pacienteId);

    List<Agendamento> findByProfissionalId(UUID profissionalId);

    List<Agendamento> findByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);

    List<Agendamento> findByProfissionalIdAndStatusInAndDataHoraBetween(
            UUID profissionalId,
            List<StatusAgendamento> statuses,
            LocalDateTime inicio,
            LocalDateTime fim
    );

    List<Agendamento> findByAgendamentoRecorrenteIdAndDataHoraGreaterThanEqualAndStatusIn(
            UUID recorrenteId, LocalDateTime dataHora, List<StatusAgendamento> statuses);

    @Query(value = "SELECT * FROM agendamentos", nativeQuery = true)
    List<Agendamento> findAllIncludingInactive();
}
