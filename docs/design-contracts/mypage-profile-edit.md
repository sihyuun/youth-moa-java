# 개인정보 수정 Step2 (`/mypage/profile/edit`) 디자인 계약

> 마지막 갱신: 2026-08-14
> 계약 파일: [`e2e/contracts/mypage-profile-edit.ts`](../../e2e/contracts/mypage-profile-edit.ts)
> 갭 리포트: `e2e/gap-reports/gap-mypage-profile-edit.md`
> 관련: [mypage.md](mypage.md) · [mypage-gap-backlog.md](../specs/mypage-gap-backlog.md) · [POLICY.md](POLICY.md)

## 1. 아키텍처

**Step1 (비밀번호 재확인) → Step2 (편집 폼) 세션 flag 분리 라우트.** prototype 은 두 스텝을 같은 `MyPage` 컴포넌트 안에서 `pwVerified` state 로 스위칭하지만, 서버 렌더 방식이라 URL 을 분리했다.

```
GET  /mypage?tab=profile        → profile-verify.html (Step1)
POST /mypage/profile/verify     → 비번 검증 성공 시 세션에 verifiedAt = now, redirect
GET  /mypage/profile/edit       → isProfileVerified(session, TTL=10분) ? profile-edit.html : redirect /mypage?tab=profile
POST /mypage/profile            → @Valid ProfileUpdateRequest 저장, 성공 시 redirect /mypage?tab=profile + flash toast
POST /mypage/withdraw           → 사용자 삭제 + 세션 무효화 + redirect /login?withdraw
```

- 세션 flag: `mypageProfileVerifiedAt`, TTL 10 분 (`MyPageController.PROFILE_VERIFY_TTL_MINUTES`)
- Step2 진입 시 flag 없거나 TTL 초과 → Step1 로 되돌림 (재진입 방지)

### 화면 요소

```
┌──────────────────────────────────────────────┐
│ .mypage-summary + .mypage-tabs (공통)        │  ← mypage.ts 계약 커버
├──────────────────────────────────────────────┤
│ .mypage-profile-edit .mypage-card            │
│  ├─ 아이디(readonly) · 이름                  │
│  ├─ 새 비밀번호 · 확인 (optional)            │
│  ├─ 핸드폰 · 성별(readonly pill) · 생년월일  │
│  ├─ 주소 (우편+검색 · 도로명 · 상세)         │
│  ├─ ⚠ 관심 지역·분야 편집 (T9 · deferred)    │
│  ├─ divider                                   │
│  └─ 탈퇴하기 · 저장                          │
└──────────────────────────────────────────────┘
```

## 2. 상태 머신

### 서버 세션
- 진입 조건: `session.mypageProfileVerifiedAt` 존재 + 현재 시각 - 그 시각 < 10 분
- 저장 후 flag 유지 (편의) — 이어서 다른 필드도 수정 가능

### 폼 로컬
- `password` optional — 비어 있으면 변경 없음. 값 있으면 `password==passwordConfirm` 검증
- `gender` — 현재 UI 상 readonly. Q-5 결정에 따라 편집 가능 여부 변동
- 탈퇴 버튼 → 확인 모달 → POST — 두 단계 확인

## 3. CTA 라우팅 매트릭스

| CTA | 라우팅 | prototype 라인 | 구현 상태 |
|---|---|---|---|
| 저장 | POST `/mypage/profile` → redirect `/mypage?tab=profile` + toast | tsx L1549 | ✅ |
| 탈퇴하기 (버튼) | 확인 모달 오픈 | tsx L1548 | ✅ |
| 탈퇴 모달 [탈퇴하기] | POST `/mypage/withdraw` → redirect `/login?withdraw` | tsx L1586 | ✅ |
| 탈퇴 모달 [취소] | 모달 닫기 | tsx L1586 | ✅ |
| 주소 검색 | window.Toast 안내 (실 검색은 미구현) | tsx L1539 | ⚠ Toast placeholder |
| 관심 정보 수정 | 없음 (T9 · deferred) | tsx L1374 (MyPage 요약 카드) | ❌ |

## 4. 데이터 소비 지점

이 화면은 **write→read 왕복 지점** 이다:

| 필드 | 저장 → 재조회 후 표시 위치 |
|---|---|
| name | mypage 요약 카드 인사말 |
| phone | mypage noti 채널 sub (카카오·SMS) |
| password | (표시 없음, 다음 로그인에 반영) |
| zipcode/address/addressDetail | Step2 재진입 시 form 필드 |
| gender | Step2 재진입 시 pill is-on |
| birthDate | Step2 재진입 시 input[type=date] |
| interestRegions/Categories | mypage 요약 카드 chip (T9 편집 UI 부재로 초기값만) |

## 5. POLICY 매핑

| POLICY | 적용 여부 | 위치 |
|---|---|---|
| P-1 카피 현행 유지 | 부분 적용 | "개인 정보 수정" (공백) 은 prototype 정합 — 유지 |
| P-2 브랜드 틴트 그림자 | 적용 | `.mypage-profile-edit.mypage-card` |
| P-3 SVG 강제 | 적용 | 눈 토글 · 성별 pill check · 주소 검색 (deferred) |
| P-4 폭 토큰 개별 | 적용 | 폼 max-width 560 (요약·탭바 1080 과 다름) |
| P-5 개선 문서화만 | 적용 | eye 토글은 wireframe #9 · 계약 반영 X |

## 6. 갭 요약

**P0**: 0건 — 폼 골격 완성, 저장/탈퇴/세션 flow 모두 동작

**P1**:
- ⚠ 관심 지역·분야 편집 UI 부재 (T9 · Q-3 결정) — CLAUDE.md 데이터 소비 지점 규칙 위반
- ⚠ 성별 pill readonly (prototype 은 선택 · Q-5 결정)
- ⚠ 주소 검색 SVG 없음 (search fragment 이식 필요)

**P2**:
- 폼 grid 폭 · 라벨 폰트 미세 조정

## 7. 인터랙션 스코프 (계약 vs 기능 E2E 경계)

**계약**:
- Step2 진입 후 폼 필드 · pill · 모달 렌더 검사
- SVG 존재 (eye · pill check · 주소 검색 [deferred])

**기능 E2E** (이 계약 밖):
- Step1 → verify POST 실패 (오답) → 에러 노출
- Step2 진입 → password 필드 채움 → passwordConfirm 불일치 → 에러
- Step2 저장 → redirect + toast + 재진입 시 값 유지
- 탈퇴 확인 모달 → submit → 로그인 페이지 + 세션 무효화

**HTMX 특수 사례**: 이 화면은 HTMX 미사용. 순수 form POST + redirect 만.

**side-effect 세팅** (계약 실행 시):
- helpers.login 후 POST `/mypage/profile/verify` (seed 계정 비밀번호) 를 fetch 로 트리거해 세션 flag 부여
- verify 는 사용자 필드를 변경하지 않으므로 **seed rotation 불필요**

## 8. 결정 확정 (2026-08-14 · prototype 통일성 원칙)

- ✅ **P-Q1 (관심 편집 UI)** — **(b) 별도 모달**
  · mypage Q-3 통일. prototype tsx L1809 `<ModalCard title="관심 정보 수정"...>` (InterestEditModal)

- ✅ **P-Q2 (성별 편집)** — **(b) pill 편집 허용**
  · mypage Q-5 통일. prototype tsx L1526 pill + check 아이콘 조합

- ✅ **P-Q3 (주소 검색 버튼)** — **prototype 대로 SVG search 아이콘 + "검색" 텍스트**
  · prototype tsx L1539 `<Btn size="m" variant="outline" icon="search"...>검색</Btn>`. 구현은 텍스트만 → **SVG search fragment 이식 필요** (P-3 준수)
  · toast 안내 문구 유지 (실 API 도입 시점까지 프런트 완성도)

- ✅ **P-Q4 (탈퇴 모달)** — **danger variant + close SVG 아이콘 이식**
  · prototype tsx L1585 `<ConfirmDialog icon="close" variant="danger" title="정말 탈퇴하시겠어요?"...>`. 구현에 아이콘·danger 변형 필요

- ✅ **P-Q5 (섹션 제목 공백)** — **"개인 정보 수정" 정합**
  · mypage Q-6 통일. prototype tsx L1488. Step1(구현 "개인정보 수정")도 동일 문구로 정합해 두 스텝 일관성 확보
