package br.com.clinicahumaniza.patient_service.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.clinicahumaniza.patient_service.dto.AssinaturaRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.AssinaturaStatusDTO;
import br.com.clinicahumaniza.patient_service.dto.AssinaturaUpdateDTO;
import br.com.clinicahumaniza.patient_service.dto.ReativarAssinaturaRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.SuspenderAssinaturaRequestDTO;
import br.com.clinicahumaniza.patient_service.exception.BusinessException;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.AssinaturaMapper;
import br.com.clinicahumaniza.patient_service.model.Agendamento;
import br.com.clinicahumaniza.patient_service.model.AgendamentoRecorrente;
import br.com.clinicahumaniza.patient_service.model.Assinatura;
import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.model.Servico;
import br.com.clinicahumaniza.patient_service.model.StatusAgendamento;
import br.com.clinicahumaniza.patient_service.model.StatusAssinatura;
import br.com.clinicahumaniza.patient_service.repository.AgendamentoRecorrenteRepository;
import br.com.clinicahumaniza.patient_service.repository.AgendamentoRepository;
import br.com.clinicahumaniza.patient_service.repository.AssinaturaRepository;
import br.com.clinicahumaniza.patient_service.repository.PatientRepository;
import br.com.clinicahumaniza.patient_service.repository.ServicoRepository;
import br.com.clinicahumaniza.patient_service.spec.AssinaturaSpecification;

@Service
public class AssinaturaService {

    private final AssinaturaRepository assinaturaRepository;
    private final PatientRepository patientRepository;
    private final ServicoRepository servicoRepository;
    private final AssinaturaMapper assinaturaMapper;
    private final AgendamentoRepository agendamentoRepository;
    private final AgendamentoRecorrenteRepository recorrenteRepository;

    @Autowired
    public AssinaturaService(
            AssinaturaRepository assinaturaRepository,
            PatientRepository patientRepository,
            ServicoRepository servicoRepository,
            AssinaturaMapper assinaturaMapper,
            AgendamentoRepository agendamentoRepository,
            AgendamentoRecorrenteRepository recorrenteRepository) {
        this.assinaturaRepository = assinaturaRepository;
        this.patientRepository = patientRepository;
        this.servicoRepository = servicoRepository;
        this.assinaturaMapper = assinaturaMapper;
        this.agendamentoRepository = agendamentoRepository;
        this.recorrenteRepository = recorrenteRepository;
    }

    @Transactional
    public Assinatura createAssinatura(AssinaturaRequestDTO dto) {
        Patient paciente = patientRepository
                .findById(dto.getPacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", dto.getPacienteId()));

        Servico servico = servicoRepository
                .findById(dto.getServicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Serviço", dto.getServicoId()));

        Assinatura assinatura = assinaturaMapper.toEntity(dto, paciente, servico);
        assinatura.setStatus(StatusAssinatura.ATIVO);
        assinatura.setSessoesRealizadas(0);

        if (assinatura.getDataVencimento() == null && servico.getPlano().getValidadeDias() != null) {
            assinatura.setDataVencimento(
                    dto.getDataInicio().plusDays(servico.getPlano().getValidadeDias()));
        }

        return assinaturaRepository.save(assinatura);
    }

    public Assinatura getAssinaturaById(UUID id) {
        return assinaturaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Assinatura", id));
    }

    public Page<Assinatura> getAllAssinaturas(
            StatusAssinatura status, UUID pacienteId, String pacienteNome, Pageable pageable) {
        Specification<Assinatura> spec = Specification.allOf(
                AssinaturaSpecification.hasStatus(status),
                AssinaturaSpecification.hasPaciente(pacienteId),
                AssinaturaSpecification.hasPacienteNome(pacienteNome));
        return assinaturaRepository.findAll(spec, pageable);
    }

    public List<Assinatura> getAssinaturasByPaciente(UUID pacienteId) {
        return assinaturaRepository.findByPacienteId(pacienteId);
    }

    public List<Assinatura> getAssinaturasByServico(UUID servicoId) {
        return assinaturaRepository.findByServicoId(servicoId);
    }

    @Transactional
    public Assinatura updateAssinatura(UUID id, AssinaturaUpdateDTO dto) {
        Assinatura assinatura =
                assinaturaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Assinatura", id));

        assinaturaMapper.updateEntityFromDto(dto, assinatura);
        return assinaturaRepository.save(assinatura);
    }

    @Transactional
    public Assinatura updateStatus(UUID id, AssinaturaStatusDTO dto) {
        Assinatura assinatura =
                assinaturaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Assinatura", id));

        if (assinatura.getStatus() == StatusAssinatura.FINALIZADO) {
            throw new BusinessException("Não é possível alterar o status de uma assinatura finalizada");
        }

        assinatura.setStatus(dto.getStatus());
        return assinaturaRepository.save(assinatura);
    }

    @Transactional
    public Assinatura registrarSessao(UUID id) {
        Assinatura assinatura =
                assinaturaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Assinatura", id));

        if (assinatura.getStatus() != StatusAssinatura.ATIVO) {
            throw new BusinessException(
                    "Não é possível registrar sessão em assinatura com status " + assinatura.getStatus());
        }

        assinatura.setSessoesRealizadas(assinatura.getSessoesRealizadas() + 1);

        if (assinatura.getSessoesRealizadas() >= assinatura.getSessoesContratadas()) {
            assinatura.setStatus(StatusAssinatura.FINALIZADO);
        }

        return assinaturaRepository.save(assinatura);
    }

    /**
     * Suspende uma assinatura ATIVO. Casos típicos: paciente engravida, lesão,
     * viagem prolongada. Saldo de sessões é preservado para retomada futura.
     *
     * Efeitos:
     *  - status -> SUSPENSO
     *  - registra dataSuspensao = hoje + motivo + dataPrevistaRetomada
     *  - cancela agendamentos futuros pendentes (AGENDADO/CONFIRMADO) com
     *    motivo "Suspensão da assinatura: ..." (sem direito a reposição)
     *  - desativa templates AgendamentoRecorrente vinculados
     *  - renovação automática não roda em SUSPENSO (query do scheduler
     *    filtra por status=ATIVO)
     */
    @Transactional
    public Assinatura suspender(UUID id, SuspenderAssinaturaRequestDTO dto) {
        Assinatura assinatura =
                assinaturaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Assinatura", id));

        if (assinatura.getStatus() != StatusAssinatura.ATIVO) {
            throw new BusinessException(
                    "Apenas assinaturas ATIVO podem ser suspensas (status atual: " + assinatura.getStatus() + ")");
        }

        assinatura.setStatus(StatusAssinatura.SUSPENSO);
        assinatura.setDataSuspensao(LocalDate.now());
        assinatura.setMotivoSuspensao(dto.getMotivo());
        assinatura.setDataPrevistaRetomada(dto.getDataPrevistaRetomada());

        // Cancelar agendamentos futuros pendentes
        List<Agendamento> futuros = agendamentoRepository.findByAssinaturaIdAndDataHoraGreaterThanEqualAndStatusIn(
                id, LocalDateTime.now(), List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONFIRMADO));
        for (Agendamento ag : futuros) {
            ag.setStatus(StatusAgendamento.CANCELADO);
            ag.setMotivoCancelamento("Suspensão da assinatura: " + dto.getMotivo());
            ag.setDireitoReposicao(false);
            agendamentoRepository.save(ag);
        }

        // Desativar templates recorrentes — a renovação não vai mais usá-los
        List<AgendamentoRecorrente> templates = recorrenteRepository.findByAssinaturaIdAndAtivoTrue(id);
        for (AgendamentoRecorrente t : templates) {
            t.setAtivo(false);
            recorrenteRepository.save(t);
        }

        return assinaturaRepository.save(assinatura);
    }

    /**
     * Reativa uma assinatura SUSPENSO. Reseta a janela de validade
     * (dataInicio = hoje, dataVencimento = hoje + plano.validadeDias) e
     * limpa os campos de suspensão. Saldo de sessões permanece intocado.
     *
     * O frontend pode chamar /regenerar-horarios depois se quiser recriar
     * os agendamentos com os horários antigos (ou novos).
     */
    @Transactional
    public Assinatura reativar(UUID id, ReativarAssinaturaRequestDTO dto) {
        Assinatura assinatura =
                assinaturaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Assinatura", id));

        if (assinatura.getStatus() != StatusAssinatura.SUSPENSO) {
            throw new BusinessException(
                    "Apenas assinaturas SUSPENSO podem ser reativadas (status atual: " + assinatura.getStatus() + ")");
        }

        LocalDate novaInicio = (dto != null && dto.getDataInicio() != null) ? dto.getDataInicio() : LocalDate.now();

        // Recalcula vencimento com a validade do plano (default 30 dias se nao informada)
        Integer validadeDias = assinatura.getServico() != null
                        && assinatura.getServico().getPlano() != null
                        && assinatura.getServico().getPlano().getValidadeDias() != null
                ? assinatura.getServico().getPlano().getValidadeDias()
                : 30;
        LocalDate novoVencimento = novaInicio.plusDays(validadeDias - 1);

        assinatura.setStatus(StatusAssinatura.ATIVO);
        assinatura.setDataInicio(novaInicio);
        assinatura.setDataVencimento(novoVencimento);
        assinatura.setDataSuspensao(null);
        assinatura.setMotivoSuspensao(null);
        assinatura.setDataPrevistaRetomada(null);

        return assinaturaRepository.save(assinatura);
    }

    @Transactional
    public void deleteAssinatura(UUID id) {
        Assinatura assinatura =
                assinaturaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Assinatura", id));
        assinatura.setAtivo(false);
        assinaturaRepository.save(assinatura);
    }
}
