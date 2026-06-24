package br.com.clinicahumaniza.patient_service.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NomeCurtoTest {

    @Test
    @DisplayName("primeiroEUltimo retorna primeiro + último ignorando conectivos")
    void primeiroEUltimo() {
        assertThat(NomeCurto.primeiroEUltimo("Maria")).isEqualTo("Maria");
        assertThat(NomeCurto.primeiroEUltimo("Maria Silva")).isEqualTo("Maria Silva");
        assertThat(NomeCurto.primeiroEUltimo("Maria das Graças Silva")).isEqualTo("Maria Silva");
        assertThat(NomeCurto.primeiroEUltimo("Clara Tupinambá Torres de Almeida"))
                .isEqualTo("Clara Almeida");
        assertThat(NomeCurto.primeiroEUltimo("  Tatiana   Vivas da Silva ")).isEqualTo("Tatiana Silva");
        assertThat(NomeCurto.primeiroEUltimo(null)).isEmpty();
        assertThat(NomeCurto.primeiroEUltimo("   ")).isEmpty();
    }
}
