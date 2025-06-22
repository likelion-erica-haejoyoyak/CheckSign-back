package com.babylion.checksign_back.service;

import com.babylion.checksign_back.dto.AnalysisRequestResponse;
import com.babylion.checksign_back.dto.AnalysisResultResponse;
import com.babylion.checksign_back.model.AiAnalysis;
import com.babylion.checksign_back.model.AnalysisStatus;
import com.babylion.checksign_back.model.Image;
import com.babylion.checksign_back.repository.AiAnalysisRepository;
import com.babylion.checksign_back.repository.ImageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    private final ImageRepository imageRepository;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final GeminiApiService geminiApiService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @Transactional
    public AnalysisRequestResponse requestAnalysis(String imageId) {
        Image image = imageRepository.findWithLockById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        Optional<AiAnalysis> existingAnalysis = aiAnalysisRepository.findById(imageId);

        if (existingAnalysis.isPresent()) {
            AiAnalysis analysis = existingAnalysis.get();
            if (analysis.getStatus() == AnalysisStatus.PENDING) {
                return AnalysisRequestResponse.builder().message("Analysis already in progress").build();
            } else if (analysis.getStatus() == AnalysisStatus.COMPLETED) {
                return AnalysisRequestResponse.builder()
                        .message("Analysis already completed")
                        .status(analysis.getStatus())
                        .result(analysis.getResult())
                        .requestTime(analysis.getRequestTime())
                        .completeTime(analysis.getCompleteTime())
                        .build();
            } else if (analysis.getStatus() == AnalysisStatus.FAILED) {
                analysis.setStatus(AnalysisStatus.PENDING);
                analysis.setRequestTime(LocalDateTime.now());
                analysis.setCompleteTime(null);
                analysis.setErrorMessage(null);
                aiAnalysisRepository.save(analysis);
            }
        } else {
            AiAnalysis newAnalysis = AiAnalysis.builder()
                    .image(image)
                    .status(AnalysisStatus.PENDING)
                    .build();
            aiAnalysisRepository.save(newAnalysis);
        }

        processAnalysisAsync(imageId);
        return AnalysisRequestResponse.builder().message("Analysis request submitted").build();
    }

    @Async
    @Transactional
    public void processAnalysisAsync(String imageId) {
        try {
            Image image = imageRepository.findById(imageId).orElseThrow(() -> new RuntimeException("Image not found"));
            Resource resource = fileStorageService.loadFileAsResource(image.getFilename());
            byte[] imageData = resource.getInputStream().readAllBytes();
            String mimeType = "image/" + image.getExtension();

            JsonNode response = geminiApiService.callGeminiApi(imageData, mimeType).block();

            AiAnalysis analysis = aiAnalysisRepository.findById(imageId).orElseThrow(() -> new RuntimeException("Analysis record not found"));
            
            if (response != null && response.has("candidates")) {
                String resultText = response.get("candidates").get(0).get("content").get("parts").get(0).get("text").asText();
                analysis.setResult(resultText);
                analysis.setStatus(AnalysisStatus.COMPLETED);
            } else {
                throw new RuntimeException("Invalid response from Gemini API: " + (response != null ? response.toString() : "null"));
            }
            
            analysis.setCompleteTime(LocalDateTime.now());
            aiAnalysisRepository.save(analysis);

        } catch (IOException | RuntimeException e) {
            AiAnalysis analysis = aiAnalysisRepository.findById(imageId).orElse(null);
            if (analysis != null) {
                analysis.setStatus(AnalysisStatus.FAILED);
                analysis.setErrorMessage(e.getMessage());
                analysis.setCompleteTime(LocalDateTime.now());
                aiAnalysisRepository.save(analysis);
            }
        }
    }

    public AnalysisResultResponse getAnalysisResult(String imageId) {
        AiAnalysis analysis = aiAnalysisRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Analysis record not found for id: " + imageId));

        return AnalysisResultResponse.from(analysis);
    }
} 