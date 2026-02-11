package com.example.backend.video.aiJob;
import lombok.*;
import java.util.List;

@Getter@Builder
@AllArgsConstructor
public class VideoAiResponse {
    private String status; //PENDING / RUNNING/ DONE / FAILED
    private String summaryShort; //optional
    private String summaryLong;
    private List<ChapterDto> chapters;
    private String errorMessage;
}
