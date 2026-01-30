package com.example.backend.video.aiJob;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoAiResultRepository extends JpaRepository<VideoAiResult , Long> {

    Optional<VideoAiResult> findByVideoId(Long videoId);
}
