# 청년모아 모바일 반응형 개발 가이드 (Claude Design 산출)

> **원본**: Claude Design 프로젝트 `05126ce9-ea5e-4e18-9058-db7682f64b95` "적응형 모바일 화면 설계" (2026-08-20 산출)
> **시안 파일**: `Mobile Responsive.dc.html` (프로젝트 URL 에서 카드 `1a`~`1p` 참조)
> **연관 문서**: `mobile-responsive.md` (개발 브리프) · `mobile-responsive-followup.md` (검토·보완 지시서)

---

## 0. 대전제

| 원칙 | 내용 |
|---|---|
| 데스크톱 계약 불변 | `≥768px` 에서 렌더 결과가 1px 도 바뀌면 안 된다. 모든 신규 규칙은 `@media (max-width: 767px)` 안에만 작성 |
| 마크업 최소 변경 | HTML 구조 · 계층 · 순서 유지. 예외는 §5 목록 (햄버거 · 탭바 · 뷰 토글 등 4건) 뿐 |
| 토큰 재사용 | 새 색 · radius 신설 금지. 값이 필요하면 `main.css :root` 의 기존 `--color-*` / `--radius-*` / `--shadow-*` 사용 |
| 접근성 | 터치 타겟 최소 44×44 · 본문 텍스트 최소 14px (라벨 · 메타 12px 허용, 11px 은 뱃지·타임스탬프만) |
| 인터랙션 | HTMX 동작은 데스크톱과 동일. 모바일 전용 JS 는 드로어 open/close · 바텀시트 · 뷰 토글 3개 스크립트뿐 |

### 기존 코드에 이미 있는 것 (재사용 대상)

| 위치 | 내용 |
|---|---|
| `main.css` L5401 `@media (max-width:767px)` | 콘텐츠형 `.modal-card` → 바텀시트 승격 + grabber `::before`. **모든 신규 시트는 이 패턴 재사용** |
| `main.css` L6323 `@media (max-width:768px)` | 정책 페이지 + 사이트 헤더 임시 대응 (PR #152). `.header-nav{display:none}` 유지, 여기에 햄버거 추가 |
| `main.css` L5726/5729 `@media (max-width:900px/600px)` | 청년센터 지도 관련 잔여 규칙. §4.4 작업 시 767 기준으로 정리 |

> ⚠️ **경계값 통일 필요**: 현재 `767px`(모달)과 `768px`(정책·헤더)가 섞여 있다. 첫 PR 에서 **전부 `max-width: 767px`** 로 통일한다 (`768px` 은 태블릿 시작점이므로 `max-width:768px` 은 태블릿 1px 을 모바일로 오염시킨다).

---

## 1. 브레이크포인트 정책

```css
/* 프로젝트 표준 — 신규 브레이크포인트 추가 없음 */
/*  ≤ 767px   mobile   : 이 문서의 대상. 1열 · 탭바 · 시트 · 가로 스크롤 */
/*  768~1023  tablet   : 기존 데스크톱 레이아웃 유지 (grid 열 수만 자연 축소) */
/*  ≥ 1024px  desktop  : 현재 계약 그대로 */
/*  ≥ 1440px            : --content-max 상한 도달 */
```

작성 규칙: **mobile-only override** 방식. 기본 CSS(데스크톱)를 건드리지 않고 아래 블록에만 추가한다.

```css
@media (max-width: 767px) {
  :root {
    --header-h: 56px;      /* 68 → 56 */
    --content-px: 16px;    /* 80 → 16 */
    --content-max: none;
    --tabbar-h: 58px;      /* 신규 (모바일 전용) */
  }
  body { padding-bottom: calc(var(--tabbar-h) + env(safe-area-inset-bottom)); }
  /* 하위 화면(탭바 없음)에는 .no-tabbar 클래스로 padding-bottom:0 */
}
```

| 구간에서 바뀌는 것 | ≤767 |
|---|---|
| 컨테이너 | `max-width:none` · 좌우 padding 16px |
| 그리드 | 3~4열 → 1열 (즐겨찾기만 2열) |
| 가로 스크롤 | 프로그램 캐러셀 · pill 그룹 · 관련 프로그램 · 공간 카드 |
| 팝오버 · 모달 | 하단 시트 (`.modal-card` 기존 패턴) |
| 내비게이션 | 하단 탭바 + 우측 드로어 |
| 폼 컨트롤 높이 | 42 → 48px (버튼 CTA 50px) |

---

## 2. 공통 컴포넌트 CSS 스니펫

> 원본 가이드 §2 참조. 시안 이미지·CSS 스니펫 전체는 Claude Design 프로젝트의 `Mobile Responsive.dc.html` 카드 `1a` 를 열어 확인.

### 2.1 헤더 · 2.2 드로어 · 2.3 하단 탭바 · 2.4 푸터

- 헤더 높이 68 → 56, `.header-menu-btn` 44×44 추가 (햄버거)
- 드로어: 우측 슬라이드 300×84vw · backdrop rgba(12,8,32,.45) · 220ms cubic-bezier
- 탭바: 4항목 (홈·프로그램·청년센터·마이) · 루트 4화면 한정 · safe-area-inset-bottom 처리
- 푸터: 세로 스택 (로고 → 링크 → Copyright → SNS), 링크 wrap 8/14 gap

### 2.5 카드 · 그리드 (§가이드 §2.5)

`.program-grid` 1열 12gap · 이미지 160 · CTA 44×full
가로 스크롤: `.home-program-row` 236 카드 · `.home-space-row` 200×150 · 스크롤바 숨김

### 2.6 폼 · 2.7 sticky CTA · 2.8 모달→바텀시트 · 2.9 pill 가로스크롤 · 2.10 테이블→카드

**폼**: 48px 입력 · label 위 배치 · `.form-inline` [입력 flex:1 + 부가 80~104]
**Sticky CTA**: `[이전 104]` + `[다음 flex:1 48]`
**시트**: `.filter-popover` bottom fixed · 상단 grabber (36×4)
**Pill 스크롤**: `.mypage-tabs / .notice-tabs / .filter-left / .notif-filter` 가로 스크롤
**공지 테이블**: `.notice-thead` hidden · row block 카드 · No/조회수 컬럼 접기

---

## 3. 화면별 media query 명세

시안 카드 id → 라우트 → 작업 내용 (원본 가이드 §3 전체 참조).

| 시안 id | 라우트 | 핵심 변화 |
|---|---|---|
| `1a` | 공통 셸 | 헤더 56 · 드로어 · 탭바 · 푸터 세로 |
| `1b` | `/` 홈 | Hero 400·좌정렬 · Quick Stats 3분할 · 프로그램 캐러셀 236px · 공지 세로 · 공간 캐러셀 |
| `1c` | `/programs` | 필터 sticky · pill 스크롤 · 필터 시트 · 1열 그리드 · 페이지네이션 36 |
| `1d` | `/programs/{id}` | 이미지→정보 stack · sticky CTA 재구성 · 관련 캐러셀 · 지도 시트 |
| `1e` `1f` | `/apply` · `/apply/complete` | 스텝 축소 · sticky nav · 완료 아이콘 68 |
| `1g` | `/centers` | 뷰 세그먼트 (지도/리스트) · 지도 430 · peek 시트 · 리스트 페이지 스크롤 |
| `1h` `1i` | `/notices` · `/notices/{id}` | 카드 리스트 · pill 스크롤 · 상세 padding 18/16 |
| `1j` `1k` `1l` | `/login` · `/find-*` · `/signup` | 100% 입력·48 · CTA 50 · 성별 pill flex:1 · 약관 시트 |
| `1m` `1n` | `/mypage` · `/mypage/profile/edit` | 요약 3블록 세로 · 탭 4개 스크롤 · 즐겨찾기 2열 · 편집 sticky |
| `1o` | `/notifications` | 종 → 페이지 이동 · sticky 그룹 헤더 · 안읽음 배경 |
| `1p` | `/search` | back+입력 헤더 · chip · 결과 카테고리 3섹션 |

**정책 페이지** (`/privacy` · `/terms` · `/email-policy`) 는 260819 PR #149 로 이미 완료. 경계값만 768→767 통일.

---

## 4. 마크업 변경 필요 목록 (5건)

CSS 만으로 불가능해 **Thymeleaf 템플릿 수정이 필요한 것은 아래 5건뿐**이다.

| # | 파일 | 변경 | 사유 |
|---|---|---|---|
| M1 | `fragments/header.html` | `.header-right` 끝에 `<button class="header-menu-btn" aria-label="전체 메뉴" aria-expanded="false" aria-controls="site-drawer">` (햄버거 SVG) 추가 | 드로어 트리거. 데스크톱은 `display:none` |
| M2 | `fragments/drawer.html` **(신규)** | 드로어 패널 전체 + 레이아웃에 `<div th:replace="~{fragments/drawer :: drawer(${currentPage})}">` 삽입 | 데스크톱에 없는 컴포넌트 |
| M3 | `fragments/tabbar.html` **(신규)** | 탭바 4항목. 루트 4페이지 (`index`, `program/list`, `center/list`, `mypage/index`) 에만 include | 모바일 전용 내비 |
| M4 | `center/list.html` | 지도/리스트 세그먼트 토글 `<div class="centers-view-toggle">` 추가 (데스크톱 `display:none`) | 두 패널을 배타 표시하기 위한 상태 컨트롤 |
| M5 | `program/list.html` | 선택 필터 요약 chip 컨테이너 `<div class="filter-applied-chips">` 추가 (데스크톱은 `.filter-left` 가 이 역할) | 시트로 필터를 옮기면 현재 조건이 화면에서 사라짐 |

부수 변경: `notice/list.html` 조회수 셀에 `.notice-cell-meta` 래퍼 — 가능하면 CSS `::after` 로 해결.

**신규 JS**: `static/js/site-drawer.js`, `static/js/bottom-sheet.js`(필터·주소 시트 공용), `static/js/centers-view-toggle.js`

---

## 5. 신규 아이콘 · 자산 요청

| 이름 | 용도 | 상태 |
|---|---|---|
| `menu` (햄버거) | 헤더 M1 | 24×24 stroke 1.7 3선 — 시안 SVG 그대로 사용 가능 |
| `close` | 드로어·시트 | ✅ 기존 재사용 |
| `home` | 탭바 | 24×24 stroke 1.6~1.7 집 실루엣 — **신규 필요** (시안 SVG 있음) |
| `grid` `pin` `user` `bell` `search` | 탭바·헤더 | ✅ 기존 재사용 |
| `refresh` | 지도 재검색 | 24×24 stroke 1.8 원형 화살표 — **신규 필요** |
| `crosshair` | 지도 내 위치 | 24×24 stroke 1.6 — **신규 필요** |
| grabber | 시트 손잡이 | CSS `::before` 36×4 — 이미지 불필요 |
| 히어로 배경 | 모바일 crop | 기존 `banner_01~03.png` + `object-position:50% 40%` |

`fragments/icons.html` 에 3종 (`home` · `refresh` · `crosshair`) 신규 추가. P-3 SVG 아이콘 정책 준수.

---

## 6. E2E 검증 포인트 · 7. PR 분할 순서

원본 가이드 §6·§7 참조. 요점:

**공통 assertion**: `scrollWidth ≤ 375` · 헤더 56 · nav hidden · 탭바 조건부 노출 · 터치 타겟 44 · 본문 14px+ · 데스크톱 회귀 유지.

**PR 순서**:

| PR | 범위 | 의존 |
|---|---|---|
| P0 | 경계값 통일 · 토큰 · sticky · 시트 유틸 · pill 유틸 | — |
| P1 | 헤더 햄버거 · 드로어 · 탭바 · 푸터 | P0 |
| P2 | 홈 | P1 |
| P3 | 프로그램 목록 (필터 시트 · M5) | P0·P1 |
| P4 | 프로그램 상세 · 신청·완료 | P0·P1 |
| P5 | 공지 목록·상세 (테이블→카드) | P0·P1 |
| P6 | 로그인·회원가입·계정찾기 | P0 |
| P7 | 마이·프로필 편집 | P0·P1 |
| P8 | 청년센터 (뷰 토글 M4) | P0·P1 |
| P9 | 알림·검색 | P1 |
| P10 | 아이콘 3종 · 잔여 900/600 정리 · HANDOFF 갱신 | 전체 |

## 데스크톱 계약 변경 flag (승인 필요 · 원본 §7)

| 항목 | 데스크톱 | 모바일 | 영향 |
|---|---|---|---|
| 알림 종 동작 | 드롭다운 | `/notifications` 이동 | 모바일 전용 분기, 데스크톱 무변경 |
| 필터 적용 시점 | 즉시 | `[n건 보기]` 탭 | 모바일 전용 분기 |
| 폼 높이 | 42px | 48px | 모바일 전용 |
| 헤더 높이 | 68px | 56px | 모바일 전용 (`--header-h` override) |

네 항목 모두 `@media (max-width:767px)` 내부에서만 발생하므로 데스크톱 기계 계약(`e2e/contracts/*.ts`)에는 변경이 없다.

---

**전체 원본**: Claude Design 프로젝트 https://claude.ai/design/p/05126ce9-ea5e-4e18-9058-db7682f64b95 의 `MOBILE-RESPONSIVE-GUIDE.md` 참조. 이 파일은 이식 편의를 위한 요약본이며, 세부 스니펫·시안 이미지는 원본에서 확인.
