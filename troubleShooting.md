
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
##### 2). Request Headers에 Authorization 있는지 ->
```
 if (status === "NONE") {
          await http.post(`/videos/${videoID}/ai`);
          if (cancelled) return;
          timer = window.setTimeout(poll, 30000);
          return;
        }

```
##### 3). Response Body에 SpringSecurity 관련 메시지 있는지 ? ->
##### 4). OPTIONS 요청이 먼저 갔는지 ? -> 
