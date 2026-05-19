package br.com.clinicahumaniza.patient_service.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReposicaoInfoDTO {

    private long reposicoesUsadasNoMes;
    private int limiteReposicoesMes;
    private List<UUID> agendamentosComDireitoReposicao;
}
