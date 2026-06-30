package com.tustudio.readingassessmentapi.service;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import com.tustudio.readingassessmentapi.dto.AnalyzedWord;
import com.tustudio.readingassessmentapi.dto.AssessmentResponse;
import com.tustudio.readingassessmentapi.dto.WordStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class AssessmentService {

    private final TranscriptionService transcriptionService;

    public AssessmentService(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    public AssessmentResponse analyzeReading(String referenceText, MultipartFile audioFile) throws IOException {
        // 1. Obtener la transcripción de OpenAI
        String transcribedText = transcriptionService.transcribeAudio(audioFile);

        // 2. Limpiar y separar los textos (Original vs. Transcrito)
        List<String> originalWords = extractWords(referenceText);
        List<String> spokenWords = extractWords(transcribedText);

        // Pasamos a minúsculas solo para la comparación, no para el resultado final
        List<String> originalLower = originalWords.stream().map(String::toLowerCase).toList();
        List<String> spokenLower = spokenWords.stream().map(String::toLowerCase).toList();

        // 3. Generar el "Diff" (Diferencias)
        Patch<String> patch = DiffUtils.diff(originalLower, spokenLower);

        List<AnalyzedWord> analyzedWords = new ArrayList<>();
        int correctCount = 0;
        int originalIndex = 0;

        // 4. Mapear las diferencias (Deltas) a nuestro contrato JSON
        for (AbstractDelta<String> delta : patch.getDeltas()) {

            // Añadir las palabras que estuvieron correctas ANTES de encontrar un error
            while (originalIndex < delta.getSource().getPosition()) {
                analyzedWords.add(new AnalyzedWord(originalWords.get(originalIndex), WordStatus.CORRECT));
                correctCount++;
                originalIndex++;
            }

            // Manejar los errores según el tipo
            if (delta.getType() == DeltaType.DELETE) {
                // La palabra estaba en el original, pero no se dijo (Omitida)
                for (int i = 0; i < delta.getSource().getLines().size(); i++) {
                    analyzedWords.add(new AnalyzedWord(originalWords.get(originalIndex), WordStatus.OMITTED));
                    originalIndex++;
                }
            } else if (delta.getType() == DeltaType.CHANGE) {
                // La palabra se dijo mal (Misread)
                for (int i = 0; i < delta.getSource().getLines().size(); i++) {
                    analyzedWords.add(new AnalyzedWord(originalWords.get(originalIndex), WordStatus.MISREAD));
                    originalIndex++;
                }
            }
        }

        // Añadir cualquier palabra correcta que haya quedado al final del texto
        while (originalIndex < originalWords.size()) {
            analyzedWords.add(new AnalyzedWord(originalWords.get(originalIndex), WordStatus.CORRECT));
            correctCount++;
            originalIndex++;
        }

        // 5. Calcular la puntuación sobre 100
        double accuracyScore = originalWords.isEmpty() ? 0.0 :
                Math.round(((double) correctCount / originalWords.size()) * 100.0);

        return new AssessmentResponse(accuracyScore, analyzedWords);
    }

    private List<String> extractWords(String text) {
        if (text == null || text.trim().isEmpty()) return new ArrayList<>();
        // Limpiamos la puntuación básica para comparar de forma justa
        return Arrays.asList(text.replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s]", "").trim().split("\\s+"));
    }
}
