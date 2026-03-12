
## 1). 403 Forbidden ai 요청 시 에러 난 상태

```
</>JOSN
status : 403
error: "Forbidden"
path: "/api/videos/8/ai"
```
### 원인예상
#### - URL 자체는 존재할 가능성이 높음
#### - 서버까지 요청은 갔지만, 서버가 권한 없음 / 허용 안됨으로 막은 상태로 추정.
#### case1).
```JavaScript
//이렇게 보낼시 403 가능성 있음.
fetch("/api/videos/8/ai")

//만약 토큰이 필요 시 아래 구조로 보내야함.
fetch("/api/videos/8/ai",{
  method: "GET",
  headers: {
    Authorization: `Bearer ${token}`
  }
})
```
#### case2).
```Java
//특정 권한 필요하게 막혀 있을 가능성 있음
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/videos/*/ai").authenticated()
    .anyRequest().permitAll()
)
//or
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```
#### case3).
#### POST ,PUT , Delete 요청이면 Spring Security의 CSRF 때문에 403이 자주 남.
```JavaScript
fetch("/api/videos/8/ai", {
  method: "POST",
  headers: {
    "Content-Type": "application/json"
  },
  body: JSON.stringify(data)
})
//이렇게 요청시 CSRF 토큰이 없으면 403 가능성이 큼.

//Java
//꺼져있을 수 있음.
http.csrf(csrf -> csrf.disable());
```
#### 확인요소
##### 1). Request Method : GET 인지 POST 인지 -> POST 
##### 2). Request Headers에 Authorization 있는지 -> 해당 기능은 인증 불필요 
##### 3). 권한 허용되어 있는지 확인 -> 이 부분이 문제로 보임
```Java
//ai 요청 총 2개
@GetMapping("/api/videos/{videoId}/ai")
//get 요청은 ai 서비스 만들기 전 비디오 조회등으로 video에대한 get요청이 열려있음 그래서 ai에 대한 Get은 문제발생 x
@POSTMapping("/api/videos/{videoId}/ai")
```
#### 결과
##### ai 서비스 POST 요청에대한 권한을 열어주는 작업필요.
```
.requestMatchers(HttpMethod.POST, "/api/videos/*/ai").permitAll()
```

