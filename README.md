# Medical Search

Spring Boot + React 기반 위치 중심 의료기관 탐색 서비스입니다.

## 주요 기능

- 국립중앙의료원 FullData 기반 병원·약국·응급실 동기화
- 현재 위치, 회원 주소 또는 검색한 주소 주변 의료기관 조회
- 진료 중 여부, 진료과목, 운영 일정, 거리와 결과 개수 필터
- 응급실 실시간 가용 병상 조회
- 회원가입·로그인, 주소 변경과 의료기관 즐겨찾기
- 목록과 지도를 연동한 의료기관 상세 정보 표시

## Tech Stack

- Java 21
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
./gradlew bootRun
```

Windows에서는 `gradlew.bat bootRun`을 실행합니다.

기본 DB 설정은 환경변수로 바꿀 수 있습니다.

```text
DB_URL=jdbc:mysql://localhost:3306/medical_search?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=root
DB_PASSWORD=password
```

국립중앙의료원 병원·약국 FullData는 기본적으로 매일 오전 3시에 동기화됩니다. HPID를 고유키로 `INSERT ... ON DUPLICATE KEY UPDATE`를 수행하며, 병원 전체 데이터 수신이 완료되면 미수신 기관을 비활성화하고 체크포인트를 저장합니다. 진료과목 관계는 모든 진료과목 수신이 완료된 경우에만 오래된 관계를 삭제합니다. API 요청 한도에 도달하면 기존 데이터를 유지하고 다음 일일 동기화에서 재시도합니다. 같은 날 이미 시도한 이력이 있으면 서버 재시작만으로 전체 조회를 반복하지 않습니다.

일반 의료기관 검색은 외부 API를 직접 호출하지 않고 MySQL에 저장된 활성 기관, 진료과목과 운영시간을 조회합니다. 응급실 검색 결과의 가용 병상만 국립중앙의료원 실시간 API로 보완합니다.

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
DATA_GO_KR_PHARMACY_FULL_DATA_URL=https://apis.data.go.kr/B552657/ErmctInsttInfoInqireService/getParmacyFullDown
DATA_GO_KR_EMERGENCY_BED_AVAILABILITY_URL=https://apis.data.go.kr/B552657/ErmctInfoInqireService/getEmrrmRltmUsefulSckbdInfoInqire
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
