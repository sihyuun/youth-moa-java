# A7 — 관리자 헤더 실시간 알림 벨 (`/admin` 헤더 알림)

| 메타 | 값 |
|---|---|
| 상태 | **`spec_confirmed`** (2026-09-17 사용자 결정: QC → Option B-1 · Qn-Δ rate limiting 이월 · 나머지 원안 A · 확장성 고려 Resolver 분리) |
| 브랜치 (예정) | `feature/A7-admin-header-live` |
| 선행 | A1 admin-shell (#205 · 완결, 헤더 자리 존재) · A2 (#211) · A4 (#213) · A5 (#215) — 링크 이동처 필요 |
| 파생 | A7-2 글로벌 검색·설정·마이페이지 (원안 A7 은 4가지 묶음, 본 티켓은 **알림 벨만**) |
| 추정 규모 | **소~중** — 파일 15~22 · +1200~1700 LOC · V18 · 리스너 2건 + Interceptor(없음, 이벤트 기반) + HTMX polling fragment |
| 성격 | 신규 이벤트 리스너 2건 + admin 전용 알림 endpoint 신설 + `NotificationType` enum 2개 확장. **기존 사용자 알림 flow (PR #143) 무회귀 최우선** |

---

## 1. 배경 · 스코프

ADMIN-00 §5-A7 원안(알림·검색·설정·마이페이지 묶음) 중 **알림 벨만** 분리 착수. A1 에서 헤더 자리(`admin-header-bell` disabled button) 는 이미 존재하므로 이 자리에 **실동작을 채우는 것**이 본 티켓의 목표.

**사용자 알림 flow (기존 PR #143) 는 손대지 않는다** — admin 전용 이벤트 유형 2종을 추가하고 admin 전용 endpoint 를 신설한다. 기존 `NotificationController`(`/notifications/**`) 은 `USER` role 대상이고, 본 티켓의 `AdminNotificationController`(`/admin/notifications/**`) 는 `CENTER_ADMIN`/`SYSTEM_ADMIN` 대상이다. 두 flow 는 동일 `Notification` 엔티티를 **`user` FK 로 격리** 공유한다 (관리자 유저 앞으로 저장).

### 포함
- `templates/admin/fragments/header.html` — 알림 벨 실동작 (disabled 해제 + 배지 + 드롭다운)
- `templates/admin/fragments/admin-notification-panel.html` — 신규 fragment (사용자 `fragments/notification-panel.html` 을 admin 다크 톤·라우팅으로 이식)
- `NotificationType` enum 확장 — `NEW_APPLICATION`, `NEW_USER` 2종 추가 (기존 6종 무변경)
- `AdminNotificationEventListener` 신규 — `ApplicationApprovedEvent` 등 기존 사용자 이벤트 재활용 + `UserRegisteredEvent` 신규 이벤트 발행
- `UserService.signUp()` 성공 후 `UserRegisteredEvent` 발행 (기존 로직 무회귀)
- `AdminNotificationController` — GET dropdown / POST mark-read / mark-all-read / DELETE
- HTMX polling — 헤더 배지 30초 간격 refresh (SSE 이월)
- CENTER_ADMIN 데이터 격리 (`AdminScope.effectiveCenterName()` 기반 수신자 결정)
- V18 마이그레이션 (`NotificationType` enum 값 검증 코멘트만, 실제 컬럼 변경 없음 — enum 은 STRING 저장)

### 제외 (deferred 표기 필수)
- SSE / WebSocket 실시간 push — `deferred: A7-realtime`
- 글로벌 검색 (프로그램·사용자 실시간 드롭다운) — `deferred: A7-search`
- 설정 3탭 (계정·알림·시스템) — `deferred: A7-settings`
- 관리자 마이페이지 (비밀번호 재확인·정보 수정) — `deferred: A7-mypage`
- 이메일 발송 알림 — `deferred: A7-email`
- 알림 필터·검색·페이지네이션 — `deferred: A8`
- 신청 상태 변경(approve/reject) 자체 알림 — Qn-7 권장 X, 담당자 본인 처리라 불필요
- 자동 만료 (30일 후 삭제) — Qn-3 권장 X (수동 삭제로 충분)

---

## 2. 디자인 출처 (3자산 정독)

| 자산 | 위치 |
|---|---|
| prototype.html 알림 벨 (헤더) | L443~494 (bell + dropdown + delete btn) |
| prototype.html mock 데이터 | L2832~2836 (`notifItems` 3건 — approval / deadline / user) |
| prototype.html 알림 설정 탭 (참고, 본 티켓 제외) | L2118~2170 (5종 토글 — deferred: A7-settings 근거) |
| A1 shell 계약 | `docs/design-contracts/admin/shell.md` + `e2e/contracts/admin-shell.ts` |
| 사용자 헤더 알림 참조 (톤 대조) | `templates/fragments/notification-panel.html` L1~113 |
| 기존 사용자 리스너 참조 | `notification/ApplicationNotificationListener.java` |
| A6 spec §14 후속 | `docs/specs/A6-admin-stats.md` (본 티켓이 A6 파생임을 명시) |

**주의**: prototype 헤더 dropdown 은 **300px** 너비 (사용자 페이지 380px 와 다름). 다크 테두리 없음 (`border:1px solid #E3E1E8` 라이트). admin 헤더 자체는 dark(#111827)지만 dropdown 은 라이트 톤이다. 이 차이는 신규 계약(`admin-header-notifications.md`)에 명시한다.

---

## 3. 자산 간 갭 표 (3자산 비교)

| 항목 | wireframe | prototype.html | 채택 |
|---|---|---|---|
| 알림 유형 개수 | (미정의) | 3종 mock — approval / deadline / user | prototype |
| 배지 색 | (미정의) | `#E72D0F` 원형 + 흰색 숫자, 다크 헤더 위 1.5px border `#111827` | prototype |
| 드롭다운 너비 | (미정의) | **300px** (사용자 페이지 380px 와 다름) | prototype |
| dropdown 톤 | (미정의) | 라이트 (다크 헤더 위 흰 카드) | prototype |
| unread dot | (미정의) | 우측 세로 스택: 삭제 X 위, unread dot 아래 (사용자 페이지는 좌측 border) | prototype |
| 아이콘 그룹 | (미정의) | 30×30 rounded square, `iconBg`/`iconColor` 유형별 매핑 | prototype |
| 시간 표기 | (미정의) | 상대 시간 ("방금 전" / "1시간 전" / "3시간 전") | prototype — Qn-Δ 처리 방식 확정 |
| polling 인터벌 | (미정의) | 명시 없음 (React state) | Qn-1 = **30s 권장** |

**wireframe 갭 없음** — admin wireframe 은 알림 벨을 상세히 다루지 않는다.

---

## 4. 데이터 모델 gap 표 (필수)

### 4-1. `NotificationType` enum (기존 · 확장)

| prototype 값 | 현재 enum | 조치 | 사유 |
|---|---|---|---|
| `approval` (승인 대기 신청 N건) | ❌ | **`NEW_APPLICATION` 추가** | 관리자 대상. 사용자 `APPLICATION_APPROVED`(신청자 대상) 와 별개 |
| `deadline` (프로그램 마감 D-1) | `PROGRAM_DEADLINE_NEAR` ✅ | 재사용 | 사용자 알림 기존 값 그대로 admin 에도 발행. `user` FK 만 관리자 |
| `user` (신규 가입) | ❌ | **`NEW_USER` 추가** | admin 전용 |
| — | `APPLICATION_APPROVED` | 무변경 | 사용자 알림 전용 |
| — | `APPLICATION_REJECTED` | 무변경 | 사용자 알림 전용 |
| — | `APPLICATION_CANCELLED` | 무변경 | 사용자 알림 전용 |
| — | `WAITLIST_PROMOTED` | 무변경 | 이월 |
| — | `WELCOME` | 무변경 | 사용자 signUp 환영 (기존) |

**enum 확장 — `getToneColor()` / `getIconName()` switch 케이스 추가 필수**:

| enum 값 | toneColor | iconName | 근거 |
|---|---|---|---|
| `NEW_APPLICATION` | `primary` | `bell` | prototype 승인 대기 = primary 톤 |
| `NEW_USER` | `success` | `check` | prototype 신규 가입 = success 톤 (Qn-Δ 검토) |

### 4-2. `Notification` 엔티티 (기존 · 무변경 권장)

| prototype 필드 | 현재 엔티티 | 조치 |
|---|---|---|
| `user` FK (수신자) | `user` FK ✅ | 관리자별 알림 row INSERT (fan-out 패턴) |
| `type` | `type` ✅ | enum 확장분 사용 |
| `title` | `title` (varchar 255) ✅ | 유지 |
| `message` | `message` (varchar 500) ✅ | 유지 |
| `link` | `link` (varchar 500) ✅ | admin 링크 경로 (`/admin/programs/{id}` 등) |
| `isRead` | `isRead` ✅ | 유지 |
| `createdAt` | `createdAt` ✅ | 유지 |
| — | (신규 필요?) `center` FK / `target_role` | **권장 X — Qn-A 결정 필요** |

**권장 결정 (Qn-A)**: 기존 `user` FK 유지 + INSERT 시 admin 유저별 각각 발송 (fan-out). 이유:
- 기존 flow 재사용 → 회귀 위험 최소
- CENTER_ADMIN 격리는 이벤트 리스너에서 `AdminScope` 로 수신자 목록 필터링 후 각각 INSERT
- 조회는 기존 `findAllByUserOrderByCreatedAtDesc(user)` 그대로 사용 가능
- 단점: SYSTEM_ADMIN·CENTER_ADMIN 다수일 경우 row N배 증가 → 초기 규모(관리자 3~10명)에서 무시 가능

**대안 (Qn-A 이월 가능성)**: `Notification.center` FK + `target_role` 컬럼 추가하고 조회 시 `user_id IN (...) OR (center_id = ? AND target_role = ?)` — 확장성 좋으나 이번 스코프에서 오버스펙.

### 4-3. V18 마이그레이션 (`V18__extend_notification_type_admin.sql`)

Postgres 는 `@Enumerated(STRING)` 을 `VARCHAR` 로 저장하므로 **스키마 변경 없음**. V18 은 다음만 담는다:

```sql
-- V18: NotificationType enum 확장 (admin: NEW_APPLICATION, NEW_USER)
-- 컬럼 스키마 변경 없음. Flyway 이력·리뷰용 코멘트 마이그레이션.
COMMENT ON COLUMN notification.type IS
  'NotificationType enum: APPLICATION_APPROVED/REJECTED/CANCELLED, WAITLIST_PROMOTED, PROGRAM_DEADLINE_NEAR, WELCOME, NEW_APPLICATION, NEW_USER';
```

**대안**: 스키마 변경 전무하면 V 파일 생략 가능. 그러나 CLAUDE.md "Flyway 로 스키마 진리 소스" 원칙상 enum 확장은 문서화 목적으로 V 파일 남기는 것을 권장 (Qn-Δ).

---

## 5. 이벤트 소스 · 수신자 결정 (Qn-A 핵심)

### 5-1. 이벤트 흐름

**A. `NEW_APPLICATION`** — Application 생성 (신규 or 재신청 PENDING 복귀)
- 트리거: `ApplicationService.apply()` 성공 후 (**Qn-B 결정 필요**)
- 방식: 기존 `ApplicationApprovedEvent` 패턴 재사용 — 신규 `ApplicationSubmittedEvent` record 신설
- 수신자 결정: `program.center` → 해당 센터 소속 `CENTER_ADMIN` + 전체 `SYSTEM_ADMIN` (**Qn-C 결정 필요**)

**B. `NEW_USER`** — User 회원가입
- 트리거: `UserService.signUp()` 성공 후
- 방식: 신규 `UserRegisteredEvent` record 신설 + `@EventListener` (AFTER_COMMIT)
- 수신자 결정: **`SYSTEM_ADMIN` 만** 발송 권장 (**Qn-E 결정 필요**) — CENTER_ADMIN 은 신규 가입자를 추적할 이유 없음 (센터 소속 파악 불가 · 회원가입 시점엔 center 미배정)

### 5-2. Q 결정 필요 항목 (핵심 5)

| # | 질문 | 옵션 | 권장 | 근거 |
|---|---|---|---|---|
| **QA** | Application 알림 트리거 | (1) `ApplicationService.apply()` 직접 호출<br>(2) Spring `@EventListener(ApplicationSubmittedEvent)` | **(2) 이벤트** | 기존 `ApplicationApprovedEvent` 3종 flow 와 정합. 커밋 후 실행 보장 (`AFTER_COMMIT`) |
| **QB** | User signUp 알림 트리거 | (1) `UserService.signUp()` 직접 호출<br>(2) `@EventListener(UserRegisteredEvent)` | **(2) 이벤트** | 같음. `signUp` 은 트랜잭션 안에서 여러 INSERT 하므로 커밋 실패 시 알림 skip 필수 |
| **QC** | Application 수신자 결정 | (1) `program.center` → CENTER_ADMIN + 전체 SYSTEM_ADMIN<br>(2) SYSTEM_ADMIN 만<br>(3) 전체 관리자 | **(1)** | CENTER_ADMIN 데이터 격리 원칙 (`AdminScope`) 준수 |
| **QD** | 알림 수신 방식 | (1) HTMX polling 30s<br>(2) SSE (Server-Sent Events)<br>(3) WebSocket | **(1) polling** | SSE 는 stateful 커넥션 유지 필요 + Fly.io 배포 환경 제약. 관리자 3~10명 규모에서 polling 부담 극소. 실시간성은 A7-realtime 이월 |
| **QE** | NEW_USER 대상 | (1) SYSTEM_ADMIN 만<br>(2) CENTER_ADMIN 자기 센터 사용자만 | **(1) SYSTEM_ADMIN 만** | 회원가입 시점엔 사용자에게 `center` 미배정. CENTER_ADMIN 이 판단할 수신 근거 없음 |

### 5-3. Qn 세부 결정 (권장안 포함)

| # | 항목 | 권장 |
|---|---|---|
| **Qn-1** | polling interval | **30s** — CPU 부담 vs UX 균형. 초기 `hx-trigger="every 30s"` 로 배지+개수만 갱신 (드롭다운 전체 아님) |
| **Qn-2** | 드롭다운 최근 N건 | **5건** — 사용자 페이지와 동일. 초과분은 "전체보기 (deferred)" 또는 이번 스코프에서 링크 미제공 |
| **Qn-3** | 자동 만료 | **X — deferred** — 알림 30일 후 삭제 스케줄러는 별도 티켓. 관리자는 수동 삭제 가능 |
| **Qn-4** | reset endpoint | **X — 미제공** — 개별 삭제 + 모두 읽음으로 충분 |
| **Qn-5** | 계약 신설 방식 | **`docs/design-contracts/admin/header-notifications.md` + `e2e/contracts/admin-header-notifications.ts` 신설** (A1 shell 계약과 별도) |
| **Qn-6** | 배지 노출 조건 | **`unread > 0` 시만 표시** — prototype L448 `sc-if unreadCount` 정합. 0 시 완전 숨김 (사용자 페이지 dot 방식과 다름 — admin 은 숫자 배지) |
| **Qn-7** | 승인/반려 알림 재발행? | **X — 미제공** — 담당자 본인이 처리한 액션이므로 재알림 불필요. 사용자 트랙에는 있으나 admin 트랙에는 없음 |
| **Qn-8** | `NEW_APPLICATION` 링크 이동처 | **`/admin/programs/{programId}/applications`** (A4 신청 현황 화면) |
| **Qn-9** | `NEW_USER` 링크 이동처 | **`/admin/users/{userId}`** (A5 상세 화면) |
| **Qn-Δ (엔진 방향)** | `PROGRAM_DEADLINE_NEAR` admin 재사용 여부 | **X — 이번 스코프 제외** — 사용자 알림에만 존재. admin 마감 임박 알림은 스케줄러 신설 필요 → `deferred: A7-scheduler` |
| **Qn-Δ (V18 파일 존재)** | enum 확장에도 V 파일 남길지 | **남긴다** — COMMENT ON COLUMN 만이라도. Flyway 이력 추적성 확보 |
| **Qn-Δ (시간 표기)** | 상대 시간 vs 절대 시간 | **절대 (MM.dd HH:mm)** — 사용자 패널 정합. "방금 전" 상대 시간은 클라이언트 JS 필요, 이번 스코프 제외 |
| **Qn-Δ (관리자 대상 fan-out)** | admin 수 증가 시 N배 row | **1차 스코프 그대로 수용** — 관리자 규모(≤10) 에서 무시 가능. 확장 시 QA 재검토 |
| **Qn-Δ (계약 실측 대상)** | 계약 검사 실측 selector | **`.admin-header-bell`, `[data-notif-badge]`, `.notif-panel`, `.notif-item`** — A1 shell 계약의 `bell-btn` 확장 |

---

## 6. 데이터 소비 지점 리스트

`Notification` 데이터를 표시·조작하는 화면 목록. admin 트랙 신설 endpoint 는 사용자 트랙과 완전 분리.

| 소비 지점 | 참조 (prototype) | 현재 상태 | 갭 |
|---|---|---|---|
| admin 헤더 알림 배지 | prototype.html L443~453 | A1 disabled button 자리 | 배지·숫자 렌더 신설 |
| admin 헤더 드롭다운 (최근 5건) | prototype.html L455~493 | ❌ 없음 | fragment 신설 |
| admin `/admin/notifications` 전체 목록 | (prototype 없음) | ❌ | **이번 스코프 제외** — 드롭다운 "전체보기" 없이 최근 5건만 |
| 사용자 헤더 알림 (기존 flow) | `templates/fragments/notification-panel.html` | ✅ PR #143 | **손대지 않음** (회귀 방지) |
| 사용자 `/notifications` 페이지 | `templates/notification/list.html` | ✅ PR #143 | **손대지 않음** |
| admin 마이페이지 알림 설정 5토글 | prototype L2118~2170 | ❌ | **이번 스코프 제외** — `deferred: A7-settings` |

**write→read 왕복 시나리오** (매 이벤트별로 서비스 테스트 + curl 검증):

1. **`NEW_APPLICATION`**: 사용자 A 가 `POST /programs/{id}/apply` 성공 → 관리자 B (SYSTEM_ADMIN) 로그인 후 헤더 배지 = 1 · 드롭다운 최근 항목에 "새 신청이 접수됐어요" 표시 · 클릭 시 `/admin/programs/{id}/applications` 이동 · `isRead=true` 저장
2. **`NEW_USER`**: 신규 사용자 C 가 `POST /signup` 성공 → 관리자 B 헤더 배지 +1 · CENTER_ADMIN D 는 배지 변화 없음 (수신 대상 아님)
3. **모두 읽음**: 관리자 B 가 `POST /admin/notifications/mark-all-read` → 배지=0 · 드롭다운 재열람 시 dot 없음 · DB `isRead` 전건 true
4. **개별 삭제**: 관리자 B 가 `DELETE /admin/notifications/{id}` → row 물리 삭제 · 드롭다운 항목 제거 · 배지 -1

---

## 7. 화면 · 라우팅

### 7-1. 헤더 변경 (`templates/admin/fragments/header.html`)

- L69~78 disabled `admin-header-bell` button → 실동작으로 교체
- 배지 span 추가: `<span id="admin-notif-badge" hx-swap-oob="outerHTML" ...>N</span>`
- HTMX 속성: `hx-get="/admin/notifications/dropdown"` `hx-target="#admin-notif-dropdown"` `hx-trigger="click"`
- polling: 별도 wrapper `<div hx-get="/admin/notifications/badge" hx-trigger="every 30s" hx-swap="outerHTML">`
- 기존 GNB · 유저 드롭다운 · 검색 버튼 **위치 무변경** (A1 순서 유지)

### 7-2. 신규 endpoint (`AdminNotificationController`)

| method | path | 응답 | 설명 |
|---|---|---|---|
| GET | `/admin/notifications/dropdown` | HTML fragment | 최근 5건 드롭다운 (HTMX target) |
| GET | `/admin/notifications/badge` | HTML fragment | 배지 span 만 (30s polling target) |
| POST | `/admin/notifications/{id}/read` | 200 OK + OOB | 개별 읽음. HX-Redirect 로 link 이동 |
| POST | `/admin/notifications/mark-all-read` | 200 OK + fragment | 모두 읽음 + 드롭다운 재렌더 |
| POST | `/admin/notifications/{id}/delete` | 200 OK + OOB | 개별 삭제 (사용자 flow 와 동일 패턴) |

**Security**: `hasAnyRole('CENTER_ADMIN','SYSTEM_ADMIN')` — 기존 P0-2 `/admin/**` 매처 상속.

### 7-3. 신규 fragment (`templates/admin/fragments/admin-notification-panel.html`)

- 사용자 `fragments/notification-panel.html` 을 admin 라우팅으로 이식 (300px 폭, 라이트 톤)
- `hx-post` 경로만 `/admin/notifications/**` 로 변경
- 아이콘 매핑은 `NotificationType.getIconName()` 확장분 그대로 재사용

---

## 8. 파일 변경 목록

| # | 파일 | 변경 |
|---|---|---|
| 1 | `notification/NotificationType.java` | enum 값 2개 추가 + switch 케이스 확장 |
| 2 | `notification/Notification.java` | 무변경 |
| 3 | `notification/NotificationRepository.java` | 무변경 (기존 쿼리 재사용) |
| 4 | `notification/NotificationService.java` | 무변경 |
| 5 | `application/event/ApplicationSubmittedEvent.java` | **신규 record** (applicationId, userId, programId, programTitle, centerName) |
| 6 | `user/event/UserRegisteredEvent.java` | **신규 record** (userId, userName, userEmail) |
| 7 | `application/ApplicationService.java` | `apply()` 성공 후 `publishEvent(ApplicationSubmittedEvent)` 추가 (기존 approve/reject 이벤트 옆) |
| 8 | `user/UserService.java` | `signUp()` 성공 후 `publishEvent(UserRegisteredEvent)` 추가 |
| 9 | `admin/AdminNotificationEventListener.java` | **신규** — `@TransactionalEventListener(AFTER_COMMIT)` 2건 · fan-out 로직 (수신자 목록 조회 후 각 관리자에게 INSERT) |
| 10 | `admin/AdminNotificationController.java` | **신규** — 5개 endpoint |
| 11 | `admin/AdminNotificationService.java` | **신규** — 헤더 조회 + 격리 (`AdminScope` 사용) |
| 12 | `user/UserRepository.java` | 신규 쿼리 `findByRole(UserRole)` `findByRoleAndCenter(...)` |
| 13 | `templates/admin/fragments/header.html` | 알림 벨 disabled 해제 + HTMX 속성 + polling wrapper |
| 14 | `templates/admin/fragments/admin-notification-panel.html` | **신규** fragment |
| 15 | `templates/admin/fragments/admin-notification-badge.html` | **신규** fragment (polling 응답) |
| 16 | `static/css/admin.css` | 알림 벨 배지·드롭다운 스타일 (사용자 `notif-*` 재사용 여부 vs 신규 `admin-notif-*` 검토 — Qn-Δ) |
| 17 | `db/migration/V18__extend_notification_type_admin.sql` | COMMENT ON COLUMN |
| 18 | `docs/design-contracts/admin/header-notifications.md` | **신규 계약** |
| 19 | `e2e/contracts/admin-header-notifications.ts` | **신규 계약** (`proto:L443~493` 인용) |
| 20 | `e2e/tests/admin-notifications.spec.ts` | **신규 기능 E2E** |
| 21~ | 테스트 클래스 (§10) | 아래 참조 |

---

## 9. 회귀 방지 (최우선)

| 항목 | 검증 방법 |
|---|---|
| 기존 사용자 알림 flow 무회귀 | `notification/*Test`, `ApplicationNotificationListenerTest` 전건 PASS 유지. `/notifications` GET · `/notifications/{id}/read` POST · `/notifications/mark-all-read` POST 응답 무변경 실측 |
| `NotificationType` enum switch 폭발 방지 | `getToneColor()`, `getIconName()` 는 exhaustive switch. IDE compile 시 누락 감지. 신규 값 2개 케이스 필수 추가 |
| admin 트랙 A1~A6 무영향 | `admin/fragments/header.html` GNB · 로고 · 유저 드롭다운 순서 유지. `admin-shell.ts` 계약 검사 통과 유지 |
| Interceptor A6 · Scheduler 충돌 없음 | 알림은 이벤트 기반, Scheduler·Interceptor 무관 |
| CSRF · Security | `/admin/notifications/**` 은 P0-2 상속 → CSRF 토큰 자동. HTMX 는 기존 공통 헤더 스크립트 재사용 |
| CENTER_ADMIN 격리 | 다른 센터 관리자가 자기 센터 외 프로그램 신청에 대해 알림 수신하지 않음을 서비스 테스트로 명시 검증 |

---

## 10. 검증 규칙

### 10-1. 정적 검증
- `compileJava`
- `AdminNotificationServiceTest` — 수신자 결정 로직 (`SYSTEM_ADMIN` 전건 + `CENTER_ADMIN` 자기 센터만)
- `AdminNotificationEventListenerTest` — 이벤트 → fan-out INSERT 검증 (관리자 수 = row 수)
- `UserServiceSignUpEventTest` — signUp 성공 시 `UserRegisteredEvent` 발행 검증
- `ApplicationServiceSubmitEventTest` — apply 성공 시 `ApplicationSubmittedEvent` 발행 검증
- `AdminNotificationControllerRenderTest` — Thymeleaf 실 렌더 (드롭다운 + 배지 fragment)
- `NotificationTypeTest` — enum 확장분 `getToneColor()`, `getIconName()` 매핑
- 기존 `notification/*Test` 전건 PASS 유지 (회귀)

### 10-2. 동적 검증 (curl)
- `GET /admin/notifications/dropdown` — 200 OK + `.notif-panel` 마크업 · Thymeleaf `${...}` 잔존 0건
- `GET /admin/notifications/badge` — 200 OK + `<span id="admin-notif-badge">` 렌더
- `POST /admin/notifications/mark-all-read` — 200 OK · OOB swap span 포함
- `POST /admin/notifications/{id}/delete` — 200 OK (204 금지 — HTMX 2.0 swap 스킵)
- USER 계정 접근 시 403 + `/admin/notifications/**` 정적 리소스 200

### 10-3. 계약 검사
- `npx playwright test --project=contracts` → `admin-header-notifications` 갭 0 확인
- A1 `admin-shell` 계약 갭 0 유지 (헤더 변경으로 인한 회귀 없음)

### 10-4. 기능 E2E (`admin-notifications.spec.ts`)
- 시나리오 1: 사용자 신청 → 관리자 30s 후 배지 갱신 → 클릭 → 드롭다운 표시 → 항목 클릭 → 페이지 이동 + `isRead` 갱신
- 시나리오 2: 신규 회원가입 → SYSTEM_ADMIN 배지 +1 · CENTER_ADMIN 배지 무변화
- 시나리오 3: 모두 읽음 → 배지 0 · dot 사라짐
- 시나리오 4: 개별 삭제 → DOM 제거 + 배지 -1

### 10-5. 시각 확인 (사용자 영역)
- 다크 헤더 배경 위 배지 대비 (E72D0F vs 111827) — prototype 정합
- 드롭다운 라이트 카드 톤 · 300px 폭
- unread dot 우측 세로 스택 (X 위, dot 아래)

---

## 11. 결정 필요 항목 요약 (사용자 컨펌 대기)

**핵심 5**:
- **QA** — Application 트리거 = `@EventListener` 신규 이벤트 (권장) / 서비스 직접 호출
- **QB** — signUp 트리거 = `@EventListener` 신규 이벤트 (권장) / 서비스 직접 호출
- **QC** — Application 수신자 = `program.center` CENTER_ADMIN + 전체 SYSTEM_ADMIN (권장) / 다른 조합
- **QD** — 수신 방식 = HTMX polling 30s (권장) / SSE / WebSocket
- **QE** — NEW_USER 대상 = SYSTEM_ADMIN 만 (권장) / CENTER_ADMIN 도 포함

**세부 5**:
- **Qn-1** — polling interval 30s (권장)
- **Qn-2** — 드롭다운 최근 5건 (권장)
- **Qn-3** — 자동 만료 X (권장)
- **Qn-4** — reset endpoint 미제공 (권장)
- **Qn-5** — `admin-header-notifications.md` 계약 신설 (권장)

**추가 4**:
- **Qn-6** — 배지 `unread>0` 시만 (권장)
- **Qn-7** — 승인/반려 admin 알림 X (권장)
- **Qn-8** — NEW_APPLICATION 링크 = `/admin/programs/{id}/applications` (권장)
- **Qn-9** — NEW_USER 링크 = `/admin/users/{id}` (권장)

**엔진 방향 3**:
- **Qn-Δ (deadline)** — admin 마감 임박 이월 (권장)
- **Qn-Δ (V18)** — enum 확장 V 파일 남김 (권장)
- **Qn-Δ (fan-out)** — 관리자별 row INSERT 방식 (권장)

---

## 12. 스코프 예상 규모

- **소~중** — 파일 15~22 · +1200~1700 LOC · V18 (문서화 목적) · 이벤트 리스너 1개 (2 핸들러) · HTMX polling wrapper + fragment 2건

---

## 13. deferred / deviation 명시

| 항목 | 처리 | 사유 |
|---|---|---|
| SSE 실시간 push | `deferred: A7-realtime` | Fly.io stateful 커넥션 제약, 관리자 소수라 polling 충분 |
| WebSocket | `deferred: A7-realtime` | 같음 |
| 이메일 발송 | `deferred: A7-email` | SMTP 인프라 별도 티켓 |
| 알림 필터·검색 | `deferred: A8` | A8 폴리시 통합 |
| 알림 설정 5토글 (사용자 트랙엔 있음) | `deferred: A7-settings` | 원안 A7 4묶음 중 별개 티켓 |
| 글로벌 검색 (프로그램·사용자) | `deferred: A7-search` | 별개 티켓 |
| 관리자 마이페이지 | `deferred: A7-mypage` | 별개 티켓 |
| 자동 만료 (30일 삭제) | `deferred: A7-scheduler` | 수동 삭제 우선 |
| deadline admin 알림 | `deferred: A7-scheduler` | 스케줄러 신설 필요 |
| 승인/반려 admin 알림 | **deviation** | 담당자 본인 처리이므로 재알림 불필요 (POLICY 준하는 결정) |
| admin-notifications 기능 E2E (`admin-header-notifications.spec.ts`) | `deferred: A7-e2e-suite` | 벨 클릭·드롭다운·개별 읽음·모두 읽음·삭제 4시나리오 Playwright · 계약 검사(`--project=contracts admin-header-notifications`) + 유닛 테스트로 이번 스코프 커버 |
| visual E2E (`visual-admin-header-notifications.spec.ts`) | `deferred: A7-e2e-suite` | 300px 라이트 dropdown · 다크 헤더 감성 확인 · 시각 검증 사용자 영역 |

---

## 14. 작업 큐 메타

| 필드 | 값 |
|---|---|
| 작업 ID | A7 (원안 4묶음 중 **알림 벨만**) |
| 우선순위 | admin 트랙 다음 단계 (A6 완결 후) |
| 추정 단위 | **1 PR** (계약·리스너·컨트롤러·fragment·테스트 일괄) |
| 상태 | `spec_confirmed` |
| 후속 티켓 (분할) | A7-search / A7-settings / A7-mypage / A7-realtime / A7-scheduler / A7-email / **A7-rate-limit** / **A7-createdBy-recipient** |

---

## §후속 — 사용자 결정 반영 (2026-09-17)

### 핵심 결정

| Qn | 결정 | 근거 |
|---|---|---|
| **QA** Notification 엔티티 | 무변경 · 기존 `user` FK 유지 · fan-out INSERT | 사용자 알림 flow 재활용 |
| **QB** 트리거 | Spring `@EventListener` (ApplicationCreatedEvent · UserCreatedEvent) | transactional decoupling |
| **QC** NEW_APPLICATION 수신자 | **Option B-1** — `program.organization == CENTER_ADMIN.effectiveCenterName()` 매칭 · **SYSTEM_ADMIN 제외** | 성수기 발송량 3~5배 감소 · A6 대시보드로 SYSTEM_ADMIN 커버 |
| **QD** 수신 방식 | HTMX polling `every 30s` | SSE 오버스펙 |
| **QE** NEW_USER 수신자 | SYSTEM_ADMIN 만 | 원안 |
| **Qn-1~9** | 원안 A | polling 30s · 5건 · 만료 X · 배지 unread>0 · 링크 A4/A5 |
| **Qn-Δ rate limiting** | **후속 A7-rate-limit 이월** | 이번 스코프 제외 · 초기 관찰 후 필요성 판단 |

### QC 수신자 설계 (확장성 고려 · B-1 → B-3 로드맵)

**이번 A7 (B-1 착수)**:
- `NotificationRecipientResolver` 인터페이스 신설 · 전략 패턴
- 초기 구현: `CenterMatchingRecipientResolver` — `program.organization` 문자열 매칭
- 이벤트 리스너에서 Resolver 로 위임 (직접 Repository 호출 X)

```java
public interface NotificationRecipientResolver<E> {
    List<User> resolve(E event);
}

@Component
public class ApplicationCreatedRecipientResolver implements NotificationRecipientResolver<ApplicationCreatedEvent> {
    // B-1: program.organization 매칭 CENTER_ADMIN
}
```

**후속 A7-createdBy-recipient (B-3 확장)**:
- Program 엔티티에 `createdBy` FK 신설 (V18 · 기존 프로그램 backfill)
- `CreatedByRecipientResolver` 신설 · createdBy 있으면 그 사용자 · 없으면 fallback (`CenterMatchingRecipientResolver` 위임)
- 추가: "선택한 사용자 알림 발송" — `ProgramWatcher` 엔티티 (M:N Program-User) 신설 · `WatcherRecipientResolver` chain
- Resolver chain 구조 (`CompositeRecipientResolver`) 도입 시 순차 병합

**설계 이점**:
- 이번 티켓에서 Resolver 인터페이스만 정의 → 후속 확장 시 구현체 추가만
- 이벤트 리스너 코드 무변경 (SRP 준수)
- 테스트 격리 용이 (Resolver mock 주입)

### 회귀 방어 핵심 3점

1. **사용자 알림 flow (PR #143) 무영향**: `NotificationType` enum 확장만 · 기존 값 (`APPLY_APPROVED` 등) 무변경 · exhaustive switch 케이스 필수 (compile 감지)
2. **`ApplicationService.apply()` · `UserService.signUp()` 무영향**: Spring `@EventListener` 는 transaction commit 후 실행 · 이벤트 발행만 추가 · signUp/apply 실패 시 알림 X
3. **admin/fragments/header.html A1·A5 무회귀**: GNB 항목 · 로고 위치 유지 · 벨은 우측 신설 슬롯

### V18 마이그레이션

- 문서화 목적 (COMMENT ON COLUMN) · 스키마 변경 X
- `NotificationType` enum 은 STRING 저장이라 DDL 불필요

### deferred / deviation

- **A7-rate-limit**: 동일 관리자에게 동일 프로그램 5분 내 반복 알림 병합 · 초기 관찰 후 판단
- **A7-createdBy-recipient**: Program.createdBy FK + 선택 watcher · A9 Program-Center FK 정식 도입과 함께 처리
- **A7-realtime**: SSE / WebSocket · 후속
- **A7-scheduler**: `PROGRAM_DEADLINE_NEAR` admin 재사용 · 마감임박 스케줄러
- **A7-email**: 이메일 발송

### 다음 액션

ym-impl 인계 프롬프트에 위 결정 반영. 특히 Resolver 인터페이스 분리 · 회귀 방어 3점 명시.
