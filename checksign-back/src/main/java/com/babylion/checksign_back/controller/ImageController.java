package com.babylion.checksign_back.controller;

import com.babylion.checksign_back.dto.ApiResponse;
import com.babylion.checksign_back.dto.ImageUploadResponse;
import com.babylion.checksign_back.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/imgupload")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(@RequestParam("image") MultipartFile file) {
        ImageUploadResponse response = imageService.uploadImage(file);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/get")
    public ResponseEntity<Resource> getImage(@RequestParam("id") String imageId) {
        Resource resource = imageService.getImageResource(imageId);
        String contentType = imageService.getImageContentType(imageId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(resource);
    }
    
    @PostMapping("/remove")
    public ResponseEntity<ApiResponse<Void>> removeImage(@RequestParam("id") String imageId) {
        imageService.deleteImage(imageId);
        return ResponseEntity.ok(ApiResponse.success(null, "Image removed successfully."));
    }
} 