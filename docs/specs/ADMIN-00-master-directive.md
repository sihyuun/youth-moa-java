# ADMIN-00 — 청년모아 관리자 페이지 개발지시서 (마스터 플랜)

> **산출**: 2026-07-09. admin prototype 3자산(HANDOFF.md · prototype.tsx · wireframe.png) 정독 + 현재 코드베이스 인벤토리 대조 완료.
> **성격**: 관리자 트랙 전체를 관통하는 마스터 지시서. 각 Phase 착수 시 본 문서를 기준으로 ym-spec 이 상세 spec 을 산출한다.

| 메타 | 값 |
|---|---|
| 상태 | **`spec_confirmed` / 착수 대기** — 2026-07-09 사용자 결정 반영 완료 (§8 결정 기록 참조) |
| 착수 시점 | **사용자 트랙 완료 후 착수** (2026-07-09 사용자 결정). 예외: P0-1 Flyway·P0-2 Security 는 공유 인프라이므로 사용자 트랙 진행 중 먼저 처리 가능 — §9 참조 |
| 트랙 | admin |
| 선행 | P0-1 Flyway, P0-2 Security 개편 (§4) |
| 관련 파생 큐 | `docs/specs/README.md` — F0c-dynamic-fields(강좌·질문·첨부), F4 자격요건 admin 입력 폼 |
| 라이프사이클 | Phase 별 ym-spec → 사용자 컨펌 → ym-impl → ym-qa → ym-verify → 머지 |

---

## 0. 목적 / 범위

청년센터 통합 관리자 웹 어드민. 관리자가 프로그램을 등록·수정·관리하고, 회원과 신청 현황을 관리하며, 방문자·신청 통계를 확인한다. 인증(로그인/회원가입/찾기) 플로우와 관리자 마이페이지 포함. (admin/HANDOFF.md "Overview")

- **In scope**: `/admin/**` 전 화면, RBAC(센터 데이터 격리), 파일 업로드, 통계 수집, 관리자 알림
- **Out of scope**: 사용자 페이지 변경(단, Program 엔티티 확장은 사용자 상세 F4 와 공유), 모바일 네이티브

---

## 1. 디자인 출처 (3자산 정독 결과)

| 자산 | 경로 | 역할 |
|---|---|---|
| **prototype.html** | `docs/00_assets/admin/prototype.html` | **디자인/동작 원본 (source of truth)** — hifi, 픽셀 확정 (HANDOFF "Fidelity" 절) |
| **prototype.tsx** | `docs/00_assets/admin/prototype.tsx` | 구조 스캐폴드 — 토큰(L24~55)·상태머신(AppState L201~251)·데이터 모델(L127~175)·파생 로직(L280~325)·컴포넌트 가이드(L479~510) |
| **HANDOFF.md** | `docs/00_assets/admin/HANDOFF.md` | 스펙 텍스트 — 디자인 토큰 실측치, 최신 기능 7종(폼 검증·삭제 모달·접근성·스켈레톤·에러 상태·반응형·마이크로카피) |
| wireframe.png | `docs/00_assets/admin/wireframe.png` | **구버전 lo-fi** (31838×15167, WF-ID 체계). hifi 와 충돌 시 hifi 우선. hifi 에 없는 세부(신청 이력·담당자 의견, 모바일 뷰, 마이페이지 "프로그램 등록 내역")의 참고 자료 |

### 1-A. wireframe ↔ hifi prototype 차이 (충돌 시 채택 기준)

| 항목 | wireframe (구) | hifi prototype (신) | 채택 |
|---|---|---|---|
| 홈 화면 | 방문자 현황 차트 + 프로그램 현황 + 사용자 현황 단일 화면 | `dashboard`(스탯카드+최근+마감임박) / `stats`(방문자·프로그램·성별연령) 분리 | **hifi** — 2화면 분리 |
| 프로그램 목록 | 카드 그리드 단일 뷰 + 지역/청년센터 필터 모달 | 목록/카드/캘린더 3뷰 토글 + 상태 필터 + 검색 + 일괄선택 + CSV | **hifi** |
| 신청 상세 모달 | 신청자 정보 + 추가 정보(답변) + **신청 이력(상태·일시) + 담당자 의견 입력·저장** | 신청자 입력 답변 표시 위주 | **병합 검토 → Q6** |
| 사용자 권한 | 사용자/관리자/시스템 관리자 radio (사용자 상세) | + 센터 스코프 셀렉터, 권한 뱃지 3종 | hifi + wireframe radio |
| 관리자 마이페이지 | "프로그램 등록 내역" + 개인 정보 수정 | dashboard 가 마이페이지 겸함 + mypage-pw-check/edit | **hifi** (등록 내역은 dashboard "최근 프로그램" 으로 흡수) |
| 반응형 | 모바일 전용 프레임 별도 제작 | `@media` 적응형 (≤1024 / ≤680) | **hifi** — 정식 CSS 브레이크포인트로 정규화 |
| 알림/설정/통계 | 없음 | 알림 벨 드롭다운, 설정 3탭, 통계 화면 | **hifi** |

### 1-B. HANDOFF 필수 반영 항목 매핑 (요약)

| HANDOFF 항목 | 본 지시서 반영 위치 |
|---|---|
| 폼 검증 (규칙·에러 표시·저장 동작) | §5 A3/A5 + Bean Validation 매핑, §6 `FormField` fragment |
| 삭제 확인 모달 (파괴적 액션 1단계) | §6 `ConfirmDialog` — HTMX `hx-confirm` 금지, 커스텀 모달 |
| 접근성 (`:focus-visible`, 네이티브 checkbox, `role="dialog"`) | §6 공통 규칙 |
| 스켈레톤 / 에러·재시도 상태 | §5 A8 (후순위 폴리시) |
| 반응형 (≤1024 / ≤680) | §5 A8 |
| 마이크로카피 존댓말 톤 ("…했어요/됐어요") | §6 공통 규칙 — **주의**: 사용자 페이지 검증 메시지 규칙("~해야 합니다")과 다른 별도 톤. admin 은 HANDOFF 톤 채택 |
| "권한 전환" 버튼은 데모용 → **프로덕션 제거** | §2 RBAC — 로그인 계정 권한으로 자동 결정 |
| status 는 신청기간·정원 파생 → **일괄 상태변경 기능 없음** | §3 — 현 `Program.getStatus()` 파생 방식과 일치, DB 컬럼 추가 금지 |

---

## 2. 아키텍처 결정 (제안)

### 2-1. 동일 Spring Boot 앱 내 `/admin/**` 라우트 (별도 모듈 분리 안 함)

- **근거**: ① 1인 학습 프로젝트 — 배포 파이프라인 단일 유지 ② 엔티티·Repository 완전 공유 (Program/Application/User) ③ Thymeleaf + HTMX 스택 그대로 재사용 ④ RBAC 는 Spring Security role 매처로 충분
- 패키지: 기존 도메인 중심 구조 유지 + `admin/` 패키지 신설 (`AdminProgramController`, `AdminUserController`, `AdminStatsController`, `AdminDashboardController`, `AdminSettingsController`). 서비스 로직은 기존 도메인 서비스 확장 우선, admin 전용 조회는 `admin/` 내 별도 서비스 허용
- 템플릿: `templates/admin/**` + `templates/admin/fragments/**` (사용자 페이지 fragment 와 분리 — 디자인 토큰이 다크 헤더 등 상이)
- CSS: `static/css/admin.css` 신설. `:root` 토큰은 prototype.tsx L24~55 실측치로 정의 (`--admin-header-bg: #111827` 등). main.css 오염 금지

### 2-2. screen → URL 라우팅 매핑 (prototype.tsx L180~188 Screen 타입 기준)

| screen | URL | 비고 |
|---|---|---|
| `login` | `/admin/login` | 별도 관리자 로그인 페이지 (밝은 헤더 + 420px 카드) — Q8 |
| `signup` / `find-id` / `find-pw` | ~~`/admin/signup`~~ 미구현 (Q8 확정 — SYSTEM_ADMIN 이 권한 부여), 찾기는 사용자 페이지 `/find-id`·`/find-password` 재사용 | Q8 |
| `dashboard` | `/admin` | 스탯 카드 + 최근 프로그램 + 마감 임박 |
| `stats` | `/admin/stats` | 방문자(월/연) + 프로그램 통계 + 성별/연령 도넛 |
| `programs` | `/admin/programs` | 3뷰(`?view=list\|card\|calendar`) + 필터/검색/페이지 쿼리스트링 |
| `program-detail` | `/admin/programs/{id}` | 신청 현황 테이블 포함 |
| `program-form` | `/admin/programs/new`, `/admin/programs/{id}/edit` | formMode create/edit |
| `course-detail` | `/admin/programs/{id}/courses/{courseId}` | 강좌별 신청 현황 |
| `users` | `/admin/users` | |
| `user-detail` | `/admin/users/{id}` | |
| `user-register` | `/admin/users/new` | |
| `settings` | `/admin/settings?tab=account\|notification\|system` | |
| `mypage-pw-check` / `mypage-edit` | `/admin/mypage/verify`, `/admin/mypage/edit` | 사용자 페이지 `/mypage/profile/verify` 패턴 재사용 |
| `calendar` | (독립 화면 제거 — programs 내 뷰로 통합, HANDOFF 명시) | |

### 2-3. React 상태 → 서버사이드 매핑 전략 (AppState L201~251)

| React 상태 | Thymeleaf + HTMX 구현 |
|---|---|
| `screen` | URL 라우팅 (위 표) |
| `programFilter/Search/View/Page/Selected` | 쿼리스트링 + HTMX 부분 갱신(`HX-Request` 분기 — F0f 기구현 패턴 재사용). 체크박스 선택은 클라이언트 JS(`admin-table.js`) |
| `confirmDialog` | 클라이언트 모달 (`data-confirm-*` 속성 + 단일 JS 모듈) → 확인 시 form submit/HTMX 요청 |
| `toast` | redirect 후 flash attribute → 렌더 시 토스트 1회 노출 + HTMX 응답 `HX-Trigger` 이벤트 |
| 각 폼 `FieldState` | 서버: Bean Validation + `th:errors` / 클라: submit 전 인라인 검증 JS (기존 signup 패턴 재사용) |
| `chartMode/calendarView/...` | 쿼리스트링 또는 경량 클라이언트 상태 |
| `notifItems` | `HeaderNotificationAdvice` 패턴 재사용 (admin 전용 Advice) |
| `adminRole/adminCenter/centerScope` | Security principal + 세션(centerScope 셀렉터 값) |

### 2-4. RBAC / 센터 데이터 격리

- **현황 (준비된 것)**: `UserRole` 에 `ADMIN`, `CENTER_ADMIN`, `SYSTEM_ADMIN` 정의됨. `User.center`(FK)·`User.centerScope`·`User.assignRole()` 존재. `Application.approve/reject(User admin)` + `processedBy` FK 존재
- **현황 (없는 것)**: SecurityConfig 에 `/admin/**` 패턴·`hasRole` 전무(인증 여부만 구분), 관리자 시드 계정 없음, **CSRF disable 상태**
- **지시**:
  1. `SYSTEM_ADMIN` = 전체 + 센터 스코프 셀렉터, `CENTER_ADMIN` = 소속 센터 고정 (prototype.tsx L280~283 `effectiveCenter` 로직을 서비스 레이어로 이식)
  2. `ADMIN` enum 값은 용도 불명 — **Q9 에서 정리** (hifi 권한 뱃지는 시스템 관리자/관리자/사용자 3종 → `ADMIN` 을 "관리자" 뱃지로 쓸지, 제거할지)
  3. 유효 센터 필터링은 모든 admin 조회 쿼리에 공통 적용 (프로그램/신청/통계/대시보드). Repository 파라미터로 강제 — 컨트롤러에서 누락할 수 없는 구조로
  4. 프로그램 등록 시 `CENTER_ADMIN` 은 소속 센터 자동 고정 (셀렉트 disabled)
  5. 헤더 "권한 전환" 버튼은 **구현하지 않음** (HANDOFF: 데모용)

---

## 3. 데이터 모델 gap 표 (CLAUDE.md 필수 산출물)

> 기준: prototype.tsx 데이터 모델 (Program L127~139, AppUser L141~152, Applicant L154~164, NotifItem L166~175) + hifi 화면 요구.

### 3-1. Program

| prototype 필드 | 현재 엔티티 | 조치 |
|---|---|---|
| `name` | `title` ✅ | 유지 |
| `center` (센터명 — **데이터 격리 기준**) | ❌ `organization` 문자열만 존재 | **`@ManyToOne Center center` FK 추가** — RBAC 전제 조건. `organization` 은 마이그레이션 후 제거 검토 (Q2) |
| `region` | `region` 문자열 ✅ | 유지 (Center FK 도입 후 center.region 파생 검토) |
| `period` (진행기간) | `startDate`/`endDate` ✅ | 유지 |
| `applyPeriod` (신청기간) | ❌ 없음 | **`applyStartDate`/`applyEndDate` 컬럼 추가** — status 파생·D-day 계산도 신청기간 기준으로 정정 필요 |
| `capacity` | `capacity` ✅ | 유지 |
| `applied` | ❌ (컬럼 불필요) | `ApplicationRepository.countByProgramAndStatus(APPROVED)` 파생 |
| `views` (조회수) | ❌ 없음 | **`viewCount int` 컬럼 추가** (Notice 패턴 동일) + 상세 진입 시 증가 |
| `status` | `getStatus()` 런타임 파생 ✅ | 유지 — DB 컬럼 추가 금지 (HANDOFF: 일괄 상태변경 기능 의도적 제외) |
| `hasCourses` / 강좌 | ❌ 없음 | **`Course` 엔티티 신설** (program FK, 이름, 기간, 정원) — F0c-dynamic-fields 파생 큐와 통합 (Q3) |
| 장소 (venue) | ❌ 없음 | **`venue` 컬럼 추가** (프로그램 상세 정보 카드 항목) |
| 문의처 (contact) | ❌ 없음 | **`contact` 컬럼 추가** |
| 첨부파일 | ❌ 없음 | **`ProgramAttachment` 엔티티 신설** — `NoticeAttachment` 패턴 복제 + 실 파일 저장 (P0-3) |
| 신청 질문 | ❌ 없음 | **`ApplyQuestion` 엔티티 신설** (program FK, type 주관식/객관식, 보기 목록, `sortOrder` — 드래그앤드롭 순서) |
| 약관 | ❌ 없음 | **`ProgramTerms`** (제공 여부 + 약관명 + 내용) — Program 컬럼 3개로 단순화 가능 (Q3) |
| 썸네일 | `imageUrl` ✅ | 실 업로드 연동 (P0-3) |

### 3-2. AppUser (사용자 관리 화면)

| prototype 필드 | 현재 엔티티 | 조치 |
|---|---|---|
| name/email/gender/phone/address | ✅ 전부 존재 | 유지 |
| `role` | `UserRole` ✅ (4종) | Q9 — `ADMIN` 값 정리 |
| `joinDate` | `createdAt` ✅ | 파생 |
| `lastAccess` | ❌ 없음 | **`lastAccessAt` 컬럼 추가** + 로그인 성공 핸들러에서 갱신 |
| `status` (active/inactive) | ❌ 없음 | **`isActive boolean` 컬럼 추가** (비활성 = 로그인 차단, `UserDetailsService` 반영) |

### 3-3. Applicant (신청 현황 테이블 / 신청 상세 모달)

| prototype 필드 | 현재 엔티티 | 조치 |
|---|---|---|
| email/gender/phone | `Application.user` 경유 ✅ | `@EntityGraph` fetch join |
| `appliedAt` | ✅ | 유지 |
| `visits` (참여횟수) | ❌ | `countByUserAndStatus(APPROVED)` 파생 |
| `status` 승인/대기/반려/취소 | `ApplicationStatus` 4종 ✅ | 라벨 매핑만 추가 (PENDING=대기, APPROVED=승인, REJECTED=반려, CANCELLED=취소) |
| `answers` (질문·답변) | ❌ | **`ApplyAnswer` 엔티티 신설** (application FK + question FK + 답변) — A3 의 ApplyQuestion 과 세트 |
| 대기자 (waitlist) | ❌ (`WAITING` 은 PR #2 에서 제거된 이력) | **Q7** — 도입 시 status 재추가 + "정원 초과 시 자동 승인" 토글은 Program 컬럼 |
| 담당자 의견 (wireframe) | `rejectReason` 만 존재 | **Q6** — 채택 시 `adminComment` 컬럼 또는 처리 이력 엔티티 |

### 3-4. 신규 엔티티 (화면 요구 기반)

| 엔티티 | 용도 | 근거 화면 |
|---|---|---|
| `DailyVisit` (date, count) 또는 `PageVisit` 원시 로그 | 방문자 통계 (월별 막대 / 연도별 꺾은선) | `stats` — **Q5 수집 방식 결정** |
| `Course`, `ApplyQuestion`, `ApplyAnswer`, `ProgramAttachment` | §3-1, §3-3 | `program-form`, `course-detail`, 신청 상세 모달 |
| 관리자 알림 | 기존 `Notification` 재사용 (user=관리자) + `NotificationType` 에 `NEW_APPLICATION`, `NEW_USER` 추가 | 헤더 알림 벨 (승인/마감/가입 3유형) |
| 알림 설정 5종 토글 | `User.notifyKakao/Sms/Email` 과 별개 — **관리자 알림 수신 설정** 컬럼 또는 `AdminNotificationPref` | `settings` 알림 탭 |

---

## 4. P0 — 선행 인프라 티켓 (admin 착수 전 필수)

### P0-1 `chore/flyway-migration` — Flyway 도입 ★최우선

- **왜 지금**: 현재 `ddl-auto: create-drop` + DataInitializer 시드 → **관리자가 편집한 값이 재기동 시 전부 소멸**. CLAUDE.md "관리자 CRUD 실효성 체크" 원칙상 admin CRUD 는 Flyway 없이는 무의미
- 지시: `V1__baseline.sql` (현 스키마 스냅샷) → `ddl-auto: validate` 전환 → 시드는 `data.sql`/Flyway seed 마이그레이션 또는 DataInitializer 멱등 삽입(존재 시 skip — 이미 count>0 skip 구조라 호환)으로 재정리. e2e/test 프로파일은 기존 create-drop 유지
- CLAUDE.md "DB / 마이그레이션" 절 갱신 + `/db-migrate` SKILL 신설 (기존 계획)

### P0-2 `chore/admin-security` — Security 개편

- `/admin/login`·`/admin/find-*` permitAll, **`/admin/**` → `hasAnyRole('CENTER_ADMIN','SYSTEM_ADMIN')`** 매처 추가 (기존 anyRequest 앞)
- **CSRF 활성화** — 현재 `csrf.disable()` 상태. admin(상태 변경 다수) 도입 전 전면 활성화. HTMX 는 `hx-headers` 로 토큰 전송(메타 태그 + 공통 JS 1곳). 기존 사용자 페이지 폼 전수 확인 (th:action 폼은 hidden 자동 삽입)
- 관리자 시드: `SYSTEM_ADMIN` 1 + `CENTER_ADMIN` 2 (센터 상이 — 격리 테스트용). 비밀번호는 환경변수 주입, 하드코딩 금지
- 로그인 실패 잠금·비밀번호 정책은 기존 사용자 규칙 재사용

### P0-3 `chore/file-upload` — 파일 업로드 인프라

- 현황: `spring.servlet.multipart` 설정 없음, NoticeAttachment 는 메타데이터 stub
- 지시: multipart 설정(max 10MB 등) + `FileStorageService` 인터페이스 → **Q4 결정** (로컬 디스크 vs Supabase Storage). Fly.io 디스크는 ephemeral 이므로 프로덕션은 Supabase Storage(S3 호환) 권장 — 기존 Supabase 프로젝트 재사용
- 적용 대상: 프로그램 썸네일, 첨부파일, (기존) NoticeAttachment 실파일 승격

### P0-4 `chore/actuator` — Actuator 도입 (소형)

- `spring-boot-starter-actuator` + `/actuator/health` 만 공개. Fly.io healthcheck 연결 + admin `settings` 시스템 정보 탭(버전·서버 상태) 데이터 소스

---

## 5. Phase 별 구현 계획

> 각 Phase = PR 1~2개 단위. 착수 시 ym-spec 이 본 절 + prototype.html 해당 화면을 정독해 상세 spec 산출.

### A1 `feature/A1-admin-shell` — 레이아웃 + 인증 + 대시보드

- 다크 헤더(56px sticky, `#111827`): 로고+ADMIN 뱃지 / 센터 스코프 셀렉터 / GNB 절대중앙(통계·프로그램 관리·사용자 관리) / 우측 검색·알림·유저 (검색·알림 실동작은 A7, 여기선 자리만)
- 인증 레이아웃(밝은 헤더 + 420px 카드): `/admin/login` (signup 미구현 — Q8 확정)
- **사용자 ↔ 관리자 상호 진입 동선** (§8-1): 사용자 헤더 드롭다운 role 조건부 "관리자 페이지" 링크 + admin 유저 드롭다운 "사용자 페이지" 링크
- 대시보드: 스탯 카드(≤1024 2열) + 최근 프로그램 + 마감 임박(D-day 색상 — tsx L92~96 `ddayColor`)
- `admin.css` 토큰 정의, `templates/admin/layout.html` + fragments
- 검증: `AdminShellRenderTest` (비인가 접근 403/redirect, GNB 렌더, 센터 스코프별 노출)

### A2 `feature/A2-admin-programs-list` — 프로그램 관리 (목록형)

- 컬럼: 체크 / No. / 프로그램명(썸네일+이름) / 진행기간 / 신청기간 / 신청현황(applied/capacity) / 조회수 / 상태뱃지 / 관리(복제·삭제)
- 상태 필터 세그먼트 + 검색(이름·센터) + 페이지네이션 8건 + 빈 상태
- 체크박스 일괄 선택 → 다크 액션바("N건 선택됨" + CSV + 삭제 + 해제)
- CSV 내보내기: 서버 사이드 생성 (`text/csv` + BOM — tsx L328~336 클라 구현을 서버로 이식, 선택 건/전체 모두)
- 삭제 = 소프트 정책 여부 **Q10**. 확인 모달 필수
- 의존: §3-1 컬럼 추가분 (applyPeriod·viewCount·center FK)
- 검증: 센터 격리 (CENTER_ADMIN 은 타 센터 프로그램 불가시), CSV 인코딩(한글)

### A3 `feature/A3-admin-program-form` — 등록/수정 폼 (탭 3)

- 탭: 프로그램 정보(기본정보+썸네일 업로드+에디터) / 신청 정보(질문 추가·삭제·**드래그앤드롭 순서**) / 약관 정보(제공 여부 토글)
- 강좌 제공 "예" 시 강좌 입력 영역 (Course)
- 폼 검증: HANDOFF 규칙 → Bean Validation 매핑 (이메일·핸드폰·양수·기간 순서 "신청 마감 < 진행 시작, 시작 ≤ 종료") + 클라 인라인(빨간 보더 `#EF4444` + 11px 에러 텍스트)
- 에디터: 경량 채택 (contenteditable 또는 Toast UI Editor webjar — ym-spec 에서 비교표 제시)
- formMode edit 프리필 + 제목 "프로그램 수정"
- 의존: P0-3, Course/ApplyQuestion/ProgramTerms 엔티티. **F0c-dynamic-fields (사용자 신청 폼 동적 질문) 와 엔티티 공유 — admin 이 선행** (specs/README 파생 큐 명시)

### A4 `feature/A4-admin-program-detail` — 상세 + 신청 현황

- 정보 카드(썸네일·센터·진행/신청기간·모집인원·장소·문의처·첨부) + 설명 카드 + 수정 버튼 + ⋯ 메뉴(복제·삭제)
- 신청 현황 테이블: 이메일/성별/핸드폰/접수일시/참여횟수/상태 **드롭다운 직접 변경**(+확인 모달) — `Application.approve/reject` 도메인 메서드 활용, `processedBy` 기록
- 신청 상세 모달: 답변 표시 (+Q6 채택 시 신청 이력·담당자 의견)
- 대기자 관리 배너 + 자동 승인 토글 — Q7 채택 시
- 상태 변경 → 기존 `ApplicationEvent` 3종 발행 유지 (사용자 알림 연동 그대로)
- course-detail 화면 (강좌별 신청 현황)

### A5 `feature/A5-admin-users` — 사용자 관리

- 목록: 권한 필터 + 검색(이름·이메일) + 페이지네이션 10건 + 일괄 선택/CSV/삭제
- 상세: 정보 수정(검증) + **주소 검색(다음 우편번호 — 사용자 signup 기구현 모듈 재사용)** + 프로그램 신청 현황(신청 상세 모달 / 취소는 뱃지 표기, 드롭다운 아님)
- 등록: 신규 사용자 등록 폼 (권한 radio 사용자/관리자/시스템 관리자 — wireframe 기준, Q9 반영)
- 의존: §3-2 (lastAccessAt·isActive)

### A6 `feature/A6-admin-stats` — 통계 + 방문자 수집

- 방문자 현황: 월별 막대(hover 툴팁) / 연도별 꺾은선(area gradient, 전년 대비 %) — 탭 전환
- 프로그램별 통계 테이블(신청률 progress bar — tsx L85~89 색상 규칙) + 성별/연령 도넛(인디고 모노톤 팔레트)
- 차트 구현: **외부 CDN 금지 전제** → 서버 렌더 SVG(prototype 방식) 우선 검토, 복잡도 초과 시 Chart.js webjar — ym-spec 비교표
- 수집: Q5 결정 (HandlerInterceptor + 일별 집계 테이블 권장 — 개인정보 미저장)

### A7 `feature/A7-admin-header-live` — 알림 · 글로벌 검색 · 설정 · 마이페이지

- 알림 벨: 미읽음 뱃지 + 드롭다운(유형별 아이콘·개별 삭제·모두 읽음·클릭 이동) — `Notification` 재사용 + 타입 2종 추가, 신규 신청/가입 이벤트 리스너
- 글로벌 검색: 프로그램/사용자 실시간 드롭다운 (HTMX `hx-trigger="keyup changed delay:300ms"`)
- 설정 3탭: 계정(비밀번호 변경 검증 — "이전과 동일 금지" 포함) / 알림 5종 토글 / 시스템 정보(actuator 연동)
- 마이페이지: 비밀번호 확인 → 내 정보 수정 (사용자 `/mypage` 패턴 이식)

### A8 `feature/A8-admin-views-polish` — 카드·캘린더 뷰 + 반응형 + 상태 폴리시

- 카드 뷰: `repeat(auto-fill,minmax(220px,1fr))` + 신청률 bar
- 캘린더 뷰: 월간 그리드 + 이벤트 클릭 → 우측 슬라이드 패널(300px, `1fr`↔`1fr 300px`)
- 반응형 정규화(≤1024 / ≤680 — HANDOFF 속성 선택자 방식을 정식 클래스로), 로딩 스켈레톤, 에러/재시도 상태
- 아이콘: **이모지 금지 규칙 적용** — prototype 인라인 SVG(Feather 스타일)를 `templates/admin/fragments/icons.html` 로 이식 (기존 `fragments/icons.html` 패턴)

---

## 6. 공통 컴포넌트 (Thymeleaf fragment 목록 — tsx L479~510 가이드 이식)

| fragment | 내용 | 사용 화면 |
|---|---|---|
| `adminBadge(status)` | 상태/신청자/권한 뱃지 — statusConfig(L58~66)·applicantBadge(L69~74)·roleConfig(L77~82) 색상표 | 전 화면 |
| `dataTable` 패턴 | 카드 컨테이너 + `overflow-x:auto` + Grid 행, 헤더 `#F0EFF3`, **No.** 컬럼, 네이티브 checkbox `accent-color:#3F30E9` | A2/A4/A5 |
| `bulkActionBar` | 다크 `#1E293B` "N건 선택됨" + CSV/삭제/해제 | A2/A5 |
| `pagination(page,total)` | 활성 `#3F30E9` | A2/A5 |
| `progressBar(pct)` | ≥80% 빨강 / ≥50% 주황 / 기본 primary | A2/A6 |
| `confirmDialog` | 파괴적 액션 1단계, "…삭제할까요? 삭제 후 복구할 수 없어요." 톤, `role="dialog"` | 전 화면 |
| `toast(type)` | 4타입, 하단 중앙, 2.8s, 존댓말 톤 | 전 화면 |
| `emptyState` / `errorState` | "검색 결과가 없어요" vs "불러오지 못했어요"+재시도 — **구분 필수** | A2/A5/A6 |
| `formField` | 라벨+인풋+에러 (빨간 보더+11px 텍스트) | A3/A5/A7 |
| `viewToggle`, `dropdown`, `radioGroup`, `toggleSwitch`, `skeleton` | HANDOFF 컴포넌트 상세 절 | 해당 화면 |

**공통 규칙**: `:focus-visible` 인디고 포커스 링 전역 / 모든 드롭다운 바깥 클릭 닫힘(단일 JS 유틸) / 체크박스는 네이티브 input / 마이크로카피 존댓말 톤 / 숫자·날짜 폰트 Inter.

---

## 7. 검증 규칙 (CLAUDE.md 준수)

1. **정적**: 매 Phase `compileJava` + 신설 `Admin*RenderTest` (Thymeleaf 실 렌더 — F0h-c2 사고 재발 방지 규칙). RBAC 는 `@WithMockUser(roles=...)` 슬라이스 테스트
2. **동적**: bootRun(e2e 프로파일, 8090) + curl — 페이지 200, `/admin` 비인가 302, 정적 리소스 200, Thymeleaf 표현식 잔존 0건
3. **prototype 시각 대조**: 화면 신설 PR 마다 Claude Preview snapshot ↔ `admin/prototype.html` 해당 화면 — 컬럼 수·필터바·카드 정보·CTA 위치 4항목 표 대조 (사용자 페이지와 동일 규칙)
4. **센터 격리 회귀**: CENTER_ADMIN 시드 계정으로 타 센터 데이터 접근 시도 → 목록 미노출 + 직접 URL 403 을 E2E 시나리오에 상시 포함
5. 보고는 정적/동적/시각 분리 표기

---

## 8. 결정 기록 (2026-07-09 사용자 확정)

| # | 질문 | 결정 |
|---|---|---|
| **Q1** | 청년센터 자체 CRUD 화면 | ✅ **추가 (Phase A9)**. prototype 확인 결과: repo 사본 `admin/prototype.html` 의 screen 은 17종(L3765~3785 라우팅 분기)이며 **센터 관리 화면 없음** — 센터는 헤더 스코프 셀렉터·`CENTERS` 상수(tsx L256~259)로만 등장. 사용자가 claude design 으로 제작한 센터 관리 디자인이 별도로 존재할 수 있음 → **A9 착수 전 최신 디자인 번들을 `docs/00_assets/admin/` 에 갱신**하고, 없으면 ym-spec 이 admin 공통 컴포넌트(§6) + `Center` 엔티티 필드(이름·지역·주소·전화·좌표·운영시간·설명·이미지·isActive·isFeatured) 기준으로 화면을 설계 |
| **Q2** | `Program.organization` → `Center` FK 전환 | ✅ 제안대로 — FK 추가 + 시드 정비, `organization` 한시 병행 후 제거 |
| **Q3** | 강좌·약관 스코프 | ✅ 제안대로 — Course·ApplyQuestion 엔티티, 약관은 Program 컬럼 3개. admin(A3) 선행 → F0c 후행 |
| **Q4** | 파일 저장소 | ✅ 제안대로 — Supabase Storage (로컬 프로파일은 파일시스템) |
| **Q5** | 방문자 통계 수집 | ✅ 제안대로 — HandlerInterceptor + `DailyVisit` 일별 집계 |
| **Q6** | 신청 상세 모달 이력·담당자 의견 병합 | ✅ 제안대로 — 병합 (`adminComment` + 상태 변경 이력) |
| **Q7** | 대기자(waitlist) | ✅ 제안대로 — 1차 제외, A8 이후 별도 티켓 |
| **Q8** | 관리자 signup / 찾기 | ✅ 제안대로 — signup 미구현, find-id/pw 사용자 페이지 재사용. **추가 결정**: 사용자 페이지 ↔ 관리자 페이지 상호 진입 동선 도입 (§8-1) |
| **Q9** | `UserRole.ADMIN` 정리 | ✅ 제안대로 — "관리자" 뱃지 = `CENTER_ADMIN`, `ADMIN` 은 deprecated (마이그레이션 정리) |
| **Q10** | 삭제 정책 | ✅ 제안대로 — 프로그램·사용자 모두 소프트 삭제, 물리 삭제 미제공 |

### 8-1. Q8 추가 결정 — 사용자 페이지 ↔ 관리자 페이지 진입 동선 (A1 범위 편입, 상세는 A1 spec 에서 검토)

- **사용자 → 관리자**: 사용자 페이지 헤더 아바타 드롭다운에 `sec:authorize="hasAnyRole('CENTER_ADMIN','SYSTEM_ADMIN')"` 조건부 "관리자 페이지" 링크 노출 → `/admin` 이동. 일반 USER 에게는 완전 미노출
- **관리자 → 사용자**: admin 헤더 유저 드롭다운(내 정보/설정/로그아웃)에 "사용자 페이지" 링크 추가 — hifi prototype 에 없는 항목이므로 A1 spec 에서 메뉴 위치·라벨 확정
- **세션 공유**: 동일 앱·동일 Security 세션이므로 별도 재로그인 없음. `/admin/login` 은 비로그인 관리자의 직접 진입용으로 유지
- **검증 항목**: USER 계정으로 드롭다운에 링크 미노출 + `/admin` 직접 접근 403, 관리자 계정 왕복 이동 시 세션 유지

---

## 9. 작업 큐 메타

| ID | 브랜치 | 내용 | 선행 | 추정 |
|---|---|---|---|---|
| P0-1 | `chore/flyway-migration` | Flyway + ddl-auto validate + 시드 재정리 | — | M |
| P0-2 | `chore/admin-security` | /admin/** RBAC + CSRF 활성화 + 관리자 시드 | P0-1 | M |
| P0-3 | `chore/file-upload` | multipart + FileStorageService (Q4) | — | M |
| P0-4 | `chore/actuator` | actuator health | — | S |
| A1 | `feature/A1-admin-shell` | 레이아웃·인증·대시보드 | P0-2 | L |
| A2 | `feature/A2-admin-programs-list` | 프로그램 목록형 + CSV + 일괄 | A1, §3-1 컬럼 | L |
| A3 | `feature/A3-admin-program-form` | 등록/수정 3탭 + 검증 + 동적 질문 | A2, P0-3, Q3 | XL |
| A4 | `feature/A4-admin-program-detail` | 상세 + 신청 현황 + 상태 변경 | A3, Q6/Q7 | L |
| A5 | `feature/A5-admin-users` | 사용자 목록/상세/등록 | A1, §3-2 | L |
| A6 | `feature/A6-admin-stats` | 통계 + 방문자 수집 | A1, Q5 | L |
| A7 | `feature/A7-admin-header-live` | 알림·검색·설정·마이페이지 | A1 | M |
| A8 | `feature/A8-admin-views-polish` | 카드/캘린더 뷰·반응형·스켈레톤 | A2 | M |
| A9 | `feature/A9-admin-centers` | 센터 CRUD (Q1 확정 — 착수 전 디자인 번들 갱신 확인) | A1 | M |

권장 순서: **P0-1 → P0-2 → A1 → A2 → A3 → A4 → A5 → (A6 ∥ A7) → A8 → A9**. P0-3/P0-4 는 A3/A1 착수 전까지 병렬 처리 가능.

### 착수 시점 (2026-07-09 결정)

- **A1~A9 (화면 트랙)**: 사용자 트랙(현재 진행 중인 F0h 잔여 fix + `spec_confirmed` 큐 F0c/F2c/F4 등) **완료 후 착수**. 화면 컨텍스트 전환 비용·듀얼 트랙 브랜치 충돌 방지
- **P0-1 Flyway / P0-2 Security(CSRF)**: 사용자 트랙과 무관하게 **조기 착수 권장** — 두 티켓은 미룰수록 비용이 커지는 성격:
  - Flyway: 도입 전에 쌓인 엔티티 변경만큼 baseline 이후 마이그레이션 부채 증가. 사용자 트랙의 F4(자격요건)·F0c 도 엔티티를 건드림
  - CSRF: 활성화 시점의 폼·HTMX 요청 수만큼 retrofit 범위 증가 — 사용자 화면이 늘어나기 전이 저렴
- **P0-3(파일 업로드)/P0-4(actuator)**: admin 화면 트랙 직전 처리로 충분

---

## 부록 — 인프라·CI/CD 개선 권고 (admin 트랙과 직결되는 항목 요약)

상세 근거는 세션 대화 참조. admin 트랙 관점에서의 우선순위만 기록.

1. **deploy.yml 게이트 버그 (확인 필요)**: `jobs.deploy.if: ${{ secrets.FLY_API_TOKEN != '' }}` — GitHub Actions 는 job-level `if` 에서 `secrets` 컨텍스트를 지원하지 않아 조건이 항상 false 로 평가될 가능성이 높음 (Actions 실행 이력에서 deploy job skip 여부 확인). 수정안: repository **variable** (`vars.FLY_DEPLOY_ENABLED`) 게이트 또는 step-level 체크로 전환 + `workflow_run`(CI 성공 후) 트리거로 CI→deploy 순서 보장
2. **CI 에 Testcontainers job 추가**: ubuntu-latest 러너는 Docker 내장 — 회사 PC 에서 못 돌리는 `YouthMoaApplicationTests` 등 통합 테스트를 CI 에서 실행 가능 (로컬 제약 우회)
3. **main 브랜치 보호**: CI + lint 를 required status check 로 (admin 트랙은 보안·마이그레이션 변경이 많아 사고 비용 상승)
4. **시크릿 스캐닝**: gitleaks action + GitHub push protection (2026-06-26 DB 비밀번호 노출 사고 재발 방지)
5. **JaCoCo 커버리지 게이트**: 리포트는 이미 생성 중 — `jacocoTestCoverageVerification` 최소선(예: 라인 60%) + PR 코멘트 액션
6. **e2e-playwright.yml 실 트리거 검증** (기존 보류 항목) — admin 화면 E2E 가 추가되면 필수화
