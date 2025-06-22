package com.babylion.checksign_back.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_analysis", uniqueConstraints = {
        @UniqueConstraint(name = "unique_image_analysis", columnNames = {"image_id"})
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AiAnalysis {

    @Id
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @jakarta.persistence.MapsId
    @JoinColumn(name = "image_id")
    private Image image;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private AnalysisStatus status;

    @Column(length = 10000)
    private String result;

    @Column
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "request_time", updatable = false)
    private LocalDateTime requestTime;

    @Column(name = "complete_time")
    private LocalDateTime completeTime;

    @Version
    private Long version;
} 