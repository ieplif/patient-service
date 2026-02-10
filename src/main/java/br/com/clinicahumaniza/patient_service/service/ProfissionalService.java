package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.ProfissionalRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.ProfissionalUpdateDTO;
import br.com.clinicahumaniza.patient_service.exception.DuplicateResourceException;
import br.com.clinicahumaniza.patient_service.exception.ResourceNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.ProfissionalMapper;
import br.com.clinicahumaniza.patient_service.model.Atividade;
import br.com.clinicahumaniza.patient_service.model.Profissional;
import br.com.clinicahumaniza.patient_service.model.Role;
import br.com.clinicahumaniza.patient_service.model.User;
import br.com.clinicahumaniza.patient_service.repository.AtividadeRepository;
import br.com.clinicahumaniza.patient_service.repository.ProfissionalRepository;
import br.com.clinicahumaniza.patient_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;
    private final UserRepository userRepository;
    private final AtividadeRepository atividadeRepository;
    private final ProfissionalMapper profissionalMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ProfissionalService(ProfissionalRepository profissionalRepository,
                               UserRepository userRepository,
                               AtividadeRepository atividadeRepository,
                               ProfissionalMapper profissionalMapper,
                               PasswordEncoder passwordEncoder) {
        this.profissionalRepository = profissionalRepository;
        this.userRepository = userRepository;
        this.atividadeRepository = atividadeRepository;
        this.profissionalMapper = profissionalMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Profissional createProfissional(ProfissionalRequestDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("E-mail", dto.getEmail());
        }

        Set<Atividade> atividades = findAtividades(dto.getAtividadeIds());

        User user = new User();
        user.setNome(dto.getNome());
        user.setEmail(dto.getEmail());
        user.setSenha(passwordEncoder.encode(dto.getSenha()));
        user.setRole(Role.ROLE_PROFISSIONAL);

        Profissional profissional = profissionalMapper.toEntity(dto, user, atividades);
        return profissionalRepository.save(profissional);
    }

    public Profissional getProfissionalById(UUID id) {
        return profissionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profissional", id));
    }

    public List<Profissional> getAllProfissionais() {
        return profissionalRepository.findAll();
    }

    public List<Profissional> getProfissionaisByAtividade(UUID atividadeId) {
        return profissionalRepository.findByAtividadesId(atividadeId);
    }

    @Transactional
    public Profissional updateProfissional(UUID id, ProfissionalUpdateDTO dto) {
        Profissional profissional = profissionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profissional", id));

        Set<Atividade> atividades = null;
        if (dto.getAtividadeIds() != null) {
            atividades = findAtividades(dto.getAtividadeIds());
        }

        profissionalMapper.updateEntityFromDto(dto, profissional, atividades);
        return profissionalRepository.save(profissional);
    }

    @Transactional
    public void deleteProfissional(UUID id) {
        Profissional profissional = profissionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profissional", id));
        profissional.setAtivo(false);
        profissionalRepository.save(profissional);
    }

    private Set<Atividade> findAtividades(Set<UUID> atividadeIds) {
        Set<Atividade> atividades = new HashSet<>();
        for (UUID atividadeId : atividadeIds) {
            Atividade atividade = atividadeRepository.findById(atividadeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Atividade", atividadeId));
            atividades.add(atividade);
        }
        return atividades;
    }
}
