# admin-user-bulk 디자인 계약

> 출처: `docs/specs/A8-admin-bulk-csv.md` · prototype.html L1201~1210, L2772, L4111~4124

## 기본 정책

- QA: floating bottom action bar (다크 `#1E293B`)
- QB: selection state 는 페이지 이동 시 초기화 (DOM Set 만 유지)
- Qn-1: per-row 트랜잭션 (POLICY.md P-BULK-1) — 부분 실패 허용, flash 에 성공/실패 카운트 + 최대 5건 사유
- Qn-8: `AdminUserSafeguard` 컴포넌트로 개별 endpoint 와 공유. 개별 A5 flow 회귀 없음

## 엔드포인트

| Method | Path | 설명 |
|---|---|---|
| POST | `/admin/users/bulk/deactivate` | 다건 차단 · `ids[]` + `reason` 필수 |
| POST | `/admin/users/bulk/reactivate` | 다건 재활성화 · `ids[]` |
| POST | `/admin/users/bulk/role` | 다건 권한 변경 · `ids[]` + `role` |

모두 302 redirect → `/admin/users` + flash.

## Safeguard (개별·bulk 공유)

1. 자기 자신 대상 X
2. 마지막 활성 SYSTEM_ADMIN 차단·강등 X
3. SYSTEM_ADMIN 승격은 SYSTEM_ADMIN 만 (컨트롤러 `@PreAuthorize` 이중 방어)

각 규칙은 `AdminUserSafeguard.assertCan{Deactivate,Reactivate,ChangeRole}` 로 캡슐화.

## 회귀 방어

- A5 개별 endpoint (`/admin/users/{id}/deactivate` 등) 그대로 유지. 서비스 개별 메서드도 새 Safeguard 를 사용하도록 refactor
- 기존 A5 테스트 (AdminUserServiceTest 등) 회귀 없음 재실행 확인 필수

## deferred

- 관리자 신규 계정 대량 발급 UI → `A5-1` 재이월
