package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.AtividadeRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.AtividadeUpdateDTO;
import br.com.clinicahumaniza.patient_service.exception.DuplicateResourceException;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.AtividadeMapper;
import br.com.clinicahumaniza.patient_service.model.Atividade;
import br.com.clinicahumaniza.patient_service.repository.AtividadeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AtividadeService {

    private final AtividadeRepository atividadeRepository;
    private final AtividadeMapper atividadeMapper;

    @Autowired
    public AtividadeService(AtividadeRepository atividadeRepository, AtividadeMapper atividadeMapper) {
        this.atividadeRepository = atividadeRepository;
        this.atividadeMapper = atividadeMapper;
    }

    @Transactional
    public Atividade createAtividade(AtividadeRequestDTO dto) {
        if (atividadeRepository.existsByNome(dto.getNome())) {
            throw new DuplicateResourceException("Nome", dto.getNome());
        }
        Atividade atividade = atividadeMapper.toEntity(dto);
        return atividadeRepository.save(atividade);
    }

    public Atividade getAtividadeById(UUID id) {
        return atividadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade", id));
    }

    public List<Atividade> getAllAtividades() {
        return atividadeRepository.findAll();
    }

    @Transactional
    public Atividade updateAtividade(UUID id, AtividadeUpdateDTO dto) {
        Atividade atividade = atividadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade", id));

        if (dto.getNome() != null && !dto.getNome().equals(atividade.getNome())) {
            if (atividadeRepository.existsByNome(dto.getNome())) {
                throw new DuplicateResourceException("Nome", dto.getNome());
            }
        }

        atividadeMapper.updateEntityFromDto(dto, atividade);
        return atividadeRepository.save(atividade);
    }

    @Transactional
    public void deleteAtividade(UUID id) {
        Atividade atividade = atividadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade", id));
        atividade.setAtivo(false);
        atividadeRepository.save(atividade);
    }
}
