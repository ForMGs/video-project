package com.example.backend.video.aiJob;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoAiQueryService {

    private final VideoAiResultRepository repo;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public VideoAiResponse getAi(Long videoId){

        var opt = repo.findByVideoId(videoId);

        if(opt.isEmpty()){
            // AI요청 자체 불가
            return VideoAiResponse.builder()
                    .status("NONE")
                    .chapters(Collections.emptyList())
                    .build();
        }
        var r = opt.get();

        List<ChapterDto> chapters = Collections.emptyList();
        try{
            if(r.getChaptersJson()!=null && !r.getChaptersJson().isBlank()){
                chapters = objectMapper.readValue(
                        r.getChaptersJson(),
                        new TypeReference<List<ChapterDto>>(){}
                );
            }
        } catch (Exception e) {
            chapters = Collections.emptyList();
        }

        return VideoAiResponse.builder()
                .status(r.getStatus().name())
                .summaryShort(r.getSummaryShort())
                .summaryLong(r.getSummaryLong())
                .chapters(chapters)
                .errorMessage(r.getErrorMessage())
                .build();
    }
}