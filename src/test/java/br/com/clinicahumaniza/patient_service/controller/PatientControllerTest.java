package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.dto.PatientRequestDTO;
import br.com.clinicahumaniza.patient_service.dto.PatientResponseDTO;
import br.com.clinicahumaniza.patient_service.dto.PatientUpdateDTO;
import br.com.clinicahumaniza.patient_service.exception.PatientNotFoundException;
import br.com.clinicahumaniza.patient_service.mapper.PatientMapper;
import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.security.JwtAuthenticationFilter;
import br.com.clinicahumaniza.patient_service.security.JwtService;
import br.com.clinicahumaniza.patient_service.security.SecurityConfig;
import br.com.clinicahumaniza.patient_service.service.PatientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientService patientService;

    @MockitoBean
    private PatientMapper patientMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;
    private Patient patient;
    private PatientResponseDTO responseDTO;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        patientId = UUID.randomUUID();

        patient = new Patient();
        patient.setId(patientId);
        patient.setNomeCompleto("João Silva");
        patient.setEmail("joao@email.com");
        patient.setCpf("12345678901");
        patient.setDataNascimento(LocalDate.of(1990, 1, 1));
        patient.setTelefone("21999999999");
        patient.setStatusAtivo(true);

        responseDTO = new PatientResponseDTO();
        responseDTO.setId(patientId);
        responseDTO.setNomeCompleto("João Silva");
        responseDTO.setEmail("joao@email.com");
        responseDTO.setDataNascimento(LocalDate.of(1990, 1, 1));
        responseDTO.setTelefone("21999999999");
        responseDTO.setStatusAtivo(true);
        responseDTO.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve criar paciente com autenticação - 201")
    @WithMockUser
    void createPatient_Authenticated_201() throws Exception {
        PatientRequestDTO requestDTO = new PatientRequestDTO();
        requestDTO.setNomeCompleto("João Silva");
        requestDTO.setEmail("joao@email.com");
        requestDTO.setCpf("12345678901");
        requestDTO.setDataNascimento(LocalDate.of(1990, 1, 1));
        requestDTO.setTelefone("21999999999");

        when(patientService.createPatient(any(PatientRequestDTO.class))).thenReturn(patient);
        when(patientMapper.toResponseDTO(patient)).thenReturn(responseDTO);

        String body = objectMapper.writeValueAsString(requestDTO);
        mockMvc.perform(post("/api/v1/patients")
                        .with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomeCompleto").value("João Silva"));
    }

    @Test
    @DisplayName("Deve listar pacientes com autenticação - 200")
    @WithMockUser
    void getAllPatients_Authenticated_200() throws Exception {
        when(patientService.getAllPatients()).thenReturn(List.of(patient));
        when(patientMapper.toResponseDTO(patient)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomeCompleto").value("João Silva"));
    }

    @Test
    @DisplayName("Deve buscar paciente por ID com autenticação - 200")
    @WithMockUser
    void getPatientById_Authenticated_200() throws Exception {
        when(patientService.getPatientById(patientId)).thenReturn(patient);
        when(patientMapper.toResponseDTO(patient)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/patients/{id}", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeCompleto").value("João Silva"));
    }

    @Test
    @DisplayName("Deve atualizar paciente com autenticação - 200")
    @WithMockUser
    void updatePatient_Authenticated_200() throws Exception {
        PatientUpdateDTO updateDTO = new PatientUpdateDTO();
        updateDTO.setNomeCompleto("João Atualizado");

        when(patientService.updatePatient(any(UUID.class), any(PatientUpdateDTO.class))).thenReturn(patient);
        when(patientMapper.toResponseDTO(patient)).thenReturn(responseDTO);

        String body = objectMapper.writeValueAsString(updateDTO);
        mockMvc.perform(put("/api/v1/patients/{id}", patientId)
                        .with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve deletar paciente com autenticação - 204")
    @WithMockUser
    void deletePatient_Authenticated_204() throws Exception {
        doNothing().when(patientService).deletePatient(patientId);

        mockMvc.perform(delete("/api/v1/patients/{id}", patientId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve retornar 401 sem autenticação")
    void getAllPatients_Unauthenticated_401() throws Exception {
        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve retornar 404 quando paciente não encontrado")
    @WithMockUser
    void getPatientById_NotFound_404() throws Exception {
        when(patientService.getPatientById(any(UUID.class)))
                .thenThrow(new PatientNotFoundException(patientId));

        mockMvc.perform(get("/api/v1/patients/{id}", patientId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 400 com dados inválidos")
    @WithMockUser
    void createPatient_InvalidData_400() throws Exception {
        PatientRequestDTO invalidDTO = new PatientRequestDTO();

        String body = objectMapper.writeValueAsString(invalidDTO);
        mockMvc.perform(post("/api/v1/patients")
                        .with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
