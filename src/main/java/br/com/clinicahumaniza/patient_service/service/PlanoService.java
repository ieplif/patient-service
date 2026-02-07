package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.PlanoRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.PlanoUpdateDTO;
import br.com.clinicahumaniza.patient_service.exception.DuplicateResourceException;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.PlanoMapper;
import br.com.clinicahumaniza.patient_service.model.Plano;
import br.com.clinicahumaniza.patient_service.repository.PlanoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PlanoService {

    private final PlanoRepository planoRepository;
    private final PlanoMapper planoMapper;

    @Autowired
    public PlanoService(PlanoRepository planoRepository, PlanoMapper planoMapper) {
        this.planoRepository = planoRepository;
        this.planoMapper = planoMapper;
    }

    @Transactional
    public Plano createPlano(PlanoRequestDTO dto) {
        if (planoRepository.existsByNome(dto.getNome())) {
            throw new DuplicateResourceException("Nome", dto.getNome());
        }
        Plano plano = planoMapper.toEntity(dto);
        return planoRepository.save(plano);
    }

    public Plano getPlanoById(UUID id) {
        return planoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plano", id));
    }

    public List<Plano> getAllPlanos() {
        return planoRepository.findAll();
    }

    @Transactional
    public Plano updatePlano(UUID id, PlanoUpdateDTO dto) {
        Plano plano = planoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plano", id));

        if (dto.getNome() != null && !dto.getNome().equals(plano.getNome())) {
            if (planoRepository.existsByNome(dto.getNome())) {
                throw new DuplicateResourceException("Nome", dto.getNome());
            }
        }

        planoMapper.updateEntityFromDto(dto, plano);
        return planoRepository.save(plano);
    }

    @Transactional
    public void deletePlano(UUID id) {
        Plano plano = planoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plano", id));
        plano.setAtivo(false);
        planoRepository.save(plano);
    }
}
