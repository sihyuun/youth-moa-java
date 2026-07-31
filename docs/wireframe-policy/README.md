# wireframe 정책 인덱스

> **출처**: `docs/00_assets/wireframe.png` (32768×10664, 6 섹션), 2026-07-29 판독
> **역할**: prototype.tsx·현재 구현·설계 계약(`docs/design-contracts/*.md`) 이 wireframe 정책과 어긋나는 지점을 문서화하고, 의사결정 대기 항목을 한곳에 모음
> **정책**: wireframe 원문 재해석은 이 문서에서만 이루어지고, 구현·테스트는 항상 이 문서 → 설계 계약 순으로 반영

---

## 섹션별 정책 문서

| # | 화면 | 파일 | 요약 |
|---|---|---|---|
| 1 | 로그인 | [login.md](login.md) | WF-1-001 로그인 / WF-1-002 아이디 · 비밀번호 찾기 |
| 2 | 회원가입 | [signup.md](signup.md) | WF-2-001 초기 / WF-2-002 주소 검색 후. 이용약관 1개 vs prototype 4개 상충 |
| 3 | 마이페이지 | [mypage.md](mypage.md) | WF-3-001 신청 내역 / WF-3-002 즐겨찾기 / WF-3-003 개인 정보 수정. 3그룹 8화면 |
| 4 | 홈 | [home.md](home.md) | WF-4-001 홈. 프로그램 30개 페이지네이션 vs 현재 4건 고정 상충 |
| 5 | 프로그램 | [programs.md](programs.md) | WF-5-001 목록 / WF-5-002 신청. 필터·정렬 계약 결정 이력 포함 |
| 6 | 공지사항 | [notice.md](notice.md) | WF-6-001 목록 / WF-6-002 상세. 첨부파일 미구현 |

---

## 통합 이슈 크로스 레퍼런스

**⚠️ = wireframe 과 현재 구현이 명백히 상이. 스코프 외 = wireframe 미언급 (prototype 확장)**

### A. 홈 · 프로그램 목록 표시

| # | 항목 | wireframe | 현재 | 상세 |
|---|---|---|---|---|
| ~~A1~~ | 홈 프로그램 노출 | 최대 30개, 4개씩 페이지네이션 | Top 4 고정 (마감임박 정렬) | ✅ **의도적 이탈 (2026-07-31)** — prototype 도 4장 고정이라 wireframe 만 근거로 UX 확장하는 것은 판정 규칙 역행. `docs/specs/F-home-30-pagination.md` §0-A 결정 근거 참조 |
| A2 | 홈 프로그램 정렬 | 최신 등록순 | 마감임박 ASC | [home.md](home.md) #2 |
| A3 | 공간안내 | 이미지 슬라이드 (움직임) | 정적 그리드 3장 | [home.md](home.md) #5 |
| A4 | Hero 배너·Quick Stats·맞춤추천 | wireframe 미언급 | 현재 구현됨 | [home.md](home.md) #6, #7, #8 |

### B. 회원가입 · 개인 정보

| # | 항목 | wireframe | 현재 | 상세 |
|---|---|---|---|---|
| B1 | 아이디 라벨 통일 | `[아이디] = 이메일` | 로그인 화면 "아이디" 표기 | [signup.md](signup.md) #1 |
| B2 | 비밀번호 상한 · 국문 | 최대 16자, 국문 허용 | 8자↑, 국문·상한 미명세 | [signup.md](signup.md) #2 |
| B3 | 비밀번호 표시 토글 | 아이콘 masked↔plain | masked only | [signup.md](signup.md) #3, [mypage.md](mypage.md) #7 |
| B4 | 이용약관 | 1개 Checkbox | prototype 4개 세분화 | [signup.md](signup.md) #6 |
| B5 | 관심 지역·카테고리 | 미언급 | 회원가입·마이페이지에서 편집 | [signup.md](signup.md) #7, [mypage.md](mypage.md) #10 |
| B6 | 생년월일 입력 | 텍스트 (숫자·특수문자) | date picker | [signup.md](signup.md) #5, [mypage.md](mypage.md) #9 |
| ~~B7~~ | 개인정보 수정 재인증 | 비밀번호 재확인 필수 | ✅ 이미 정합 — `/mypage/profile/verify` + 세션 flag + 10분 TTL | [mypage.md](mypage.md) #6 |
| ~~B8~~ | 회원 탈퇴 | 탈퇴하기 → 확인 → 처리 | ✅ 반영 (2026-07-31) — 로직·모달·redirect 이미 완비. 로그인 페이지 완료 알림을 alert → 토스트로 변경 ("탈퇴처리되었습니다.") | [mypage.md](mypage.md) #8 |

### C. 마이페이지 신청 · 즐겨찾기

| # | 항목 | wireframe | 현재 | 상세 |
|---|---|---|---|---|
| ~~C1~~ | 즐겨찾기 상한 | 20개, 초과 시 자동 삭제 | ✅ 반영 (2026-07-31) — `BookmarkService.MAX_BOOKMARKS_PER_USER=20` + toggle 시 오래된 것 자동 삭제. 정책 강제 테스트 2건 추가 | [mypage.md](mypage.md) #2 |
| C2 | 즐겨찾기 정렬 | 즐겨찾기한 최신순 | 정렬 축 미확인 | [mypage.md](mypage.md) #12 |
| ~~C3~~ | 신청 상태 enum | 대기/승인/반려/취소 | ✅ 이미 정합 — ApplicationStatus enum (PENDING/APPROVED/REJECTED/CANCELLED) + `history.html` badge 매핑 완료 | [mypage.md](mypage.md) #3 |
| ~~C4~~ | 취소 사유 필드 | Radio 필수 | ✅ 이미 정합 — `Application.cancelReason` + `CancelReason` enum + `history.html` Radio required + OTHER 텍스트 | [mypage.md](mypage.md) #4 |
| ~~C5~~ | 기간 필터 | 3M/6M/1Y/3Y | ✅ 이미 정합 — `MyPageController.periodCutoff` + `history.html` 탭 UI | [mypage.md](mypage.md) #5 |

### D. 공지사항

| # | 항목 | wireframe | 현재 | 상세 |
|---|---|---|---|---|
| D1 | 구분(카테고리) 값 | `전체` + `{구분값}` (미명시) | NoticeCategory (NOTICE 등) | [notice.md](notice.md) #1 |
| D2 | 첨부파일 | 상세 하단 다운로드 | 스키마 미존재 | [notice.md](notice.md) #5 |
| D3 | 페이지당 개수 | 10개 | 확인 필요 | [notice.md](notice.md) #2 |

### E. 프로그램 · 신청 폼
[programs.md](programs.md) 별도 관리 — 정렬(즐겨찾기+최신), 필터(현행 계약 유지), 신청 폼 필드 등.

---

## 우선 결정 필요 (Top 결정 리스트)

의사결정 순서대로 나열:

1. **홈 프로그램 표시 방식** (A1, A2) — 30개 페이지네이션 vs 4건 마감임박. 현재 홈 UX 전체 재작업 여부 좌우
2. **아이디 = 이메일 라벨 통일** (B1) — 전 화면 문구 일관성
3. **즐겨찾기 20개 상한 + 자동 삭제** (C1) — Repository 정책 결정
4. **신청 상태 enum 매핑** (C3) — 스키마 확정 후 마이페이지 badge 구현 가능
5. **취소 사유 · 기간 필터 도입** (C4, C5) — 마이페이지 신청 내역 UX 완성 조건
6. **개인정보 수정 재인증** (B7) — 보안 요구사항 결정
7. **회원 탈퇴 플로우 구현** (B8) — 스코프 확정
8. **이용약관 세분화** (B4) — 1개 vs 4개, 스키마 결정
9. **비밀번호 표시 토글** (B3) — 접근성 개선, 소요 낮음
10. **첨부파일 (공지사항)** (D2) — 스토리지 스코프 결정

### 스코프 외로 판정할 후보 (wireframe 이후 prototype 확장 가능성)
- Hero 배너 (A4)
- Quick Stats — 모집중/청년센터/누적 참여 (A4)
- 맞춤 추천 섹션 (A4)
- 관심 지역·카테고리 (B5) — 개인화 확장

각 항목은 확정 시 [design-contracts](../design-contracts/) 에 계약으로 반영하거나 `deferred` 필드로 티켓과 함께 유예 기록.

---

## 이 문서와 설계 계약의 관계

| 문서 | 역할 | 우선순위 |
|---|---|---|
| **wireframe-policy/** (이 디렉토리) | 원본 wireframe 판독 결과. 재해석 근거 | 1 (근거) |
| **design-contracts/** | wireframe·prototype·의사결정 종합 결과. CI 가 참조 | 2 (실행) |
| **e2e/contracts/** | design-contract 를 코드로 컴파일한 TS 계약 | 3 (검증) |

구현 순서는 wireframe-policy → design-contract 갱신 → e2e/contracts 반영 → 코드 변경.
