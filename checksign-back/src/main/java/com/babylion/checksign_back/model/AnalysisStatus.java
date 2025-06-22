package com.babylion.checksign_back.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AnalysisStatus {
    PENDING,
    COMPLETED,
    FAILED;

    @JsonValue
    public String getName() {
        return name();
    }
} 