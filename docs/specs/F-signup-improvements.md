---
id: F-signup-improvements
status: spec_confirmed
created: 2026-07-13
decided_by: 사용자
---

# F-signup — 회원가입 3건 통합 (인증요청 실 SMS · 주소 API · WelcomeScreen)

## 3 PR 분리

| 순서 | 브랜치 | 범위 |
|---|---|---|
| 1 | `feature/F-signup-02-postcode` | Daum Postcode embed (가장 작음, 즉시 착수 가능) |
| 2 | `feature/F-signup-03-welcome` | `/welcome` 온보딩 페이지 + signup 성공 시 자동 로그인 |
| 3 | `feature/F-signup-01-verify-sms` | CoolSMS 연동 실 SMS 인증 (별도 대형 티켓) |

## 확정 사항 (사용자 결정)

### A-Q1 = B (실 SMS, CoolSMS)
- **벤더**: CoolSMS (Solapi) — 무료 크레딧 300원, SMS ~9원/건, LMS ~30원/건
- **엔티티 신설**: `PhoneVerification(id, phone, code, expiresAt, attempts, verified)`
- **User 필드**: `phoneVerified: Boolean` 추가
- **흐름**:
  1. `POST /api/phone/send-code` (phone) → CoolSMS 발송 + PhoneVerification save (`expiresAt = now + 3min`, attempts=0)
  2. `POST /api/phone/verify-code` (phone, code) → 매칭·만료·시도수 검증 (5회 초과 잠금)
  3. 성공 시 세션 attr `phoneVerifiedAt` 세팅 → signup submit 시 서버가 세션 검증
- **환경변수** (env 주입, 커밋 금지): `COOLSMS_API_KEY`, `COOLSMS_API_SECRET`, `COOLSMS_SENDER`

### A-Q5 = 이식 (WelcomeScreen)
- prototype.tsx L1541~1591 이식
- `/welcome` GET/POST 신설 (SecurityConfig `.authenticated()`)
- 헤더/푸터 미노출 (noHeader 대상)
- 관심 지역 12개 (Region 재사용) + 관심 분야 7종 (하드코딩, `Category` 엔티티는 향후 승격)
- CTA 2종: `[관심 정보 저장하고 시작하기]` · `[나중에 할게요 · 건너뛰기]`

### A-Q6 = a (자동 로그인 채택)
- signup 성공 직후 `SecurityContextHolder` 에 `UsernamePasswordAuthenticationToken` 주입
- `SecurityContextRepository.saveContext()` 로 세션 반영 (Spring Security 6+ 방식)
- Redirect: `/login?registered` → `/welcome`

### A-Q7 = a (컬럼 분리)
- `User.interestRegions: Set<String>` (`@ElementCollection`, `Region.name` 저장)
- `User.interestCategories: Set<String>` (하드코딩 카테고리 7종)
- 기존 `User.interests` 마이그레이션 (`ddl-auto: create-drop` 학습단계라 시드만 갱신)
- 관계형 FK 승격은 admin 트랙에서

### A-Q8 = Region 재사용
- 관심 지역 옵션 = `regionRepository.findByIsFeaturedTrueOrderBySortOrder()` 12건 (F0f 시드)
- 하드코딩 지양, Region 엔티티 진리 소스

## 부가 결정 (impl 중 자연 확정)

- **A-Q2** (mock 코드 표기) — 실 SMS 로 결정되어 무효
- **A-Q3** (SMS 벤더) — CoolSMS 확정
- **A-Q4** (이메일 인증 병행) — 현재 미포함, 후속 검토
- **A-Q9** (관심 미선택 저장) — 저장 통과 (`skip` 과 동일 처리)

## 검증

### 정적
- `PhoneVerificationServiceTest` (발송·검증·만료·시도수 잠금)
- `SignupRenderTest` — Daum Postcode script 태그, 자동로그인 리다이렉트
- `WelcomeRenderTest` — 관심 지역 12개 + 분야 7개 렌더

### 동적
- `POST /api/phone/send-code` 200 + DB row 생성
- `POST /api/phone/verify-code` 200 + 세션 attr
- `POST /signup` (phoneVerified 세션 통과) → 302 `/welcome` (인증 세션 확인)
- `POST /welcome` → 302 `/` + User.interestRegions/Categories DB 반영

### 시각 (사용자)
- Daum Postcode 팝업 → 선택 → 자동 채움
- 인증요청 → 실 SMS 수신 → 6자리 입력 → 인증완료 배지
- WelcomeScreen prototype 대조 (12+7 토글)
