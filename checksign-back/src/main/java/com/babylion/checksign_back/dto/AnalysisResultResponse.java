package com.babylion.checksign_back.dto;

import com.babylion.checksign_back.model.AnalysisStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalysisResultResponse {
    private final AnalysisStatus status;
    private final String message;
    private final Object result;
    private final String errorMessage;
    private final LocalDateTime requestTime;
    private final LocalDateTime completeTime;

    public static AnalysisResultResponse from(com.babylion.checksign_back.model.AiAnalysis analysis) {
        Object resultObject = null;
        if (analysis.getStatus() == AnalysisStatus.COMPLETED && analysis.getResult() != null) {
            try {
                resultObject = new ObjectMapper().readValue(analysis.getResult(), Object.class);
            } catch (JsonProcessingException e) {
                // Should not happen if data is valid JSON
                resultObject = "Error parsing result JSON";
            }
        }

        String message = null;
        if (analysis.getStatus() == AnalysisStatus.PENDING) {
            message = "Analysis in progress";
        }

        return AnalysisResultResponse.builder()
                .status(analysis.getStatus())
                .message(message)
                .result(resultObject)
                .errorMessage(analysis.getErrorMessage())
                .requestTime(analysis.getRequestTime())
                .completeTime(analysis.getCompleteTime())
                .build();
    }
} 