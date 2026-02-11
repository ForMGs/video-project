package com.example.backend.video.aiJob;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VideoAiService {

    private final VideoAiResultRepository repo;
    private final RedisTemplate<String,String> redisTemplate;

    private static final String QUEUE = "ai:jobs";

    @Transactional
    public Long requestAi(Long videoId){

        VideoAiResult result = repo.findByVideoId(videoId)
                .orElseGet(() -> repo.save(
                        VideoAiResult.builder()
                                .videoId(videoId)
                                .status(AiJobStatus.PENDING)
                                .attempt(0)
                                .build()
                ));
        if(result.getStatus() == AiJobStatus.RUNNING ||
            result.getStatus() == AiJobStatus.DONE){
            return result.getId();
        }
        result.markPending();
        repo.save(result);

        //큐에 job id push
        redisTemplate.opsForList()
                .leftPush(QUEUE, result.getId().toString());
        return result.getId();
    }
}
