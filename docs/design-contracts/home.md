# 디자인 계약 — 홈 `/`

> **추출 기준**: `docs/00_assets/prototype.tsx` L499~694 (`HomeScreen`) / 2026-07-28
> **검증 상태**: 테스트 연결됨 (`e2e/tests/visual-home.spec.ts`) · 스크린샷 baseline 미등록
> **기계 계약**: `e2e/contracts/home.ts` — px·색·폰트·개수는 전부 그쪽에서 자동 검사된다. 이 문서는 **판단이 필요한 구조**만 담는다.

## 1. 섹션 구조와 폭 정책

위에서부터 순서대로. prototype 은 대부분 섹션을 **뷰포트 전폭 + 좌우 80px 패딩**으로 두고, 퀵메뉴 하나만 `maxWidth: 1080` 으로 좁힌다. 이 비대칭은 의도된 것이므로 `.container` 로 일괄 처리하면 안 된다.

| # | 섹션 | 폭 정책 | 배경 | proto |
|---|---|---|---|---|
| 1 | Hero | 전폭 (헤더 뒤로 확장, `margin-top: -68`) | 배경 이미지 + 2겹 scrim | L513~545 |
| 2 | Quick Stats | 전폭 + 80px | `surface` (#fff), 하단 1px border | L547~559 |
| 3 | 퀵메뉴 4그리드 | **maxWidth 1080** ← 유일한 예외 | `bg` | L561~575 |
| 4 | 프로그램 / 맞춤 추천 | 전폭 + 80px | `bg` | L577~638 |
| 5 | 공지사항 | 전폭 + 80px | **`primaryBg`** (연보라) | L640~669 |
| 6 | 공간안내 | 전폭 + 80px | `bg` | L671~689 |
| 7 | Footer | — | — | L691 |

## 2. 상태 분기 — 로그인 여부

4번 섹션은 **완전히 교체**된다 (동시 노출 아님).

| | 비로그인 | 로그인 |
|---|---|---|
| 섹션 제목 | `프로그램` | `{이름}님 맞춤 추천` + 관심 태그 chip |
| 부제 | 진행중인 프로그램을 소개해드려요 | 관심 지역과 분야를 바탕으로 골라드렸어요 |
| 카드 이미지 높이 | 170 | 150 |
| 카드 제목 폰트 | 15 | 14 |
| 카드 우상단 | 즐겨찾기 별 (토글) | `관심지역`(첫 카드) / `추천` 배지 |
| 카드 하단 | 날짜 + CapacityBar + **CTA 버튼** | 없음 (제목 + 센터명만) |
| proto | L605~638 | L577~603 |

**주의**: 로그인 카드는 CTA 도 CapacityBar 도 없는 **간소 카드**다. 비로그인 카드와 같은 컴포넌트를 재사용하면 안 된다.

## 3. 카드 하단 CTA 5분기 (비로그인 섹션)

`capInfo(pg)` 결과와 알림 신청 여부로 결정된다 (proto L629~632).

| 조건 | 라벨 | 아이콘 | 테두리 / 색 | 클릭 동작 |
|---|---|---|---|---|
| 진행예정 (`upcoming`) | 오픈 알림 받기 | bell | `secondary` (주황) | 로그인 시 모달, 아니면 로그인 유도 |
| 진행예정 + 알림 신청됨 | 알림 신청됨 · 해제 | bell | `secondary` | 알림 해제 |
| 정원 마감 (`full`) | 빈자리 알림 받기 | bell | `border` / 배경 `borderLight` | 로그인 시 모달, 아니면 로그인 유도 |
| 정원 마감 + 알림 신청됨 | 알림 신청됨 · 해제 | bell | 위와 동일 | 알림 해제 |
| 그 외 (모집중) | 신청하기 | check | `primary` | 상세로 이동 |

높이 34, radius 20(tagR), 폰트 13/600.

## 4. CTA·링크 라우팅

| 요소 | 목적지 |
|---|---|
| Hero 검색바 / 인기 검색어 칩 | `search` (`/search?q=`) |
| 퀵메뉴 4개 | `programs` · `centers` · `mypage` · `notices` |
| 섹션 `전체보기` (프로그램·추천) | `programs` |
| 공지 `전체보기` / 카드 / 리스트 행 | `notices` · `notice-detail` |
| 공간안내 `지도에서 전체 센터 보기` | `centers` |
| 프로그램 카드 본체 | `program-detail` |

**현재 구현 갭**: 공지 대표 카드·리스트 행이 모두 `/notices` 목록으로 가고 상세(`/notices/{id}`)로 가지 않는다. 공간안내 버튼 라벨도 prototype 은 `지도에서 전체 센터 보기` 인데 구현은 `전체 센터 보기` 다. (기계 계약으로는 못 잡는 항목이라 여기 기록)

## 5. 기계 계약이 커버하지 않는 항목

자동 검사 대상이 아니므로 화면 작업 시 **사람이 확인**한다.

- Hero 배경 이미지 **8초 간격 크로스페이드** (`transition: opacity 1.2s`) — proto L506~509
- Hero scrim 2겹: 브랜드 틴트 `linear-gradient(135deg, ...)` + 하단 darken + 상단 110px darken (헤더 가독성) — proto L518~521
- 카드 hover `transform: translateY(-2px)` + shadow 상승
- 즐겨찾기 별 토글 시 amber(`#F59E0B`) 전환, 비로그인 클릭 시 로그인 유도
- 알림 신청 모달 (`WaitlistModal`) 열림·완료 후 라벨 전환 — proto L690
- 공지 리스트 마지막 행 `border-bottom` 없음 — proto L661 (`i<2`)

## 6. 현재 갭 (2026-07-29 계약 검사 결과)

`e2e/gap-reports/gap-home.md` 참조. **41/41 통과 · 갭 0건** (2026-07-29 fix/home-contract-gaps 반영) · 의도적 이탈 2건(카피, POLICY P-1).

### 2026-07-29 갭 19건 청산 내역 (fix/home-contract-gaps)

| # | 심각도 | 항목 | 조치 |
|---|---|---|---|
| 1 | P0 | Hero 콘텐츠 좌측 정렬 | `.hero-inner`: `align-items: center` + `text-align: center` (proto L522) |
| 2 | P0 | Hero 텍스트 좌측 정렬 | 위와 동일 (한 규칙에서 처리) |
| 3 | P1 | Hero 배지 chip 스타일 | `.hero-eyebrow` 반투명 chip (bg rgba(255,255,255,0.2) · radius 20 · padding 6/16 · backdrop-filter blur, proto L523) |
| 4 | P1 | Hero 배지 radius 20 | 위와 동일 |
| 5 | P1 | Hero 타이틀 42 | `.hero-title font-size: 42px` (proto L526) |
| 6 | P1 | 검색바 460 | `.hero-search-bar width: 460px` (proto L528) |
| 7 | P1 | 검색바 52 | `.hero-search-bar height: 52px` (proto L528) |
| 8 | P1 | Stats 아이콘 SVG | `index.html` ✓ 텍스트 3건 → `~{fragments/icons :: check(22, primary)}` (proto L551, POLICY P-3 SVG 이탈 금지) |
| 9 | P1 | 퀵메뉴 폭 1080 | `.home-quickmenu max-width: 1080px` + `container` 클래스 제거 (proto L561, POLICY P-4) |
| 10 | P1 | 카드 즐겨찾기 별 | 비로그인 카드에 `bookmark-button` fragment 삽입 + `HomeController` 에 `bookmarkedIds` 모델 attr 노출 (proto L620~622) |
| 11 | P1 | 카드 하단 CTA | 비로그인 카드에 `.program-card-cta` block 신설 (dto.ctaType 5분기, 목록 카드와 동일 마크업, proto L629~632) |
| 12 | P2 | Hero 배지 font 14 | `.hero-eyebrow font-size: 14px` |
| 13 | P2 | Hero 배지 weight 500 | `.hero-eyebrow font-weight: 500` |
| 14 | P2 | Hero desc font 16 | `.hero-desc font-size: 16px` |
| 15 | P2 | 검색 input font 15 | `.hero-search-input font-size: 15px` |
| 16 | P2 | 비로그인 카드 이미지 170 | `.home-program-card .program-card-image height: 170px` (Q4 = A prototype 대로 분리) |
| 17 | P1 | 맞춤 추천 관심 chip | `HomeService.getRecommendInterestChip()` + `HomeController` model + `.interest-chip` CSS + `index.html` 렌더 (proto L581). seed1 유저에 관심 지역·분야 시드 (부천시 / 취업·창업) |
| 18 | P2 | 로그인 카드 이미지 150 | `.home-program-card--recommend .program-card-image height: 150px` (Q4 = A) |
| 19 | P2 | 로그인 카드 제목 14 | `.home-program-card--recommend .program-card-title font-size: 14px` (비로그인은 15) — modifier class 로 분기 |

의도적 이탈 2건 유지:
- `hero.title.text` — POLICY P-1 (카피)
- `hero.desc.text` — POLICY P-1 (카피)

## 7. 결정 현황

| # | 항목 | prototype | 현재 | 상태 |
|---|---|---|---|---|
| ~~Q1~~ | Hero 타이틀 카피 | 경기도 청년의 내일을 / 함께 만들어갑니다 | 청년의 모든 기회를 / 한곳에서 | **✅ 현행 유지** — POLICY P-1, `deviation` 처리됨 |
| ~~Q2~~ | Hero 설명 카피 | 경기도 31개 시·군 청년센터의 프로그램을 한눈에 확인하세요 | 프로그램·공간·정책을 청년모아에서 만나보세요 | **✅ 현행 유지** — POLICY P-1, `deviation` 처리됨 |
| ~~Q3~~ | Stats 숫자 | 127 / 31 / 15,420 (목업) | 실제 시드값 | **✅ 계약 제외** — 목업 숫자를 재현할 이유가 없다. 애초에 계약에 미포함 |
| ~~Q4~~ | 카드 이미지 높이 | 170(비로그인) / 150(로그인) | 둘 다 160 | **✅ prototype 대로 분리 반영 (2026-07-29)** — `.home-program-card--recommend` modifier 로 로그인 카드만 150 지정, 비로그인 카드는 default 170 |
