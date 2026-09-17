# admin 헤더 알림 벨 (A7)

> 계약 파일: [`e2e/contracts/admin-header-notifications.ts`](../../../e2e/contracts/admin-header-notifications.ts)
> spec: [`docs/specs/A7-admin-header-live.md`](../../specs/A7-admin-header-live.md)
> prototype: `docs/00_assets/admin/prototype.html` L443~494 (bell + dropdown), L2832~2836 (mock items)

## 범위

`/admin/**` 어느 경로에서든 등장하는 다크 헤더 우측의 알림 벨 · 드롭다운 · 30s polling wrapper 를 다룬다. 사용자 페이지 헤더 알림(PR #143)과는 URL prefix (`/admin/notifications/**` vs `/notifications/**`), 톤(라이트 300px vs 라이트 380px), 배지 방식(숫자 vs dot) 이 모두 다르다.

## 화면 아키텍처

```
.admin-header (다크 #111827)
 └─ .admin-header-right
     └─ .admin-header-notif (position:relative)
         ├─ .admin-header-bell (button · hx-get /admin/notifications/dropdown)
         │   └─ #admin-notif-badge (data-notif-badge="N")
         ├─ div[hx-get=/admin/notifications/badge, hx-trigger="every 30s"] (스타일상 hidden)
         └─ #admin-notif-dropdown (hidden 초기, innerHTML swap 대상)
             └─ .admin-notif-panel (300px 라이트 카드)
                 ├─ .admin-notif-panel-header (제목 + 모두 읽음 btn)
                 ├─ .admin-notif-list > .admin-notif-item * N
                 └─ .admin-notif-empty (fallback)
```

## 상태 머신

| 상태 | 트리거 | DOM 변화 |
|---|---|---|
| 초기 | 페이지 진입 | 벨 + 배지(unread=0 이면 hidden) + dropdown[hidden] |
| 벨 클릭 | 사용자 | GET /admin/notifications/dropdown → panel innerHTML 삽입 · dropdown hidden 해제 (JS) |
| 30s tick | polling | GET /admin/notifications/badge → #admin-notif-badge outerHTML swap |
| 개별 클릭 | 사용자 | POST /admin/notifications/{id}/read → HX-Redirect (A4/A5) |
| 모두 읽음 | 사용자 | POST /admin/notifications/mark-all-read → panel innerHTML 재렌더 · unread=0 |
| 삭제 | 사용자 | POST /admin/notifications/{id}/delete → item DOM 제거 + OOB 배지 갱신 |

## 링크 라우팅 (Qn-8/9)

| NotificationType | link 컬럼 값 |
|---|---|
| `NEW_APPLICATION` | `/admin/programs/{programId}/applications` |
| `NEW_USER` | `/admin/users/{userId}` |

## 결정 사항 (spec A7 §후속 요약)

| Qn | 결정 |
|---|---|
| QA Notification 엔티티 | 무변경 · 기존 user FK + fan-out INSERT |
| QB 트리거 | Spring `@TransactionalEventListener(AFTER_COMMIT)` |
| QC NEW_APPLICATION 수신자 | **B-1** program.organization 매칭 CENTER_ADMIN 만 (SYSTEM_ADMIN 은 대시보드로 커버) |
| QD 수신 방식 | HTMX polling `every 30s` |
| QE NEW_USER 수신자 | SYSTEM_ADMIN 만 |
| Qn-1 polling interval | 30s |
| Qn-2 드롭다운 건수 | 5건 |
| Qn-3 자동 만료 | X (deferred: A7-scheduler) |
| Qn-6 배지 노출 | unread > 0 시만 (hidden 속성) |

## POLICY 매핑

- P-1 카피: "새 신청이 접수됐어요" / "새 회원이 가입했어요" — 존댓말 (feedback_tone_jondaetmal)
- 시간 표기: `MM.dd HH:mm` (사용자 트랙 정합, 상대 시간은 이월)
- 배지 색상: `#E72D0F` (POLICY 대비 다크 헤더 위 유일 예외 컬러 — proto 정합)

## deviation / deferred

| 항목 | 사유 |
|---|---|
| dropdown.width 계약 검사 | **deferred** — 벨 클릭 후 상태에서만 유효. `visual-admin-header-notifications.spec.ts` 인터랙션 spec 에서 후속 검사 |
| 상대 시간 ("방금 전") | deviation — 클라이언트 JS 필요, 사용자 트랙과 정합 위해 절대 시간 채택 |
| SYSTEM_ADMIN NEW_APPLICATION 수신 | deviation (QC B-1 결정) — A6 대시보드 카드로 커버 |
| 승인/반려 admin 알림 | deviation (Qn-7) — 담당자 본인 처리라 재알림 불필요 |
| rate limiting (5분 병합) | deferred — A7-rate-limit |
| SSE / WebSocket 실시간 | deferred — A7-realtime |

## 실측 selector (§Qn-Δ)

- `.admin-header-bell` — 벨 button
- `[data-notif-badge]` / `#admin-notif-badge` — 배지 span (polling swap 대상)
- `#admin-notif-dropdown` — dropdown wrapper (hx-target)
- `.admin-notif-panel` — 드롭다운 컨테이너 (열린 상태)
- `.admin-notif-item` / `.admin-notif-item--unread` — 항목 · unread 강조

## 회귀 방어 3점

1. 사용자 알림 flow (PR #143) 무영향 — `NotificationType` 기존 값 6종 무변경, `/notifications` endpoint · templates/fragments/notification-panel.html 무수정
2. `ApplicationService.apply()` · `UserService.signUp()` 트랜잭션 무영향 — `@TransactionalEventListener(AFTER_COMMIT)` + 리스너 예외 격리
3. `admin/fragments/header.html` A1 정합 유지 — GNB/유저 드롭다운 위치 무변경, 벨은 우측 신설 슬롯
