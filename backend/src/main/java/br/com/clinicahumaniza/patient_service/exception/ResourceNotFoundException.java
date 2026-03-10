package br.com.clinicahumaniza.patient_service.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, UUID id) {
        super(resourceName + " não encontrado(a) com o ID: " + id);
    }
}
