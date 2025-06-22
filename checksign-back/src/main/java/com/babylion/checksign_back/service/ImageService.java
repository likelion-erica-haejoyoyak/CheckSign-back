package com.babylion.checksign_back.service;

import com.babylion.checksign_back.dto.ImageUploadResponse;
import com.babylion.checksign_back.model.Image;
import com.babylion.checksign_back.repository.AiAnalysisRepository;
import com.babylion.checksign_back.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageService {

    private final ImageRepository imageRepository;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public ImageUploadResponse uploadImage(MultipartFile file) {
        try {
            String fileHash = fileStorageService.calculateFileHash(file);

            Optional<Image> existingImage = imageRepository.findByFileHash(fileHash);
            if (existingImage.isPresent()) {
                return ImageUploadResponse.builder().id(existingImage.get().getId()).build();
            }

            String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String imageId = UUID.randomUUID().toString().substring(0, 10);
            String filename = imageId + "." + extension;

            fileStorageService.storeFile(file, filename);

            Image image = Image.builder()
                    .id(imageId)
                    .filename(filename)
                    .extension(extension)
                    .fileHash(fileHash)
                    .build();

            imageRepository.save(image);

            return ImageUploadResponse.builder().id(imageId).build();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not upload image", e);
        }
    }
    
    public Resource getImageResource(String imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + imageId));
        return fileStorageService.loadFileAsResource(image.getFilename());
    }

    public String getImageContentType(String imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + imageId));
        
        return "image/" + image.getExtension();
    }

    @Transactional
    public void deleteImage(String imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + imageId));
        
        aiAnalysisRepository.deleteById(imageId);
        imageRepository.deleteById(imageId);
        fileStorageService.deleteFile(image.getFilename());
    }
} 