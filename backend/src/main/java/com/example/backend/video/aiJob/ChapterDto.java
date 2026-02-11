package com.example.backend.video.aiJob;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
public class ChapterDto {
    private int start;
    private String title;
    private String description; //optional
}
