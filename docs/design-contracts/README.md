# 디자인 계약 (Design Contracts)

프로토타입 갭이 반복 발생하는 구조적 원인을 제거하기 위한 장치. 2026-07-28 도입.

## 왜 만들었나

기존 방식은 매 세션 `prototype.tsx` (2,733줄) 를 에이전트가 새로 정독해서 갭을 정성 판단했다. 문제:

1. **재현되지 않음** — 같은 화면도 세션마다 다른 갭이 나온다. 기준이 고정 수치가 아니라 매번 재해석되는 원문이기 때문
2. **정량값을 놓침** — 스크린샷 육안 비교로는 `width: 400 vs 460`, `font-size: 34 vs 42` 를 못 잡는다 (2026-07-27 batch2 학습)
3. **회귀를 못 막음** — "이제 일치한다" 는 baseline 이 어디에도 고정되지 않아, 다음 수정이 되돌려놔도 사용자 눈에 띌 때까지 아무도 모른다
4. **규칙 추가의 한계 수익** — CLAUDE.md 에 시각 대조 규칙을 신설(7/22)했는데도 7/27 전수 스캔에서 60갭이 나왔다. 산문 규칙을 늘릴수록 개별 준수율은 떨어진다

## 구조 — 2개 레이어로 분리

기준을 "기계가 검사할 수 있는 것" 과 "사람·에이전트가 판단할 것" 으로 쪼갠다.

| 레이어 | 위치 | 담는 것 | 검증 주체 |
|---|---|---|---|
| **기계 계약** | `e2e/contracts/<screen>.ts` | px·색·폰트·개수·텍스트 등 **단정 가능한 수치** | `e2e/tests/visual-<screen>.spec.ts` 가 자동 assert |
| **서술 계약** | `docs/design-contracts/<screen>.md` | 아키텍처·상태머신·CTA 라우팅 등 **판단이 필요한 구조** | 사람 / ym-spec·ym-verify 가 읽음 |

핵심은 **기계 계약이 매 PR CI 에서 자동 실행**된다는 점이다. 정량 갭은 더 이상 사람이 발견하지 않는다.

## 계약 현황 (2026-07-28)

공통 정책은 [POLICY.md](POLICY.md) 를 먼저 본다. 아래는 2026-07-28 정책 반영 후 수치.

| 화면 | 경로 | 기계 계약 | 서술 계약 | 검사 결과 |
|---|---|---|---|---|
| 홈 | `/` | `home.ts` | `home.md` | 22/41 · 갭 **19** (P0 2) |
| 프로그램 목록 | `/programs` | `programs.ts` | `programs.md` | 58/75 · 갭 **17** |
| 프로그램 상세 | `/programs/{id}` | `program-detail.ts` | `program-detail.md` | 90/101 · 갭 **11** |
| 청년센터 | `/centers` | `centers.ts` | `centers.md` | 80/98 · 갭 **18** |
| 헤더·푸터 (공통) | `/programs` | `common.ts` | `common.md` | 95/107 · 갭 **12** |
| 헤더·푸터 (홈 transparent) | `/` | `common.ts` | `common.md` | 16/16 · 갭 **0** ✅ |
| 개인정보처리방침 | `/privacy` | `policy.ts` | `legal-pages.md` | 10/10 · 갭 **0** ✅ |
| 이용약관 | `/terms` | `policy.ts` | `legal-pages.md` | 8/8 · 갭 **0** ✅ |
| 이메일 무단 수집거부 | `/email-policy` | `policy.ts` | `legal-pages.md` | 8/8 · 갭 **0** ✅ |
| **합계** | | | | **361/438 · 갭 77 · 의도적 이탈 10 · 이월 4** |

수정 착수 권장 순서: **공통(12) → 홈(19, P0 포함) → 목록(17) → 상세(11) → 센터(18)**. 헤더·푸터는 전 화면에 렌더되므로 나중에 고치면 그 전에 등록한 화면별 스크린샷 baseline 이 모두 무효화된다.

미착수: `/login` · `/signup` · `/programs/{id}/apply` · `/apply/complete` · `/notices` · `/notices/{id}` · `/mypage` (4탭) · `/mypage/profile/edit` · `/notifications`

### admin 계약 (2026-09-03 A1 신설)

관리자 트랙(A1~A9) 계약은 [`admin/README.md`](admin/README.md) 참조. `admin-login` · `admin-shell` · `admin-dashboard` 세 계약.

## 실행

```bash
# 전체 계약 검사 (기능 E2E 와 분리된 프로젝트)
cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts

# 단일 화면
cd e2e && BASE_URL=http://localhost:8090 npx playwright test visual-home
```

`--project=contracts` 는 `visual-*.spec.ts` 만 잡고, 기능 E2E(`--project=chromium`, 65 tests)와 섞이지 않는다. **기능 E2E 는 green 유지가 원칙이고 계약 검사는 갭이 남아 있는 동안 의도적으로 red** 이므로 반드시 분리해서 돌린다. CI 에서는 계약 검사가 `continue-on-error` 논블로킹 스텝이며, 갭 리포트를 `design-contract-gaps` 아티팩트로 올린다.

## 사용법

### 화면 작업을 시작할 때

`prototype.tsx` 원문 대신 **해당 화면의 계약 2개 파일을 읽는다.** 원문 정독은 계약을 새로 만들거나 갱신할 때만 한다.

### 갭을 확인할 때

```bash
cd e2e && BASE_URL=http://localhost:8090 npx playwright test visual-home
```

soft assertion 이라 **한 번 실행하면 통과·실패 전 항목이 리스트로 나온다.** 이 출력이 곧 갭 리포트다.

### 계약을 고칠 때

계약 수정은 **prototype 이 바뀌었을 때만** 한다. 구현이 계약과 다르다고 계약을 구현에 맞추면 장치 전체가 무의미해진다. 의도적으로 prototype 을 벗어나기로 결정했다면 계약의 `deviation` 필드에 사유를 남긴다 (검사에서 제외되고, 왜 벗어났는지가 기록으로 남는다).

## 상태 표기

각 계약 파일 상단에 다음을 명시한다.

- **추출 기준**: prototype.tsx 라인 범위 + 추출 날짜
- **검증 상태**: `계약만 작성` / `테스트 연결됨` / `스크린샷 baseline 등록됨`

## 로드맵

1. ✅ 홈 화면 파일럿 (2026-07-28)
2. 🟡 13개 사용자 대면 화면 확산 — **5개 완료** (홈·목록·상세·센터·공통), 9개 미착수
3. ✅ CI 편입 — `e2e-playwright.yml` 에 논블로킹 `contracts` 스텝 + 갭 리포트 아티팩트
4. ✅ 공통 정책 확정 — [POLICY.md](POLICY.md) P-1~P-5 + `deviation`/`deferred` 필드 반영
5. ✅ CLAUDE.md 축약 — 시각 대조 산문 규칙을 본 장치 참조로 대체 (917 → 581줄, 커밋 `aa77009`)
6. ⏳ **갭 77건 수정** ← 현재 여기. P0 2건(홈 Hero 정렬) 부터
7. ⏳ 갭이 0 이 된 화면부터 `toHaveScreenshot` baseline 등록 → 픽셀 회귀 차단 + 블로킹 승격
8. ⏳ 남은 9화면 계약 신설 (`/login` `/signup` `/apply` `/apply/complete` `/notices` `/notices/{id}` `/mypage` 4탭 `/mypage/profile/edit` `/notifications`)

## 알려진 함정 (재발 방지)

- **갭 리포트를 `test-results/` 에 쓰지 마라.** Playwright 가 매 실행 시작 시 그 디렉토리를 통째로 지우므로 스펙을 하나씩 돌릴 때마다 다른 화면 리포트가 사라진다. `gap-reports/` 로 분리한 이유다 (2026-07-28 실측).
- **비로그인·로그인 검사를 별도 `test()` 로 쪼개지 마라.** Playwright 는 테스트 실패 시 워커를 재시작하므로 모듈 변수로 결과를 모을 수 없고 리포트가 마지막 상태만 남는다. 한 테스트 안에서 두 상태를 순차 검사한다.
- **`prototype.html` 과 `prototype.tsx` 는 같은 소스다** (단일 파일 React 앱, 컴포넌트 목록 차이 0건, `html 라인 = tsx 라인 + 35`). 둘을 "충돌 시 우선순위" 로 비교하는 절차는 실행할 일이 없다.
- **브라우저가 computed 값을 정규화하는 항목은 계약에 넣지 마라.** `transition` 의 기본 `ease` 생략, `border-width` 의 디바이스 픽셀 반올림(dpr=1 에서 1.5px → 1px) 등은 허위 갭이 된다.
