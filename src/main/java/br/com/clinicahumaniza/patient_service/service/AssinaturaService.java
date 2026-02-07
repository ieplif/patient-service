package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.AssinaturaRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.AssinaturaStatusDTO;
import br.com.clinicahumaniza.patient_service.dto.AssinaturaUpdateDTO;
import br.com.clinicahumaniza.patient_service.exception.BusinessException;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.AssinaturaMapper;
import br.com.clinicahumaniza.patient_service.model.Assinatura;
import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.model.Servico;
import br.com.clinicahumaniza.patient_service.model.StatusAssinatura;
import br.com.clinicahumaniza.patient_service.repository.AssinaturaRepository;
import br.com.clinicahumaniza.patient_service.repository.PatientRepository;
import br.com.clinicahumaniza.patient_service.repository.ServicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AssinaturaService {

    private final AssinaturaRepository assinaturaRepository;
    private final PatientRepository patientRepository;
    private final ServicoRepository servicoRepository;
    private final AssinaturaMapper assinaturaMapper;

    @Autowired
    public AssinaturaService(AssinaturaRepository assinaturaRepository,
                             PatientRepository patientRepository,
                             ServicoRepository servicoRepository,
                             AssinaturaMapper assinaturaMapper) {
        this.assinaturaRepository = assinaturaRepository;
        this.patientRepository = patientRepository;
        this.servicoRepository = servicoRepository;
        this.assinaturaMapper = assinaturaMapper;
    }

    @Transactional
    public Assinatura createAssinatura(AssinaturaRequestDTO dto) {
        Patient paciente = patientRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", dto.getPacienteId()));

        Servico servico = servicoRepository.findById(dto.getServicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Serviço", dto.getServicoId()));

        Assinatura assinatura = assinaturaMapper.toEntity(dto, paciente, servico);
        assinatura.setStatus(StatusAssinatura.ATIVO);
        assinatura.setSessoesRealizadas(0);

        if (assinatura.getDataVencimento() == null && servico.getPlano().getValidadeDias() != null) {
            assinatura.setDataVencimento(dto.getDataInicio().plusDays(servico.getPlano().getValidadeDias()));
        }

        return assinaturaRepository.save(assinatura);
    }

    public Assinatura getAssinaturaById(UUID id) {
        return assinaturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", id));
    }

    public List<Assinatura> getAllAssinaturas() {
        return assinaturaRepository.findAll();
    }

    public List<Assinatura> getAssinaturasByPaciente(UUID pacienteId) {
        return assinaturaRepository.findByPacienteId(pacienteId);
    }

    public List<Assinatura> getAssinaturasByServico(UUID servicoId) {
        return assinaturaRepository.findByServicoId(servicoId);
    }

    @Transactional
    public Assinatura updateAssinatura(UUID id, AssinaturaUpdateDTO dto) {
        Assinatura assinatura = assinaturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", id));

        assinaturaMapper.updateEntityFromDto(dto, assinatura);
        return assinaturaRepository.save(assinatura);
    }

    @Transactional
    public Assinatura updateStatus(UUID id, AssinaturaStatusDTO dto) {
        Assinatura assinatura = assinaturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", id));

        if (assinatura.getStatus() == StatusAssinatura.FINALIZADO) {
            throw new BusinessException("Não é possível alterar o status de uma assinatura finalizada");
        }

        assinatura.setStatus(dto.getStatus());
        return assinaturaRepository.save(assinatura);
    }

    @Transactional
    public Assinatura registrarSessao(UUID id) {
        Assinatura assinatura = assinaturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", id));

        if (assinatura.getStatus() != StatusAssinatura.ATIVO) {
            throw new BusinessException("Não é possível registrar sessão em assinatura com status " + assinatura.getStatus());
        }

        assinatura.setSessoesRealizadas(assinatura.getSessoesRealizadas() + 1);

        if (assinatura.getSessoesRealizadas() >= assinatura.getSessoesContratadas()) {
            assinatura.setStatus(StatusAssinatura.FINALIZADO);
        }

        return assinaturaRepository.save(assinatura);
    }

    @Transactional
    public void deleteAssinatura(UUID id) {
        Assinatura assinatura = assinaturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura", id));
        assinatura.setAtivo(false);
        assinaturaRepository.save(assinatura);
    }
}
