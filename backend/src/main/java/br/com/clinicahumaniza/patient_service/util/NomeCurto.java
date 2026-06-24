package br.com.clinicahumaniza.patient_service.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Encurta um nome para "Primeiro + Último", ignorando conectivos (de, da, dos, e…).
 * Espelha o shortenName do frontend — usado, por ex., no título do evento do
 * Google Calendar para não expor o nome completo da paciente.
 */
public final class NomeCurto {

    private NomeCurto() {}

    private static final Set<String> CONECTIVOS = Set.of(
            "de", "da", "do", "das", "dos", "e", "di", "del", "della", "dello", "van", "von", "der", "den", "la", "le",
            "lo");

    public static String primeiroEUltimo(String nomeCompleto) {
        if (nomeCompleto == null) return "";
        String trimmed = nomeCompleto.trim();
        if (trimmed.isEmpty()) return "";

        List<String> significativas = new ArrayList<>();
        for (String parte : trimmed.split("\\s+")) {
            if (!CONECTIVOS.contains(parte.toLowerCase())) significativas.add(parte);
        }
        if (significativas.isEmpty()) return trimmed;
        if (significativas.size() == 1) return significativas.get(0);
        return significativas.get(0) + " " + significativas.get(significativas.size() - 1);
    }
}
