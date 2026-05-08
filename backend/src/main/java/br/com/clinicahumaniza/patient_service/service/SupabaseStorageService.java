package br.com.clinicahumaniza.patient_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.service-key:}")
    private String supabaseServiceKey;

    @Value("${supabase.storage.bucket:prontuarios}")
    private String bucket;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Sanitiza o nome do arquivo para o path do Supabase Storage:
     * - Remove acentos (à -> a, ê -> e, ç -> c)
     * - Substitui caracteres não suportados ([^A-Za-z0-9._-]) por "_"
     * - Mantém a extensão e legibilidade
     *
     * Exemplo: "Captura de Tela às 21.39.png" -> "Captura_de_Tela_as_21.39.png"
     */
    static String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) return "arquivo";
        // NFD decompõe acentos; depois removemos os marcadores de acento
        String semAcentos = Normalizer.normalize(original, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // Substitui qualquer caractere que não seja [A-Za-z0-9._-] por "_"
        String safe = semAcentos.replaceAll("[^A-Za-z0-9._-]", "_");
        // Comprime sequências de "_" e remove "_" no início/fim (preservando extensão)
        safe = safe.replaceAll("_+", "_").replaceAll("^_|_$", "");
        return safe.isBlank() ? "arquivo" : safe;
    }

    public String upload(UUID pacienteId, MultipartFile file) throws IOException {
        String safeName = sanitizeFilename(file.getOriginalFilename());
        String fileName = pacienteId + "/" + UUID.randomUUID() + "_" + safeName;

        String url = supabaseUrl + "/storage/v1/object/" + bucket + "/" + fileName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseServiceKey);
        headers.setContentType(MediaType.parseMediaType(
                file.getContentType() != null ? file.getContentType() : "application/octet-stream"));

        HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (HttpClientErrorException e) {
            // Loga o status e body do Supabase para diagnóstico
            log.error("Falha no upload Supabase Storage. status={}, body={}, path={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), fileName);
            throw new IOException(
                    "Supabase Storage rejeitou o upload (HTTP " + e.getStatusCode().value() + "): "
                            + e.getResponseBodyAsString(),
                    e);
        } catch (Exception e) {
            log.error("Erro inesperado no upload Supabase Storage. path={}, msg={}", fileName, e.getMessage(), e);
            throw new IOException("Erro inesperado ao enviar arquivo: " + e.getMessage(), e);
        }

        return fileName;
    }

    public String getPublicUrl(String storagePath) {
        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + storagePath;
    }

    public String getSignedUrl(String storagePath) {
        // For private buckets, generate a signed URL
        String url = supabaseUrl + "/storage/v1/object/sign/" + bucket + "/" + storagePath;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseServiceKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"expiresIn\": 3600}";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            // Parse signedURL from response
            String responseBody = response.getBody();
            if (responseBody != null && responseBody.contains("signedURL")) {
                String signedPath = responseBody.split("\"signedURL\":\"")[1].split("\"")[0];
                return supabaseUrl + "/storage/v1" + signedPath;
            }
        } catch (Exception e) {
            // Fallback to public URL
        }
        return getPublicUrl(storagePath);
    }

    public void delete(String storagePath) {
        String url = supabaseUrl + "/storage/v1/object/" + bucket + "/" + storagePath;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseServiceKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
        } catch (Exception e) {
            // Log but don't fail
        }
    }
}
