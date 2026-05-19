package br.com.clinicahumaniza.patient_service.dto;

import java.time.LocalDate;

import br.com.clinicahumaniza.patient_service.model.FormaPagamento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoUpdateDTO {

    private FormaPagamento formaPagamento;

    private LocalDate dataVencimento;

    private String observacoes;
}
