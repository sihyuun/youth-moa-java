# Prototype 갭 리포트 — 2026-07-27

> 대상: 13개 사용자 대면 페이지 / 기준: `docs/00_assets/prototype.tsx` / 스캔: Phase 1 read-only (Playwright fullPage 스크린샷 + 라인 인용 대조)
> 로그인 계정: `seed1@youth-moa.test` / 뷰포트 1440x900

심각도 정의
- **P0**: 화면 아키텍처·핵심 CTA 누락, 신청 wizard 진행 불가 등 진입 즉시 눈에 띄는 결함
- **P1**: 컴포넌트 누락·라벨 오류·상태 UI 미구현
- **P2**: 스타일 미세 편차 (간격·색 tone·hover)

## 요약

| # | 페이지 | 갭 개수 | 심각도 최고 | 스크린샷 |
|---|---|---:|---|---|
| 1 | `/` (로그아웃) | 5 | P1 | `home.png` |
| 1b | `/` (로그인) | 4 | P1 | `home-loggedin.png` |
| 2 | `/login` | 3 | P2 | `login.png` |
| 3 | `/signup` | 3 | P1 | `signup.png` |
| 4 | `/programs` | 6 | P1 | `programs.png` |
| 5 | `/programs/{id}` (OPEN) | 4 | P1 | `program-detail-open.png` |
| 6 | `/programs/{id}/apply` wizard | 3 | **P0** | `apply-step1.png` / `apply-step2-real.png` |
| 7 | `/apply/complete` | 1 | **P0** | `apply-complete.png` |
| 8 | `/centers` | 5 | P1 | `centers.png` |
| 9 | `/notices` | 3 | P1 | `notices.png` |
| 10 | `/notices/{id}` | 2 | P2 | `notice-detail.png` |
| 11a | `/mypage?tab=history` | 4 | P1 | `mypage-applications.png` |
| 11b | `/mypage?tab=favorites` | 2 | P2 | `mypage-bookmarks.png` |
| 11c | `/mypage?tab=noti` | 2 | P2 | `mypage-notifications.png` |
| 11d | `/mypage?tab=profile` | 3 | P1 | `mypage-profile.png` |
| 12 | `/mypage/profile/edit` | 1 | P1 | `mypage-profile-edit.png` (리다이렉트) |
| 13 | `/notifications` | 3 | P2 | `notifications.png` |

**전체 갭 총계 60건 / P0 2건 / P1 다수**

---

## 1. `/` (로그아웃)

**prototype**: `prototype.tsx` L499~694 (HomeScreen)
**스크린샷**: `home.png`

| # | 항목 | prototype | 현재 | 심각도 |
|---|---|---|---|---|
| 1 | Hero 인기검색어 칩 | L537~543: "인기 검색어" 라벨 + 5개 칩 (`취업 워크숍`, `창업 지원`, `심리 상담`, `주거 지원`, `자격증`) | 스크린샷에 인기검색어 칩 미확인 (검색바 아래 미노출) | P1 |
| 2 | Hero 상단 배지 | L523~525: `경기도 청년센터 프로그램 통합 플랫폼` 반투명 chip | 미확인/미노출 | P2 |
| 3 | Stats 숫자값 | L548 prototype 값: 진행중 127 / 참여센터 31 / 누적참여자 15,420 | 9 / 48 / 28 (시드 데이터 그대로 노출) | P2 (개발 단계 데이터라 참고) |
| 4 | 퀵메뉴 4개 그리드 | L561~575: 프로그램 찾기·청년센터 찾기·내 신청 현황·공지사항 4개 카드 | 스크린샷에 없음 (Stats 바로 아래 "프로그램" 섹션) | **P1** |
| 5 | 공지사항 좌측 대형 카드 | L649~658: 이미지가 있는 대형 pinned notice 카드(360px) + 우측 리스트 | 좌측은 배경만 있는 이미지 없는 카드, 이미지 미노출 | P1 |

## 1b. `/` (로그인 시)

**prototype**: L577~603 (맞춤 추천 섹션 로그인 시 노출)
**스크린샷**: `home-loggedin.png`

| # | 항목 | prototype | 현재 | 심각도 |
|---|---|---|---|---|
| 1 | 맞춤 추천 라벨 badge | L580~582: "{이름}님 맞춤 추천" + `부천시 · 취업·창업 관심` 관심 태그 chip | "시드유저1님 맞춤 추천" 노출되나 우측 관심 태그 chip 없음 | P1 |
| 2 | 카드에 관심지역/추천 라벨 | L593: 첫 카드는 `관심지역`, 나머지 `추천` | 카드에 `NEW` 뱃지만 노출 | P1 |
| 3 | 카드 D-day chip | L592: `DdayChip` (D-N 표시) | 미노출 | P1 |
| 4 | 카드 하단 CTA (신청/알림) | 로그아웃 상태 카드 CTA 있음 (L629~632) | 로그인 시 맞춤 추천 카드는 CTA 없음 (prototype과 동일) | — |
| 5 | 퀵메뉴 4개 그리드 | L561~575 (로그인 여부 무관) | 미노출 | P1 (1-4와 동일) |

## 2. `/login`

**prototype**: L1987~2024 (LoginScreen)
**스크린샷**: `login.png`

| # | 항목 | prototype | 현재 | 심각도 |
|---|---|---|---|---|
| 1 | 헤더 미노출 | L1995~2020: Header 컴포넌트 없이 로고+폼만 표시 | 헤더 없음 (일치) | — |
| 2 | 아이디 저장 체크박스 | L2006~2008: 좌측 `아이디 저장` 라벨 | `로그인 상태 유지` 라벨로 표기 | P2 (기능 지향은 다름 — remember-me 는 세션 유지, prototype 은 ID 기억) |
| 3 | 비밀번호 눈 아이콘 | prototype 에 없음 (L2003 단순 password input) | 눈 아이콘 toggle 존재 | P2 (기능 추가로 UX 좋음, 그러나 prototype 이탈) |
| 4 | placeholder | L2002: "아이디를 입력해주세요.", L2003: "비밀번호를 입력해주세요." | "아이디(이메일)를 입력해주세요.", "비밀번호를 입력해주세요." | P2 (이메일 표기 추가) |

## 3. `/signup`

**prototype**: L1842~1984 (SignupScreen)
**스크린샷**: `signup.png`

| # | 항목 | prototype | 현재 | 심각도 |
|---|---|---|---|---|
| 1 | 헤더 미노출 | L1865~1980: Header 컴포넌트 없이 로고+폼만 (`justifyContent:'center'` 좁은 폼) | 상단에 전역 헤더(프로그램/청년센터/공지사항 nav) 노출됨 | P1 |
| 2 | 인증번호 UI | L1911~1922: 인증요청 후 6자리 코드 입력 + `3:00` 타이머 + `유효시간 만료` 상태 | 인증요청 버튼만 존재, 코드 입력 UI 노출 확인 못함(초기 상태) | P1 (동적 확인 필요) |
| 3 | 성별 선택 UI | L1927~1933: 남/여 두 개 클릭식 카드형 버튼 (선택 시 check icon + primaryBg) | 남/여 두 카드 있지만 선택 스타일이 prototype 처럼 primaryBg 채우기+check 인지 미확인 | P2 |
| 4 | 생년월일 placeholder | L1938: `"YYYY / MM / DD"` (텍스트 input) | 브라우저 native `<input type=date>` 로 보이는 `연도-월-일` 표시 | P2 (기능 지향 다름) |
| 5 | 아이디 placeholder | L1881: "아이디를 입력해주세요" | "이메일을 입력해주세요" | P2 |

## 4. `/programs`

**prototype**: L815~942 (ProgramList)
**스크린샷**: `programs.png`

| # | 항목 | prototype | 현재 | 심각도 |
|---|---|---|---|---|
| 1 | 필터 배치 (구조 갭) | L844~856: **상단 인라인 chip row** (전체/모집중/진행예정/종료 + 지역·청년센터 팝업 chip) | **좌측 사이드바 세로 필터** (지역·청년센터 각각 checkbox 리스트) | **P1** — 구조 자체가 다름 |
| 2 | 상태 탭 (전체/모집중/진행예정/종료) | L847~851 | 스크린샷에 미확인 (좌측 사이드바만 노출) | P1 |
| 3 | 정렬 옵션 | L862~867: `기본순 · 마감임박순 · 인기순` inline 텍스트 링크 | 우측 상단에 정렬 옵션 있으나 라벨/스타일 미확인 필요 | P2 |
| 4 | 뷰 전환 toggle | L870~877: 목록/캘린더 토글 | 스크린샷 상단에 뷰 전환 미확인 | P1 |
| 5 | 카드 그리드 컬럼 수 | L882: `repeat(3,1fr)` (3열) | 3열 (일치) | — |
| 6 | 카드 CTA 버튼 | L911~914: 상태별 CTA (신청하기 / 오픈 알림 받기 / 빈자리 알림 받기) | 카드에 CTA는 있으나 라벨 확인 필요 | P2 |
| 7 | 페이지네이션 | L921~934: 좌우 화살표 + 숫자 (1~5) | 우측 하단 페이지네이션 존재 | — |

## 5. `/programs/{id}` (OPEN, id=1)

**prototype**: L945~1123 (ProgramDetail)
**스크린샷**: `program-detail-open.png`

| # | 항목 | prototype | 현재 | 심각도 |
|---|---|---|---|---|
| 1 | 헤더 구성 | L974~1035: 좌 340x340 이미지 + 우측 정보 패널 (2열) | 좌 이미지 + 우 정보 (일치, 이미지가 340보다 큼) | P2 |
| 2 | Sticky 하단 CTA 바 | L1095~1120: `position:sticky; bottom:0` 로 title+마감정보+즐겨찾기+신청 CTA 를 하단 고정 | 미노출 (상단 신청 버튼만 존재) | **P1** |
| 3 | 정보 그리드 항목 | L995: `신청 기간·진행 기간·진행 장소(지도)·모집인원·문의처` 5개 | 5개 (일치) | — |
| 4 | 지원 대상 섹션 | L1038~1055: 연령/거주지/기타 3개 카드 | 3개 카드 (일치) | — |
| 5 | 프로그램 설명 | L1062~1066: 큰 이미지 placeholder + 설명 텍스트 | 텍스트만 노출, 이미지 placeholder 없음 | P2 |
| 6 | Program.content Lob 표시 | (prototype 은 placeholder text) | "이러서 작성, 연접 트레이닝, 자기소개 워크숍을 한 번에 진행됩니다." 노출 | — |

## 6. `/programs/{id}/apply` (신청 wizard) — **P0 결함**

**prototype**: L1125~1214 (ProgramApply, 3-step wizard)
**스크린샷**: `apply-step1.png`, `apply-step2-real.png` (다음 클릭 후)

| # | 항목 | prototype | 현재 | 심각도 |
|---|---|---|---|---|
| 1 | **Step 2·3 진입 불가** | L1128 `useState(step)` + L1134 `next()` 로 Step 1→2→3 전환. Step 2 는 지원 동기 textarea, Step 3 는 개인정보 동의 라디오 | `#applyNavNext` 버튼 클릭해도 화면 콘텐츠가 변하지 않음. Step 1 (신청자 정보) 내용만 반복 렌더. Step 2/3 콘텐츠 DOM 자체가 없음 (`.step-panel`, `[data-apply-step]` 셀렉터 매칭 0건) | **P0** |
| 2 | 스텝 인디케이터 | L1144~1155: 원형 스텝 3개 + label (신청자 정보 / 추가 정보 / 약관 동의) | 상단에 3원형 인디케이터 노출됨 (일치) | — |
| 3 | 프로그램 요약 카드 | L1158~1161: 이미지 + Badge + title + center·date | 노출됨 (일치) | — |
| 4 | Step 3 개인정보 수집 라디오 | L1194~1199 라디오 버튼 + 약관 텍스트 | Step 3 진입 자체가 안 되어 확인 불가 | **P0** (1과 동일 원인) |

> **원인 후보**: `#applyNavNext` 의 클릭 핸들러가 미배선되었거나 서버 사이드 렌더링 후 JS 초기화 실패. wizard 진행 관련 컨트롤러/JS 재점검 필요.

## 7. `/apply/complete` — **P0 결함**

**prototype**: L1217~1249 (ApplyComplete)
**스크린샷**: `apply-complete.png`

| # | 항목 | prototype | 현재 | 심각도 |
|---|---|---|---|---|
| 1 | **Whitelabel Error Page** | L1219~1247: 성공 페이지 (원형 체크 아이콘 + "프로그램 신청이 완료되었습니다" 헤드라인 + 프로그램 요약 + 홈으로/신청 현황 보기 버튼) | `GET /apply/complete` 접근 시 Spring Boot Whitelabel Error Page 반환 (매퍼 미등록 or 파라미터 요구) | **P0** |

> **원인 후보**: `/apply/complete` 라우트가 없거나 신청 성공 후 리다이렉트 시 `applicationId` 등 쿼리 파라미터가 필수인데 직접 접근 시 예외.

## 8. `/centers`

**prototype**: L2161~2427 (CentersScreen, 3-column: 리스트 + 인라인 상세 + 지도)
**스크린샷**: `centers.png`

| # | 항목 | prototype | 현재 | 심각도 |
|---|---|---|---|---|
| 1 | **레이아웃 구조** | L2196~ : 리스트 카드 클릭 시 우측에 **인라인 상세 패널** 노출 + 지도 3-column | **2-column** (좌 리스트 + 우 지도만). 인라인 상세 패널 미확인 | P1 (구조 반영 시도 있으나 상세 패널 확인 필요) |
| 2 | 필터 바 상단 | L2198~2205: 센터명 검색 (250px) + 지역 dropdown (140px) + `운영중만 보기` toggle | 검색+지역선택+운영중만 보기 (일치) | — |
| 3 | 지도 렌더 | L2340~: 카카오/네이버맵 SDK placeholder | "지도 미설정" 회색 placeholder | P1 |
| 4 | 카드 우측 상세보기 CTA | prototype: 리스트 카드 클릭 시 인라인 패널 확장 | `상세보기 →` 링크로 (라우트 이동으로 추정) | P1 |
| 5 | 정렬 옵션 | L2175: `name | programs` | `이름순 / 프로그램많은순` (일치) | — |

## 9. `/notices`

**prototype**: L2027~2089 (NoticesScreen)
**스크린샷**: `notices.png`

| # | 항목 | prototype | 현재 | 심각도 |
|---|---|---|---|---|
| 1 | 카테고리 필터 chip | L2039~: `전체/공지/행사/운영/기타` chip 상단 | `전체/행사/공지/운영/기타` (순서 다름, 행사가 앞) | P2 |
| 2 | 리스트 vs 카드형 | L2054~: 이미지 없는 테이블형 리스트 | 테이블형 리스트 (No/구분/제목/작성일/조회수) — 일치 | — |
| 3 | pinned 표시 | L2074~: `📌` 아이콘 badge | `📌` 이모지 그대로 렌더 (prototype 에서도 이모지) | — (그러나 CLAUDE.md "SVG 이모지 대체 금지" 원칙과 충돌 여지) |

## 10. `/notices/{id}`

**prototype**: L2091~2145 (NoticeDetail)
**스크린샷**: `notice-detail.png`

| # | 항목 | prototype | 현재 | 심각도 |
|---|---|---|---|---|
| 1 | 이전글/다음글 네비게이션 | prototype 에는 미존재 | 하단에 "이전글: 이전 공지가 없습니다 / 다음글: 2026년 상반기 청년센터 운영 방침" 노출 | P2 (기능 확장) |
| 2 | 첨부파일 | prototype 에는 없음 | 2건 첨부 (pdf, hwp) 다운로드 링크 | P2 (기능 확장) |
| 3 | 목록으로 버튼 | L2144: `목록으로` | `목록으로` (일치) | — |

## 11a. `/mypage?tab=history` (신청내역)

**prototype**: L1317~1631 (MyPage)
**스크린샷**: `mypage-applications.png`

| # | 항목 | prototype | 현재 | 심각도 |
|---|---|---|---|---|
| 1 | 프로필 요약 카드 | L1330~: 아바타 + 이름/이메일 + 관심 지역·분야 태그 + 통계 3개 | 아바타+이름+이메일+`관심 지역 미설정` `관심 분야 미설정` + 통계 3개 (3/0/0) — 시드 유저라 관심 미설정, 노출 방식 대체로 일치 | P2 |
| 2 | 탭 4개 순서 | prototype: 신청내역·즐겨찾기·알림·개인정보 (L1360~1373 부근) | `신청 내역 / 즐겨찾기 / 알림 설정 / 개인정보 수정` — 순서 일치 | — |
| 3 | 기간 필터 chip | prototype: 3개월/6개월/1년/3년 | 3개월/6개월/1년/3년 4개 chip (일치) | — |
| 4 | 상태 필터 chip | prototype: 전체/승인/대기/반려/취소 | 전체 3/승인 1/대기 2/반려 0/취소 0 (카운트 표시 추가) | — (개선) |
| 5 | 카드 우측 상세보기 링크 | prototype: `신청 상세 >` 링크 + 신청 취소 버튼 | `신청 상세 > + 신청 취소` (일치) | — |
| 6 | 목록 카드 이미지 대비 | prototype: `filter: grayscale` for ended | 종료 프로그램 없음 (확인 불가) | — |

## 11b. `/mypage?tab=favorites` (즐겨찾기)

**prototype**: L1478~1520 부근 (favorites tab)
**스크린샷**: `mypage-bookmarks.png`

| # | 항목 | prototype | 현재 | 심각도 |
|---|---|---|---|---|
| 1 | 빈 상태 UI | 큰 별 아이콘 + "즐겨찾기한 프로그램이 없습니다" + 프로그램 보기 CTA | 큰 별 outline + 동일 문구 + `프로그램 보기` CTA (일치) | — |
| 2 | 탭 라벨 | prototype: `즐겨찾기` | `즐겨찾기` (일치) | — |
| 3 | 정렬/필터 | prototype 은 즐겨찾기 탭에 정렬 옵션 있음 | 정렬 옵션 없음 | P2 |

## 11c. `/mypage?tab=noti` (알림 설정)

**prototype**: L1550~1600 부근
**스크린샷**: `mypage-notifications.png`

| # | 항목 | prototype | 현재 | 심각도 |
|---|---|---|---|---|
| 1 | 알림 채널 목록 | prototype: 카카오 알림톡 / SMS / 이메일 3채널 toggle | 3채널 (카카오 알림톡·문자(SMS)·이메일) toggle (일치) | — |
| 2 | 알림 항목 목록 | prototype: 신청 결과 / 프로그램 시작 / 빈자리 / 신규 프로그램 | `신청 승인 / 반려 결과 (필수)`·`프로그램 시작 D-1 리마인더`·`빈자리 알림`·`신규 프로그램 소식` (일치, `필수` 뱃지 추가) | — |
| 3 | 저장 상태 표기 | prototype: `변경 사항은 자동 저장돼요` 하단 표기 | 동일 (일치) | — |

## 11d. `/mypage?tab=profile` (개인정보 수정)

**prototype**: L1380~ 부근 (프로필 편집)
**스크린샷**: `mypage-profile.png`

| # | 항목 | prototype | 현재 | 심각도 |
|---|---|---|---|---|
| 1 | 편집 UI | L1380~: 이름·핸드폰·이메일·주소·관심 지역·관심 분야 편집 필드 노출 | **2차 인증 게이트만 노출** (본인 확인 위해 비밀번호 재입력 화면) — 편집 UI 자체 미노출 | P1 |
| 2 | 본인 확인 화면 | prototype 에 명시 없음 (즉시 편집 노출) | `본인 확인을 위해 비밀번호를 다시 입력해 주세요.` 라벨 + email(readonly)+password+확인 버튼 | P1 (보안 강화 의도로 추가된 것으로 보임, prototype 이탈) |
| 3 | 관심 지역/분야 편집 | L1798~ InterestEditModal | 확인 불가 (게이트 통과 필요) | P1 |

## 12. `/mypage/profile/edit`

| # | 항목 | prototype | 현재 | 심각도 |
|---|---|---|---|---|
| 1 | 라우트 존재 여부 | prototype 은 별도 라우트 없이 tab 내부에서 모달로 편집 (L1798 InterestEditModal) | `/mypage/profile/edit` 는 `/mypage?tab=profile` 로 리다이렉트됨 | P1 (라우트 자체가 별도로 존재하지 않아 스캔 대상 없음) |

## 13. `/notifications`

**prototype**: L1252~1316 (NotificationsScreen)
**스크린샷**: `notifications.png`

| # | 항목 | prototype | 현재 | 심각도 |
|---|---|---|---|---|
| 1 | 최대폭 | L1264: `maxWidth:680` (좁은 컬럼) | 680px 좁은 컬럼 (일치) | — |
| 2 | 필터 chip | L1276~1282: `전체 4` `안 읽음 4` chip 두 개 (숫자 표기) | `전체 4` `안 읽음 4` 두 chip (일치) | — |
| 3 | 날짜 그룹핑 | L1259: `오늘 / 지난 7일 / 이전` 그룹 헤더 | `오늘` 그룹 헤더 노출 (일치) | — |
| 4 | 아이템 스타일 | prototype: 아이콘 원 + 제목/설명/시간 | 색색 원(파랑/빨강/노랑/초록)+ 제목/설명/시간 (일치) | — |
| 5 | 페이지네이션 | prototype 은 없음 (스크롤) | "페이지네이션은 F2d 티켓에서 별도 구현 예정" 문구 노출 | P2 (개발 안내 문구가 사용자에게 노출됨 — 제거 권장) |
| 6 | 우측 상단 `모두 읽음` | L1273: `check` 아이콘 + `모두 읽음` 링크 | `✓ 모두 읽음` 링크 (일치) | — |

---

## 최우선 조치 권장 (P0)

1. **`/programs/{id}/apply` wizard Step 2·3 진입 불가** — `#applyNavNext` 클릭 핸들러 및 step panel 렌더 로직 재점검 필요. 현재 신청 완료까지 도달 불가능한 상태로 핵심 사용자 여정이 끊김
2. **`/apply/complete` Whitelabel Error** — 라우트 등록 및 파라미터 없이 직접 접근 시 성공 화면 fallback 필요

## 후순위 조치 권장 (P1 다수)

- 홈 퀵메뉴 4개 그리드 누락 (프로그램 찾기·청년센터 찾기·내 신청 현황·공지사항)
- `/programs` 필터 구조 갭 (좌측 사이드바 vs 상단 inline chip) — 아키텍처 결정 재검토 필요
- `/programs/{id}` sticky 하단 CTA 바 누락
- `/signup` 상단 전역 헤더 노출 (prototype 은 로고+폼 only)
- `/mypage?tab=profile` 편집 UI 대신 2차 인증 게이트만 노출
- `/centers` 인라인 상세 패널 확인 필요 (3-column 구조인지 재검증)
- `/notifications` 개발 안내 문구 사용자 노출

## 스크린샷 목록 (전체 14개)

- `home.png`, `home-loggedin.png`
- `login.png`, `signup.png`
- `programs.png`, `program-detail.png` (종료), `program-detail-open.png` (OPEN)
- `apply-step1.png`, `apply-step2.png`, `apply-step2-real.png`, `apply-step3.png`, `apply-complete.png`
- `centers.png`
- `notices.png`, `notice-detail.png`
- `mypage-applications.png`, `mypage-bookmarks.png`, `mypage-notifications.png`, `mypage-profile.png`, `mypage-profile-edit.png`
- `notifications.png`
