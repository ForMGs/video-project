package com.example.backend.video.aiJob;
//AI 작업 상태 머신..
public enum AiJobStatus {
    PENDING , // 대기중(큐에 있음)
    RUNNING , // 워커가 처리중
    DONE , // 완료
    FAILED // 실패
}
