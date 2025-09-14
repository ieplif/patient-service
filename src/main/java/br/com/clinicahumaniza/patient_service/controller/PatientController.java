package br.com.clinicahumaniza.patient_service.controller;

import br.com.clinicahumaniza.patient_service.model.Patient;
import br.com.clinicahumaniza.patient_service.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController // 1. Combina @Controller e @ResponseBody. Indica que os retornos dos métodos serão o corpo da resposta, serializados para JSON.
@RequestMapping("/api/v1/patients" ) // 2. Mapeia todas as requisições que começam com este caminho para este controller.
public class PatientController {

    private final PatientService patientService;

    @Autowired // 3. Injeta a dependência da nossa camada de serviço.
    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    // Endpoint para CRIAR um novo paciente
    @PostMapping // 4. Mapeia requisições HTTP POST para este método. A URL completa será: POST /api/v1/patients
    public ResponseEntity<Patient> createPatient(@RequestBody Patient patient) { // 5. @RequestBody desserializa o JSON do corpo da requisição para um objeto Patient.
        try {
            Patient createdPatient = patientService.createPatient(patient);
            // 6. Retorna o paciente criado e o status HTTP 201 Created.
            return new ResponseEntity<>(createdPatient, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            // Tratamento de erro simples para o caso de CPF/E-mail duplicado.
            // Em uma aplicação real, usaríamos @ControllerAdvice para um tratamento mais elegante.
            return ResponseEntity.badRequest().body(null); // Retorna 400 Bad Request.
        }
    }

    // Endpoint para LISTAR todos os pacientes
    @GetMapping // 7. Mapeia requisições HTTP GET. URL: GET /api/v1/patients
    public ResponseEntity<List<Patient>> getAllPatients() {
        List<Patient> patients = patientService.getAllPatients();
        return ResponseEntity.ok(patients); // 8. Retorna a lista de pacientes e o status HTTP 200 OK.
    }

    // Endpoint para BUSCAR um paciente por ID
    @GetMapping("/{id}") // 9. Mapeia requisições GET com uma variável na URL. Ex: GET /api/v1/patients/123e4567-e89b-12d3-a456-426614174000
    public ResponseEntity<Patient> getPatientById(@PathVariable UUID id) { // 10. @PathVariable extrai o valor da URL e o atribui ao parâmetro 'id'.
        return patientService.getPatientById(id)
                .map(patient -> ResponseEntity.ok(patient)) // Se o paciente for encontrado, retorna 200 OK com o paciente.
                .orElse(ResponseEntity.notFound().build()); // Se não for encontrado, retorna 404 Not Found.
    }
}
