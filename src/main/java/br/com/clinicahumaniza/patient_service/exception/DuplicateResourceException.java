package br.com.clinicahumaniza.patient_service.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String field, String value) {
        super(field + " já cadastrado: " + value);
    }
}
