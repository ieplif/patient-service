package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.FeriadoRequestDTO;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.FeriadoMapper;
import br.com.clinicahumaniza.patient_service.model.Feriado;
import br.com.clinicahumaniza.patient_service.repository.FeriadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FeriadoService {

    private final FeriadoRepository feriadoRepository;
    private final FeriadoMapper feriadoMapper;

    @Autowired
    public FeriadoService(FeriadoRepository feriadoRepository, FeriadoMapper feriadoMapper) {
        this.feriadoRepository = feriadoRepository;
        this.feriadoMapper = feriadoMapper;
    }

    @Transactional
    public Feriado createFeriado(FeriadoRequestDTO dto) {
        Feriado feriado = feriadoMapper.toEntity(dto);
        return feriadoRepository.save(feriado);
    }

    public Feriado getFeriadoById(UUID id) {
        return feriadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feriado", id));
    }

    public Page<Feriado> getAllFeriados(Pageable pageable) {
        return feriadoRepository.findAll(pageable);
    }

    @Transactional
    public Feriado updateFeriado(UUID id, FeriadoRequestDTO dto) {
        Feriado feriado = feriadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feriado", id));
        feriadoMapper.updateEntityFromDto(dto, feriado);
        return feriadoRepository.save(feriado);
    }

    @Transactional
    public void deleteFeriado(UUID id) {
        Feriado feriado = feriadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feriado", id));
        feriadoRepository.delete(feriado);
    }
}
