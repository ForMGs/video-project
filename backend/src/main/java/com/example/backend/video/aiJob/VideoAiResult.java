package com.example.backend.video.aiJob;

import lombok.*;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "video_ai_result",
        uniqueConstraints = {
                @UniqueConstraint(name= "uk_video_ai_video", columnNames = "video_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class VideoAiResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name ="video_id", nullable=false)
    private Long videoId;

    //잡상태
    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private AiJobStatus status;

    //재시도 횟수
    @Column(nullable = false)
    private int attempt;

    @Column(length =10)
    private String language;

    //STT 결과
    @Lob
    @Column(columnDefinition = "json")
    private String transcriptJson;

    //짧은 요약
    @Lob
    private String summaryShort;

    //긴요약
    @Lob
    private String summaryLong;

    @Lob
    @Column(columnDefinition = "json")
    private String chaptersJson;

    //실패사유
    @Lob
    private String errorMessage;

    @Column(nullable=false, updatable =false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updateAt;

    //상태변경 메서드
    public void markPending(){
        this.status = AiJobStatus.PENDING;
    }

    public void markRunning(){
        this.status = AiJobStatus.RUNNING;
        this.attempt++;
    }

    public void markDone(
            String language,
            String transcriptJson,
            String summaryShort,
            String summaryLong,
            String chaptersJson,
            String errorMessage) {
        this.status = AiJobStatus.DONE;
        this.language = language;
        this.transcriptJson = transcriptJson;
        this.summaryShort = summaryShort;
        this.summaryLong = summaryLong;
        this.chaptersJson = chaptersJson;
        this.errorMessage = errorMessage;
    }

    public void markFailed(String errorMessage){
        this.status = AiJobStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    @PrePersist
    void onCreate(){
        this.createdAt = this.updateAt = LocalDateTime.now();
        if(this.status == null){
            this.status = AiJobStatus.PENDING;
        }
    }

    @PreUpdate
    void onUpdate(){
        this.updateAt = LocalDateTime.now();
    }
}
