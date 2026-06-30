package com.tustudio.readingassessmentapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class TranscriptionService{

    @Value("${openai.api-url}")
    private String apiUrl;

    @Value("${openai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public TranscriptionService() {
        this.restTemplate = new RestTemplate();
    }

    public String transcribeAudio(MultipartFile audioFile) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(apiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // Convertimos el MultipartFile de Spring a un Resource con nombre de archivo
        ByteArrayResource audioResource = new ByteArrayResource(audioFile.getBytes()) {
            @Override
            public String getFilename() {
                // OpenAI exige que el archivo tenga un nombre con extensión válida
                return audioFile.getOriginalFilename() != null ? audioFile.getOriginalFilename() : "audio.webm";
            }
        };

        body.add("file", audioResource);
        body.add("model", "whisper-1");
        body.add("language", "en"); // Forzamos inglés para el reto de lectura

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // Hacemos la petición POST a OpenAI
        ResponseEntity<WhisperResponse> response = restTemplate.postForEntity(
                apiUrl,
                requestEntity,
                WhisperResponse.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody().text();
        }

        throw new RuntimeException("Fallo al comunicarse con la API de OpenAI: " + response.getStatusCode());
    }

    // Usamos un Record interno para mapear la respuesta JSON de OpenAI de forma limpia
    private record WhisperResponse(String text) {}
}