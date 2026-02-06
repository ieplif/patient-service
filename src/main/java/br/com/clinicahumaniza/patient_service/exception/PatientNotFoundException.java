package br.com.clinicahumaniza.patient_service.exception;

import java.util.UUID;

public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException(UUID id) {
        super("Paciente não encontrado com o ID: " + id);
    }
}
