package com.babylion.checksign_back.controller;

import com.babylion.checksign_back.dto.AnalysisRequestResponse;
import com.babylion.checksign_back.dto.AnalysisResultResponse;
import com.babylion.checksign_back.dto.ApiResponse;
import com.babylion.checksign_back.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<AnalysisRequestResponse>> requestAnalysis(@RequestParam("image_id") String imageId) {
        if (imageId == null || imageId.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("image_id is required"));
        }
        AnalysisRequestResponse response = analysisService.requestAnalysis(imageId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/result")
    public ResponseEntity<ApiResponse<AnalysisResultResponse>> getAnalysisResult(@RequestParam("image_id") String imageId) {
        AnalysisResultResponse response = analysisService.getAnalysisResult(imageId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
} 