# 마이페이지 (`/mypage`) 디자인 계약 — 아키텍처 · 상태 · CTA · 정책

> 마지막 갱신: 2026-08-14
> 계약 파일: [`e2e/contracts/mypage.ts`](../../e2e/contracts/mypage.ts)
> 갭 리포트: `e2e/gap-reports/gap-mypage.md` (계약 실행 시 자동 갱신)
> 관련: [mypage-gap-backlog.md](../specs/mypage-gap-backlog.md) · [notifications.md](notifications.md) · [POLICY.md](POLICY.md)

## 1. 아키텍처

**단일 라우트 + 쿼리 파라미터로 4탭 분기.** 상단 요약 · 탭바는 4탭 공통, 하단 콘텐츠 카드만 탭별로 다르게 렌더된다.

```
/mypage?tab=history         → mypage/history.html   (신청 내역, 기본값)
/mypage?tab=favorites       → mypage/favorites.html (즐겨찾기)
/mypage?tab=noti            → mypage/notifications.html (알림 설정)
/mypage?tab=profile         → mypage/profile-verify.html (개인정보 Step1: 비밀번호 재확인)
POST /mypage/profile/verify → 세션 flag 세팅 후 redirect
/mypage/profile/edit        → mypage/profile-edit.html (개인정보 Step2: 편집 폼) — 별도 계약
```

공통 fragment: [`mypage/layout.html`](../../src/main/resources/templates/mypage/layout.html) `th:fragment="head"` — 프로필 요약 카드 + 탭바 4개.

### 화면 요소

```
┌───────────────────────────────────────────────────┐
│ .mypage-summary (프로필 요약)                     │
│  아바타 · 이름·이메일 · 관심 지역/분야 · KPI 3열  │
├───────────────────────────────────────────────────┤
│ .mypage-tabs (세그먼트 4개: 신청내역·즐겨찾기·알림·개인정보) │
├───────────────────────────────────────────────────┤
│ .mypage-card (탭별 콘텐츠)                        │
│  ├─ history  : 기간+상태 필터 · 카드 리스트 · 취소/재신청 CTA │
│  ├─ favorites: 즐겨찾기 카드 그리드 (or empty)   │
│  ├─ noti     : 채널 3 + 항목 4 (자동 저장)       │
│  └─ profile  : 비밀번호 재확인 폼 (Step1)         │
└───────────────────────────────────────────────────┘
```

## 2. 상태 머신

### 페이지 단위
- `currentTab ∈ {history, favorites, noti, profile}` — 서버 파라미터. 기본값 `history`
- history 하위: `currentPeriod ∈ {3M, 6M, 1Y, 3Y}` · `currentStatusFilter ∈ {ALL, APPROVED, PENDING, REJECTED, CANCELLED}`

### 탭 전환
- 탭 <a> 클릭 → `?tab=X` 로 전체 페이지 리로드 (prototype 은 SPA 로 클라 상태만 변경)
- 서버 렌더 방식이므로 **탭 전환 = 페이지 이동**. HTMX partial swap 은 D5b 후속 (mypage-gap-backlog)

### 인터랙션 (side-effect 있음 — 계약 검사 단계 회피)
- **취소 모달** (history) — `openCancelModal()` JS → POST `/mypage/applications/{id}/cancel`
- **즐겨찾기 삭제** (favorites) — 현재 미구현. prototype 카드 액션에서 유도 예정 (T6)
- **알림 토글** (noti) — HTMX 부분 전환 or 자동 저장 hint 노출. 상세는 [notifications.md](notifications.md) 별개
- **관심 정보 수정 링크** → `?tab=profile` 이동 (현재 편집 UI 부재 — T9 deferred)

## 3. CTA 라우팅 매트릭스

| CTA | 라우팅 | prototype 라인 | 구현 상태 |
|---|---|---|---|
| KPI 카드 클릭 | 해당 tab 이동 | tsx L1384 | ❌ 현재 <div> — T10 deferred |
| 탭 <a> 클릭 | `/mypage?tab=X` | tsx L1394 | ✅ |
| 관심 정보 수정 | `?tab=profile` | tsx L1374 | ✅ (하지만 편집 UI 없음, T9) |
| history 카드 제목 | `/programs/{id}` | tsx L1450 | ✅ |
| history "신청 상세" | `/apply/complete?applicationId=X` | tsx L1442 | ⚠ prototype 은 별도 `application-detail` 라우트 — T4 deferred |
| history 신청 취소 | 모달 오픈 → POST `/mypage/applications/{id}/cancel` | tsx L1459 | ✅ |
| history 재신청 | `/programs/{id}/apply` | tsx L1464 | ✅ |
| favorites 프로그램 클릭 | `/programs/{id}` | prototype 은 empty state 만 | ✅ |
| profile-verify 확인 | POST `/mypage/profile/verify` → redirect `/mypage/profile/edit` | tsx L1501 | ✅ |

## 4. 데이터 소비 지점 (`myUser`, `applications`, `bookmarks`)

- `myUser.name, email, phone, notifyKakao/Sms/Email/RemindD1/WaitlistEmpty/NewProgramNews`
- `myUser.interestRegions/Categories` — 요약 카드 chip 그룹으로 노출
- `applications` (필터 후) — history 카드 리스트
- `bookmarks` — favorites 리스트 (현재 title+sub · prototype 은 그리드 카드)
- KPI: `kpiOngoing, kpiFinished, kpiFavorites` — 컨트롤러 계산

## 5. POLICY 매핑

| POLICY | 적용 여부 | 위치 |
|---|---|---|
| P-1 카피 현행 유지 | 적용 | "개인정보 수정" (구현 붙임) vs prototype "개인 정보 수정" (공백) → deviation |
| P-2 브랜드 틴트 그림자 | 적용 | `.mypage-summary`, `.mypage-tabs`, `.mypage-card`, `.mypage-history-card` |
| P-3 SVG 강제 | 적용 | 관심 그룹 라벨(pin/star) · 관심 수정(edit) · 탭 4종(calendar/star/bell/user) · KPI 아이콘 · history detail chevR · favorites empty star · noti bell/check |
| P-4 폭 토큰 개별 | 적용 | mypage inner 1080 · noti 폼 560 · profile-edit 폼 560 |
| P-5 prototype 이외 개선 | 적용 | eye 토글 (wireframe #9) 는 문서화만 |

## 6. 갭 요약 (mypage-gap-backlog.md 세부, P 심각도로 재분류)

**P0 (기능 골격)**: 0건 — 4탭 라우팅 · 요약 · 필터 · 취소 모달 완성

**P1 (구조·SVG·상호작용)**:
- ⚠ history 카드 → 별도 `/mypage/applications/{id}` 라우트 (T4 deferred)
- ⚠ favorites → ProgramCard.compact grid (T6 · Q3 결정)
- ⚠ KPI clickable 라우팅 (T10 deferred)
- ✅ 탭 SVG (T1 완료)

**P2 (문구·간격·미세)**:
- POLICY P-1 로 카피 미세 차이는 deviation (T12)
- 관심 chip padding · 배경 실측 후 조정 여지

## 7. 인터랙션 스코프 (계약 vs 기능 E2E 경계)

**계약 (`--project=contracts`)**:
- 4탭 렌더 · SVG 존재 · 폰트·간격·색 정량값
- Step1 폼 진입 렌더 (하지만 실제 POST verify 는 mypage-profile-edit 계약 세팅에서 트리거)

**기능 E2E (`--project=chromium`)** — 이 계약 범위 밖:
- history 취소 모달 → radio 선택 → submit → 카드 사라짐
- noti 토글 → 자동 저장 → hint 노출
- profile verify → 세션 flag → `/mypage/profile/edit` 이동
- 탈퇴 확인 모달 → submit → `/login?withdraw`

**HTMX 특수 사례** (CLAUDE.md 인터랙션 조항 준수):
- 취소 모달 form 은 non-HTMX POST → redirect. HTMX 는 이 화면에서 미사용
- 알림 토글 자동 저장 여부 실측 → mypage-noti-instant.js 확인 시 fetch 사용 → 별도 fetch E2E 필요

## 8. 결정 확정 (2026-08-14 · prototype 통일성 원칙)

**원칙**: prototype 정의가 명확한 항목은 prototype 을 따른다. 미정의 항목만 사용자 판단.

- ✅ **Q-1 (T4 · P1)** — **(b) `/mypage/applications/{id}` 신설**
  · 근거: prototype tsx L1442 `onClick={()=>go('application-detail',{pg:app})}` — 전용 상세 라우트

- ✅ **Q-2 (T6 · P1)** — **(a) ProgramCard 재사용**
  · 근거: prototype 미정의 (empty state 만 존재, L1476~1484). 사용자 결정 = 일관성·재사용성 우선

- ✅ **Q-3 (T9 · P1)** — **(c) 별도 모달 (InterestEditModal 재사용)**
  · 근거: prototype tsx L1374 `setShowInterest(true)` + L1589 InterestEditModal

- ✅ **Q-4 (T10 · P2)** — **(a) `<a th:href="?tab=X">` 로 변환**
  · 근거: prototype tsx L1380 `onClick={()=>setTab(s.goTab)}` — 클릭 이벤트 명시. 서버 렌더 등가는 `?tab=X` 링크

- ✅ **Q-5 (성별)** — **(b) prototype 대로 편집 허용**
  · 근거: prototype tsx L1524~1531 pill 클릭으로 setEditForm

- ✅ **Q-6 (POLICY P-1)** — **(b) "개인 정보 수정" 공백 정합**
  · 근거: prototype tsx L1488 `>개인 정보 수정<`. P-1 카피 원칙에 따라 prototype 채택

- ✅ **Q-7 (탭 라벨)** — **현행 "알림 설정" 유지 확인**
  · 근거: prototype tsx L1393 `{key:'noti', label:'알림 설정'}`
