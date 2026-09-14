# admin-program-applications — 관리자 프로그램 신청 관리

| 메타 | 값 |
|---|---|
| 화면 | `/admin/programs/{id}/applications`, `/admin/programs/{id}/applications/{aid}` |
| 신설 | 2026-09-15 · A4 admin-program-detail (Qn 24건 모두 A) |
| 상위 spec | [`docs/specs/A4-admin-program-detail.md`](../../specs/A4-admin-program-detail.md) |
| prototype | `admin/prototype.html` L1370~1441 (목록) · L2677~2749 (상세 모달) |
| 공통 정책 | [`POLICY.md`](../POLICY.md) — 다크 헤더 · 인디고 primary · 존댓말 |
| 상태 | active |

---

## 아키텍처

**두 화면**:
1. **목록** `/admin/programs/{id}/applications` — 신청 관리 페이지 (테이블 + 요약 배지 + 필터)
2. **상세** `/admin/programs/{id}/applications/{aid}` — 신청 상세 fragment (모달 방식 · Qn-B A · 실 구현은 별도 페이지로 렌더링되나 Header + main 레이아웃 · prototype 480px 모달 UI 를 페이지 wrapper 로 재배치)

## 상태 머신

```
PENDING → APPROVED  (approve · 이벤트 ApplicationApprovedEvent)
PENDING → REJECTED  (reject · 사유 필수 · 이벤트 ApplicationRejectedEvent)
PENDING → CANCELLED (관리자 강제 · Qn-C A · 사유 필수 · "관리자 취소: " 접두 · 이벤트 ApplicationCancelledEvent)
APPROVED → CANCELLED (관리자 강제)
APPROVED → PENDING  (deferred · Qn-Δ12 B)
REJECTED → (재신청 차단 유지 · Qn-Δ10 A)
CANCELLED → PENDING (사용자 재신청만 · admin 화면에서 불가)
```

- **idempotent**: 이미 목표 상태이면 no-op (예외 없이 return)
- **원자성**: 상태 변경 · adminNote · rejectReason 은 각각 별도 POST endpoint 로 저장 (원 spec §Qn-Δ9 A "원자적" 대안이었으나 구현은 endpoint 분리 방식 채택 · deviation 참조)

## RBAC

| 사용자 | 목록 | 상세 | 상태 변경 |
|---|---|---|---|
| SYSTEM_ADMIN | 전체 | 전체 | 전체 |
| CENTER_ADMIN | 자기 센터 프로그램만 | 자기 센터 | 자기 센터 |
| USER / 익명 | 403 / 로그인 리다이렉트 | 동 | 동 |

- `AdminScope.effectiveCenterName()` 로 CENTER_ADMIN 스코프 산출 → Program.organization 문자열 매칭
- 스코프 위반 시 `IllegalAccessError` → `AccessDeniedException` (403)
- 프로그램 미존재 → `ResponseStatusException(NOT_FOUND, ...)` (404)
- Program-Center FK 도입은 A9 이월

## CTA 라우팅

| Action | Method | URL | 결과 |
|---|---|---|---|
| 목록 조회 | GET | `/admin/programs/{id}/applications?status=&q=&page=` | 200 · 목록 페이지 |
| 상세 조회 | GET | `/admin/programs/{id}/applications/{aid}` | 200 · 모달 fragment 페이지 |
| 승인 | POST | `/admin/programs/{id}/applications/{aid}/approve` | 302 → 목록 (flashMessage) |
| 반려 | POST | `/admin/programs/{id}/applications/{aid}/reject` (rejectReason) | 302 → 목록 |
| 강제 취소 | POST | `/admin/programs/{id}/applications/{aid}/cancel` (cancelReason) | 302 → 목록 |
| 담당자 의견 | POST | `/admin/programs/{id}/applications/{aid}/note` (adminNote) | 302 → 목록 |

- **CSRF 필수** · 모든 POST form 에 hidden `_csrf` 토큰
- **redirect** 이후 `flashMessage` / `flashError` 로 성공/실패 노출

## 데이터 계약

### 목록 파라미터

| 파라미터 | 타입 | 기본 | 설명 |
|---|---|---|---|
| `status` | string / ApplicationStatus | (all) | PENDING/APPROVED/REJECTED/CANCELLED · 대소문자 무관 · 잘못된 값은 무시 |
| `q` | string | (empty) | 신청자 name / email LIKE (대소문자 무관) |
| `page` | int | 0 | 0-based |

- **PAGE_SIZE = 10** — spec §Qn-6 은 A(=20) 권장이었으나 구현은 10 채택. **deviation 명시**
- 정렬: `appliedAt DESC` 고정

### 요약 배지 (5종)

- 전체 · 대기(PENDING) · 승인(APPROVED) · 반려(REJECTED) · 취소(CANCELLED) · 정원(program.capacity)
- `AdminApplicationService.summaryCounts(programId)` 로 상태별 count 일괄 조회

### 참여횟수 (visits) 파생

- **N+1 방지**: 목록 로드 시 userId 일괄 조회 `countByUserIdInAndStatus(userIds, APPROVED)`
- 각 row 에 `visits[userId]` 로 표시 · APPROVED 신청 수 기준

## 상세 모달 UI

- 신청자 정보 grid 6칸 (이름 · 이메일 · 성별 · 핸드폰 · 접수일시 · 참여횟수)
- 지원 동기 (applyReason · 있을 때만)
- F0c 신청 답변 (ApplyAnswer · TEXT 회색 박스 · DROPDOWN 보라 pill)
- 처리 이력 (processedAt · processedBy · rejectReason · cancelReason)
- 상태 변경 액션 3개 (승인 · 반려 with reason · 강제 취소 with reason)
- 담당자 의견 (textarea maxlength=1000)

## deferred

- **CSV 내보내기** → A8
- **Bulk 승인/반려** → A8
- **대기 전체 승인** (approveAllPending) → A8
- **승인율 통계** → A6
- **Course 별 신청 화면** → A6/후행 (Qn-Δ3 B)
- **마이페이지 adminNote 노출** → 후행 (Qn-Δ5 B)
- **APPROVED → PENDING 되돌리기** → Qn-Δ12 B (미도입)
- **HTMX partial swap** → 후행 (현재 PRG redirect + flash 방식)

## deviation

| 항목 | 사유 |
|---|---|
| PAGE_SIZE = 10 (spec §Qn-6 A 는 20) | 구현 선택 · A2 admin-programs `ADMIN_PAGE_SIZE` 와 통일된 10 |
| 상세 UI = 별도 페이지 (spec §Qn-B A "480px 모달") | 페이지 wrapper 로 렌더 · 상세 fragment 자체는 modal fragment 로 정의되어 후속 HTMX 도입 시 재활용 가능 |
| 상태 변경 endpoint 분리 (spec §Qn-Δ9 A "원자적") | approve / reject / cancel / note 4개 endpoint · form action 이 분기 |
| Bulk selection bar 완전 제거 (Qn-Δ1 A) | A8 이월 · UI 자리 정리 |

## 참고

- 관련 계약: [`admin-programs.ts`](../../../e2e/contracts/admin-programs.ts) · [`admin-program-form.ts`](../../../e2e/contracts/admin-program-form.ts)
- 회귀 방어: 사용자 apply flow / mypage cancel flow / notification listener 전수 무회귀 (spec §8)
