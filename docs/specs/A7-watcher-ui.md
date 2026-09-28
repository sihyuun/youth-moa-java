# A7-watcher-ui — Watcher 축 UI + ProgramWatch 엔티티

> 상태: spec_confirmed → impl_in_progress (2026-09-28)
> 파생 축: B-3-B (fan-out Composite 3번째 축)
> 선행: A7-createdBy-recipient (B-3-A, PR #229 · commit `2585385`)

---

## 1. 배경

A7 관리자 헤더 알림 벨의 fan-out 축을 확장한다.

- **B-2** — 프로그램 소속 센터 CENTER_ADMIN
- **B-3-A** — 프로그램 작성자 (`Program.createdBy`)
- **B-3-B (본 티켓)** — 프로그램을 "지켜보기" 등록한 admin (센터/역할 무관)

Watcher 는 **감독 관점** — SYSTEM_ADMIN 이 특정 사업(프로그램) 을 상시 모니터링, 또는 CENTER_ADMIN 이 타 센터 프로그램을 벤치마킹 목적으로 관찰.

---

## 2. 사용자 결정 이력 (Q-A7W 전건)

| ID | 질문 | 결정 |
|---|---|---|
| Q-A7W-1 | UNIQUE 제약 | UNIQUE(admin_id, program_id) |
| Q-A7W-2 | 토글 UI 위치 | (c) 목록 각 row + 편집 폼 헤더 |
| Q-A7W-3 | 관심 프로그램 목록 | (b) 대시보드 카드 (최근 5 + "전체 보기" 이월) |
| Q-A7W-4 | 디자인 | 눈 아이콘 SVG, active=`--color-primary`, idle=outline `--color-text-tri`, label "지켜보기" |
| Q-A7W-5 | 3축 순서 | B-2 → B-3-A → B-3-B |
| Q-A7W-6 | 비활성 admin watcher | (a) Resolver skip (row 유지) |
| Q-A7W-7 | SUSPENDED 프로그램 알림 | (a) 발송 |
| Q-A7W-8 | 상한 | 20개 · 초과 시 오래된 자동 삭제 (Bookmark 일관) |

---

## 3. Watcher vs Bookmark 차이

| 항목 | Bookmark | ProgramWatch |
|---|---|---|
| 주체 | 일반 사용자 (User) | admin (CENTER/SYSTEM) |
| 목적 | 관심 프로그램 UX (알림 트리거 없음) | 신청 발생 시 알림 fan-out |
| 상한 | 20 (WF-3-002) | 20 (일관성 유지) |
| Center scope | 무관 | 무관 (감독 관점) |
| UNIQUE | user_id + program_id | admin_id + program_id |
| 아이콘 | 별 (star) | 눈 (eye) |
| 삭제 정책 | 오래된 자동 삭제 | 오래된 자동 삭제 |

---

## 4. 데이터 모델

### `program_watch` 테이블 (V24)

```sql
CREATE TABLE program_watch (
  id           BIGSERIAL PRIMARY KEY,
  admin_id     BIGINT      NOT NULL REFERENCES users(id),
  program_id   BIGINT      NOT NULL REFERENCES program(id),
  created_at   TIMESTAMP   NOT NULL,
  CONSTRAINT uk_program_watch_admin_program UNIQUE(admin_id, program_id)
);
CREATE INDEX idx_program_watch_program            ON program_watch(program_id);
CREATE INDEX idx_program_watch_admin_created      ON program_watch(admin_id, created_at DESC);
```

### `ProgramWatch` 엔티티

- Bookmark 패턴 준용 (@ManyToOne LAZY admin · @ManyToOne EAGER program)
- @CreatedDate `createdAt` — `@EntityListeners(AuditingEntityListener.class)`

---

## 5. 3축 fan-out 흐름 (Composite union · Q-A7W-5)

```
ApplicationCreatedEvent
        │
        ▼
CompositeApplicationCreatedResolver.resolve(event)
        │
        ├─ B-2  centerAdminResolver.resolve(event)   ─┐
        ├─ B-3-A createdByResolver.resolve(event)     │
        └─ B-3-B watcherResolver.resolve(event)       ▼
                                       LinkedHashMap distinct union
                                       (삽입 순서 = B-2 → B-3-A → B-3-B)
                                                 │
                                                 ▼
                    AdminNotificationEventListener.onApplicationCreated
                                                 │
                                                 ▼
                    NotificationService.create per admin
```

### 각 축 skip 규칙

| 축 | Skip 조건 |
|---|---|
| B-2 | event.centerId null · 매칭 CENTER_ADMIN 없음 |
| B-3-A | createdBy null · createdBy 비활성 |
| B-3-B | programId null · 활성 admin watcher 없음 · 프로그램 없음 |

---

## 6. UI

### 6-1. 눈 아이콘 SVG (Q-A7W-4)

- fragment `fragments/icons :: eye` / `eyeFill` 신설
- active: fill=`--color-primary`
- idle: stroke=`--color-text-tri`, fill=none
- 크기: 20px (Bookmark 별 아이콘과 동일)
- aria-label: watched → "지켜보기 해제" / false → "지켜보기 등록"

### 6-2. Fragment: `admin/fragments/watch-button :: button`

파라미터: `programId · watched · styleClass`

styleClass 종류:
- `list-watch-btn` — 목록 row 액션 열
- `form-watch-btn` — 편집 폼 헤더

HTMX:
- `hx-post="/admin/programs/{id}/watch/toggle"`
- `hx-swap="outerHTML"`
- `hx-vals={"styleClass":"..."}`

### 6-3. 대시보드 카드 (Q-A7W-3 b)

- 위치: dashboard.html 우측 컬럼 (승인 대기 카드 아래)
- 데이터: `findWatchedPrograms(admin, PageRequest.of(0, 5))`
- 헤더: "지켜보는 프로그램" + "전체 보기 (준비 중)" 링크 (별도 페이지는 이월)

---

## 7. 컨트롤러 API

### `POST /admin/programs/{programId}/watch/toggle`

- `@PreAuthorize("hasAnyRole('CENTER_ADMIN','SYSTEM_ADMIN')")`
- `@RequestParam(defaultValue="list-watch-btn") String styleClass`
- 응답: `admin/fragments/watch-button :: button` fragment (200 OK)
- 서비스 반환: insert 시 true, delete 시 false → fragment 재렌더

---

## 8. 회귀 감시 대상

- CompositeApplicationCreatedResolverTest — 기존 6 TC PASS + 3축 union 시나리오 확장
- ApplicationCreatedRecipientResolverTest (B-2) — 무변경
- CreatedByRecipientResolverTest (B-3-A) — 무변경
- AdminNotificationEventListenerTest — 무변경
- AdminEagerFetchN1Test — 5 TC baseline 유지
- AdminProgramFormServiceTest — 17 TC 무변경

---

## 9. 함정 주의

- HTMX outerHTML swap + fragment id — wrapper 와 fragment root 는 다른 id (F0h-c2 사고)
- Composite 삽입 순서 보존 = LinkedHashMap
- 20개 초과 delete-then-insert 는 단일 트랜잭션 안에서 (Bookmark 패턴)
- CENTER_ADMIN 도 Watcher 등록 가능 (center scope 격리 없음)

---

## 10. 이월 항목

- `/admin/programs/watched` 별도 페이지 — Q-A7W-3 b 로 대시보드 카드만 도입. 전체 목록 페이지는 후속 티켓
- AdminScope 기반 center 격리 — Watcher 는 감독 관점이라 미적용
- Watcher 등록 후 admin 이 아닌 계정으로 role 변경 시 정리 — 별도 스케줄러 티켓

---

## 11. 구현 매핑

| spec 행 | 구현 파일 |
|---|---|
| V24 마이그레이션 | `db/migration/V24__create_program_watch.sql` |
| 엔티티 | `notification/watch/ProgramWatch.java` |
| Repository | `notification/watch/ProgramWatchRepository.java` |
| Service (toggle · 20 상한) | `notification/watch/ProgramWatchService.java` |
| Controller (POST fragment) | `notification/watch/ProgramWatchController.java` |
| Watcher Resolver (B-3-B) | `notification/admin/WatcherRecipientResolver.java` |
| Composite 3축 union | `notification/admin/CompositeApplicationCreatedResolver.java` |
| Watch button fragment | `templates/admin/fragments/watch-button.html` |
| 눈 아이콘 fragment | `templates/fragments/icons.html` (eye/eyeFill) |
| 목록 row 통합 | `templates/admin/program/list.html` |
| 편집 폼 헤더 통합 | `templates/admin/program/form.html` |
| 대시보드 카드 | `templates/admin/dashboard.html` + `AdminDashboardService` |
| 편집 폼 watched 주입 | `AdminProgramController.editForm` |
| E2E | `e2e/tests/admin-program-watch.spec.ts` |
