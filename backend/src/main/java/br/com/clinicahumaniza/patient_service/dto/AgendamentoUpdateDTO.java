package br.com.clinicahumaniza.patient_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoUpdateDTO {

    private LocalDateTime dataHora;
    private Integer duracaoMinutos;
    private String observacoes;

    /**
     * Novo profissional da sessão. Só é aplicado quando {@link #alterarProfissional}
     * for true. Pode ser null (= remover profissional, deixar "Sem profissional").
     */
    private UUID profissionalId;

    /**
     * Sinaliza intenção de alterar o profissional. Necessário porque profissionalId
     * null é ambíguo: "não mexer" vs "remover". Quando false/ausente, o profissional
     * atual é mantido.
     */
    private Boolean alterarProfissional;
}
