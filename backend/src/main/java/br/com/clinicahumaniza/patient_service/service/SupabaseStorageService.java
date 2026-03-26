package br.com.clinicahumaniza.patient_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.service-key:}")
    private String supabaseServiceKey;

    @Value("${supabase.storage.bucket:prontuarios}")
    private String bucket;

    private final RestTemplate restTemplate = new RestTemplate();

    public String upload(UUID pacienteId, MultipartFile file) throws IOException {
        String fileName = pacienteId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        String url = supabaseUrl + "/storage/v1/object/" + bucket + "/" + fileName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseServiceKey);
        headers.setContentType(MediaType.parseMediaType(
                file.getContentType() != null ? file.getContentType() : "application/octet-stream"));

        HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

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
