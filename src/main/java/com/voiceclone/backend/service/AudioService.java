package com.voiceclone.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.voiceclone.backend.entity.AudioFile;
import com.voiceclone.backend.entity.User;
import com.voiceclone.backend.repository.AudioFileRepository;
import com.voiceclone.backend.repository.UserRepository;

@Service
public class AudioService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private final AudioFileRepository audioFileRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AiDetectionClient aiDetectionClient;
    private final Path uploadDirectory;

    public AudioService(
            AudioFileRepository audioFileRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            AiDetectionClient aiDetectionClient,
            @Value("${file.upload-dir:uploads}") String uploadDir) {

        this.audioFileRepository = audioFileRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.aiDetectionClient = aiDetectionClient;

        this.uploadDirectory = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();
    }

    // ---------------------------------------------------------
    // UPLOAD AUDIO
    // ---------------------------------------------------------

    public AudioFile uploadAudio(
            MultipartFile file,
            String userEmail) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Audio file is required.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Audio file must not exceed 20MB.");
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null
                || originalFilename.isBlank()) {
            throw new IllegalArgumentException(
                    "Audio filename is required.");
        }

        // Verify the file signature, not only the browser-supplied MIME type.
        if (!hasSupportedAudioSignature(file)) {
            throw new IllegalArgumentException(
                    "The file contents are not a supported audio format.");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        String filePath = fileStorageService.storeFile(file);

        Path storedFile = Path.of(filePath)
                .toAbsolutePath()
                .normalize();

        String storedFilename = storedFile.getFileName().toString();

        AudioFile audioFile = new AudioFile();

        audioFile.setOriginalFilename(
                org.springframework.util.StringUtils
                        .cleanPath(originalFilename));

        audioFile.setStoredFilename(storedFilename);
        audioFile.setFilePath(storedFile.toString());

        audioFile.setContentType(
                file.getContentType() == null
                        ? "application/octet-stream"
                        : file.getContentType());

        audioFile.setFileSize(file.getSize());
        audioFile.setStatus("UPLOADED");
        audioFile.setUploadedAt(LocalDateTime.now());
        audioFile.setUser(user);

        try {
            return audioFileRepository.save(audioFile);
        } catch (RuntimeException e) {

            // Avoid leaving an orphaned file when the database save fails.
            try {
                Files.deleteIfExists(storedFile);
            } catch (IOException ignored) {
                // Preserve the original database error.
            }

            throw e;
        }
    }

    // ---------------------------------------------------------
    // ANALYZE AUDIO AND SAVE RESULTS
    // ---------------------------------------------------------

    @Transactional
    public Map<String, Object> analyzeAndSave(
            Long audioId,
            String userEmail) {

        if (audioId == null) {
            throw new IllegalArgumentException(
                    "Audio ID is required.");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        // Ensure the recording belongs to the authenticated user.
        AudioFile audioFile = audioFileRepository
                .findByIdAndUserId(audioId, user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Audio file not found for this user."));

        String storedPath = audioFile.getFilePath();

        if (storedPath == null || storedPath.isBlank()) {
            throw new IllegalArgumentException(
                    "Stored audio file path is missing.");
        }

        Path audioPath;

        try {
            Path configuredRoot = uploadDirectory.toRealPath();

            Path requestedPath = Path.of(storedPath)
                    .toAbsolutePath()
                    .normalize();

            if (!requestedPath.startsWith(uploadDirectory)
                    || !Files.isRegularFile(requestedPath)
                    || !Files.isReadable(requestedPath)) {

                throw new IllegalArgumentException(
                        "Stored audio file is unavailable.");
            }

            // Resolve symbolic links to prevent paths escaping the upload directory.
            audioPath = requestedPath.toRealPath();

            if (!audioPath.startsWith(configuredRoot)) {
                throw new IllegalArgumentException(
                        "Stored audio path is outside the upload directory.");
            }

        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Stored audio file is unavailable.");
        }

        // The AI result comes from FastAPI, not from the browser.
        Map<String, Object> result = aiDetectionClient.detect(audioPath);

        Double meanScore =
                doubleValue(result.get("mean_bona_fide_score"));

        Double medianScore =
                doubleValue(result.get("median_bona_fide_score"));

        Integer segments =
                integerValue(result.get("segments_analyzed"));

        Integer sampleRate =
                integerValue(result.get("sample_rate"));

        if (meanScore == null
                || medianScore == null
                || segments == null
                || segments <= 0
                || sampleRate == null
                || sampleRate <= 0) {

            throw new IllegalStateException(
                    "AI service returned incomplete analysis data.");
        }

        // No definitive classification until a threshold is validated.
        
        String classification =
                stringValue(result.get("classification"));

        if (!"BONAFIDE".equals(classification)
                && !"SPOOF".equals(classification)) {
            throw new IllegalStateException(
                    "AI service returned an invalid classification.");
        }

        result.put("classification", classification);
        audioFile.setDetectionStatus(classification);

        audioFile.setMeanBonaFideScore(meanScore);
        audioFile.setMedianBonaFideScore(medianScore);
        audioFile.setSegmentsAnalyzed(segments);
        audioFile.setSampleRate(sampleRate);
        audioFile.setAnalysisMessage(
                stringValue(result.get("message")));
        audioFile.setAnalyzedAt(LocalDateTime.now());

        AudioFile saved = audioFileRepository.save(audioFile);

        Map<String, Object> response = new HashMap<>();

        response.put(
                "message",
                "Analysis completed and saved successfully.");

        response.put("audioId", saved.getId());
        response.put("result", result);
        response.put("detectionStatus", saved.getDetectionStatus());
        response.put("meanBonaFideScore", saved.getMeanBonaFideScore());
        response.put("medianBonaFideScore", saved.getMedianBonaFideScore());
        response.put("segmentsAnalyzed", saved.getSegmentsAnalyzed());
        response.put("sampleRate", saved.getSampleRate());
        response.put("analyzedAt", saved.getAnalyzedAt());

        return response;
    }

    // ---------------------------------------------------------
    // GET USER'S UPLOADED AUDIO FILES
    // ---------------------------------------------------------

    public List<AudioFile> getUserAudioFiles(String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        return audioFileRepository
                .findByUserIdOrderByUploadedAtDesc(user.getId());
    }

    // ---------------------------------------------------------
    // AUDIO SIGNATURE VALIDATION
    // ---------------------------------------------------------

    private boolean hasSupportedAudioSignature(MultipartFile file)
            throws IOException {

        byte[] header = new byte[12];
        int count;

        try (var inputStream = file.getInputStream()) {
            // Read as much of the header as possible.
            count = inputStream.readNBytes(
                    header, 0, header.length);
        }

        if (count < 4) {
            return false;
        }

        // WAV: RIFF....WAVE
        if (count >= 12
                && matches(header, 0, "RIFF")
                && matches(header, 8, "WAVE")) {
            return true;
        }

        // RF64 WAV: RF64....WAVE
        if (count >= 12
                && matches(header, 0, "RF64")
                && matches(header, 8, "WAVE")) {
            return true;
        }

        // AIFF and AIFF-C: FORM....AIFF or FORM....AIFC
        if (count >= 12
                && matches(header, 0, "FORM")
                && (matches(header, 8, "AIFF")
                    || matches(header, 8, "AIFC"))) {
            return true;
        }

        // FLAC
        if (matches(header, 0, "fLaC")) {
            return true;
        }

        // OGG, OGA and typical Ogg-contained OPUS files.
        if (matches(header, 0, "OggS")) {
            return true;
        }

        // WebM / Matroska EBML signature
        if (count >= 4
                && (header[0] & 0xFF) == 0x1A
                && (header[1] & 0xFF) == 0x45
                && (header[2] & 0xFF) == 0xDF
                && (header[3] & 0xFF) == 0xA3) {
            return true;
        }

        // MP4 / M4A: ISO base media file starts with an ftyp box.
        if (count >= 8 && matches(header, 4, "ftyp")) {
            return true;
        }

        // MP3: ID3 tag
        if (matches(header, 0, "ID3")) {
            return true;
        }

        // MP3: MPEG audio frame sync
        if ((header[0] & 0xFF) == 0xFF
                && ((header[1] & 0xE0) == 0xE0)) {
            return true;
        }

        // AAC ADTS frame sync
        if ((header[0] & 0xFF) == 0xFF
                && ((header[1] & 0xF6) == 0xF0)) {
            return true;
        }

        return false;
    }

    private boolean matches(
            byte[] bytes,
            int offset,
            String expected) {

        if (offset + expected.length() > bytes.length) {
            return false;
        }

        for (int i = 0; i < expected.length(); i++) {
            if ((bytes[offset + i] & 0xFF)
                    != expected.charAt(i)) {
                return false;
            }
        }

        return true;
    }

    // ---------------------------------------------------------
    // RESULT CONVERSION HELPERS
    // ---------------------------------------------------------

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Double doubleValue(Object value) {

        if (value instanceof Number number) {
            double result = number.doubleValue();

            return Double.isFinite(result) ? result : null;
        }

        if (value instanceof String text && !text.isBlank()) {
            try {
                double result = Double.parseDouble(text);

                return Double.isFinite(result) ? result : null;

            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private Integer integerValue(Object value) {

        if (value instanceof Number number) {
            double result = number.doubleValue();

            if (Double.isFinite(result)
                    && result == Math.rint(result)
                    && result >= Integer.MIN_VALUE
                    && result <= Integer.MAX_VALUE) {
                return (int) result;
            }

            return null;
        }

        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.valueOf(text);

            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }
}