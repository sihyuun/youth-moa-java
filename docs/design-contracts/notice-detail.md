# 디자인 계약 — 공지사항 상세 `/notices/{id}`

> **추출 기준**: `docs/00_assets/prototype.tsx` L2091~2144 (`NoticeDetail`) / 2026-08-11
> **검증 상태**: 신설 (`spec_review`) — Q-1~Q-5 사용자 결정 대기
> **기계 계약**: `e2e/contracts/notice-detail.ts` — 총 24 check (첨부 관련 6건은 초기 `deferred`). px·색·개수는 그쪽에서 자동 검사
> **관련**: 목록 화면은 [notices.md](notices.md) 와 세트. 첨부 UI 는 F-notice-attachment (PR #130 · 2026-07-31) 로 도입됨

## 1. 화면 아키텍처

**공용 헤더 + 중앙 900px 콘텐츠 + Footer** — 뒤로가기 → 배지 → 제목 → 메타 → 본문 → 첨부 → 인접글 → 하단 목록 버튼.

```
┌────────────── viewport ───────────────────────────┐
│  [ Header (fragments/header, active=notices) ]    │
├───────────────────────────────────────────────────┤
│  .notice-detail (max 900, padding 32/24/56)       │
│                                                    │
│   [← .notice-back-btn 38×38]  ← SVG arrowL         │
│                                                    │
│   .notice-detail-badges (gap 8)                   │
│     [공지 badge]  📌 고정                          │
│                                                    │
│   .notice-detail-title (26/700)                   │
│     "7월 청년센터 프로그램 일정 안내"                │
│                                                    │
│   .notice-detail-meta (gap 16, 13/tri)            │
│     작성일 2026.07.03    조회 152                  │
│   ─────────────────── border-bottom ──────────────  │
│                                                    │
│   .notice-detail-body (15/text, line-height 1.85) │
│     <p>본문</p> ...                                │
│     [inline image radius 8]                        │
│     ...                                            │
│                                                    │
│   .notice-attachments (gap 8)                     │
│   ┌── .notice-attachment (padding 14/16) ──────┐  │
│   │ [⬇ SVG] 파일명.pdf ......... 1.2MB          │  │
│   └────────────────────────────────────────────┘  │
│                                                    │
│   .notice-adjacent (border-top)                   │
│   ┌ 이전글 │ 7월 청년센터 프로그램 일정 안내       │  │
│   └ 다음글 │ 7월 휴관 일정 안내                     │  │
│                                                    │
│   .notice-detail-actions (center)                 │
│     [ 목록으로 (btn-secondary) ]                    │
├───────────────────── Footer ──────────────────────┤
```

- 컴포넌트 계층: `body > .main-content > .notice-detail (max 900 center) > 뒤로가기 → 배지 → 제목 → 메타 → 본문 → 첨부 → 인접 → 하단 액션`
- prototype L2097 `maxWidth:900 margin:'0 auto' padding:'32px 24px 56px'`
- **prototype 은 헤더 없음** — 구현은 공용 Header 유지 (notices.md Q-1 과 통일)

## 2. 상태 머신

상세 페이지는 상태 변화가 거의 없다. 첨부 다운로드 클릭 → 실 파일 다운로드가 유일한 사이드 이펙트.

| 상태 | 초기값 | 트리거 | 결과 |
|---|---|---|---|
| 조회수 (`viewCount`) | seed | 페이지 진입 시 서버가 +1 | 렌더 값에 반영 |
| 첨부 다운로드 | — | 클릭 | 실 파일 응답 (PR #130) |
| 인접글 존재 | seed | prev/next 계산 | 없으면 `--empty` 클래스 + "이전 공지가 없습니다." 안내 |

## 3. CTA·링크 라우팅

| 요소 | prototype 목적지 | 구현 목적지 | 판정 |
|---|---|---|---|
| 뒤로가기 버튼 | `go('notices')` (L2098) | GET `/notices` | 정합 |
| 첨부 다운로드 | mock (L2123 UI 만) | GET `/notices/{noticeId}/attachments/{attachmentId}/download` | **정보구조 강화** — 실 다운로드 |
| 이전글 row | `go('notices')` (L2131) | GET `/notices/{prev.id}` | **정보구조 강화** — 실제 이전 글로 이동 (prototype 은 목록으로 이동) |
| 다음글 row | `go('notices')` (L2131) | GET `/notices/{next.id}` | 정보구조 강화 (동상) |
| 하단 목록 버튼 | `go('notices')` (L2138) | GET `/notices` | 정합 |

**인접 글 이동은 prototype 이 목록으로 회귀시키지만 구현은 실제 이전/다음 공지로 이동한다** — 정보구조 강화. Q-2 로 명시 결정.

## 4. POLICY 준수

| 정책 | 상태 | 비고 |
|---|---|---|
| P-1 카피 | ✅ 준수 | 뒤로가기 title, "작성일", "조회", "이전글/다음글", "목록으로", "이전/다음 공지가 없습니다." 등 |
| P-2 그림자 | ✅ 준수 | 상세 컨테이너 shadow 없음 (prototype 도 없음) |
| P-3 SVG 아이콘 | ⚠️ **이탈** | 뒤로가기 `←` 문자 (list.html/detail.html), 첨부 `⬇` 문자 (detail.html L51) — prototype L2099 `Icon arrowL` / L2124 `Icon download` 는 **SVG** (tsx L73·L76). **P-3 위반**. 별도 티켓 필요 (Q-3) |
| P-4 폭 토큰 | ✅ 준수 | `.notice-detail` 는 900 하드코딩 (prototype 명시값). `--content-max` 미사용 |
| P-5 prototype 없는 추가 | ✅ 기록 | 공용 Header · 실제 이전/다음 이동 · 실 첨부 다운로드 · 인접 글 empty 안내 · XSS unescape 정책 · `th:utext` 주석 (§5) |

## 5. prototype 에 없는 구현 추가 요소

POLICY P-5. 계약 검사 대상 아님.

- **공용 Header** — prototype 은 헤더 없음
- **실제 이전/다음 이동** — prototype 은 목록 회귀만
- **실 첨부 다운로드 (PR #130)** — prototype 은 mock. 첨부 컴포넌트가 `<a>` 링크로 키보드 접근성 자동 확보
- **인접 글 empty 안내** — "이전 공지가 없습니다." / "다음 공지가 없습니다." (detail.html L65~68 · L76~79)
- **본문 XSS 정책** — `th:utext` 로 HTML 렌더. seed 데이터만 신뢰 (detail.html L38~41 주석). 사용자 입력 저장 도입 시 sanitize 필요
- **CSRF meta** — POST 없음, 다운로드는 GET
- **404 처리** — 존재하지 않는 id → NotFound. Controller 책임 (계약 대상 아님)

## 6. 계약이 커버하지 않는 항목

- 조회수 증가 로직 (Service 계층)
- 첨부 실 다운로드 (E2E 시나리오 · `notices.spec.ts` 별도)
- 본문 이미지·링크 렌더 (콘텐츠 스펙)
- 첨부 seed 존재 여부 (data policy · Q-4)
- 이전/다음 계산 로직 (Repository 테스트)
- Footer 정합성 — `common.ts` 가 커버

## 7. 이월 (deferred) · 영구 이탈 (deviation) 요약

기계 계약 (`notice-detail.ts`) 에 기록된 표기.

| id | 필드 | 사유 |
|---|---|---|
| `attachment.exists` | `deferred: docs/specs/F-notice-seed-attachment.md` | seed 첫 공지 (id=1) 에 첨부 존재 보장 미확정 (Q-4) |
| `attachment.padding` | `deferred` | 첨부 seed 활성화와 연동 |
| `attachment.radius` | `deferred` | 동상 |
| `attachment.icon.svg` | `deferred: docs/specs/F-notice-attachment-svg.md` | 현재 `⬇` 문자. SVG 이식 대기 (Q-3) |
| `attachment.name.font-size` | `deferred` | 첨부 seed 활성화와 연동 |
| `attachment.size.font-size` | `deferred` | 동상 |
| **`back-btn.svg`** | **활성** | 현재 `←` 문자 → **impl 단계에서 SVG 이식 필수 (P-3, Q-3)** |

## 8. 결정 확정 (2026-08-11)

### ✅ Q-1. 공용 Header 유지 — **deviation 확정 (notices Q-1 통일)**

### ✅ Q-2. 이전/다음 실제 이동 — **정보구조 강화 확정, 계약 대상 아님**

### ✅ Q-3. 아이콘 SVG 이식 — **(A) 이번 impl 범위 포함**
- 뒤로가기 `←` (detail.html L22) → SVG arrowL
- 첨부 `⬇` (detail.html L51) → SVG download
- P-3 이탈 불가

### ✅ Q-4. seed 첨부 정책 — **(A) DataInitializer 확장**
- 첫 공지(id=1)에 `sample.pdf` 1개 자동 seed
- 첨부 관련 6 check 모두 `deferred` 제거 → 활성 검사
- 하위 문서 참조 `docs/specs/F-notice-seed-attachment.md` 는 이번 impl 로 흡수

### ✅ Q-5. 상세 path — **(A) `/notices/1` 하드코딩**
- seed 순서 재현 가능 (DataInitializer idempotent)

## 9. 다음 단계

1. ~~Q-1 ~ Q-5 사용자 결정~~ ✅ 2026-08-11 완료
2. `notice-detail.ts` 갱신 (첨부 6 check `deferred` 제거)
3. ym-impl 인계 — 뒤로가기·첨부 SVG 이식 + DataInitializer 첨부 seed + Q-2 정보구조 강화 주석
4. bootRun 후 `npx playwright test --project=contracts visual-notice-detail` 실행 → 갭 목록 확정
5. ym-verify 최종 관문 → 커밋

## 관련

- 목록 계약: [notices.md](notices.md) · `e2e/contracts/notices.ts`
- 첨부 도입 PR: F-notice-attachment (#130 · 2026-07-31 이력, memory `project_youth_moa_java.md`)
- 공통 헤더·푸터: [common.md](common.md)
- 전 화면 공통 정책: [POLICY.md](POLICY.md)
- 구현 템플릿: `src/main/resources/templates/notice/detail.html`
- 스타일: `src/main/resources/static/css/main.css` L3881~4017

## 부속: `download` SVG fragment 재사용 여지

이번 impl (2026-08-11) 에서 `templates/fragments/icons.html` 에 신설한 `download(size, color)` SVG fragment 는 현재 notice/detail.html 이 유일 소비자다. 다만 아래 화면에서 재사용 예정이므로 표준 fragment 로 유지한다.

- **mypage 트랙**: 활동 이력·자기소개서 다운로드 (예정)
- **apply 트랙**: 신청 상세 첨부 다운로드 (예정)
- **admin 트랙**: 공지 첨부 관리·프로그램 첨부 관리 (예정)

향후 위 화면 작업 시 fragment 를 직접 재사용하고, 새로운 다운로드 UI 관습(예: 파일 크기 · MIME · truncate 등)이 필요하면 이 계약을 확장한다. verify UNVERIFIED 판정 사유: 재사용 확정 로드맵 부재 (현 시점 예정 화면 3개 명시로 해소).
