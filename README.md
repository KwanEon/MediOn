# Medical Search

Spring Boot + React 기반 위치 중심 의료기관 탐색 서비스입니다.

## First Milestone

- 국립중앙의료원 FullData를 로컬 DB에 증분 동기화한 뒤 현재 위치 주변 병·의원 조회
- 거리순, 이름순, 운영 종료 임박순 정렬
- 데이터 갱신 시각 표시
- 의료기관 선택 시 해당 위치로 지도 이동
- React에서 현재 위치 권한 사용 후 목록 표시

## Tech Stack

- Java 17
- Spring Boot 3.x
- Spring Data JPA
- MySQL 8.x
- Spring Security
- React + Vite

## Project Structure

```text
backend/   Spring Boot API server
frontend/  React client
```

Backend package layout:

```text
com.example.medicalsearch
├── config      Spring Security, CORS, properties, exception handling
├── controller  REST API controllers
├── dto         Request/response DTOs
├── entity      JPA entities and enums
├── repository  Spring Data JPA repositories and projections
├── service     Service interfaces
└── serviceImpl Service implementations
```

## Backend Run

MySQL에 `medical_search` 데이터베이스를 만든 뒤 실행합니다.

```bash
cd backend
gradle bootRun
```

기본 DB 설정은 환경변수로 바꿀 수 있습니다.

```text
DB_URL=jdbc:mysql://localhost:3306/medical_search?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=root
DB_PASSWORD=password
```

국립중앙의료원 병·의원 FullData는 기본적으로 매일 오전 3시에 동기화됩니다. HPID를 고유키로 `INSERT ... ON DUPLICATE KEY UPDATE`를 수행하고, 전체 동기화와 진료과목 동기화가 모두 성공한 경우에만 이번 실행에서 수신되지 않은 병·의원을 비활성화합니다. 검색 요청은 외부 API를 호출하지 않고 MySQL에 저장된 활성 기관과 진료과목, 운영시간만 조회합니다. 당일 성공 이력이 없으면 애플리케이션 시작 후 백그라운드에서 한 번 동기화합니다.

```powershell
$env:DATA_GO_KR_SERVICE_KEY='공공데이터포털 일반 인증키'
cd backend
.\gradlew.bat bootRun
```

```text
PUBLIC_DATA_ENABLED=true
DATA_GO_KR_SERVICE_KEY=공공데이터포털 일반 인증키
DATA_GO_KR_HOSPITAL_FULL_DATA_URL=https://apis.data.go.kr/B552657/HsptlAsembySearchService/getHsptlMdcncFullDown
DATA_GO_KR_HOSPITAL_DEPARTMENT_LIST_URL=https://apis.data.go.kr/B552657/HsptlAsembySearchService/getHsptlMdcncListInfoInqire
DATA_GO_KR_TIMEOUT=20s
DATA_GO_KR_SYNC_PAGE_SIZE=1000
DATA_GO_KR_SYNC_ON_STARTUP=true
DATA_GO_KR_SYNC_CRON=0 0 3 * * *
```

회원가입 주소를 검색 좌표로 변환하려면 NAVER Cloud Platform Maps 애플리케이션에서 Geocoding API를 활성화하고 서버 환경 변수에 인증키를 지정합니다. 비밀번호는 BCrypt로 저장되며 로그인 상태는 Spring Security HTTP 세션으로 유지됩니다.

```text
NAVER_MAPS_API_KEY_ID=Maps API Key ID
NAVER_MAPS_API_KEY=Maps API Key
NAVER_MAPS_TIMEOUT=10s
```

## Frontend Run

```bash
cd frontend
npm install
npm run dev
```

개발 서버는 기본적으로 `http://localhost:5173`에서 백엔드 `http://localhost:8080`으로 API를 프록시합니다.

## Main API

```http
GET /api/v1/institutions/nearby?lat=37.5665&lng=126.9780&radiusMeters=3000&types=HOSPITAL,PHARMACY&page=0&size=20
```
