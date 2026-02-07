package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.ServicoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.ServicoUpdateDTO;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.ServicoMapper;
import br.com.clinicahumaniza.patient_service.model.Atividade;
import br.com.clinicahumaniza.patient_service.model.Plano;
import br.com.clinicahumaniza.patient_service.model.Servico;
import br.com.clinicahumaniza.patient_service.repository.AtividadeRepository;
import br.com.clinicahumaniza.patient_service.repository.PlanoRepository;
import br.com.clinicahumaniza.patient_service.repository.ServicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final AtividadeRepository atividadeRepository;
    private final PlanoRepository planoRepository;
    private final ServicoMapper servicoMapper;

    @Autowired
    public ServicoService(ServicoRepository servicoRepository,
                          AtividadeRepository atividadeRepository,
                          PlanoRepository planoRepository,
                          ServicoMapper servicoMapper) {
        this.servicoRepository = servicoRepository;
        this.atividadeRepository = atividadeRepository;
        this.planoRepository = planoRepository;
        this.servicoMapper = servicoMapper;
    }

    @Transactional
    public Servico createServico(ServicoRequestDTO dto) {
        Atividade atividade = atividadeRepository.findById(dto.getAtividadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Atividade", dto.getAtividadeId()));
        Plano plano = planoRepository.findById(dto.getPlanoId())
                .orElseThrow(() -> new ResourceNotFoundException("Plano", dto.getPlanoId()));

        Servico servico = servicoMapper.toEntity(dto, atividade, plano);
        return servicoRepository.save(servico);
    }

    public Servico getServicoById(UUID id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço", id));
    }

    public List<Servico> getAllServicos() {
        return servicoRepository.findAll();
    }

    public List<Servico> getServicosByAtividade(UUID atividadeId) {
        return servicoRepository.findByAtividadeId(atividadeId);
    }

    public List<Servico> getServicosByPlano(UUID planoId) {
        return servicoRepository.findByPlanoId(planoId);
    }

    @Transactional
    public Servico updateServico(UUID id, ServicoUpdateDTO dto) {
        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço", id));

        Atividade atividade = null;
        Plano plano = null;

        if (dto.getAtividadeId() != null) {
            atividade = atividadeRepository.findById(dto.getAtividadeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Atividade", dto.getAtividadeId()));
        }
        if (dto.getPlanoId() != null) {
            plano = planoRepository.findById(dto.getPlanoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Plano", dto.getPlanoId()));
        }

        servicoMapper.updateEntityFromDto(dto, servico, atividade, plano);
        return servicoRepository.save(servico);
    }

    @Transactional
    public void deleteServico(UUID id) {
        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço", id));
        servico.setAtivo(false);
        servicoRepository.save(servico);
    }
}
