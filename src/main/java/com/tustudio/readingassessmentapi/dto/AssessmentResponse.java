package com.tustudio.readingassessmentapi.dto;

import java.util.List;

public record AssessmentResponse(
        double accuracyScore,
        List<AnalyzedWord> analyzedWords
) {}
