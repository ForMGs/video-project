package com.example.backend.video;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Configuration
@RequiredArgsConstructor
public class MinioConfig {
    private final MinioProps props;

    //서버 내부 통신용 : 버킷 생성/목록/삭제/업로드
    @Bean
    public MinioClient minioInternalClient(){
        return MinioClient.builder()
                .endpoint(props.getEndpointInternal())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
    }

    //presigned URL 전용: 브라우저가 접근하는 호스트/프로토콜로 서명
    @Bean
    public MinioClient minioPublicPresignClient(){
        System.out.println("presigned URL ");

        try{
            MinioClient result = MinioClient.builder()
                    .endpoint(props.getEndpointPublic())
                    .credentials(props.getAccessKey(), props.getSecretKey())
                    .region("us-east-1")
                    .build();
            System.out.println("결과: " + result);
            return result;
        }catch (Exception e){
            System.out.println(e);
            throw e;
        }

    }

    @Bean
    public CommandLineRunner ensureBuckets(MinioClient minioInternalClient){
        return args -> {
            for (String bucketName : props.getBuckets().values()){
                boolean exists = minioInternalClient.bucketExists(
                        BucketExistsArgs.builder().bucket(bucketName).build());
                if(!exists){
                    minioInternalClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                }
            }
        };
    }
}
