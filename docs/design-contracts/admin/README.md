# admin 계약

관리자 트랙(A1~A9) 화면의 디자인 계약. 사용자 트랙 계약과 파일은 분리하지만 **툴체인은 공유** — 동일한 `e2e/contracts/*.ts`, `runner.ts`, `visual-*.spec.ts` 를 사용한다.

## 현재 화면

| 화면 | 경로 | 기계 계약 | 서술 계약 | 도입 |
|---|---|---|---|---|
| 관리자 로그인 | `/admin/login` | `e2e/contracts/admin-login.ts` | `docs/design-contracts/admin/login.md` | A1 (2026-09-03) |
| 관리자 shell (다크 헤더) | `/admin` | `e2e/contracts/admin-shell.ts` | `docs/design-contracts/admin/shell.md` | A1 |
| 대시보드 콘텐츠 | `/admin` | `e2e/contracts/admin-dashboard.ts` | `docs/design-contracts/admin/dashboard.md` | A1 |

## prototype 출처

- **`docs/00_assets/admin/prototype.html`** — 최우선. tsx 는 A1 시점 스캐폴드뿐이라 admin 은 html 만 인용한다.
- HANDOFF.md — 색·간격·헤더 정책 (호환 참조)

## 실행

사용자 계약과 동일하게 `--project=contracts` 로 함께 돈다.

```bash
cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts visual-admin-login
cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts visual-admin-shell
cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts visual-admin-dashboard
```

로그인 상태가 필요한 `admin-shell` · `admin-dashboard` 는 spec 파일이 시드 관리자 계정 (`sysadmin@youth-moa.test` / `Admin!234`) 로 selenium form 로그인 후 계약을 돌린다.
