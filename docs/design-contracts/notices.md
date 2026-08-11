# 디자인 계약 — 공지사항 목록 `/notices`

> **추출 기준**: `docs/00_assets/prototype.tsx` L2027~2088 (`NoticesScreen`) / 2026-08-11
> **검증 상태**: 신설 (`spec_review`) — Q-1~Q-4 사용자 결정 대기
> **기계 계약**: `e2e/contracts/notices.ts` — 총 21 check. px·색·개수는 그쪽에서 자동 검사. 이 문서는 **판단이 필요한 구조**만 담는다.
> **관련**: 상세 화면은 [notice-detail.md](notice-detail.md) 와 세트

## 1. 화면 아키텍처

**공용 헤더 + 페이지 제목 바 + 카테고리 pill 탭 + 5열 테이블 + 페이지네이션 + Footer** — 전체 폭 콘텐츠, 최상단 h2 중앙 정렬.

```
┌────────────── viewport ───────────────────────────┐
│  [ Header (fragments/header, active=notices) ]    │
├───────────────────────────────────────────────────┤
│  .page-title-bar                                  │
│      "공지사항" (h2 28/700 center)                 │
├───────────────────────────────────────────────────┤
│  .container.notice-container (max 1440, px 80)    │
│                                                    │
│    .notice-tabs (center, gap 8)                   │
│    [전체][행사][공지][운영][기타]  ← pill radius 20  │
│                                                    │
│    .notice-table (border-top 1px text, bot border) │
│    ┌── .notice-thead (grid 80/80/1fr/120/80) ──┐   │
│    │ No │ 구분 │ 제목 │ 작성일 │ 조회수 │       │   │
│    └────────────────────────────────────────────┘   │
│    ┌── .notice-row (repeat) ────────────────────┐   │
│    │ 21 │ [공지]│ 📌 제목 │ 2026.07.03 │ 152    │   │
│    └────────────────────────────────────────────┘   │
│    ...                                             │
│                                                    │
│    .notice-pagination (gap 4)                     │
│      [‹][1][2][3][4][5][›]  ← 5개 그룹              │
├───────────────────── Footer ──────────────────────┤
```

- 컴포넌트 계층: `body > .main-content > .page-title-bar + .container.notice-container > .notice-tabs + #notice-list-region (table + pagination)`
- 폭 정책: prototype L2039 padding `28px 80px 0`, L2042 padding `0 80px 48px` — **P-4 준수**: 전역 `--content-max` 미사용, 자체 padding
- **prototype 은 헤더 없이 h2 만** (L2040 h2 중앙 정렬). 구현은 공용 Header + `.page-title-bar h2` 조합. **정보구조 강화** — 이탈 아님 (Q-1)
- prototype 은 `padding 28px 80px 0` + `padding 0 80px 48px` (분리). 구현은 `.main-content` + `.notice-container.padding` 조합

## 2. 상태 머신

카테고리 필터와 페이지 상태만 있다.

| 상태 | 초기값 | 트리거 | 결과 |
|---|---|---|---|
| `filterCategory` (query param `category`) | `null` (전체) | 탭 클릭 → HTMX GET | 서버가 필터·정렬 후 fragment 재렌더 |
| `page` (query param `page`) | `0` | 페이지 버튼 클릭 → HTMX GET | 해당 페이지 목록 재렌더 |
| `pinned` (서버 계산) | seed 기반 | 상단 고정 렌더 | pinned 먼저, 나머지 뒤에 |
| 빈 상태 | — | 필터 결과 0건 | `.notice-empty` "해당 구분의 공지사항이 없습니다." |

### HTMX 부분 갱신 흐름

```
[전체][행사][공지][운영][기타]
       ↓ 탭 클릭
       ↓ hx-get=/notices?category=EVENT
       ↓ hx-target=#notice-content
       ↓ hx-swap=outerHTML
       ↓ hx-push-url=true
서버: `_list-fragment :: content-region` 재렌더
      → wrapper (id=notice-content) 를 포함해 탭 active 도 함께 갱신
```

- HTMX 헤더 (`HX-Request`) 존재 시 프래그먼트만 반환, 없으면 full page
- prototype 은 클라이언트 state (`useState(cat)`) — 구현은 URL 쿼리 + 서버 재렌더. **정보구조 강화** (뒤로가기·공유 URL·SEO 이점)

## 3. CTA·링크 라우팅

| 요소 | prototype 목적지 | 구현 목적지 | 판정 |
|---|---|---|---|
| 카테고리 pill | `setCat(c); setPage(1)` (L2046) | GET `/notices?category=<code>` (HTMX partial) | 정보구조 강화 |
| 목록 row | `go('notice-detail', {notice:n})` (L2056) | GET `/notices/{id}` | 정합 |
| 페이지 이전/다음 | `setPage(p→p-1)` (L2071) | GET `/notices?page=N` (그룹 5개) | 정보구조 강화 |
| 페이지 번호 | `setPage(n)` (L2075) | 동상 | 정합 |

**prototype 은 페이지네이션이 3버튼 하드코딩 (`[1,2,3].map`, L2074)** — 구현은 총 페이지 수 기반 동적 렌더 + 5개 그룹 이동. **정보구조 강화 (실제 페이지 수에 종속)**. 계약에서는 페이지 버튼 크기/스타일만 검증.

## 4. POLICY 준수

| 정책 | 상태 | 비고 |
|---|---|---|
| P-1 카피 | ✅ 준수 | "공지사항" · 카테고리 라벨 · "해당 구분의 공지사항이 없습니다." prototype 정합 |
| P-2 그림자 | ✅ 준수 | 목록 자체는 shadow 없음. hover 시 배경만 전환 |
| P-3 SVG 아이콘 | ⚠️ 부분 이탈 | pin 아이콘이 `📌` 이모지 (main.css L3833, prototype L2060 도 동일 `📌`) — **prototype 자체가 이모지**이므로 P-3 위반 아님. 페이지네이션 ‹ › 화살표는 `content:'‹'` 문자 (prototype L2072 도 `‹` 문자) — 동일 근거로 P-3 위반 아님 |
| P-4 폭 토큰 | ✅ 준수 | `.notice-container` 는 별도 폭 미지정 — main-content 폭 그대로 |
| P-5 prototype 없는 추가 | ✅ 기록 | 공용 Header · URL 기반 필터·페이지 · HTMX partial swap · 페이지네이션 그룹 이동 (§5) |

## 5. prototype 에 없는 구현 추가 요소

POLICY P-5. 계약 검사 대상 아님.

- **공용 Header** — prototype 은 h2 만 (L2040). 구현은 `fragments/header :: header('notices')` 로 사이트 전역 네비 유지
- **URL 기반 필터·페이지 state** — 뒤로가기·공유·SEO
- **HTMX partial swap** — 전체 페이지 리로드 없이 탭·페이지 전환
- **페이지네이션 그룹 이동 (5개 단위)** — prototype 은 항상 3버튼 고정. 구현은 총 페이지 수 기반
- **`title.text` 실제 값 검증** — prototype 은 mock, 구현은 서버 렌더 실 텍스트
- **`.notice-empty` 문구 서버 렌더** — prototype 은 클라이언트 조건부

## 6. 계약이 커버하지 않는 항목

- HTMX 부분 갱신 성공/실패 분기 — E2E 시나리오 (`notices.spec.ts` 별도)
- 페이지 그룹 이동 로직 정확성 — E2E 시나리오
- pinned 우선 정렬 — 데이터 스펙 (Repository 테스트)
- SEO meta 태그
- Footer 정합성 — `common.ts` 가 커버
- Header active 표시 — `common.ts` 가 커버
- `.notice-thead` `grid-template-columns` 1fr 실측 정확도 — 컨테이너 폭에 종속 (Q-2)

## 7. 이월 (deferred) · 영구 이탈 (deviation) 요약

기계 계약 (`notices.ts`) 에 기록된 표기.

| id | 필드 | 사유 |
|---|---|---|
| `thead.grid` | `deviation` | 1fr 는 컨테이너 폭에 종속. 실측 대신 상수 기대값으로 표기했으나 재계산 필요할 수 있음 (Q-2 확정 시 수정) |
| `pagination.page-btn.size` | `deferred: docs/specs/F-notices-seed-volume.md` | seed 데이터가 pageSize 초과일 때만 노출. seed 볼륨 정책 확정 후 활성화 |
| `pagination.page-btn.radius` | `deferred` | 동상 |

## 8. 결정 확정 (2026-08-11)

### ✅ Q-1. 공용 Header 유지 — **deviation 확정 (정보구조 강화)**
- prototype 무헤더 → 구현 `fragments/header :: header('notices')` 유지
- 사용자 다른 화면 이탈 경로 확보. header 관련 계약 항목에 deviation 명시

### ✅ Q-2. `.notice-thead` grid — **(B) column-count 만 검사**
- 정확한 폭은 시각 스크린샷이 잡음. `notices.ts` 에서 grid px 검사 축소·`display:grid` sanity 유지

### ✅ Q-3. 페이지네이션 — **(B) deferred 유지**
- `docs/specs/F-notices-seed-volume.md` 이월. seed 볼륨 정책은 별도 티켓
- pagination 관련 항목 `deferred` 필드 유지

### ✅ Q-4. 카테고리 탭 5개 — **정합 유지**
- prototype 5개 (`전체·행사·공지·운영·기타`) 기준. 구현 실측 시 4개면 impl 단계 갭
- `NoticeCategory` enum 확인은 impl 단계에서 수행

## 9. 다음 단계

1. ~~Q-1 ~ Q-4 사용자 결정~~ ✅ 2026-08-11 완료
2. `notices.ts` 갱신 (Q-2 결정 반영 → grid px check 축소)
3. bootRun 후 `npx playwright test --project=contracts visual-notices` 실행 → 갭 목록 확정
4. ym-impl 인계 — 갭 청산
5. ym-verify 최종 관문 → 커밋

## 관련

- 상세 화면 계약: [notice-detail.md](notice-detail.md) · `e2e/contracts/notice-detail.ts`
- 공통 헤더·푸터: [common.md](common.md)
- 전 화면 공통 정책: [POLICY.md](POLICY.md)
- 구현 템플릿: `src/main/resources/templates/notice/list.html` · `notice/_list-fragment.html`
- 스타일: `src/main/resources/static/css/main.css` L3742~3879
