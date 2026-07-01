package com.tustudio.readingassessmentapi.controller;
import com.tustudio.readingassessmentapi.dto.AssessmentResponse;
import com.tustudio.readingassessmentapi.service.AssessmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/assessment")
@CrossOrigin(origins = "http://localhost:4200")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<AssessmentResponse> analyzeReading(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam("referenceText") String referenceText) {

        try {
            AssessmentResponse response = assessmentService.analyzeReading(referenceText, audio);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            System.err.println("Error procesando el archivo de audio: " + e.getMessage());
            return ResponseEntity.internalServerError().build();

        } catch (RuntimeException e) {
            System.err.println("Error en el servicio de evaluación: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}