package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.PatientRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.PatientUpdateDTO;
import br.com.clinicahumaniza.patient_service.exception.DuplicateResourceException;
import br.com.clinicahumaniza.patient_service.exception.PatientNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.PatientMapper;
import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientMapper patientMapper;

    @InjectMocks
    private PatientService patientService;

    private Patient patient;
    private PatientRequestDTO requestDTO;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();

        patient = new Patient();
        patient.setId(patientId);
        patient.setNomeCompleto("João Silva");
        patient.setEmail("joao@email.com");
        patient.setCpf("12345678901");
        patient.setDataNascimento(LocalDate.of(1990, 1, 1));
        patient.setTelefone("21999999999");
        patient.setStatusAtivo(true);

        requestDTO = new PatientRequestDTO();
        requestDTO.setNomeCompleto("João Silva");
        requestDTO.setEmail("joao@email.com");
        requestDTO.setCpf("12345678901");
        requestDTO.setDataNascimento(LocalDate.of(1990, 1, 1));
        requestDTO.setTelefone("21999999999");
    }

    @Test
    @DisplayName("Deve criar paciente com sucesso")
    void createPatient_Success() {
        when(patientMapper.toEntity(requestDTO)).thenReturn(patient);
        when(patientRepository.findByCpf(patient.getCpf())).thenReturn(Optional.empty());
        when(patientRepository.findByEmail(patient.getEmail())).thenReturn(Optional.empty());
        when(patientRepository.save(patient)).thenReturn(patient);

        Patient result = patientService.createPatient(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getNomeCompleto()).isEqualTo("João Silva");
        verify(patientRepository).save(patient);
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar paciente com CPF duplicado")
    void createPatient_DuplicateCpf() {
        when(patientMapper.toEntity(requestDTO)).thenReturn(patient);
        when(patientRepository.findByCpf(patient.getCpf())).thenReturn(Optional.of(patient));

        assertThatThrownBy(() -> patientService.createPatient(requestDTO))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar paciente com e-mail duplicado")
    void createPatient_DuplicateEmail() {
        when(patientMapper.toEntity(requestDTO)).thenReturn(patient);
        when(patientRepository.findByCpf(patient.getCpf())).thenReturn(Optional.empty());
        when(patientRepository.findByEmail(patient.getEmail())).thenReturn(Optional.of(patient));

        assertThatThrownBy(() -> patientService.createPatient(requestDTO))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Deve buscar paciente por ID com sucesso")
    void getPatientById_Success() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        Patient result = patientService.getPatientById(patientId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(patientId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando paciente não encontrado")
    void getPatientById_NotFound() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.getPatientById(patientId))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    @DisplayName("Deve lançar exceção quando ID é nulo")
    void getPatientById_NullId() {
        assertThatThrownBy(() -> patientService.getPatientById(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve listar todos os pacientes")
    void getAllPatients_Success() {
        when(patientRepository.findAll()).thenReturn(List.of(patient));

        List<Patient> result = patientService.getAllPatients();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNomeCompleto()).isEqualTo("João Silva");
    }

    @Test
    @DisplayName("Deve atualizar paciente com sucesso")
    void updatePatient_Success() {
        PatientUpdateDTO updateDTO = new PatientUpdateDTO();
        updateDTO.setNomeCompleto("João Atualizado");

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        doNothing().when(patientMapper).updateEntityFromDto(updateDTO, patient);
        when(patientRepository.save(patient)).thenReturn(patient);

        Patient result = patientService.updatePatient(patientId, updateDTO);

        assertThat(result).isNotNull();
        verify(patientRepository).save(patient);
    }

    @Test
    @DisplayName("Deve deletar paciente com sucesso (soft delete)")
    void deletePatient_Success() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        patientService.deletePatient(patientId);

        assertThat(patient.isStatusAtivo()).isFalse();
        verify(patientRepository).save(patient);
    }
}
