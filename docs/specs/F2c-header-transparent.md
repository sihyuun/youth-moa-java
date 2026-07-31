# 작업 명세: F2c-header-transparent — 공통 헤더 transparent 모드

> 산출: ym-spec, 2026-07-07. 상태: **`impl_done` — PR #72 (`68df6b6` 260707_F2c_header_transparent)**
> 구현 위치: `main.css` `.site-header--transparent` + `body.is-scrolled` 전환. 홈 스크롤 60px 백색 전환 완료

## ✅ 결정 확정 (2026-07-07, 전부 권장안 채택)

| # | 결정 |
|---|---|
| Q1 | **prototype 채택** — transparent 2모드 구현 (wireframe 백색 고정은 초안, HANDOFF 4.4 명시 기준) |
| Q2 | **`filter: brightness(0) invert(1)` 우선** — 시각 확인에서 어색하면 `logo_white.png` 스왑으로 전환 |
| Q3 | **hero 높이만 556px 로 보정** — 정렬(좌측)·카피(자체 3칩)는 F0e 사용자 결정 존중, 현행 유지 |
| Q4 | **신청 완료 페이지 헤더는 별도 fix 분리** — `fix/apply-complete-header` 작업 큐 등재 (complete.html 주석 오기 정정 포함) |
> 홈 hero 위 투명 → 스크롤 시 백색 전환. 검색(D4)·알림 종(F2/F2b)은 2026-07-06 머지 완료 — 남은 갭이 본 작업.

## 1. 디자인 출처 (3자산 모두 명시)

| 자산 | 위치 | 내용 |
|---|---|---|
| `docs/00_assets/prototype.html` | **line 408~471** `Header` | `transparent = isHome && !scrolled` 분기 — 색상·필터·전환 규격의 절대 기준 |
| 〃 | **line 2405~2465** App 셸 | 스크롤 감지 `scrollTop > 60` → `scrolled`, 헤더 `position:sticky; top:0; z-index:200` |
| 〃 | **line 517~551** HomeScreen hero | hero 가 헤더 뒤로 확장: `height: 488+68`, `marginTop: -68`. 상단 110px darken scrim (line 527) |
| 〃 | line 2427 | `noHeader = ['login','signup','find-id','welcome','error-503','session-expired']` |
| `docs/00_assets/HANDOFF.md` | **4.4 Header** (line 204~211), line 1089 | "배경 2모드: 홈은 투명 시작(hero 위 흰 글자) → 스크롤 시 흰 배경. 그 외 페이지는 항상 흰 배경" |
| `docs/00_assets/prototype.tsx` | line 375~437, 2371·2393 | prototype.html 과 완전 동일 — 충돌 없음 |
| `docs/00_assets/wireframe.png` | 홈 섹션 (x≈20800~23400, y≈900~3100) | 헤더 **백색 고정 바**. transparent 개념 없음 → **Q1** |
| 에셋 | `header_transparent.png` / `header_white.png` | 상태별 스냅샷 (transparent: 백색 로고·텍스트·아이콘) |
| 비교 대상 | `templates/fragments/header.html` (line 12~14 "transparent 모드 미적용" 주석), `templates/index.html` (line 17~44), `static/css/main.css` (line 86~282 헤더 / 307~370 hero) | |

## 1-A. 자산 간 갭

| 항목 | wireframe | prototype.html | tsx/에셋 | 채택 |
|---|---|---|---|---|
| transparent 모드 | **없음** (백색 고정) | 있음 (홈 + 미스크롤) | 있음 | **prototype 채택 제안** — HANDOFF 4.4 명시. wireframe 충돌이므로 **Q1 확인** |
| nav 구성 | 홈·프로그램·공지사항 | 프로그램·청년센터·공지사항 | white.png 에셋은 '홈' 포함 (구버전) | prototype — **이미 구현 완료** (변경 없음) |
| 로고 white 처리 | — | `filter: brightness(0) invert(1)` | `logo_white.png` 별도 파일 존재 | filter 권장 (전환 자연스러움) — **Q2** |
| 우측 구분선 | — | 없음 | 없음 | 미구현 유지 (HANDOFF 텍스트에만 언급) |
| white 모드 그림자 | — | `0 1px 8px rgba(0,0,0,0.06)` | 동일 | 현행 `var(--shadow-sm)` 유지 (토큰 우선) |

## 2. 변경 범위 (파일 단위)

- [ ] `templates/fragments/header.html` — `th:classappend="${currentPage == 'home'} ? 'site-header--transparent'"` + line 12~14 주석 갱신
- [ ] `static/css/main.css` — ① `.site-header--transparent:not(.is-scrolled)` 상태 오버라이드 세트 ② `transition: 300ms ease` ③ hero 헤더 뒤 확장 (`margin-top: calc(-1 * var(--header-h))` + 높이 보정) ④ 상단 110px darken scrim
- [ ] `templates/index.html` — 스크롤 JS 로드 (홈 한정)
- [ ] `static/js/header-scroll.js` — **신규**. `window.scrollY > 60` → `is-scrolled` 토글 + 로드 시 초기 상태 1회 평가 (스크롤 위치 복원 대응)
- Java / Controller / application.yml — **변경 없음**

## 3. 상태표 (transparent ↔ white)

전환 조건: **홈(`currentPage=='home'`) && `scrollY <= 60`** 일 때만 transparent. 전환은 `background · border-color · box-shadow · filter · color` 300ms ease.

| 요소 | transparent | white (현행 유지) |
|---|---|---|
| 헤더 컨테이너 | transparent bg, border 없음, shadow 없음 | surface + 1px border + shadow-sm |
| 로고 심볼 img | `filter: brightness(0) invert(1)` | filter 없음 |
| 로고 텍스트 | `#fff` | `--color-primary` |
| nav 링크 (비활성) | `rgba(255,255,255,0.88)` | `--color-text` |
| nav 링크 (활성) | `#fff` + 하단 2px `#fff` | primary + 2px primary |
| 검색 아이콘 | `rgba(255,255,255,0.85)` | `--color-text-sec` |
| 알림 종 | `rgba(255,255,255,0.85)` | `--color-text-sec` |
| 알림 red dot 테두리 | `1.5px solid transparent` | `1.5px solid var(--color-surface)` |
| 아바타 | bg `rgba(255,255,255,0.25)` / 글자 `#fff` | primary-light / primary |
| 사용자 이름 | `#fff` | `--color-text` |
| chevron ▾ | `rgba(255,255,255,0.8)` | `--color-text-sec` |
| 로그인 아이콘 (비인증) | `#fff` | primary |
| 드롭다운·알림 패널 | **변경 없음** (백색 패널 그대로) | 〃 |

**hero 연계 변경 (홈 한정):**

| 항목 | prototype | 현재 |
|---|---|---|
| hero 헤더 뒤 확장 | `margin-top: -68px`, 높이 `488+68=556px` | 별도 섹션, `min-height: 460px` |
| 상단 darken scrim | `linear-gradient(rgba(0,0,0,0.28), transparent)` height 110px | 없음 (tint + 하단 darken 만) |
| 헤더 z-index | 헤더(sticky) > hero | 이미 충족 |

## 4. 갭 리스트

| # | 항목 | 현재 | prototype | 우선순위 |
|---|---|---|---|---|
| 1 | transparent 모드 전체 (색상 13종) | 없음 | §3 상태표 | **높음 (핵심)** |
| 2 | 스크롤 감지 JS | 없음 | `>60px` 토글 + 초기화 | **높음** |
| 3 | hero 헤더 뒤 확장 (-68px/556px) | 분리 렌더 460px | line 519 | **높음** (1 의 전제) |
| 4 | hero 상단 110px darken scrim | 없음 | line 527 | **높음** (가독성) |
| 5 | 전환 transition 300ms | 0.15s | 300ms ease | 중간 |
| 6 | `application/complete.html` 헤더 미포함 (주석이 사실과 다름 — prototype `noHeader` 에 apply-complete 없음 → 헤더 표시가 정답) | 헤더 없음 | 헤더 있음 | 낮음 — **Q4, 별도 분리 권장** |

**완료 확인 (변경 불필요):** 68px sticky 헤더 · 로고+청년모아 · nav 3종 + 활성 underline · 검색(D4) · 알림 종+dot+패널(F2/F2b) · 아바타 드롭다운 · 비인증 로그인 아이콘 · noHeader 페이지 (signup 은 사용자 결정으로 헤더 포함 — 문서화된 의도적 편차)

## 5. 검증 시나리오 (ym-qa)

### 정적
- `.\gradlew.bat compileJava` 통과 (Java 무변경이나 회귀 확인) + Controller 테스트 전체 회귀

### 동적 (curl / preview)
- `GET /` 200 + `site-header--transparent` 클래스 존재
- `GET /programs` 200 + `site-header--transparent` **부재**
- `GET /css/main.css` 200 + `site-header--transparent` / `is-scrolled` 룰 grep
- `GET /js/header-scroll.js` 200
- `th:*` / `${...}` 잔존 없음

### 시각 (사용자 영역)
1. 홈 최초 진입: 투명 헤더 + hero 0px 시작 + 백색 로고·nav (상단 scrim 가독)
2. 60px 스크롤: 300ms 백색 전환, 복귀 시 역전환
3. 스크롤 중간 새로고침: 즉시 백색 (투명 플래시 없음)
4. transparent 상태 종 hover·아바타 hover 패널 정상
5. 서브 페이지 항상 백색
6. 로고 `filter: invert` 결과 확인 (어색하면 Q2 스왑 전환)

## 6. 의존성
- 선행 없음. hero CSS 는 F0e 크로스페이드 산출물 수정이므로 `hero-rotator.js` 회귀 확인 필요

## 7. 작업 큐 메타
- 작업 ID: F2c-header-transparent / 우선순위: 중 (헤더 70% → 95%+ 예상) / 추정: 1 PR / 상태: spec_done

## 사용자 결정 필요 질문

- **Q1. wireframe ↔ prototype 정책 충돌 확인** — wireframe 은 백색 고정. HANDOFF 4.4 가 2모드 명시라 prototype 채택 제안, 규칙상 wireframe 충돌은 확인 필수. **(제안: prototype 채택)**
- **Q2. 로고 white 처리** — `filter: brightness(0) invert(1)` (전환 자연) vs `logo_white.png` 스왑 (결과 확실). **(제안: filter 우선, 시각 확인 후 어색하면 스왑)**
- **Q3. hero 높이·정렬** — prototype 556px + 중앙 정렬 + 인기검색어 5칩 vs 현행 460px + 좌측 정렬 + 자체 카피 3칩 (F0e 사용자 결정). **(제안: 높이만 556px, 정렬·카피 현행 유지)**
- **Q4. 신청 완료 페이지 헤더** — prototype 은 헤더 표시, 현행 없음 + 주석 오기. **(제안: 별도 fix 분리)**
