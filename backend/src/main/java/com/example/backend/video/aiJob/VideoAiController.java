package com.example.backend.video.aiJob;

import lombok.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/videos")
public class VideoAiController {

    private final VideoAiQueryService queryService;
    private final VideoAiService commandService;

    //결과조회
    @GetMapping("/{videoId}/ai")
    public VideoAiResponse getAi(@PathVariable Long videoId){
        return queryService.getAi(videoId);
    }

    //AI 생성 요청 : 프론트에서 버튼 누르면 호출 가능.
    @PostMapping("/{videoId}/ai")
    public Map<String, Object> requestAi(@PathVariable Long videoId){
        Long jobId = commandService.requestAi(videoId);
        return Map.of("jobId",jobId, "status","PENDING");
    }

}



