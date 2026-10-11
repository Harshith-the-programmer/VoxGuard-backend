
package com.voiceclone.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;


import com.voiceclone.backend.entity.AudioFile;


public interface AudioFileRepository extends JpaRepository<AudioFile, Long> {

    List<AudioFile> findByUserIdOrderByUploadedAtDesc(Long userId);

    Optional<AudioFile> findByIdAndUserId(Long id, Long userId);
}
