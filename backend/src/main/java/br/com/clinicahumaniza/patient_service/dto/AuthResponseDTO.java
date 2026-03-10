package br.com.clinicahumaniza.patient_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String token;
    private String tipo = "Bearer";
    private String nome;
    private String email;
    private String role;

    public AuthResponseDTO(String token, String nome, String email, String role) {
        this.token = token;
        this.nome = nome;
        this.email = email;
        this.role = role;
    }
}
