package com.tustudio.readingassessmentapi.dto;

public record AnalyzedWord(
        String word,
        WordStatus status
) {}
