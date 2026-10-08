# FOLLOW-waitlist-auto-approve — 확정 명세

> 상태: `spec_confirmed` → `impl_in_progress` (2026-10-08)
> ym-spec ID: a946634eb664b6a5f
> 브랜치: `feature/FOLLOW-waitlist-auto-approve`
> 베이스: main (4951b90)

## 1. 배경

관리자 프로그램 상세 (`/admin/programs/{id}`) 는 FOLLOW-admin-program-detail-readonly 로 복원됐으나,
prototype L1580~1592 의 "자동 승인" 노란 배너 + 토글 스위치가 Q4 이월됨.
본 티켓은 Q4 를 되살려 **정원 꽉 참 상태에서 승인 취소/반려 발생 시 PENDING 선입선출 1건 자동 승격** 흐름을 도입한다.

## 2. 사용자 결정 (사용자 확정)

| Q | 결정 | 근거 |
|---|---|---|
| **Q1** 저장 방식 | boolean 신설 — `Program.autoApproveWhenFull BOOLEAN NOT NULL DEFAULT false` (approvalMode 와 독립) | approvalMode enum 확장 금지 (A4 범위 외) |
| **Q2** 트리거 시점 | **② 대기자 승격** — APPROVED→CANCELLED/REJECTED 시 PENDING 최선순위 1건 자동 APPROVED. apply() 시점 자동 승인은 하지 않음 | 정원 꽉 참 후 공석 생겨야 발동. 신청 시점 자동 승인은 A4 스코프 (approvalMode=AUTO) 가 담당할 영역 |
| **Q3** processedBy | 기존 sysadmin 재활용 + `adminNote = "SYSTEM 자동 승인 (대기자 승격)"` | 감사 추적 가능. 신규 "SYSTEM" user row 생성 금지 |
| **Q4** OFF→ON 전환 | 신규 승격 이벤트부터 적용 — 기존 PENDING 일괄 승급 금지 | 예측 불가능한 대량 승인 방지 |
| **Q5** 배너 노출 조건 | `capacity != null && countByStatus(APPROVED) + countByStatus(PENDING) >= capacity` | capacity 없는 프로그램은 "꽉 참" 개념 없음 → 배너 미노출 |
| 범위 | **1 PR** (스키마 + 백엔드 + UI + 테스트) | 사용자 확정 |

## 3. 변경 범위

### 3-1. DB (Flyway)

| 파일 | 변경 |
|---|---|
| `src/main/resources/db/migration/V28__add_program_auto_approve_when_full.sql` | 신설 — `ALTER TABLE program ADD COLUMN auto_approve_when_full BOOLEAN NOT NULL DEFAULT false` |

> V 번호: main 최신이 V27 (backfill_apply_period_and_not_null) → V28 부여.

### 3-2. Entity / Repository

| 파일 | 변경 |
|---|---|
| `program/Program.java` | `autoApproveWhenFull boolean` 필드 (NOT NULL) + `enableAutoApproveWhenFull()` / `disableAutoApproveWhenFull()` 도메인 메서드. `@Setter` 금지. Builder·updateFromAdminForm 서명은 **건드리지 않음** (Q4 토글 전용 endpoint 로 격리) |
| `application/ApplicationRepository.java` | `findFirstByProgramIdAndStatusOrderByAppliedAtAsc(Long programId, ApplicationStatus status)` 신설 |

### 3-3. Service

| 파일 | 변경 |
|---|---|
| `admin/AdminProgramService.java` | `updateAutoApproveWhenFull(Long programId, boolean enabled)` — scopeSpec 재검증 (`find(id)` 재활용), 도메인 메서드 호출. `@Transactional` |
| `admin/AdminApplicationService.java` | `reject()` / `forceCancel()` 메서드 말미 (상태 전이 + 이벤트 발행 후) 에 `promoteWaitlistIfEligible(program)` 호출. 승격 로직은 private helper. **방식 A (직접 호출)** — Spring Event 분리는 후속 리팩터 티켓 |

**승격 로직 (private helper)**:
1. `program.isAutoApproveWhenFull() == false` → return
2. `program.getCapacity() == null` → return (정원 없는 프로그램은 "꽉 참" 개념 없음)
3. 승격 후 APPROVED 가 capacity 를 초과하지 않도록, 현재 APPROVED 수가 capacity 미만일 때만 승격 (CANCELLED/REJECTED 로 공석 생긴 뒤이므로 `APPROVED < capacity` 성립)
4. `ApplicationRepository.findFirstByProgramIdAndStatusOrderByAppliedAtAsc(programId, PENDING)` 로 가장 오래된 PENDING 1건
5. sysadmin 조회 (`UserRepository.findByEmail("sysadmin@youth-moa.test")`)
6. `app.approve(sysadmin)` + `app.updateAdminNote("SYSTEM 자동 승인 (대기자 승격)")`
7. `ApplicationApprovedEvent` 발행 (사용자 알림 자동)

### 3-4. Controller

| 파일 | 변경 |
|---|---|
| `admin/AdminProgramController.java` | `POST /admin/programs/{id}/auto-approve` endpoint 추가. `@PreAuthorize("hasAnyRole('SYSTEM_ADMIN','CENTER_ADMIN')")` (상세 열람 권한과 동일). 파라미터 `boolean enabled`. 성공 시 302 → `/admin/programs/{id}` + flash |

### 3-5. 템플릿 / CSS

| 파일 | 변경 |
|---|---|
| `templates/admin/program/_waitlist-banner.html` | 신설 — fragment `waitlistBanner(program, enabled)`. 노란 배경 (`--color-warning-light`), 토글 스위치. prototype admin L1580~1592 markup 재현 |
| `templates/admin/program/detail.html` | 좌측 정보 카드 하단 (또는 신청 현황 보기 위) 에 `_waitlist-banner.html` fragment 조건부 include. 조건: `capacity != null && (approved + pending) >= capacity` |
| `static/css/main.css` | `.admin-waitlist-banner` + `.admin-toggle-switch` 추가. CSS 변수 사용 (`--color-warning-light`, `--radius-md`, `--shadow-sm` 등). 하드코딩 금지 |

### 3-6. 테스트

| 파일 | 변경 |
|---|---|
| `test/.../admin/AdminProgramDetailRenderTest.java` | TC 2건 추가 — ① 배너 노출 조건 충족 시 markup 포함 ② 미충족 시 미노출 |
| `test/.../admin/AdminProgramAutoApproveTest.java` | 신설 — ① SYSTEM_ADMIN 토글 ON round-trip ② 토글 OFF round-trip |
| `test/.../application/ApplicationServiceAutoApproveTest.java` | 신설 — ① 정원 미달 + 토글OFF (승격 안 함) ② 정원 꽉 참 + 토글OFF (승격 안 함) ③ 정원 꽉 참 + 토글ON + CANCELLED (승격 발동) ④ 토글ON + REJECTED (승격 발동) |
| `e2e/tests/admin-waitlist-auto.spec.ts` | 신설 — Playwright 기능 E2E. 토글 클릭 → 응답 302 + 배너 상태 변경 |
| `e2e/contracts/admin-program-detail.ts` | `waitlist.banner.missing` 체크를 **조건부 (deferred)** 로 완화. 기본 시드 (program 1) 가 조건 미충족이면 배너 없는 상태를 기대 (수정 전과 동일한 결과) |

## 4. 구현 매핑 (완료 후 채움)

| 명세 섹션 | 코드 위치 | 비고 |
|---|---|---|
| 3-1 V28 | `src/main/resources/db/migration/V28__...sql` | |
| 3-2 Program 필드/메서드 | `Program.java:LLL` | |
| 3-2 Repository 쿼리 | `ApplicationRepository.java:LLL` | |
| 3-3 AdminProgramService | `AdminProgramService.java:LLL` | |
| 3-3 승격 helper | `AdminApplicationService.java:LLL` | |
| 3-4 Controller endpoint | `AdminProgramController.java:LLL` | |
| 3-5 banner fragment | `_waitlist-banner.html` | |
| 3-5 detail include | `detail.html:LLL` | |
| 3-5 CSS | `main.css:LLL` | |
| 3-6 테스트 4종 | 각 테스트 파일 | |

## 5. 금지

- `@Setter` / `apply()` 시점 자동 승인 / enum 확장 → 모두 Q1·Q2 결정에 의해 금지
- sysadmin 신규 생성 금지 — 기존 `sysadmin@youth-moa.test` 재활용
- 범위 외 리팩토링 금지 (대기자 알림·이메일·대기자 전용 화면 등은 이월)

## 6. 이월

- 대기자 전용 UI (대기자 리스트, 승격 로그 뷰어)
- 대기자 승격 알림 특별 메시징 (현재는 ApplicationApprovedEvent 재활용 = "승인 완료" 알림)
- Spring Event 분리 (현재는 방식 A 직접 호출)
- approvalMode=AUTO 와 autoApproveWhenFull 의 상호작용 — 현재는 완전히 독립
