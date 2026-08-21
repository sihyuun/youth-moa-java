# 청년모아 모바일 반응형 개발 가이드 (Claude Design 산출 · rev.2)

> **원본**: Claude Design 프로젝트 `05126ce9-ea5e-4e18-9058-db7682f64b95` "적응형 모바일 화면 설계"
> **시안 파일**: `Mobile Responsive.dc.html` (카드 `1a`~`1r`)
> **연관 문서**: `mobile-responsive.md` (개발 브리프) · `mobile-responsive-followup.md` (검토·보완 지시서)
>
> **rev.2 (2026-08-21)** — F1~F6 반영. 신규 시안 카드 `1q`(상태 규격) `1r`(모션 규격). D1~D4 승인 완료.

---

## rev.2 요약 (rev.1 대비 변경)

**신규 시안 카드 2개**
- `1q` — 상태 규격 (알림 뱃지·탭 active·pill 스크롤 위치)
- `1r` — 모션 규격 (캐러셀 peek·시트 swipe-dismiss)

**F1~F6 확정 사항**

| F | 결정 | 위치 |
|---|---|---|
| **F1** | 숫자 뱃지 (16×16 · `--color-danger` · 흰 1.5px 링 · `99+` 상한) | §2.1 + M6 |
| **F2** | `currentTab` 모델 인터셉터 · 경로 prefix 매칭 · 쿼리스트링 무시 · `/search` `/notifications` `/notices` `/login` 등은 null | §2.3 + M7 |
| **F3** | 로그아웃 즉시 닫음 · 401 핸들러 `/login?redirect=…` · 링크 탭 시 즉시 닫음 (transitionend 대기 X) | §2.2 |
| **F4** | Peek 채택 (dot·arrow 미채택). 236px 카드 + 16px padding → 111px peek. 좌우 mask-image 페이드 (JS 클래스 토글) | §2.5 |
| **F5** | 시트 swipe-down: 100px 거리 or 0.5px/ms 속도 · Full-screen 시트(postcode·약관) 제외 · `prefers-reduced-motion` 시 추종 없이 즉시 닫음 | §2.8 |
| **F6** | `revealActive` (활성 pill 을 좌측 16px 에 물림) + sessionStorage 복원 · `scroll-behavior: smooth` 미사용 | §2.9 + M8 |

**추가 마크업 3건 (M6·M7·M8)**
- M6: `.header-bell-dot` 안읽음 개수 텍스트 + `aria-label`
- M7: 탭바 조건부 include (`th:if="${currentTab != null}"`)
- M8: pill 컨테이너 5곳에 `data-pill-row` 속성 (`status`·`mypage`·`notice`·`notif`·`apply`)

**신규 JS 5개**
- `static/js/site-drawer.js` (P1)
- `static/js/bottom-sheet.js` (P0 · F5 swipe 포함)
- `static/js/centers-view-toggle.js` (P8)
- `static/js/pill-scroll.js` (P0 · F6)
- `static/js/carousel-fade.js` (P2 · F4)

**PR 배치 갱신**
- F1·F2·F3 → **P1**
- F4 → **P2** (홈), 상세 캐러셀은 **P4**
- F5·F6 → **P0** (유틸 확정) · 화면 PR 에서 검증

**D1~D4 승인 완료** (2026-08-21): 알림 종 → 페이지 이동 · 필터 지연 적용 · 폼 48px · 헤더 56px. 모두 mobile-only 분기, 데스크톱 무변경.

**스코프 확정**: admin 모바일 이월 · 다크모드 미대응 · 태블릿 768~1023 데스크톱 유지

---

## 0. 대전제

| 원칙 | 내용 |
|---|---|
| 데스크톱 계약 불변 | `≥768px` 에서 렌더 결과가 1px 도 바뀌면 안 된다. 모든 신규 규칙은 `@media (max-width: 767px)` 안에만 작성 |
| 마크업 최소 변경 | HTML 구조 · 계층 · 순서 유지. 예외는 §4 목록 (햄버거 · 탭바 · 뷰 토글 등 8건) 뿐 |
| 토큰 재사용 | 새 색 · radius 신설 금지. 값이 필요하면 `main.css :root` 의 기존 `--color-*` / `--radius-*` / `--shadow-*` 사용 |
| 접근성 | 터치 타겟 최소 44×44 · 본문 텍스트 최소 14px (라벨 · 메타 12px 허용, 11px 은 뱃지·타임스탬프만) |
| 인터랙션 | HTMX 동작은 데스크톱과 동일. 모바일 전용 JS 는 §3 참조 |

### 기존 코드 재사용

| 위치 | 내용 |
|---|---|
| `main.css` L5401 `@media (max-width:767px)` | `.modal-card` → 바텀시트. **모든 신규 시트는 이 패턴 재사용** |
| `main.css` L6323 `@media (max-width:768px)` | 정책 페이지 + 헤더 임시 대응 (PR #152). P0 에서 767 로 통일 |
| `main.css` L5726/5729 `@media (max-width:900px/600px)` | 청년센터 지도 잔여. P0 에서 767 로 정리 |

> ⚠️ 경계값 통일 필요 — 767/768 혼재 → P0 에서 전부 **`max-width: 767px`** 로 통일 (768 은 태블릿 시작점)

---

## 1. 브레이크포인트 정책 · 토큰

```css
/* ≤ 767px mobile · 768~1023 tablet · ≥ 1024 desktop · ≥ 1440 --content-max 상한 */
@media (max-width: 767px) {
  :root {
    --header-h: 56px;      /* 68 → 56 (D4) */
    --content-px: 16px;    /* 80 → 16 */
    --content-max: none;
    --tabbar-h: 58px;      /* 신규 */
  }
  body { padding-bottom: calc(var(--tabbar-h) + env(safe-area-inset-bottom)); }
  body.no-tabbar { padding-bottom: 0; }  /* 상세·폼·편집 등 탭바 미노출 화면 */
}
```

---

## 2. 공통 컴포넌트 CSS

### 2.1 헤더 + F1 알림 뱃지 (`fragments/header.html` · 시안 `1a` + `1q`)

```css
@media (max-width: 767px) {
  .site-header { height: var(--header-h); }
  .site-header .header-inner { padding: 0 6px 0 16px; }
  .site-header .header-nav { display: none; }
  .site-header .header-user-name { display: none; }
  .site-header .header-logo-img { height: 26px; }
  .site-header .header-right { gap: 0; }
  .site-header .header-icon-btn,
  .site-header .header-menu-btn { width: 44px; height: 44px; }
  .header-bell-dropdown-wrap { display: none !important; }
  .header-user-dropdown { display: none !important; }
  .header-menu-btn { display: inline-flex; }

  /* F1: 안읽음 숫자 뱃지 */
  .header-bell-btn { position: relative; }
  .header-bell-dot {
    position: absolute; top: 7px; right: 6px;
    min-width: 16px; height: 16px; padding: 0 4px; box-sizing: border-box;
    border-radius: 8px; background: var(--color-danger); border: 1.5px solid var(--color-surface);
    color: #fff; font-size: 10px; font-weight: 700; line-height: 1;
    display: flex; align-items: center; justify-content: center;
  }
}
```

| 안읽음 | 표기 |
|---|---|
| 0 | 뱃지 미렌더 |
| 1–9 | 숫자 그대로 |
| 10–99 | 숫자 그대로 (min-width 확장) |
| 100+ | `99+` 고정 |

- 데스크톱 dot 은 그대로 유지
- `aria-label="알림 {n}건 안읽음"` 로 개수 포함

### 2.2 드로어 + F3 인증 전환 (시안 `1a` + `1q`)

기본 드로어 CSS 는 rev.1 그대로. F3 추가 규칙:

| 트리거 | 동작 |
|---|---|
| 드로어 안 로그아웃 | 즉시 닫음 → `/` 이동 → 토스트 |
| 세션 만료 (401) | 닫음 → `/login?redirect={현재경로}` |
| 드로어 안 링크 탭 | 즉시 닫음 (transitionend 대기 X) |
| 뒤로가기 복귀 | 항상 닫힌 상태 (drawer 상태를 history 에 넣지 않음) |

```js
// site-drawer.js
document.body.addEventListener('htmx:responseError', e => {
  if (e.detail.xhr.status === 401) {
    close();
    location.href = '/login?redirect=' + encodeURIComponent(location.pathname);
  }
});
drawer.querySelectorAll('a').forEach(a => a.addEventListener('click', () => close()));
```

### 2.3 하단 탭바 + F2 active 판정 (시안 `1a` + `1q`)

**F2 판정 규칙**: 경로 prefix 매칭, 쿼리스트링 무시.

| 경로 | `currentTab` | 탭바 |
|---|---|---|
| `/` | `home` | O |
| `/programs`, `/programs/{id}`, `/programs/{id}/apply`, `/apply/complete` | `programs` | 루트만 |
| `/centers`, `/centers/{id}` | `centers` | 루트만 |
| `/mypage`, `/mypage?tab=*`, `/mypage/profile/edit` | `mypage` | 루트만 (쿼리 있어도 active) |
| `/search`, `/notifications`, `/notices`, `/login`, `/signup`, 정책 | `null` | X |

```java
// 공통 인터셉터 or @ModelAttribute
String p = request.getRequestURI();
model.addAttribute("currentTab",
  p.equals("/") ? "home"
: p.startsWith("/programs") || p.startsWith("/apply") ? "programs"
: p.startsWith("/centers") ? "centers"
: p.startsWith("/mypage") ? "mypage" : null);
```

- 상세 back 시 별도 탭 복귀 로직 없음 (history.back() 이 자연스러운 이전 경로로 이동)
- `/search` 등 탭바 없음 화면은 어느 탭도 active 아님 ("이전 탭 유지" 미채택)

### 2.4 푸터 · 2.5 카드/그리드 + F4 peek · 2.6 폼 · 2.7 sticky · 2.8 시트 + F5 swipe · 2.9 pill + F6 · 2.10 테이블→카드

**F4 캐러셀 peek** (§2.5 · 시안 `1r`)

Peek 은 추가 CSS 없이 **고정 카드폭 + 컨테이너 padding** 으로 성립.

```
375 − 16(좌 padding) − 236(카드) − 12(gap) = 111px  → 다음 카드 111px 노출
375 − 16 − 200 − 12 = 147px                          → 공간 카드
375 − 16 − 180 − 12 = 167px                          → 관련 프로그램
```

```css
@media (max-width: 767px) {
  .home-program-row, .home-space-row, .detail-related-row {
    scroll-padding-inline-start: 16px;
    mask-image: linear-gradient(to right, #000 0, #000 calc(100% - 40px), transparent 100%);
  }
  .is-scroll-mid { mask-image: linear-gradient(to right, transparent 0, #000 40px, #000 calc(100% - 40px), transparent 100%); }
  .is-scroll-end { mask-image: linear-gradient(to right, transparent 0, #000 40px, #000 100%); }
}
```

```js
// carousel-fade.js
const atStart = el.scrollLeft <= 2;
const atEnd = el.scrollLeft + el.clientWidth >= el.scrollWidth - 2;
el.classList.toggle('is-scroll-mid', !atStart && !atEnd);
el.classList.toggle('is-scroll-end', atEnd);
```

- 카드 1장일 때 (`scrollWidth <= clientWidth`) 마스크 미부착
- `mask-image` 미지원 브라우저는 마스크만 사라지고 스크롤 정상 (점진적 향상)

**F5 시트 swipe-to-dismiss** (§2.8 · 시안 `1r`)

대상: `.filter-popover` · 콘텐츠형 `.modal-card` · 지도 peek 시트. **전체화면 시트 (주소·약관) 제외**.

| 파라미터 | 값 |
|---|---|
| 임계 거리 | `100px` |
| 임계 속도 | `0.5px/ms` |
| 추종 | `translateY` 1:1, 위로는 하한 0 |
| backdrop | `opacity = 0.45 × (1 − dy/240)`, 하한 0.08 |
| 복귀 | `transform 160ms ease` |
| 닫힘 | `translateY(100%)` 220ms |
| 시작 조건 | grabber/헤더 터치 OR 본문 `scrollTop === 0` |

```js
// bottom-sheet.js
let y0, t0, dy = 0;
sheet.addEventListener('touchstart', e => {
  if (!isHandle(e.target) && body.scrollTop > 0) return;
  y0 = e.touches[0].clientY; t0 = performance.now(); sheet.style.transition = 'none';
}, { passive: true });
sheet.addEventListener('touchmove', e => {
  if (y0 == null) return;
  dy = Math.max(0, e.touches[0].clientY - y0);
  sheet.style.transform = `translateY(${dy}px)`;
  backdrop.style.opacity = Math.max(0.08, 0.45 * (1 - dy / 240)) / 0.45;
}, { passive: true });
sheet.addEventListener('touchend', () => {
  sheet.style.transition = 'transform 160ms ease';
  const v = dy / (performance.now() - t0);
  (dy >= 100 || v >= 0.5) ? close() : reset();
  y0 = null; dy = 0;
});
```

- `prefers-reduced-motion` 시 추종 없이 즉시 닫음
- 드래그로 닫아도 필터 미적용 (D2 지연 적용 — 취소와 동일)

**F6 pill 스크롤 복원** (§2.9 · 시안 `1q`)

```js
// pill-scroll.js
const KEY = 'pillScroll:' + location.pathname;

function revealActive(row) {
  const a = row.querySelector('.is-active, .active, [aria-selected="true"]');
  if (!a) return;
  const target = a.offsetLeft - 16;
  if (a.offsetLeft < row.scrollLeft || a.offsetLeft + a.offsetWidth > row.scrollLeft + row.clientWidth) {
    row.scrollLeft = Math.max(0, target);
  }
}

document.body.addEventListener('htmx:beforeSwap', () => {
  const m = {};
  document.querySelectorAll('[data-pill-row]').forEach(r => m[r.dataset.pillRow] = r.scrollLeft);
  sessionStorage.setItem(KEY, JSON.stringify(m));
});
document.body.addEventListener('htmx:afterSwap', restore);
window.addEventListener('DOMContentLoaded', restore);

function restore() {
  const m = JSON.parse(sessionStorage.getItem(KEY) || '{}');
  document.querySelectorAll('[data-pill-row]').forEach(r => {
    if (m[r.dataset.pillRow] != null) r.scrollLeft = m[r.dataset.pillRow];
    revealActive(r);
  });
}
```

- `data-pill-row` 부착 5곳: `status` · `mypage` · `notice` · `notif` · `apply`
- `scroll-behavior: smooth` 미사용 (복원 시 이동으로 오해되지 않도록)

**기본 스니펫** (푸터·카드·폼·sticky·시트·pill·테이블→카드) 은 rev.1 §2.4~§2.10 참조 — 변경 없음.

---

## 3. 화면별 media query 명세

시안 카드 id → 라우트 매핑 요약 (rev.1 §3 상세 그대로):

| 카드 | 라우트 | 핵심 |
|---|---|---|
| `1a` | 공통 셸 | 헤더 56 · 드로어 · 탭바 · 푸터 세로 |
| `1b` | `/` 홈 | Hero 400·좌정렬 · Quick Stats 3분할 · 프로그램 캐러셀 236 + F4 peek |
| `1c` | `/programs` | 필터 sticky · pill 스크롤 · 필터 시트 (F5) · 1열 그리드 |
| `1d` | `/programs/{id}` | 이미지→정보 stack · sticky CTA · 관련 캐러셀 + F4 · 지도 시트 |
| `1e` `1f` | `/apply` · `/apply/complete` | 스텝 축소 · sticky nav · 완료 아이콘 68 |
| `1g` | `/centers` | 뷰 세그먼트 (지도/리스트) · peek 시트 |
| `1h` `1i` | `/notices` · `/notices/{id}` | 카드 리스트 · pill 스크롤 · 상세 padding 18/16 |
| `1j` `1k` `1l` | `/login` · `/find-*` · `/signup` | 100% 입력·48 · CTA 50 · 성별 pill flex:1 · 약관 시트 |
| `1m` `1n` | `/mypage` · `/mypage/profile/edit` | 요약 3블록 세로 · 탭 4개 스크롤 (F6) · 즐겨찾기 2열 |
| `1o` | `/notifications` | 종 → 페이지 · sticky 그룹 헤더 |
| `1p` | `/search` | back+입력 헤더 · chip · 결과 카테고리 3섹션 |
| **`1q`** | 상태 규격 (전 화면) | F1 뱃지 · F2 탭 active · F6 pill 복원 |
| **`1r`** | 모션 규격 (전 화면) | F4 캐러셀 peek · F5 시트 swipe |

**정책 페이지** (`/privacy` · `/terms` · `/email-policy`) 완료. 경계값만 768→767 통일.

---

## 4. 마크업 변경 필요 목록 (8건)

| # | 파일 | 변경 | 담당 PR |
|---|---|---|---|
| M1 | `fragments/header.html` | `.header-menu-btn` (햄버거) 추가 | P1 |
| M2 | `fragments/drawer.html` (신규) | 드로어 패널 + 레이아웃 include | P1 |
| M3 | `fragments/tabbar.html` (신규) | 탭바 4항목 · 루트 4페이지에만 include | P1 |
| M4 | `center/list.html` | 뷰 세그먼트 토글 (지도/리스트) | P8 |
| M5 | `program/list.html` | 선택 필터 요약 chip 컨테이너 | P3 |
| **M6** | `fragments/header.html` | `.header-bell-dot` 텍스트/aria-label + `unreadCount` 조건 | P1 (F1) |
| **M7** | 레이아웃 include 지점 | 탭바 조건부 include (`th:if="${currentTab != null}"`) | P1 (F2) |
| **M8** | pill 컨테이너 5곳 | `data-pill-row` 속성 부착 | P0 유틸, 화면 PR 확인 (F6) |

부수: `notice/list.html` 조회수 셀 `.notice-cell-meta` 래퍼 — CSS 로 해결 시도.

**신규 JS 5개**: `site-drawer.js` · `bottom-sheet.js` (F5) · `centers-view-toggle.js` · `pill-scroll.js` (F6) · `carousel-fade.js` (F4)

---

## 5. 신규 아이콘 (3종)

`fragments/icons.html` 에 24×24 stroke `currentColor` 규격으로 추가:
- `home` (탭바)
- `refresh` (지도 재검색)
- `crosshair` (지도 내 위치)

`menu`(햄버거) 는 시안 인라인 SVG 그대로 사용.

---

## 6. E2E 검증

**공통 assertion**:
1. `scrollWidth ≤ 375` (body)
2. 헤더 56 · `.header-nav` hidden · `.header-menu-btn` visible
3. 탭바 루트 4페이지에서만 visible
4. 터치 타겟 44px+
5. 본문 14px+
6. 데스크톱 계약 회귀 유지

**화면별 spec** (원본 §6 참조):
- `home-mobile.spec` · `programs-mobile.spec` · `program-detail-mobile.spec` · `centers-mobile.spec` · `notices-mobile.spec` · `auth-mobile.spec` · `apply-mobile.spec` · `mypage-mobile.spec` · `drawer-mobile.spec` (F3 확장)
- **F 신규 spec 5**: `badge-mobile.spec` (F1) · `tabbar-active.spec` (F2) · `carousel-mobile.spec` (F4) · `sheet-swipe.spec` (F5) · `pill-scroll.spec` (F6)

---

## 7. PR 분할 순서

| PR | 범위 | 의존 | F 배치 |
|---|---|---|---|
| **P0** | 경계값 통일 · 토큰 · `.sticky-actions` · 시트/pill 유틸 (bottom-sheet.js · pill-scroll.js) | — | F5·F6 유틸 |
| **P1** | 헤더 햄버거(M1·M6) + 드로어(M2) + 탭바(M3·M7) + 푸터 · `currentTab` 인터셉터 | P0 | F1·F2·F3 |
| **P2** | 홈 (`carousel-fade.js`) | P1 | F4 홈 |
| **P3** | 프로그램 목록 (필터 시트 · M5) | P0·P1 | F5·F6 검증 |
| **P4** | 프로그램 상세 (sticky CTA · 관련 캐러셀) + 신청 폼·완료 | P0·P1 | F4 상세 |
| **P5** | 공지 목록·상세 (테이블→카드) | P0·P1 | F6 검증 |
| **P6** | 로그인·회원가입·계정찾기 | P0 | — |
| **P7** | 마이·프로필 편집 | P0·P1 | F6 검증 |
| **P8** | 청년센터 (뷰 토글 M4) | P0·P1 | — |
| **P9** | 알림·검색 | P1 | — |
| **P10** | 아이콘 3종 · 잔여 900/600 정리 · HANDOFF 갱신 | 전체 | — |

---

**전체 원본**: Claude Design 프로젝트 https://claude.ai/design/p/05126ce9-ea5e-4e18-9058-db7682f64b95 참조.
