
package com.voiceclone.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.voiceclone.backend.entity.AudioFile;
import com.voiceclone.backend.service.AudioService;

@RestController
@RequestMapping("/api/audio")
public class AudioController {

    private final AudioService audioService;

    public AudioController(AudioService audioService) {
        this.audioService = audioService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadAudio(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please log in first."));
        }

        try {
            AudioFile audioFile = audioService.uploadAudio(
                    file, authentication.getName());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Audio uploaded successfully");
            response.put("audioId", audioFile.getId());
            response.put("filename", audioFile.getOriginalFilename());
            response.put("status", audioFile.getStatus());
            response.put("uploadedAt", audioFile.getUploadedAt());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to upload audio"));
        }
    }

    /*
     * The browser provides only the ID of an audio file owned by the
     * authenticated user. Spring Boot obtains the AI result from FastAPI.
     */
    @PostMapping("/{audioId}/analysis")
    public ResponseEntity<?> runAndSaveAnalysis(
            @PathVariable Long audioId,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please log in first."));
        }

        try {
            Map<String, Object> response =
                    audioService.analyzeAndSave(
                            audioId, authentication.getName());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                            "message",
                            "AI analysis failed. Check the AI service and audio file."));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getAudioHistory(
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please log in first."));
        }

        try {
            List<AudioFile> files = audioService.getUserAudioFiles(
                    authentication.getName());

            List<Map<String, Object>> history = files.stream()
                    .map(file -> {
                        Map<String, Object> item = new HashMap<>();

                        item.put("id", file.getId());
                        item.put("filename", file.getOriginalFilename());
                        item.put("contentType", file.getContentType());
                        item.put("fileSize", file.getFileSize());
                        item.put("status", file.getStatus());
                        item.put("uploadedAt", file.getUploadedAt());

                        item.put("detectionStatus", file.getDetectionStatus());
                        item.put("meanBonaFideScore", file.getMeanBonaFideScore());
                        item.put("medianBonaFideScore", file.getMedianBonaFideScore());
                        item.put("segmentsAnalyzed", file.getSegmentsAnalyzed());
                        item.put("sampleRate", file.getSampleRate());
                        item.put("analysisMessage", file.getAnalysisMessage());
                        item.put("analyzedAt", file.getAnalyzedAt());

                        return item;
                    })
                    .toList();

            return ResponseEntity.ok(history);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch audio history"));
        }
    }
}
