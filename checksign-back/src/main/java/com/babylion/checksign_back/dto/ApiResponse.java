package com.babylion.checksign_back.dto;

import lombok.Getter;

@Getter
public class ApiResponse<T> {

    private final T data;
    private final boolean success;
    private final String message;

    public ApiResponse(T data, boolean success, String message) {
        this.data = data;
        this.success = success;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(data, true, message);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, true, null);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(null, false, message);
    }
} 