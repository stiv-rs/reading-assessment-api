package com.tustudio.readingassessmentapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class TranscriptionService {

    @Value("${gemini.api-url}")
    private String apiUrl;

    @Value("${gemini.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public TranscriptionService() {
        this.restTemplate = new RestTemplate();
    }

    public String transcribeAudio(MultipartFile audioFile) throws IOException {
        String base64Audio = Base64.getEncoder().encodeToString(audioFile.getBytes());
        String mimeType = audioFile.getContentType() != null ? audioFile.getContentType() : "audio/webm";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Estructura oficial para la API de Gemini
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", "Transcribe the following audio accurately."),
                                Map.of("inline_data", Map.of(
                                        "mime_type", mimeType,
                                        "data", base64Audio
                                ))
                        ))
                )
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        String fullUrl = apiUrl + apiKey;

        // Usamos el modelo directamente en la URL si es necesario,
        // pero intentemos primero esta estructura limpia
        ResponseEntity<Map> response = restTemplate.postForEntity(fullUrl, requestEntity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> bodyRes = response.getBody();
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) bodyRes.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");
        }
        throw new RuntimeException("Fallo en la API de Gemini: " + response.getStatusCode());
    }
}