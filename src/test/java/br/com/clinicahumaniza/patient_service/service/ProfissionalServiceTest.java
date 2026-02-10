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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfissionalServiceTest {

    @Mock
    private ProfissionalRepository profissionalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AtividadeRepository atividadeRepository;

    @Mock
    private ProfissionalMapper profissionalMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProfissionalService profissionalService;

    private Profissional profissional;
    private User user;
    private Atividade atividade;
    private ProfissionalRequestDTO requestDTO;
    private UUID profissionalId;
    private UUID atividadeId;

    @BeforeEach
    void setUp() {
        profissionalId = UUID.randomUUID();
        atividadeId = UUID.randomUUID();

        atividade = new Atividade();
        atividade.setId(atividadeId);
        atividade.setNome("Fisioterapia Pélvica");
        atividade.setAtivo(true);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setNome("Maria Silva");
        user.setEmail("maria@email.com");
        user.setSenha("encodedPassword");
        user.setRole(Role.ROLE_PROFISSIONAL);

        profissional = new Profissional();
        profissional.setId(profissionalId);
        profissional.setNome("Maria Silva");
        profissional.setTelefone("11999999999");
        profissional.setUser(user);
        profissional.setAtividades(new HashSet<>(Set.of(atividade)));
        profissional.setAtivo(true);

        requestDTO = new ProfissionalRequestDTO();
        requestDTO.setNome("Maria Silva");
        requestDTO.setTelefone("11999999999");
        requestDTO.setEmail("maria@email.com");
        requestDTO.setSenha("senha123");
        requestDTO.setAtividadeIds(Set.of(atividadeId));
    }

    @Test
    @DisplayName("Deve criar profissional com sucesso e User com ROLE_PROFISSIONAL")
    void createProfissional_Success() {
        when(userRepository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
        when(atividadeRepository.findById(atividadeId)).thenReturn(Optional.of(atividade));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(profissionalMapper.toEntity(any(), any(), any())).thenReturn(profissional);
        when(profissionalRepository.save(profissional)).thenReturn(profissional);

        Profissional result = profissionalService.createProfissional(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getNome()).isEqualTo("Maria Silva");
        assertThat(result.getUser().getRole()).isEqualTo(Role.ROLE_PROFISSIONAL);
        verify(profissionalRepository).save(profissional);
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar profissional com email duplicado")
    void createProfissional_DuplicateEmail() {
        when(userRepository.existsByEmail(requestDTO.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> profissionalService.createProfissional(requestDTO))
                .isInstanceOf(DuplicateResourceException.class);

        verify(profissionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar profissional com atividade inexistente")
    void createProfissional_AtividadeNotFound() {
        when(userRepository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
        when(atividadeRepository.findById(atividadeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profissionalService.createProfissional(requestDTO))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(profissionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar profissional por ID com sucesso")
    void getProfissionalById_Success() {
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));

        Profissional result = profissionalService.getProfissionalById(profissionalId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(profissionalId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando profissional não encontrado")
    void getProfissionalById_NotFound() {
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profissionalService.getProfissionalById(profissionalId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve listar todos os profissionais")
    void getAllProfissionais_Success() {
        when(profissionalRepository.findAll()).thenReturn(List.of(profissional));

        List<Profissional> result = profissionalService.getAllProfissionais();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNome()).isEqualTo("Maria Silva");
    }

    @Test
    @DisplayName("Deve listar profissionais por atividade")
    void getProfissionaisByAtividade_Success() {
        when(profissionalRepository.findByAtividadesId(atividadeId)).thenReturn(List.of(profissional));

        List<Profissional> result = profissionalService.getProfissionaisByAtividade(atividadeId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAtividades()).contains(atividade);
    }

    @Test
    @DisplayName("Deve atualizar profissional com sucesso")
    void updateProfissional_Success() {
        ProfissionalUpdateDTO updateDTO = new ProfissionalUpdateDTO();
        updateDTO.setNome("Maria Silva Santos");
        updateDTO.setTelefone("11888888888");
        updateDTO.setAtividadeIds(Set.of(atividadeId));

        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));
        when(atividadeRepository.findById(atividadeId)).thenReturn(Optional.of(atividade));
        doNothing().when(profissionalMapper).updateEntityFromDto(any(), any(), any());
        when(profissionalRepository.save(profissional)).thenReturn(profissional);

        Profissional result = profissionalService.updateProfissional(profissionalId, updateDTO);

        assertThat(result).isNotNull();
        verify(profissionalRepository).save(profissional);
    }

    @Test
    @DisplayName("Deve deletar profissional com sucesso (soft delete)")
    void deleteProfissional_Success() {
        when(profissionalRepository.findById(profissionalId)).thenReturn(Optional.of(profissional));
        when(profissionalRepository.save(any(Profissional.class))).thenReturn(profissional);

        profissionalService.deleteProfissional(profissionalId);

        assertThat(profissional.isAtivo()).isFalse();
        verify(profissionalRepository).save(profissional);
    }

    @Test
    @DisplayName("Deve verificar que User criado tem role ROLE_PROFISSIONAL")
    void createProfissional_UserHasCorrectRole() {
        when(userRepository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
        when(atividadeRepository.findById(atividadeId)).thenReturn(Optional.of(atividade));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(profissionalMapper.toEntity(any(), any(), any())).thenAnswer(invocation -> {
            User capturedUser = invocation.getArgument(1);
            assertThat(capturedUser.getRole()).isEqualTo(Role.ROLE_PROFISSIONAL);
            assertThat(capturedUser.getEmail()).isEqualTo("maria@email.com");
            assertThat(capturedUser.getSenha()).isEqualTo("encodedPassword");
            return profissional;
        });
        when(profissionalRepository.save(any())).thenReturn(profissional);

        profissionalService.createProfissional(requestDTO);

        verify(profissionalMapper).toEntity(any(), any(), any());
    }
}
