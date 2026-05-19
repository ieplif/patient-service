package br.com.clinicahumaniza.patient_service.service;

import br.com.clinicahumaniza.patient_service.dto.ProntuarioResponseDTO;
import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.model.Prontuario;
import br.com.clinicahumaniza.patient_service.model.TipoDocumento;
import br.com.clinicahumaniza.patient_service.repository.PatientRepository;
import br.com.clinicahumaniza.patient_service.repository.ProntuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProntuarioService")
class ProntuarioServiceTest {

    @Mock
    private ProntuarioRepository prontuarioRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private SupabaseStorageService storageService;

    @InjectMocks
    private ProntuarioService prontuarioService;

    private UUID pacienteId;
    private Patient paciente;
    private MockMultipartFile pdfValido;

    @BeforeEach
    void setUp() {
        pacienteId = UUID.randomUUID();
        paciente = new Patient();
        paciente.setId(pacienteId);
        paciente.setNomeCompleto("Maria Santos");

        pdfValido = new MockMultipartFile(
                "file", "recibo.pdf", "application/pdf", "conteudo-pdf".getBytes());

        // Usuario autenticado para o campo uploadedBy
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("caissa@humaniza.com", null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- Validacao de arquivo ---

    @Test
    @DisplayName("Deve rejeitar arquivo vazio")
    void upload_ArquivoVazio() {
        MockMultipartFile vazio = new MockMultipartFile(
                "file", "vazio.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> prontuarioService.upload(pacienteId, TipoDocumento.PRONTUARIO, "T", null, vazio))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("vazio");
    }

    @Test
    @DisplayName("Deve rejeitar arquivo acima de 10MB")
    void upload_ArquivoMuitoGrande() {
        byte[] grande = new byte[10_485_761]; // 10MB + 1 byte
        MockMultipartFile big = new MockMultipartFile(
                "file", "grande.pdf", "application/pdf", grande);

        assertThatThrownBy(() -> prontuarioService.upload(pacienteId, TipoDocumento.PRONTUARIO, "T", null, big))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("muito grande");
    }

    @Test
    @DisplayName("Deve rejeitar tipo de arquivo nao permitido")
    void upload_TipoInvalido() {
        MockMultipartFile exe = new MockMultipartFile(
                "file", "virus.exe", "application/x-msdownload", "x".getBytes());

        assertThatThrownBy(() -> prontuarioService.upload(pacienteId, TipoDocumento.PRONTUARIO, "T", null, exe))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tipo de arquivo");
    }

    @Test
    @DisplayName("Deve rejeitar nome de arquivo com path traversal")
    void upload_NomeArquivoInvalido() {
        MockMultipartFile malicioso = new MockMultipartFile(
                "file", "../../etc/passwd.pdf", "application/pdf", "x".getBytes());

        assertThatThrownBy(() -> prontuarioService.upload(pacienteId, TipoDocumento.PRONTUARIO, "T", null, malicioso))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("inválido");
    }

    // --- Upload ---

    @Test
    @DisplayName("Deve fazer upload com sucesso")
    void upload_Sucesso() throws IOException {
        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(storageService.upload(any(), any())).thenReturn("caminho/arquivo.pdf");
        when(storageService.getSignedUrl(any())).thenReturn("https://signed-url");
        when(prontuarioRepository.save(any(Prontuario.class))).thenAnswer(inv -> {
            Prontuario p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        ProntuarioResponseDTO dto = prontuarioService.upload(
                pacienteId, TipoDocumento.NOTA_FISCAL, "NF Maio", "obs", pdfValido);

        assertThat(dto).isNotNull();
        assertThat(dto.getTipo()).isEqualTo(TipoDocumento.NOTA_FISCAL);
        assertThat(dto.getTitulo()).isEqualTo("NF Maio");
        verify(storageService).upload(any(), any());
        verify(prontuarioRepository).save(any(Prontuario.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando paciente não existe")
    void upload_PacienteNaoEncontrado() {
        when(patientRepository.findById(pacienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prontuarioService.upload(
                pacienteId, TipoDocumento.PRONTUARIO, "T", null, pdfValido))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Deve remover arquivo do Storage (rollback) quando o save no banco falha")
    void upload_RollbackQuandoSaveFalha() throws IOException {
        when(patientRepository.findById(pacienteId)).thenReturn(Optional.of(paciente));
        when(storageService.upload(any(), any())).thenReturn("caminho/arquivo.pdf");
        when(storageService.getSignedUrl(any())).thenReturn("https://signed-url");
        when(prontuarioRepository.save(any(Prontuario.class)))
                .thenThrow(new RuntimeException("value too long for type"));

        assertThatThrownBy(() -> prontuarioService.upload(
                pacienteId, TipoDocumento.PRONTUARIO, "T", null, pdfValido))
                .isInstanceOf(RuntimeException.class);

        // O arquivo orfao deve ter sido removido do Storage
        verify(storageService).delete("caminho/arquivo.pdf");
    }

    // --- Delete ---

    @Test
    @DisplayName("Deve excluir prontuário e o arquivo do Storage")
    void delete_Sucesso() {
        UUID id = UUID.randomUUID();
        Prontuario p = Prontuario.builder()
                .id(id).paciente(paciente).storagePath("caminho/arquivo.pdf").build();
        when(prontuarioRepository.findById(id)).thenReturn(Optional.of(p));

        prontuarioService.delete(id);

        verify(storageService).delete("caminho/arquivo.pdf");
        verify(prontuarioRepository).delete(p);
    }

    @Test
    @DisplayName("Deve lançar exceção ao excluir prontuário inexistente")
    void delete_NaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(prontuarioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prontuarioService.delete(id))
                .isInstanceOf(EntityNotFoundException.class);
        verify(storageService, never()).delete(any());
    }

    // --- GetById ---

    @Test
    @DisplayName("Deve lançar exceção ao buscar prontuário inexistente")
    void getById_NaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(prontuarioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prontuarioService.getById(id))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
