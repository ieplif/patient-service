package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.PatientExportDTO;
import br.com.clinicahumaniza.patient_service.dto.PatientRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.PatientUpdateDTO;
import br.com.clinicahumaniza.patient_service.exception.DuplicateResourceException;
import br.com.clinicahumaniza.patient_service.exception.PatientNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.PatientMapper;
import br.com.clinicahumaniza.patient_service.model.Agendamento;
import br.com.clinicahumaniza.patient_service.model.Assinatura;
import br.com.clinicahumaniza.patient_service.model.Pagamento;
import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.repository.AgendamentoRepository;
import br.com.clinicahumaniza.patient_service.repository.AssinaturaRepository;
import br.com.clinicahumaniza.patient_service.repository.PagamentoRepository;
import br.com.clinicahumaniza.patient_service.repository.PatientRepository;
import br.com.clinicahumaniza.patient_service.spec.PatientSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final AssinaturaRepository assinaturaRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final PagamentoRepository pagamentoRepository;

    @Autowired
    public PatientService(PatientRepository patientRepository,
                          PatientMapper patientMapper,
                          AssinaturaRepository assinaturaRepository,
                          AgendamentoRepository agendamentoRepository,
                          PagamentoRepository pagamentoRepository) {
        this.patientRepository = patientRepository;
        this.patientMapper = patientMapper;
        this.assinaturaRepository = assinaturaRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.pagamentoRepository = pagamentoRepository;
    }

    @Transactional
    public Patient createPatient(PatientRequestDTO patientDTO) {
        Patient patient = patientMapper.toEntity(patientDTO);

        patientRepository.findByCpf(patient.getCpf()).ifPresent(p -> {
            throw new DuplicateResourceException("CPF", patient.getCpf());
        });
        patientRepository.findByEmail(patient.getEmail()).ifPresent(p -> {
            throw new DuplicateResourceException("E-mail", patient.getEmail());
        });

        if (patient.isConsentimentoLgpd()) {
            patient.setDataConsentimentoLgpd(LocalDateTime.now());
        }

        return patientRepository.save(patient);
    }

    public Patient getPatientById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Patient ID cannot be null");
        }
        return patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));
    }

    public Page<Patient> getAllPatients(String nome, String email, String cpf, Pageable pageable) {
        Specification<Patient> spec = Specification
                .where(PatientSpecification.hasNome(nome))
                .and(PatientSpecification.hasEmail(email))
                .and(PatientSpecification.hasCpf(cpf));
        return patientRepository.findAll(spec, pageable);
    }

    @Transactional
    public Patient updatePatient(UUID id, PatientUpdateDTO patientUpdateDTO) {
        if (id == null) {
            throw new IllegalArgumentException("Patient ID cannot be null");
        }
        Patient existingPatient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));

        boolean consentimentoAnterior = existingPatient.isConsentimentoLgpd();

        patientMapper.updateEntityFromDto(patientUpdateDTO, existingPatient);

        if (patientUpdateDTO.getConsentimentoLgpd() != null) {
            boolean novoConsentimento = patientUpdateDTO.getConsentimentoLgpd();
            if (novoConsentimento && !consentimentoAnterior && existingPatient.getDataConsentimentoLgpd() == null) {
                existingPatient.setDataConsentimentoLgpd(LocalDateTime.now());
            } else if (!novoConsentimento) {
                existingPatient.setDataConsentimentoLgpd(null);
            }
        }

        return patientRepository.save(existingPatient);
    }

    @Transactional
    public void deletePatient(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Patient ID cannot be null");
        }
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));

        patient.setStatusAtivo(false);
        patientRepository.save(patient);
    }

    @Transactional
    public void deletePermanente(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Patient ID cannot be null");
        }
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));

        patient.setNomeCompleto("DADOS REMOVIDOS");
        patient.setCpf("000.000.000-00");
        patient.setEmail("removido@anonimo.com");
        patient.setTelefone("00000000000");
        patientRepository.save(patient);

        String usuarioLogado = obterUsuarioLogado();
        patientRepository.delete(patient);
        log.info("Exclusão permanente LGPD: paciente {} removido por {}", id, usuarioLogado);
    }

    public PatientExportDTO exportarDados(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Patient ID cannot be null");
        }
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));

        List<Assinatura> assinaturas = assinaturaRepository.findByPacienteId(id);
        List<Agendamento> agendamentos = agendamentoRepository.findByPacienteId(id);
        List<Pagamento> pagamentos = pagamentoRepository.findByPacienteId(id);

        List<PatientExportDTO.AssinaturaResumoDTO> assinaturasResumo = assinaturas.stream()
                .map(a -> PatientExportDTO.AssinaturaResumoDTO.builder()
                        .id(a.getId())
                        .servico(a.getServico() != null && a.getServico().getAtividade() != null
                                ? a.getServico().getAtividade().getNome() : null)
                        .status(a.getStatus() != null ? a.getStatus().name() : null)
                        .dataInicio(a.getDataInicio())
                        .dataVencimento(a.getDataVencimento())
                        .valor(a.getValor())
                        .build())
                .collect(Collectors.toList());

        List<PatientExportDTO.AgendamentoResumoDTO> agendamentosResumo = agendamentos.stream()
                .map(a -> PatientExportDTO.AgendamentoResumoDTO.builder()
                        .id(a.getId())
                        .servico(a.getServico() != null && a.getServico().getAtividade() != null
                                ? a.getServico().getAtividade().getNome() : null)
                        .profissional(a.getProfissional() != null ? a.getProfissional().getNome() : null)
                        .dataHora(a.getDataHora())
                        .status(a.getStatus() != null ? a.getStatus().name() : null)
                        .build())
                .collect(Collectors.toList());

        List<PatientExportDTO.PagamentoResumoDTO> pagamentosResumo = pagamentos.stream()
                .map(p -> PatientExportDTO.PagamentoResumoDTO.builder()
                        .id(p.getId())
                        .valor(p.getValor())
                        .status(p.getStatus() != null ? p.getStatus().name() : null)
                        .formaPagamento(p.getFormaPagamento() != null ? p.getFormaPagamento().name() : null)
                        .dataVencimento(p.getDataVencimento())
                        .dataPagamento(p.getDataPagamento())
                        .build())
                .collect(Collectors.toList());

        return PatientExportDTO.builder()
                .id(patient.getId())
                .nomeCompleto(patient.getNomeCompleto())
                .cpf(patient.getCpf())
                .email(patient.getEmail())
                .telefone(patient.getTelefone())
                .dataNascimento(patient.getDataNascimento())
                .endereco(patient.getEndereco())
                .estadoCivil(patient.getEstadoCivil())
                .profissao(patient.getProfissao())
                .nomeMedicoResponsavel(patient.getMedicoResponsavel())
                .consentimentoLgpd(patient.isConsentimentoLgpd())
                .dataConsentimentoLgpd(patient.getDataConsentimentoLgpd())
                .createdAt(patient.getCreatedAt())
                .assinaturas(assinaturasResumo)
                .agendamentos(agendamentosResumo)
                .pagamentos(pagamentosResumo)
                .build();
    }

    private String obterUsuarioLogado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "desconhecido";
    }
}
