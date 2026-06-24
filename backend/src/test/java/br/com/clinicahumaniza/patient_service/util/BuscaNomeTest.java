package br.com.clinicahumaniza.patient_service.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BuscaNomeTest {

    @Test
    @DisplayName("normalizar remove acento, baixa caixa, apara e colapsa espaços")
    void normalizar() {
        assertThat(BuscaNome.normalizar("Tânia")).isEqualTo("tania");
        assertThat(BuscaNome.normalizar("JOSÉ")).isEqualTo("jose");
        assertThat(BuscaNome.normalizar("  Maria   Clara  ")).isEqualTo("maria clara");
        assertThat(BuscaNome.normalizar("Conceição")).isEqualTo("conceicao");
        assertThat(BuscaNome.normalizar(null)).isEmpty();
        assertThat(BuscaNome.normalizar("   ")).isEmpty();
    }

    @Test
    @DisplayName("tokens separa palavras normalizadas e ignora vazios")
    void tokens() {
        assertThat(BuscaNome.tokens("  Tânia   Alves ")).containsExactly("tania", "alves");
        assertThat(BuscaNome.tokens("José")).containsExactly("jose");
        assertThat(BuscaNome.tokens("")).isEmpty();
        assertThat(BuscaNome.tokens(null)).isEmpty();
    }
}
