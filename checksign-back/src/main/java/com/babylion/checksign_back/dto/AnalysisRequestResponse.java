package com.babylion.checksign_back.dto;

import com.babylion.checksign_back.model.AnalysisStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalysisRequestResponse {
    private final String message;
    private final AnalysisStatus status;
    private final Object result;
    private final LocalDateTime requestTime;
    private final LocalDateTime completeTime;
} 