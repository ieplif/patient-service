package br.com.clinicahumaniza.patient_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import br.com.clinicahumaniza.patient_service.model.Agendamento;
import br.com.clinicahumaniza.patient_service.model.StatusAgendamento;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, UUID>, JpaSpecificationExecutor<Agendamento> {

    List<Agendamento> findByPacienteId(UUID pacienteId);

    List<Agendamento> findByProfissionalId(UUID profissionalId);

    List<Agendamento> findByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);

    List<Agendamento> findByProfissionalIdAndStatusInAndDataHoraBetween(
            UUID profissionalId, List<StatusAgendamento> statuses, LocalDateTime inicio, LocalDateTime fim);

    List<Agendamento> findByAgendamentoRecorrenteIdAndDataHoraGreaterThanEqualAndStatusIn(
            UUID recorrenteId, LocalDateTime dataHora, List<StatusAgendamento> statuses);

    List<Agendamento> findByAssinaturaIdAndDataHoraGreaterThanEqualAndStatusIn(
            UUID assinaturaId, LocalDateTime dataHora, List<StatusAgendamento> statuses);

    @Query(value = "SELECT * FROM agendamentos", nativeQuery = true)
    List<Agendamento> findAllIncludingInactive();

    boolean existsByReposicaoOrigemIdAndStatusIn(UUID origemId, List<StatusAgendamento> statuses);

    List<Agendamento> findByPacienteIdAndDireitoReposicaoTrue(UUID pacienteId);

    // Ressincronização do Google Calendar: todos os futuros e ativos (cria os que faltam
    // e repinta/atualiza os que já têm evento).
    List<Agendamento> findByStatusInAndDataHoraGreaterThanEqual(
            List<StatusAgendamento> statuses, LocalDateTime dataHora);

    // Órfãos do Google Calendar: cancelados futuros que ainda têm evento (delete falhou).
    List<Agendamento> findByStatusAndDataHoraGreaterThanEqualAndGoogleCalendarEventIdIsNotNull(
            StatusAgendamento status, LocalDateTime dataHora);
}
