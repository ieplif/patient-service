package br.com.clinicahumaniza.patient_service.util;

import java.text.Normalizer;

/**
 * Normalização de texto para busca flexível por nome.
 *
 * Remove acentos, baixa a caixa, apara e colapsa espaços — de modo que "Tânia",
 * "  tania " e "TANIA" produzam o mesmo termo. Combinada com a busca por tokens
 * (cada palavra precisa aparecer no nome), permite achar nomes do meio fora de
 * ordem (ex.: "tania alves" encontra "Tânia Mara Barreto Alves").
 */
public final class BuscaNome {

    private BuscaNome() {}

    /** Sem acento, minúsculo, sem espaços nas pontas e com espaços internos colapsados. */
    public static String normalizar(String texto) {
        if (texto == null) return "";
        String semAcento =
                Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcento.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    /** Palavras da busca já normalizadas (sem entradas vazias). */
    public static String[] tokens(String texto) {
        String normalizado = normalizar(texto);
        if (normalizado.isEmpty()) return new String[0];
        return normalizado.split(" ");
    }
}
