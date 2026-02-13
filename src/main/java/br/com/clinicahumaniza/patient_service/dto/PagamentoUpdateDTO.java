package br.com.clinicahumaniza.patient_service.dto;

import br.com.clinicahumaniza.patient_service.model.FormaPagamento;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoUpdateDTO {

    private FormaPagamento formaPagamento;

    private LocalDate dataVencimento;

    private String observacoes;
}
