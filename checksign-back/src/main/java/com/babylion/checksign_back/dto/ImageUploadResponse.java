package com.babylion.checksign_back.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ImageUploadResponse {
    private final String id;

    @Builder
    public ImageUploadResponse(String id) {
        this.id = id;
    }
} 