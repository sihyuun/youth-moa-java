# admin · 사용자 상세 (발급 직후 상태) 계약

A5-1-e2e Part 1 Q3 (2026-10-16) 신설. 관리자 사용자 상세 (`/admin/users/{id}`) 를 **임의 시점이 아닌 "스태프 발급 직후 리다이렉트된 상태"** 에서 검사하는 전용 계약.

- 기계 계약: `e2e/contracts/admin-user-detail-after-issue.ts`
- 러너 spec: `e2e/tests/visual-admin-user-detail-after-issue.spec.ts`

## 왜 별도 계약인가

기존 `admin-user-detail.ts` 는 "seed1 상세로 이동 후 임의 상태" 를 전제한다. A5-1 발급 flash (초기 password 카드 · 메일 성공 배너 · 메일 실패 배너) 는 **flashInitialPassword / flashMailSent / flashMailFailed** 가 심어진 1회성 리다이렉트 상태에서만 렌더되므로, 기존 계약에 `optional: true` 로 끼워 넣으면 기대값이 모호해지고 성공·실패 경로의 상호배타 조건을 명시할 수 없다.

따라서 러너 spec 이 발급 flow 를 완주한 뒤 "성공" 또는 "실패 fallback" 상태로 갈라져 각각의 계약을 돌리는 구조로 분리했다.

## prototype 전제 (deviation)

A5-1 발급 flash 섹션 (성공 배너 · 실패 배너 · 초기 password 카드) 은 `admin/prototype.html` 에 **명시되지 않은 추가 UI** 이다. A5-1 본체 spec 에서 보안 정책 (Q3 A — 메일 발송 성공 시 password 는 메일에만 담김, 실패 시에만 화면 fallback) 에 따라 도입된 요소라, 본 계약 역시 **prototype 미명시 deviation 을 전제** 한다.

- 출처: `templates/admin/user/detail.html` L40~98 (flash 렌더 블록)
- 정책: `docs/specs/A5-1-admin-staff-management.md` + `docs/specs/A5-1-e2e-admin-staff-management.md` §5

## 2 variant

### mail-sent (`adminUserDetailAfterIssueMailSentContract`)

| 조건 | 기대 |
|---|---|
| `[data-testid="admin-user-mail-sent"]` | 노출 (P0) |
| 배너 첫 문단 텍스트 | `초대 메일이 발송되었어요` (P1) |
| `[data-testid="admin-user-initial-password"]` | 미노출 — 메일 성공 시 password 는 메일에만 (P0) |
| `[data-testid="admin-user-mail-failed"]` | 미노출 (P1) |
| `button[data-testid="admin-user-reset-password-btn"]` | 노출 (P0) |
| `.admin-flash.admin-flash--error` | 미노출 (P1) |

### mail-fallback (`adminUserDetailAfterIssueMailFallbackContract`)

| 조건 | 기대 |
|---|---|
| `[data-testid="admin-user-mail-failed"]` | 노출 (P0) |
| `[data-testid="admin-user-mail-sent"]` | 미노출 (P1) |
| `[data-testid="admin-user-initial-password"]` | 노출 (P0) |
| `[data-testid="admin-user-initial-password-value"]` | value 비어있지 않음 (P0) |
| `button[data-testid="admin-user-reset-password-btn"]` | 노출 (P0) |

## 러너 세팅 순서

러너 spec 이 아래 순서를 **반드시** 지켜야 한다. 특히 fallback variant 는 finally 로 force-fail OFF 복구를 보장해야 한다 — 미복구 시 후속 spec 이 전부 fallback 경로로 오염됨.

### mail-sent

1. SYSTEM_ADMIN 로그인
2. `/admin/users/new` 발급 폼 입력 (유니크 이메일 — `Date.now()` 패턴)
3. submit → 302 `/admin/users/{id}` 리다이렉트 대기
4. `runContract` 호출

### mail-fallback

1. `POST /__test__/mail/force-fail?enabled=true` (204)
2. try 블록 안에서 1~4 (mail-sent 와 동일)
3. finally 블록에서 `POST /__test__/mail/force-fail?enabled=false` (204) — 반드시

## 실행

```bash
cd e2e
BASE_URL=http://localhost:8090 \
    npx playwright test --project=contracts visual-admin-user-detail-after-issue
```

갭 리포트는 `e2e/gap-reports/gap-admin-user-detail-after-issue-mail-sent.md` · `gap-admin-user-detail-after-issue-mail-fallback.md` 로 생성된다.

## 다음 작업

- Part 2 (Q4 — `/password/change` 계약 신설) 수령 시 `docs/design-contracts/password-change.md` 가 별도로 추가될 예정.
- Q4 B 로 결정되면 본 계약만으로 발급~강제 변경 왕복이 커버되고 별도 신설은 생략.
