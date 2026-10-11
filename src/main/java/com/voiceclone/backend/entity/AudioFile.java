package com.voiceclone.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "audio_files")
public class AudioFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, unique = true)
    private String storedFilename;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    // Detection results
    @Column(name = "detection_status")
    private String detectionStatus;

    @Column(name = "mean_bona_fide_score")
    private Double meanBonaFideScore;

    @Column(name = "median_bona_fide_score")
    private Double medianBonaFideScore;

    @Column(name = "segments_analyzed")
    private Integer segmentsAnalyzed;

    @Column(name = "sample_rate")
    private Integer sampleRate;

    @Column(name = "analysis_message", columnDefinition = "TEXT")
    private String analysisMessage;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public AudioFile() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getDetectionStatus() {
        return detectionStatus;
    }

    public void setDetectionStatus(String detectionStatus) {
        this.detectionStatus = detectionStatus;
    }

    public Double getMeanBonaFideScore() {
        return meanBonaFideScore;
    }

    public void setMeanBonaFideScore(Double meanBonaFideScore) {
        this.meanBonaFideScore = meanBonaFideScore;
    }

    public Double getMedianBonaFideScore() {
        return medianBonaFideScore;
    }

    public void setMedianBonaFideScore(Double medianBonaFideScore) {
        this.medianBonaFideScore = medianBonaFideScore;
    }

    public Integer getSegmentsAnalyzed() {
        return segmentsAnalyzed;
    }

    public void setSegmentsAnalyzed(Integer segmentsAnalyzed) {
        this.segmentsAnalyzed = segmentsAnalyzed;
    }

    public Integer getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
    }

    public String getAnalysisMessage() {
        return analysisMessage;
    }

    public void setAnalysisMessage(String analysisMessage) {
        this.analysisMessage = analysisMessage;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}