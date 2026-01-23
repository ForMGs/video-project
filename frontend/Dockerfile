# syntax=docker/dockerfile:1.4

# 1단계: 빌드
FROM node:20 AS build
WORKDIR /app

# 의존성 설치 (package.json / package-lock.json만 먼저 복사)
COPY package*.json ./

# npm 캐시 사용해서 의존성 설치 속도 최적화
RUN --mount=type=cache,target=/root/.npm \
    npm ci          # 배포용이면 npm ci 추천 (lock파일 기준)

# 소스 복사 후 빌드
COPY . .
RUN npm run build   # /app/dist 생성

# 2단계: Nginx로 정적 파일 서빙
FROM nginx:stable-alpine AS prod
WORKDIR /usr/share/nginx/html

# 기존 기본 파일 삭제
RUN rm -rf ./*

# 빌드 결과만 복사
COPY --from=build /app/dist ./

#Nginx 설정으로 교체
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Nginx 기본 설정 그대로 사용 (필요하면 나중에 커스터마이징)
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

