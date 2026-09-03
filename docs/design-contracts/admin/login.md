# 관리자 로그인 (`/admin/login`) — 서술 계약

- 추출 기준: `docs/00_assets/admin/prototype.html` L60~96 · 2026-09-03 A1 신설
- 관련 기계 계약: `e2e/contracts/admin-login.ts` (~20 assertions)

## 정보 구조

- Auth Header (64px 흰색) — 로고 + ADMIN 뱃지
- 본문 (`min-height:100vh` · center) — 420px 카드
    - h1 "관리자 로그인" (26/700)
    - 서브 카피 "청년모아 관리자 페이지에 오신 것을 환영합니다"
    - 카드 (white / radius 16 / padding 32 / shadow / border 1px `#F0EFF3`)
        - 아이디 label + input
        - 비밀번호 label + input
        - 에러 alert (`?error` 파라미터에서 노출)
        - 로그아웃 alert (`?logout` 파라미터에서 노출)
        - 로그인 버튼 (primary `#3F30E9`)
    - 하단 링크: `아이디 찾기 | 비밀번호 찾기` — 사용자 페이지 재사용 (`/find-id`, `/find-password`)

## 이탈 · 이월

| 항목 | 종류 | 사유 |
|---|---|---|
| 회원가입 버튼 | **deviation** | ADMIN-00 Q8 관리자 signup 미구현. prototype 이탈 확정 |
| 눈 아이콘(비밀번호 가시성) | **미도입** | Auth 카드 단순 유지 (사용자 login 은 있음) |
| 반응형 미디어쿼리 | **deferred: A8** | 정규화 통합 후 진행 |

## CTA

- `POST /admin/login` → Spring Security formLogin (`SecurityConfig#adminSecurityFilterChain`)
    - 성공 → `/admin` (default success url · always)
    - 실패 → `/admin/login?error` + `savedUsername` 세션 보존
- 로그아웃 → `POST /admin/logout` → `/admin/login?logout`
