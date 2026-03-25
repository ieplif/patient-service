package br.com.clinicahumaniza.patient_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReposicaoInfoDTO {

    private long reposicoesUsadasNoMes;
    private int limiteReposicoesMes;
    private List<UUID> agendamentosComDireitoReposicao;
}
