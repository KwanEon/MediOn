# Medical Search

Spring Boot + React 기반 위치 중심 의료기관 탐색 서비스입니다.

## First Milestone

- 국립중앙의료원 공공데이터에서 현재 위치 주변의 실제 병원과 약국 조회
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

실제 의료기관 조회는 국립중앙의료원 공공데이터 API를 기준으로 사용합니다. 병원은 위치 API 결과에 더해 지역별 목록 API의 좌표를 직접 주변 목록으로 구성하고, 주소·전화번호·기관 종류·요일별 운영시간을 함께 적용합니다. 공공데이터 조회 결과가 없거나 실패한 종류에만 OpenStreetMap Overpass API를 대체 좌표/목록으로 사용합니다. PowerShell에서 인증키를 환경 변수로 지정한 뒤, 같은 창에서 백엔드를 시작해야 합니다. 외부 조회 결과는 2분 동안 캐시됩니다.

```powershell
$env:DATA_GO_KR_SERVICE_KEY='공공데이터포털 일반 인증키'
cd backend
.\gradlew.bat bootRun
```

```text
PUBLIC_DATA_ENABLED=true
DATA_GO_KR_SERVICE_KEY=공공데이터포털 일반 인증키
DATA_GO_KR_HOSPITAL_LOCATION_ENABLED=true
DATA_GO_KR_EMERGENCY_MEDICAL_ENABLED=true
DATA_GO_KR_EMERGENCY_AVAILABILITY_CACHE_TTL=1m
DATA_GO_KR_TIMEOUT=10s
DATA_GO_KR_CACHE_TTL=2m
DATA_GO_KR_OPERATING_HOURS_CACHE_TTL=1h
DATA_GO_KR_MAX_OPERATING_HOURS_ROWS=5000
DATA_GO_KR_MAX_RESULTS=1000
```

회원가입 주소를 검색 좌표로 변환하려면 NAVER Cloud Platform Maps 애플리케이션에서 Geocoding API를 활성화하고 서버 환경 변수에 인증키를 지정합니다. 비밀번호는 BCrypt로 저장되며 로그인 상태는 Spring Security HTTP 세션으로 유지됩니다.

```text
NAVER_MAPS_API_KEY_ID=Maps API Key ID
NAVER_MAPS_API_KEY=Maps API Key
NAVER_MAPS_TIMEOUT=10s
```

OpenStreetMap 대체 조회 설정은 다음과 같습니다.

```text
OSM_OVERPASS_ENABLED=true
OSM_OVERPASS_URL=https://overpass.private.coffee/api/interpreter
OSM_OVERPASS_FALLBACK_URL_1=https://overpass-api.de/api/interpreter
OSM_OVERPASS_FALLBACK_URL_2=https://maps.mail.ru/osm/tools/overpass/api/interpreter
OSM_OVERPASS_TIMEOUT=15s
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
