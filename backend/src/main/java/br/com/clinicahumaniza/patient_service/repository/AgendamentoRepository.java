package br.com.clinicahumaniza.patient_service.repository;

import br.com.clinicahumaniza.patient_service.model.Agendamento;
import br.com.clinicahumaniza.patient_service.model.StatusAgendamento;
import br.com.clinicahumaniza.patient_service.model.TipoAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, UUID>, JpaSpecificationExecutor<Agendamento> {

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

    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.paciente.id = :pacienteId " +
           "AND a.tipoAgendamento = 'REPOSICAO' " +
           "AND a.createdAt >= :inicio AND a.createdAt <= :fim " +
           "AND a.status <> 'CANCELADO'")
    long countReposicoesNoMes(UUID pacienteId, LocalDateTime inicio, LocalDateTime fim);

    boolean existsByReposicaoOrigemIdAndStatusIn(UUID origemId, List<StatusAgendamento> statuses);

    List<Agendamento> findByPacienteIdAndDireitoReposicaoTrueAndDataLimiteReposicaoAfter(
            UUID pacienteId, LocalDateTime dataAtual);
}
