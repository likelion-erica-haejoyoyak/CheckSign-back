package com.babylion.checksign_back.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "images")
public class Image {

    @Id
    private String id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String extension;

    @Column(name = "file_hash", nullable = false, unique = true)
    private String fileHash;

    @CreationTimestamp
    @Column(name = "upload_timestamp")
    private LocalDateTime uploadTimestamp;

    @Builder
    public Image(String id, String filename, String extension, String fileHash) {
        this.id = id;
        this.filename = filename;
        this.extension = extension;
        this.fileHash = fileHash;
    }
} 