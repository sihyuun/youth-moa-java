# Gap Scan — `/mypage` · `/find-account`

> 개인 PC E2E 검증 세션 준비용 갭 리포트. Prototype (`docs/00_assets/prototype.tsx`) 과 현재 Thymeleaf 구현을 대조하여 시각·구조·기능 갭을 목록화합니다.

---

## §1 스캔 범위 · 방법 · 타임스탬프

| 항목 | 값 |
|---|---|
| 작성일 | 2026-07-10 |
| 대상 브랜치 | `fix/F0h-operating-hours-badge` |
| 스캔 방식 | read-only. prototype.tsx (L1201~1432 MyPage, L1723~1760 LoginScreen) + templates/mypage/**, templates/user/find-*.html, MyPageController.java, FindAccountController.java 정독 후 대조 |
| 검증 도구 | 정적 판독만 (bootRun · Playwright 미실행 — 회사 PC) |
| 산출 규칙 | 심각도 P0 (기능 결손 / 필수 UI 부재) · P1 (레이아웃·시각 갭) · P2 (마이너 문구·톤) |

### Prototype 상 중요 사실

- **`find-id` 라우트가 prototype 에서는 `LoginScreen` 컴포넌트로 매핑됨** (`prototype.tsx:2410`). 즉 prototype 원본에는 별도의 아이디/비밀번호 찾기 화면이 **존재하지 않으며**, 로그인 화면의 "아이디 찾기 / 비밀번호 찾기" 링크 두 개 모두 `go('find-id')` 로 동일 라우트 (=LoginScreen 자체) 로 이동합니다 (prototype.tsx:1746·1748). 현재 구현은 이 부재를 자체 spec (F0i) 으로 보완한 상태입니다 — 따라서 §3 갭 표는 "prototype 결손을 보완한 자체 spec vs 현재 구현" 관점으로 비교합니다.
- MyPage 는 prototype 에 완전한 형태로 정의되어 있어 §2 는 순수 갭 대조가 가능합니다.

---

## §2 `/mypage` 갭 표

### 2.1 레이아웃 · 컨테이너

| 항목 | Prototype | 현재 구현 | 심각도 | 비고 |
|---|---|---|---|---|
| 컨테이너 max-width | `1080px`, padding `36px 80px 56px` (prototype.tsx:1229) | `.container` 클래스에 의존 (main.css 기준 1440px 로 추정) | P1 | 시각 확인 필요 — 프로필 카드가 지나치게 넓게 보일 가능성 |
| 프로필 카드 padding | `26px 30px`, gap 20 (L1231) | `.mypage-summary-inner` (CSS 확인 필요) | P2 | 개인 PC 시각 검증 대상 |
| 카드 그림자·radius | `boxShadow:T.shadow`, `radius:T.radius` (12) | CSS 변수 사용 여부 확인 필요 | P2 | |

### 2.2 프로필 요약 카드

| 항목 | Prototype | 현재 구현 | 심각도 |
|---|---|---|---|
| 아바타 | `<Avatar size={58}/>` (SVG 컴포넌트) | 이름 첫 글자 텍스트 (`layout.html:14` `${#strings.substring(myUser.name,0,1)}`) | P1 |
| 인사말 | `반가워요, 박시현님!` | `반가워요, {name}님!` | ✅ 일치 |
| 서브 정보 (이메일) | `hyuuun0321@naver.com` — email 노출 | `myUser.email` 노출 | ✅ 일치 |
| 관심 태그 | `관심 지역 · 수원시`, `관심 · 취업·창업` (2개) — key-value 형태 (L1237) | `#태그` 형식으로 최대 2개 (L20~24) | P1 — 라벨 형식 차이: prototype 은 `관심 지역 · 수원시`, 구현은 `#태그` |
| KPI | 3개: `진행중인 신청`, `종료된 신청`, `즐겨찾기` (L1244~1247) | 3개: `진행중`, `종료`, `즐겨찾기` (L27~38) | P2 — 라벨 축약. `진행중인 신청` → `진행중` |
| KPI 스타일 | 세로 divider (`borderLeft`), 클릭 시 tab 전환 (L1249) | 클릭 액션 없음, divider 여부 CSS 확인 필요 | P1 |
| KPI 값 색상 | `T.primary` (#3F30E9) | CSS 확인 필요 | P2 |

### 2.3 탭 바

| 항목 | Prototype | 현재 구현 | 심각도 |
|---|---|---|---|
| 스타일 | 세그먼트형 (surface 배경 · 라운드 8 · gap 4 · padding 5), 활성 탭은 `primaryLight` 배경 (L1257~1263) | `<nav>` + `<a>` (URL 기반). `active` 클래스만 부여 (layout.html:43~52) | P1 — prototype 은 client-side 상태로 즉시 전환, 구현은 URL 이동. UX 차이 |
| 탭 라벨 | `신청 현황`, `즐겨찾기`, `알림 설정`, `개인정보 수정` (L1258) | `신청내역`, `즐겨찾기`, `알림설정`, `개인정보` | P2 — 라벨 문구 상이 (`현황` vs `내역`, `수정` 누락) |
| 아이콘 | 각 탭 앞에 `calendar/star/bell/user` SVG icon | 아이콘 없음 (텍스트만) | P1 — SVG 아이콘 누락. 이모지 대체 금지 규칙 준수해 SVG fragment 필요 |

### 2.4 신청 현황 탭 (`history`)

| 항목 | Prototype | 현재 구현 | 심각도 |
|---|---|---|---|
| 섹션 헤더 | `프로그램 신청 내역` + `최대 지난 3년간의 프로그램 신청 내역까지 확인할 수 있어요` (L1269~1270) | `신청 내역` (짧은 h3) — 서브카피 없음 (history.html:20) | P1 |
| 기간 필터 (period) | `3개월 / 6개월 / 1년 / 3년` 4버튼 (L1273) | **미구현** | **P0** — 기능 결손. Controller 에서 period 처리 없음 |
| 상태 필터 칩 | `전체 / 승인 / 대기 / 반려 / 취소` — 각 칩에 카운트 배지 (L1279~1288) | **미구현** — 리스트 전체 노출 | **P0** — 기능 결손 |
| 빈 상태 (empty) | Icon (calendar 64px) + 문구 + CTA (`프로그램 보기`) (L1292~1296) | 문구 + `프로그램 둘러보기` 링크. 아이콘 없음 (L22~25) | P1 |
| 카드 레이아웃 | 상단: `신청일시 + 신청 상세 →` (L1305~1311), 본문: 좌 88px 이미지 + 우 정보 (L1312~1332) | 상단: 신청일 + status badge, 본문: 프로그램 제목 링크 + 액션 버튼 (L28~48). **이미지 · 신청 상세 링크 · 센터/일정 정보 누락** | **P0** |
| 신청일시 포맷 | `2024.07.05 17:11:31` (시분초 포함) | `yyyy.MM.dd` (날짜만) | P2 |
| 상태 뱃지 색상 | 승인=success · 대기=warning · 반려=error · 취소=muted (L1218·1226) | `.category-badge status-*` 클래스 사용 (CSS 확인 필요) | P1 — 색상 매핑 대조 필요 |
| 운영중단 뱃지 | 프로그램이 `status:'중단'` 이면 추가 뱃지 노출 (L1318) | 미구현 | P1 |
| 액션 버튼 (승인/대기) | `Btn variant="dangerOutline" fullWidth` — `신청 취소` (L1323) | `.mypage-cancel-btn` — 스타일 대조 필요 (L38~42) | P2 |
| 액션 버튼 (반려/취소) | 재신청 (secondary) + 신청 취소 (ghost, disabled), 2컬럼 (L1325~1330) | `재신청` 링크만 (L43~45), 취소 disabled 버튼 없음 | P1 |
| 취소 모달 | ConfirmDialog 사용, 사유 5개 radio + 기타 사유 textarea (L1398~1428) | `#cancelModal` 커스텀 모달, 동일 5개 사유 (history.html:53~75) | ✅ 기능 일치, 스타일 대조 필요 |
| 취소 사유 라벨 | `단순 변심 / 일정이 맞지 않음 / 중복 신청 / 개인 사유 / 기타` | 동일 라벨 + reason code `CHANGE_MIND/SCHEDULE_CONFLICT/DUPLICATE_APPLY/PERSONAL/OTHER` | ✅ 일치 |

### 2.5 즐겨찾기 탭 (`favorites`)

| 항목 | Prototype | 현재 구현 | 심각도 |
|---|---|---|---|
| 헤더 | `즐겨찾기한 프로그램` | `즐겨찾기` | P2 |
| 빈 상태 | Icon (star 64px) + 문구 + CTA `프로그램 보기` (L1343~1347) | 문구 + `프로그램 둘러보기`. 아이콘 없음 (L21~24) | P1 |
| 목록 카드 | prototype 원본에는 실제 카드 렌더 코드 없음 (empty state 만 정의) | 프로그램 제목 + 기관 + 지역 노출 (L27~36) | ✅ — 구현이 prototype 을 능가하는 부분 |

### 2.6 알림 설정 탭 (`noti`)

| 항목 | Prototype | 현재 구현 | 심각도 |
|---|---|---|---|
| 섹션 1 헤더 | `알림 받을 방법` + `여러 방법을 동시에 선택할 수 있어요` (L1371~1372) | `알림 채널` + `알림을 받을 채널을 선택하세요.` (L19~20) | P2 |
| 채널 리스트 항목 | 카카오/문자/이메일 — 각 항목에 아이콘 · 라벨 · 서브텍스트(전화번호/이메일) · Toggle 스위치 (L1374~1380) | 3개 항목 — 라벨만 있고 서브텍스트 없음, HTML checkbox 사용 (L23~37) | **P0** — Toggle 스위치 컴포넌트 + 서브텍스트 미구현 |
| 섹션 2 헤더 | `알림 항목` + `받고 싶은 알림만 켜두세요` (L1382~1383) | 없음 | P1 |
| 항목 리스트 | 4개: `신청 승인/반려 결과(필수·lock)`, `D-1 리마인더`, `빈자리 알림`, `신규 프로그램 소식` (L1385) | 필수 항목 1개만 disabled checkbox 로 (L38~41), 나머지 3개 미구현 | **P0** |
| 저장 버튼 | `Btn size="m"` 가운데 정렬, maxWidth 560 (L1392) | `.btn-auth--primary` (스타일 대조 필요) | P2 |

### 2.7 개인정보 수정 탭 (`profile`)

| 항목 | Prototype | 현재 구현 | 심각도 |
|---|---|---|---|
| Step 1 (재확인) 폼 그리드 | `grid-template-columns: 90px 1fr`, gap `14px 16px` (L1356) | `.mypage-verify-form` — visually-hidden label 사용 (L26~35) | P1 |
| Step 1 필드 | `아이디 (readonly, gray bg) + 비밀번호 *` (L1357~1360) | `이메일 (readonly) + 비밀번호` (L28~34) | P1 — 라벨 상이. prototype 은 `아이디` |
| Step 1 안내문 | `회원님의 정보를 안전하게 보호하기 위해 비밀번호를 다시 한번 확인해주세요.` (L1354) | `본인 확인을 위해 비밀번호를 다시 입력해 주세요.` (L20) | P2 — 문구 요약 |
| Step 1 확인 버튼 | 가운데 정렬 `확인` (L1362~1364) | `.btn-auth--primary` (스타일 확인) | ✅ |
| Step 2 (편집) 폼 | prototype 원본에 미정의 — Step 1 성공 후 addToast 만 (L1363) | 이름/전화/주소/생년월일 등 상세 폼 구현 (profile-edit.html:20~66) | ✅ — 구현이 prototype 을 초과 |
| TTL 안내 | prototype 미정의 | 10분 세션 관리 (`PROFILE_VERIFY_TTL_MINUTES`) 화면 안내 없음 | P1 — 사용자에게 TTL 안내 문구 필요 여부 검토 |

---

## §3 `/find-account` 갭 표

> **주의**: prototype 은 이 화면들을 미구현 상태로 두었고, 현재 구현은 자체 spec (F0i) 기반. 아래는 자체 spec 관점에서 prototype 톤·컴포넌트 재사용 여부·UX 일관성 관점으로 검토합니다.

### 3.1 공통 (auth 화면 톤)

| 항목 | Prototype (LoginScreen 참조) | 현재 구현 | 심각도 |
|---|---|---|---|
| 폭 | 400px 고정 (LoginScreen L1734) | 480px (`auth-screen-inner--wide` — 주석에 명시) | P2 — 의도된 차별 |
| 헤더 | 없음 (`noHeader` 목록 포함, L2393) | 없음 (`auth-page-body`) | ✅ |
| 푸터 | 있음 (L1757) | 있음 (모든 페이지) | ✅ |
| 로고 | 상단 중앙, `height:36`, `margin:0 auto 28px` (L1735) | `auth-logo-img` (CSS 대조 필요) | P2 |
| 인풋 스타일 | `height:46, radius:8, border:1.5px solid ${input?T.primary:T.border}` — focus/filled 시 primary 색 (L1738) | `.auth-input` (CSS 확인 필요) | P2 |
| CTA 버튼 | `Btn size="l" fullWidth` (primary/secondary) (L1752~1753) | `.btn-auth--primary/--secondary/--ghost` | ✅ |

### 3.2 `/find-id` (Step 1 — 가입정보 입력)

| 항목 | Spec 의도 | 현재 구현 | 심각도 |
|---|---|---|---|
| Stepper | ① 가입정보 입력 (active) → ② 아이디 찾기 (L30~33) | 구현됨 | ✅ |
| 폼 필드 | 이름 + 핸드폰 (L40~52) | 구현됨 | ✅ |
| 에러 렌더 | 필드별 `#fields.hasErrors` + 상단 `alert-error` (L35) | 두 방식 병행 | ✅ |
| autofocus | 첫 필드에 부여되어야 UX 양호 | `autofocus` 속성 없음 | P2 |
| autocomplete | `autocomplete="name"`, `"tel"` | 부여됨 (L42, 49) | ✅ |
| CTA | `확인` + `로그인으로 돌아가기` (L54~57) | 구현됨 | ✅ |

### 3.3 `/find-id` 결과 (Step 2)

| 항목 | Spec 의도 | 현재 구현 | 심각도 |
|---|---|---|---|
| Stepper | ② active | 구현됨 (L27) | ✅ |
| 결과 박스 | `abc***@youth-moa.test` 마스킹 이메일 노출 (L32) | 구현됨 (`EmailMaskingUtil.mask()`) | ✅ |
| 마스킹 정책 | 앞 3자 + `***` + 도메인 유지 (일반적) | 서비스 코드 미확인 — 개인 PC 검증 대상 | P2 |
| CTA | 로그인하기 (primary) + 비밀번호 찾기 (ghost) | 구현됨 (L36~37) | ✅ |
| 재시도 안내 | 없음 | 없음 | ✅ |

### 3.4 `/find-password` (Step 1 — 본인 확인)

| 항목 | Spec 의도 | 현재 구현 | 심각도 |
|---|---|---|---|
| Stepper | ① 본인 확인 → ② 새 비밀번호 설정 | 구현됨 | ✅ |
| 폼 필드 | 이메일 + 이름 + 핸드폰 (find-id 대비 이메일 1개 추가) | 구현됨 (L35~54) | ✅ |
| 에러 렌더 | 실패 시 `일치하는 계정이 없습니다.` (Controller L94) | 구현됨 (L30) | ✅ |
| 지연 응답 | 계정 미매칭 시 200ms sleep (열거 공격 완화) | 구현됨 (`MISMATCH_DELAY_MS`) | ✅ 보안 조치 |
| autofocus | 첫 필드 (email) 부여 검토 | 없음 | P2 |
| CTA | `확인` + `로그인으로 돌아가기` | 구현됨 | ✅ |

### 3.5 `/find-password/reset` (Step 2 — 새 비밀번호)

| 항목 | Spec 의도 | 현재 구현 | 심각도 |
|---|---|---|---|
| Stepper | ② active | 구현됨 | ✅ |
| 세션 가드 | verifiedUserId + verifiedAt 10분 TTL | 구현됨 (`isVerified()`) | ✅ |
| TTL 만료 처리 | `/find-password` 로 redirect | 구현됨 | ✅ |
| 폼 필드 | 새 비밀번호 + 확인 (L37~50) | 구현됨 | ✅ |
| 비밀번호 규칙 안내 | placeholder `영문+숫자 포함 8자 이상` (L38) | 구현됨 | ✅ (다만 label/hint 로 승격 고려) |
| 확인 미일치 처리 | `bindingResult.rejectValue("passwordConfirm", "mismatch", ...)` (Controller L128~129) | 구현됨 | ✅ |
| 재설정 성공 후 | `/login?reset` 로 redirect, session invalidate | 구현됨 | ✅ (로그인 화면에 성공 배너 표시 필요 여부 검토 대상) |
| `/login?reset` 배너 | login 화면에서 `?reset` 파라미터 감지해 성공 토스트/배너 표시 여부 확인 필요 | login.html 미확인 | P1 — 개인 PC 검증 시 확인 필요 |
| 취소 CTA | 없음 (Step 2 에서 되돌아가기 링크 부재) | 없음 | P2 |

### 3.6 접근성

| 항목 | 개선 필요 여부 | 심각도 |
|---|---|---|
| Stepper `aria-label` | 이미 `aria-label="진행 단계"` 부여 | ✅ |
| Stepper 현재 단계 `aria-current` | 부여 안 됨 (`auth-step--active` 클래스만) | P1 |
| Error alert `role="alert"` | 부여 안 됨 | P1 |
| 첫 인풋 `autofocus` | 부여 안 됨 (모든 auth 화면) | P2 |
| 폼 label — 시각 label 없음 (placeholder-only) | prototype LoginScreen 도 placeholder-only 이나 접근성상 `<label class="visually-hidden">` 부여 권장 | P1 |

---

## §4 종합 우선순위 — 개인 PC 시각 검증 체크리스트

### P0 (기능 결손 — 스펙 대비 미구현)

1. **`/mypage?tab=history` 의 기간 필터 (3개월/6개월/1년/3년)** 존재 여부 확인. 미구현 확정 시 후속 티켓 발행.
2. **`/mypage?tab=history` 의 상태 필터 칩** (전체/승인/대기/반려/취소 + 각 카운트) 부재 확인.
3. **`/mypage?tab=history` 카드에 프로그램 이미지 · 센터/일시 정보 · 신청 상세 링크** 부재 확인.
4. **`/mypage?tab=noti` 알림 항목 섹션** (D-1 리마인더/빈자리 알림/신규 소식) 부재 확인 + Toggle 스위치 컴포넌트 부재 확인.

### P1 (레이아웃 · 시각 갭 — 브라우저 대조 필요)

5. **탭 바 세그먼트형 스타일** 렌더 확인 — 활성 탭 배경 `primaryLight`, 라운드 8, 아이콘 부착 여부.
6. **탭 아이콘 (calendar/star/bell/user)** — SVG 부착 (이모지 금지 규칙 준수 필요).
7. **프로필 카드 KPI 클릭 가능 여부** — 클릭 시 해당 탭으로 전환되는지.
8. **취소 모달 스타일** — ConfirmDialog 톤과 시각 대조.
9. **`/login?reset` 배너** — 비밀번호 재설정 성공 후 로그인 화면에 성공 토스트 노출 여부.
10. **Stepper `aria-current`, error `role="alert"` 접근성 속성** 부재 확인.
11. **폼 필드 시각 label (visually-hidden)** 부재 확인.
12. **비밀번호 확인 미일치 인라인 에러** 실제 렌더 확인 (`hasErrors('passwordConfirm')` 경로).
13. **프로필 요약 카드 KPI 라벨 (`진행중` vs `진행중인 신청`)** 확정.
14. **관심 태그 라벨 형식** — prototype 은 `관심 지역 · 수원시` key-value, 구현은 `#태그` 해시. 어느 쪽 유지할지 확정.
15. **profile-verify 라벨** — prototype 은 `아이디`, 구현은 `이메일` (사실상 이메일 로그인이므로 구현이 맞음). 라벨 문구를 `이메일 (아이디)` 로 통일할지 결정.

### P2 (문구·톤 통일)

16. **탭 라벨 문구** — `신청 현황 / 신청내역`, `알림 설정 / 알림설정`, `개인정보 수정 / 개인정보` 중 어느 쪽 확정.
17. **신청일시 포맷** — `yyyy.MM.dd HH:mm:ss` vs `yyyy.MM.dd` 결정.
18. **에러 메시지 어조** — CLAUDE.md "메시지 어조 통일 규칙 (안 B)" `~해야 합니다.` 패턴 준수 확인.
19. **폼 첫 필드 `autofocus`** — 4개 auth 페이지 모두 부여.
20. **비밀번호 규칙 placeholder → hint 승격** — 접근성·시인성 개선.

---

## §5 관련 파일 경로 인덱스

### Prototype
- `C:\Users\User\IdeaProjects\youth-moa-java\docs\00_assets\prototype.tsx`
  - MyPage: L1201~1432
  - ApplicationDetail (참고): L1434~1500
  - LoginScreen (find-id 라우트 매핑 확인): L1723~1760
  - 라우팅 매핑 (`find-id`→LoginScreen): L2410

### `/mypage` 구현
- Controller: `C:\Users\User\IdeaProjects\youth-moa-java\src\main\java\io\github\sihyuuun\youthmoa\user\MyPageController.java`
- Templates:
  - `C:\Users\User\IdeaProjects\youth-moa-java\src\main\resources\templates\mypage\layout.html` (공통 헤더 + 탭)
  - `C:\Users\User\IdeaProjects\youth-moa-java\src\main\resources\templates\mypage\history.html`
  - `C:\Users\User\IdeaProjects\youth-moa-java\src\main\resources\templates\mypage\favorites.html`
  - `C:\Users\User\IdeaProjects\youth-moa-java\src\main\resources\templates\mypage\notifications.html`
  - `C:\Users\User\IdeaProjects\youth-moa-java\src\main\resources\templates\mypage\profile-verify.html`
  - `C:\Users\User\IdeaProjects\youth-moa-java\src\main\resources\templates\mypage\profile-edit.html`
- 관련 DTO: `ProfileUpdateRequest`, `NotificationChannelRequest` (동일 패키지)
- 신청 취소 로직: `C:\Users\User\IdeaProjects\youth-moa-java\src\main\java\io\github\sihyuuun\youthmoa\application\MyPageCancelController.java`

### `/find-account` 구현 (find-id · find-password)
- Controller: `C:\Users\User\IdeaProjects\youth-moa-java\src\main\java\io\github\sihyuuun\youthmoa\user\FindAccountController.java`
- Service: `C:\Users\User\IdeaProjects\youth-moa-java\src\main\java\io\github\sihyuuun\youthmoa\user\FindAccountService.java`
- DTO:
  - `FindIdRequest.java`
  - `FindPasswordRequest.java`
  - `PasswordResetRequest.java` (동일 패키지에 위치 추정)
- Templates:
  - `C:\Users\User\IdeaProjects\youth-moa-java\src\main\resources\templates\user\find-id.html`
  - `C:\Users\User\IdeaProjects\youth-moa-java\src\main\resources\templates\user\find-id-result.html`
  - `C:\Users\User\IdeaProjects\youth-moa-java\src\main\resources\templates\user\find-password.html`
  - `C:\Users\User\IdeaProjects\youth-moa-java\src\main\resources\templates\user\find-password-reset.html`
- 유틸: `EmailMaskingUtil` (동일 패키지 추정)

### 참조 자산
- `C:\Users\User\IdeaProjects\youth-moa-java\docs\00_assets\prototype.html` (렌더 결과)
- `C:\Users\User\IdeaProjects\youth-moa-java\docs\00_assets\HANDOFF.md`
- CSS: `C:\Users\User\IdeaProjects\youth-moa-java\src\main\resources\static\css\main.css` (`--color-primary` 등 디자인 토큰)
- Icon fragment (SVG 재사용): `C:\Users\User\IdeaProjects\youth-moa-java\src\main\resources\templates\fragments\icons.html` (존재 시)
