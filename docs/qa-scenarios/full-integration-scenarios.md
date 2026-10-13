# youth-moa-java 전체 통합 테스트 시나리오

> ⚠️ **Draft — 검토 후 확정.** 산출 시점: 2026-09-30. 대상 브랜치: `main` (커밋 `4d51b56` 이후, PR #216~#226 반영).
> QA 담당자·개발자가 문서만 보고 브라우저에서 그대로 재현할 수 있도록 UI 관점으로 작성됨.
> Playwright 자동화 이식은 별도 티켓 (하단 "이월" 절 참조).

---

## 개요

### 스코프

- **사용자 트랙 (User)** — 회원가입 · 로그인 · 홈 · 프로그램 탐색 · 신청 · 마이페이지 · 알림 · 공지사항 · 청년센터
- **관리자 트랙 (Admin)** — admin 로그인 (강제 비밀번호 변경) · 대시보드 · 프로그램/신청/사용자 관리 · 관리자 계정 발급 · 공지·약관·동적 필드·자격요건 관리 · 통계 대시보드
- **크로스 flow** — 사용자 액션이 admin 알림 벨/목록에 반영되는 3축 fan-out 및 그 반대 방향

### 우선순위 정의

| 태그 | 정의 | 예상 실행 빈도 |
|---|---|---|
| **P0** | 릴리즈 전 필수 회귀. 서비스 근간(가입·로그인·신청·승인) | 전 릴리즈 |
| **P1** | 주요 flow · 재발 티켓 축적 영역 (알림 fan-out · rate-limit · 통계) | 격주 |
| **P2** | 엣지 케이스 · 이월 항목 검증 (SMS 실패 재시도 · 프로필 편집 재인증) | 월간 or 회귀 이슈 시 |

총 시나리오 30개: **P0 15 · P1 10 · P2 5**

---

## 사전 조건

### 실행 환경

| 항목 | 값 |
|---|---|
| 서버 기동 | `./gradlew bootRun` (기본) 또는 `bootrun-e2e.cmd` (E2E 프로파일, H2 in-memory) |
| 기본 포트 | 8080 (기본) / 8090 (E2E 로컬) / 8080 (CI) |
| 브라우저 | Chrome 최신 · Firefox 최신 (반응형은 375px · 768px · 1440px) |
| DB seed | `DataInitializer` 자동 실행 (프로그램 34건 · 사용자 30건 · 공지·약관·동적 필드 등 포함) |
| 프로파일 | 기본 `application.yml` (PostgreSQL) 또는 E2E (`application-e2e.properties`, H2) |

### 시드 계정 (2026-09-30 실측)

| 이메일 | 비밀번호 | 역할 | 비고 |
|---|---|---|---|
| `sysadmin@youth-moa.test` | `Admin!234` (기본값, `admin.seed.password.system` 로 override) | SYSTEM_ADMIN | 전체 권한 |
| `center1@youth-moa.test` | `Admin!234` | CENTER_ADMIN | centers[0] 소속 |
| `center2@youth-moa.test` | `Admin!234` | CENTER_ADMIN | centers[1] 소속 |
| `admin_center2@youth-moa.test` | `Admin!234` | CENTER_ADMIN | centers[2] 소속 (A7 3축 fan-out 검증용) |
| `admin_inactive@youth-moa.test` | `Admin!234` | CENTER_ADMIN | `isActive=false`, 알림 skip 검증용 |
| `seed1@youth-moa.test` ~ `seed30@youth-moa.test` | `Test1234!` | USER | 일반 사용자 (관심 지역·분야 세팅됨) |

> **비밀번호 override**: 운영/스테이징 배포 시 `admin.seed.password.system` 등 환경변수로 재정의. QA 환경에서는 기본값 사용.

### 실행 방식

- **수동 재현**: 브라우저에서 각 시나리오 Steps 를 순차 실행
- **Playwright 참조**: 각 시나리오 하단 "커버 spec" 에 관련 자동화 스펙 명시. 로컬에서 `npx playwright test <spec-path>` 로 회귀 실행 가능
- **크로스 flow (2 세션)**: 서로 다른 브라우저 프로필 (또는 시크릿 창) 두 개를 열어 USER 브라우저 A / admin 브라우저 B 로 분리 로그인

---

## 시나리오 카탈로그

### === 사용자 트랙 (P0 회원가입~신청) ===

---

### T1. 회원가입 (SMS 인증 + 이메일 + 관심 지역/분야) — P0

**Pre-condition**: 미로그인 상태. `010-9999-0001` 은 DB 미등록.

**Steps**:
1. 홈(`/`) 우상단 "회원가입" 클릭 → `/signup` 이동
2. 약관 4종 각각 체크 (또는 "전체 동의" 체크) → "다음" 활성화 확인
3. 이름 `홍길동`, 생년월일 `19950115`, 성별 `남성` 선택
4. 휴대폰 `01099990001` 입력 → "인증번호 발송" 클릭 → 토스트 "인증번호가 발송되었습니다" 확인
5. 인증번호 입력창에 개발용 코드 (콘솔/서버 로그에 출력, 또는 DevProfile 은 `000000`) 입력 → "인증" 클릭 → 초록 체크 표시
6. 이메일 `newuser01@test.com`, 비밀번호 `Newpass!234`, 비밀번호 확인 재입력 → "다음"
7. 관심 지역 화면: 서울 `강남구`, `서초구` 2개 선택 (최대 5개 제한 확인)
8. 관심 분야 화면: `취업지원`, `창업지원` 2개 선택
9. "가입 완료" 클릭

**Expected**:
- WelcomeScreen (`/welcome`) 렌더 · "홍길동님, 환영합니다" 문구
- 3초 후 또는 "홈으로" 클릭 시 `/` 이동, 우상단이 "홍길동님" 표시 (로그인 상태)
- DB: `lms_user` 신규 row · `user_agreement` 4건 · `user_interest_region`/`user_interest_field` 2건씩

**Post-condition**: 신규 유저 로그인 상태.

**커버 spec**: `e2e/tests/signup.spec.ts`, `visual-signup.spec.ts`, `docs/design-contracts/signup.md`

---

### T2. 로그인 · 자동 로그인 유지 — P0

**Pre-condition**: `seed1@youth-moa.test` 존재.

**Steps**:
1. `/login` 접속
2. 이메일 `seed1@youth-moa.test`, 비밀번호 `Test1234!` 입력
3. "자동 로그인" 체크박스 체크
4. "로그인" 클릭
5. 홈(`/`) 이동, 우상단 사용자명 표시 확인
6. 브라우저 완전 종료 후 재기동 → `/` 재접속

**Expected**:
- Step 5: 이전 접속 이력에 따라 홈 또는 원래 요청 페이지로 redirect
- Step 6: 여전히 로그인 상태 (Remember-me 쿠키 유효)

**Post-condition**: seed1 로그인 유지.

**커버 spec**: `login.spec.ts`, `visual-login.spec.ts`, `design-contracts/login.md`

---

### T3. 홈 화면 (Hero · 관심 지역 · 추천 프로그램 · 검색바) — P0

**Pre-condition**: `seed1@youth-moa.test` 로그인. 관심 지역 = 강남구·서초구.

**Steps**:
1. `/` 접속
2. Hero 배너 확인 — 3장 이미지 자동 rotation (약 5초 간격) 확인
3. "관심 지역 프로그램" 섹션 스크롤 → 강남구·서초구 소재 프로그램 카드만 표시
4. "추천 프로그램" 섹션 확인 (마감임박·인기순 등)
5. 상단 검색바에 `창업` 입력 → Enter
6. `/search?q=창업` 이동, 검색 결과 리스트 확인

**Expected**:
- Hero 좌우 화살표 hover 시 노출 · 클릭 시 수동 이동 가능
- 관심 지역 섹션이 비어있으면 "관심 지역을 설정하세요" CTA
- 검색 결과에 프로그램·공지 카드 각각 별도 섹션

**커버 spec**: `home.spec.ts`, `home-hero-rotation.spec.ts`, `visual-home.spec.ts`

---

### T4. 프로그램 목록 (필터 · 검색 · 정렬 · 페이징 · 즐겨찾기) — P0

**Pre-condition**: 로그인 (seed1).

**Steps**:
1. 헤더 "프로그램" 클릭 → `/programs`
2. 좌측 필터: 지역 = 강남구, 분야 = 취업지원 체크
3. 목록에 조건 매칭 프로그램만 표시 확인
4. 정렬 드롭다운 → "마감임박순" 변경 → 재정렬
5. 검색바에 `청년` 입력 → Enter
6. 페이징 하단 "다음" 클릭 → 2페이지 이동, URL 에 `?page=2`
7. 임의 프로그램 카드의 하트(즐겨찾기) 아이콘 클릭
8. 하트가 채워짐 (색상 변경), 페이지 새로고침 후에도 유지

**Expected**:
- 필터 변경 시 URL query string 갱신 · 뒤로가기 시 필터 상태 복원
- 정렬 옵션: 등록순 · 마감임박순 · 인기순 (prototype 확인)
- 즐겨찾기 토글은 즉시 (로그인 없으면 로그인 페이지로 redirect)

**커버 spec**: `program-list.spec.ts`, `programs-calendar.spec.ts`, `bookmark.spec.ts`, `design-contracts/programs.md`

---

### T5. 프로그램 상세 · 자격요건 · 즐겨찾기 · 신청 CTA — P0

**Pre-condition**: 로그인 (seed1). 신청 미완료 프로그램 존재.

**Steps**:
1. 프로그램 목록에서 임의 항목 클릭 → `/programs/{id}`
2. 상단 대표 이미지 · 제목 · 센터명 · 신청기간 · 정원 progress bar 확인
3. "자격요건" 섹션에 나이·거주지·직업 조건 리스트 확인
4. 우측 (또는 하단) 사이드바의 "신청하기" 버튼 상태 확인 — 자격 미충족 시 disabled + 사유 툴팁
5. 하트 아이콘 클릭 → 즐겨찾기 추가
6. "신청하기" 클릭 (자격 충족 케이스) → `/apply/{programId}` 이동

**Expected**:
- viewCount 1 증가 (같은 세션 5분 내 재조회는 dedup, admin 조회는 skip)
- 자격 미충족 사유가 사람이 읽을 수 있는 문장 (예: "만 25세 이상만 신청 가능")

**커버 spec**: `program-detail.spec.ts`, `visual-program-detail.spec.ts`, `design-contracts/program-detail.md`

---

### T6. 프로그램 신청 (3단계 wizard) — P0

**Pre-condition**: T5 에서 "신청하기" 클릭 후.

**Steps**:
1. Step 1 — 자격요건 확인 체크박스 전부 체크 → "다음"
2. Step 2 — 개인정보 화면: 프로필에서 미리 채워진 필드 확인, 비어있는 필드 입력 (직업·소속기관 등)
3. 동적 필드 (관리자가 설정한 추가 질문) 답변 입력
4. Step 3 — 최종 확인 화면: 입력값 요약 확인 → "신청 완료"
5. `/apply/{id}/complete` 이동, "신청이 접수되었습니다" 문구 확인

**Expected**:
- 각 Step 이동 시 이전 Step 값 유지 (뒤로가기 시 재입력 불필요)
- 필수 필드 미입력 시 인라인 에러 · 다음 버튼 disabled
- 완료 시 DB `application` row 생성 (status=PENDING)

**Post-condition**: seed1 이 해당 프로그램 신청 완료 상태.

**커버 spec**: `apply.spec.ts`, `apply-complete.spec.ts`, `apply-dynamic-response.spec.ts`, `design-contracts/apply.md`

---

### T7. 마이페이지 4탭 (신청내역 · 즐겨찾기 · 프로필 · 알림설정) — P0

**Pre-condition**: T6 완료 후 seed1 로그인 유지.

**Steps**:
1. 우상단 사용자명 클릭 → 드롭다운 "마이페이지" → `/mypage`
2. "신청내역" 탭: T6 신청건이 "신청완료" 상태로 표시 확인
3. 신청건 우측 "취소" 클릭 → 확인 다이얼로그 → "취소하기"
4. 상태가 "취소" 로 변경 확인
5. "즐겨찾기" 탭 클릭 → T4/T5 에서 담은 프로그램 카드 리스트 확인
6. "프로필" 탭 → 이름·이메일·휴대폰·관심 지역·분야 요약 확인
7. "회원 탈퇴" 링크 확인 (실제 클릭하지 말 것 — T30 에서 검증)

**Expected**:
- 취소 시 admin 알림 발송 (T21 참조)
- 즐겨찾기 카드 클릭 시 프로그램 상세로 이동

**커버 spec**: `mypage-interactions.spec.ts`, `visual-mypage.spec.ts`, `design-contracts/mypage.md`

---

### === Admin 트랙 (P0 admin 필수 flow) ===

---

### T8. Admin 로그인 · 강제 비밀번호 변경 — P0

**Pre-condition**: SYSTEM_ADMIN 이 신규 관리자 계정을 발급한 직후 상태 (T14 에서 생성된 계정).

**Steps**:
1. `/admin/login` 접속
2. 신규 발급 계정 이메일 + 초기 비밀번호 (SecureRandom 12자, T14 에서 사용자에게 전달) 입력 → 로그인
3. 서버가 `passwordChangeRequired=true` 감지 → `/admin/password-change` 로 강제 redirect
4. 현재 비밀번호 (초기) + 새 비밀번호 (`NewPass!2345`) + 확인 입력 → "변경"
5. `/admin` 대시보드 이동

**Expected**:
- Step 3 우회 시도 (`/admin/programs` 직접 진입) 도 password-change 로 강제 redirect
- 비밀번호 정책 위반 시 인라인 에러 (12자 미만·4문자 클래스 미충족 등)

**커버 spec**: `admin-login.spec.ts`, `visual-admin-login.spec.ts`, `design-contracts/admin/login.md`

---

### T9. Admin 대시보드 · 헤더 알림 벨 — P0

**Pre-condition**: `sysadmin@youth-moa.test` / `Admin!234` 로그인.

**Steps**:
1. `/admin` 접속
2. 상단 KPI 카드 4종 (총 회원 · 총 프로그램 · 오늘 신청 · 미처리) 확인
3. 방문자 라인차트 · 성별/연령 도넛 · 마감임박 리스트 렌더 확인
4. 헤더 우측 벨 아이콘 확인 — 미확인 알림 있으면 빨간 badge + 숫자
5. 벨 클릭 → 드롭다운에 최근 알림 5건 리스트
6. 알림 클릭 → 해당 대상 (프로그램 상세 · 신청건 등) 으로 이동

**Expected**:
- 벨은 30초 주기 HTMX polling 으로 갱신
- badge 개수가 unread 만 카운트

**커버 spec**: `admin-dashboard.spec.ts`, `admin-header-notifications.spec.ts`, `admin-stats.spec.ts`

---

### T10. Admin 프로그램 CRUD (3탭 폼) — P0

**Pre-condition**: `center1@youth-moa.test` 로그인 (CENTER_ADMIN, centers[0] 만 관리).

**Steps**:
1. `/admin/programs` 이동 → centers[0] 소속 프로그램만 목록에 표시 확인
2. "새 프로그램" 클릭 → `/admin/programs/new` 이동, 3탭 (기본정보 · 상세 · 첨부) 확인
3. 기본정보 탭: 제목 `테스트 프로그램` · 분야 · 신청기간 (`2026-10-01 ~ 2026-10-15`) · 운영기간 (`2026-10-20 ~ 2026-11-30`) · 정원 `30` 입력
4. 상세 탭: 리치 에디터에 설명 입력 · course (커리큘럼) 3회차 추가
5. 첨부 탭: 대표 이미지 업로드 · PDF 첨부 파일 추가
6. "저장" 클릭 → 목록 이동 · 신규 항목 최상단 확인
7. 목록의 신규 항목 클릭 → 상세 편집 → 제목에 " (수정)" 추가 → "저장"
8. 목록으로 돌아와 우측 "삭제" → 확인 다이얼로그 → 삭제

**Expected**:
- 다른 센터 프로그램 수정/삭제 시도 시 403
- 신청기간·운영기간 순서 검증 (start ≤ end · 신청종료 ≤ 운영시작)

**커버 spec**: `admin-program-form.spec.ts`, `admin-programs-list.spec.ts`, `admin-program-center-fk.spec.ts`

---

### T11. Admin 신청 관리 · 개별 승인/반려 — P0

**Pre-condition**: `center1@youth-moa.test` 로그인. centers[0] 프로그램에 seed1 이 신청한 건 존재 (T6 참조, 미리 준비).

**Steps**:
1. `/admin/programs` → 대상 프로그램 클릭 → 상세 진입
2. "신청 관리" 탭 클릭 → 신청자 리스트에 seed1 표시
3. seed1 행의 "승인" 클릭 → 담당자 의견 입력 다이얼로그 → 확인
4. 상태가 "승인" 으로 변경
5. 다른 신청건에서 "반려" 클릭 → 반려 사유 (필수) 입력 → 확인
6. 상태 "반려" 로 변경

**Expected**:
- 승인/반려 시 신청자에게 알림 발송 (T20 크로스 flow 참조)
- 반려 사유 미입력 시 다이얼로그 확인 버튼 disabled

**커버 spec**: `admin-program-form.spec.ts` (일부), `qa-checklists/program-applications.md`

---

### T12. Admin 사용자 관리 · 검색 · 차단 · 역할 변경 — P0

**Pre-condition**: `sysadmin@youth-moa.test` 로그인.

**Steps**:
1. `/admin/users` 이동
2. 검색 바에 `seed15` 입력 → 결과 필터링
3. seed15 행 클릭 → 상세 화면
4. "역할 변경" 드롭다운 → CENTER_ADMIN → 소속 센터 선택 → 저장
5. 목록 복귀 후 seed15 재검색 → 역할 컬럼 변경 확인
6. seed15 상세에서 "계정 차단" → 사유 입력 → 확인
7. seed15 로 로그인 시도 (다른 브라우저) → "차단된 계정입니다" 에러

**Expected**:
- 차단은 soft (`isActive=false`), 탈퇴는 hard delete (T30 참조)
- SYSTEM_ADMIN 만 역할 변경 가능 (CENTER_ADMIN 시도 시 403)

**커버 spec**: `admin-users.spec.ts`, `admin-users-actions.spec.ts`, `admin-user-detail.spec.ts`

---

### T13. Admin 공지 CRUD · 파일 업로드 — P0

**Pre-condition**: `sysadmin@youth-moa.test` 로그인.

**Steps**:
1. `/admin/notices` → "새 공지" 클릭
2. 제목 `테스트 공지 2026-09-30` · 본문 · 첨부 파일 (PDF 5MB 이하) 업로드
3. "게시" 클릭 → 목록 최상단 확인
4. 사용자 브라우저 (별도 세션) 로 `/notices` 접속 → 신규 공지 확인
5. 공지 클릭 → 첨부 파일 다운로드 가능 확인
6. admin 으로 돌아와 공지 편집 → 제목 " (수정)" 추가 → 저장

**Expected**:
- 파일 크기 초과 시 인라인 에러
- 삭제 시 첨부 파일도 함께 삭제

**커버 spec**: `admin-notice-form.spec.ts`, `admin-notice-upload.spec.ts`, `notices.spec.ts`

---

### T14. Admin 관리자 계정 발급 (SecureRandomPassword) — P0

**Pre-condition**: `sysadmin@youth-moa.test` 로그인.

**Steps**:
1. `/admin/users` → "새 관리자" 클릭 → `/admin/users/new`
2. 이메일 `newadmin01@test.com` · 이름 · 역할 CENTER_ADMIN · 소속 센터 선택
3. "생성" 클릭
4. 결과 화면에 자동 생성 비밀번호 (12자, 4문자 클래스) 노출 확인
5. 이 비밀번호를 복사해서 T8 시나리오로 이관

**Expected**:
- 비밀번호는 최초 1회만 노출 (재조회 불가)
- 생성 즉시 `passwordChangeRequired=true` 세팅

**커버 spec**: `admin-staff-management.spec.ts`, `A5-1-admin-staff-management-qa-report.md`

---

### === 크로스 flow (P0 fan-out) ===

---

### T15. USER 신청 → CENTER_ADMIN 승인 → USER 알림 — P0 크로스

**Pre-condition**: 2 세션 준비.
- 브라우저 A: `seed1@youth-moa.test` (USER)
- 브라우저 B: `center1@youth-moa.test` (CENTER_ADMIN, centers[0])

**Steps**:
1. [A] `/programs` → centers[0] 소속 프로그램 상세 → "신청하기" → 3단계 완료
2. [B] `/admin` 벨 badge +1 확인 · 벨 클릭 · "NEW_APPLICATION" 알림 확인
3. [B] 알림 클릭 → 해당 신청건 상세 → "승인" 클릭 → 확인
4. [A] 헤더 벨 (또는 마이페이지) 새로고침 → "신청이 승인되었습니다" 알림 확인
5. [A] 마이페이지 신청내역 → 상태 "승인" 확인

**Expected**:
- Step 2 알림은 30초 이내 (polling 주기) 자동 도착
- 3축 fan-out: `program.createdBy` · centers[0] CENTER_ADMIN · watcher chain
- 다른 센터 admin (center2) 은 이 알림을 받지 않음

**커버 spec**: `admin-notification-fanout.spec.ts`, `notification-bell.spec.ts`

---

### === 사용자 트랙 (P1 주요 flow) ===

---

### T16. 소셜 로그인 (Google/Kakao) — P1

**Pre-condition**: 소셜 provider 로 최초 접속.

**Steps**:
1. `/login` → "Google 로 로그인" 클릭
2. Google OAuth 화면에서 계정 선택
3. 최초 가입이면 관심 지역·분야 설정 화면 (T1 Step 7~8) 이동
4. 기존 유저이면 홈으로 직행

**Expected**:
- 이메일 중복 시 기존 계정에 소셜 연결 병합 (정책 확인 필요 · draft)

**커버 spec**: (없음 — 이월)

---

### T17. 검색 결과 페이지 (프로그램 + 공지 통합) — P1

**Pre-condition**: 로그인 무관.

**Steps**:
1. 헤더 검색바에 `창업` 입력 → Enter → `/search?q=창업`
2. "프로그램 (N)" · "공지사항 (M)" 탭 각각 확인
3. 프로그램 탭에서 카드 클릭 → 프로그램 상세 이동
4. 브라우저 뒤로가기 → 검색 결과 페이지 유지 확인

**Expected**:
- 결과 0건 시 "검색어에 해당하는 결과가 없습니다" placeholder
- 검색어 하이라이트 (제목·본문)

**커버 spec**: (탐색 커버리지 낮음 — 이월)

---

### T18. 청년센터 지도 · 목록 — P1

**Pre-condition**: 로그인 무관.

**Steps**:
1. 헤더 "청년센터" → `/centers`
2. 지도 로드 확인 · 마커 클릭 시 팝오버로 센터 요약
3. 좌측 리스트에서 항목 클릭 → 지도 해당 위치로 이동 + 상세 패널 노출
4. 상세 패널 "이 센터 프로그램 보기" → `/programs?centerId={id}` 로 필터링된 목록

**커버 spec**: `visual-centers.spec.ts`, `design-contracts/centers.md`

---

### T19. 공지사항 상세 · 첨부 다운로드 — P1

**Pre-condition**: 공지 존재 (seed 또는 T13 결과).

**Steps**:
1. `/notices` → 임의 공지 클릭
2. 제목 · 게시일 · 본문 렌더 확인
3. 첨부 파일 링크 클릭 → 다운로드 성공
4. 하단 "이전글/다음글" 네비 확인 (없으면 목록 링크만)

**커버 spec**: `notices.spec.ts`, `visual-notice-detail.spec.ts`

---

### T20. 헤더 알림 벨 (사용자) — P1

**Pre-condition**: seed1 로그인 · 미확인 알림 존재.

**Steps**:
1. 임의 페이지에서 우상단 벨 아이콘 확인 → badge 숫자
2. 벨 클릭 → 최근 5건 드롭다운
3. 알림 클릭 → 대상 (신청 상세 · 프로그램 등) 으로 이동 + 읽음 처리
4. "전체 알림 보기" → `/notifications` 목록
5. 목록에서 개별 삭제 · 전체 삭제 액션

**커버 spec**: `notification-bell.spec.ts`, `visual-notifications.spec.ts`, `design-contracts/notifications.md`

---

### T21. USER 신청 취소 → admin 알림 수신 — P1 크로스

**Pre-condition**: 2 세션 (T15 와 동일 구조). seed1 이 이미 신청한 건 존재.

**Steps**:
1. [A seed1] 마이페이지 신청내역 → "취소" → 확인
2. [B center1] 벨 새로고침 → "APPLICATION_CANCELED" 알림 확인
3. [B] 알림 클릭 → 신청건 상세 (취소 상태)

**Expected**:
- 취소 후 재신청 정책 확인 필요 (draft)

**커버 spec**: `admin-notification-fanout.spec.ts` (일부)

---

### === Admin 트랙 (P1) ===

---

### T22. Admin 대량 승인 (Approve bulk) — P1

**Pre-condition**: `center1@youth-moa.test` 로그인. 미처리 신청 5건 이상.

**Steps**:
1. `/admin/programs/{id}/applications` 진입
2. 좌측 체크박스로 3~5건 선택
3. 상단 "일괄 승인" 버튼 활성화 확인 → 클릭
4. 확인 다이얼로그 → "승인"
5. 선택 항목 전체 상태 "승인" 확인
6. 각 신청자에게 알림 발송 (별도 세션 검증 생략 가능)

**Expected**:
- 반려는 개별만 (bulk 미지원 — Qn-App1 A안, 사유 개별성)
- CENTER_ADMIN 이 다른 센터 신청건은 선택 불가 (필터로 격리)

**커버 spec**: `admin-bulk-csv.spec.ts`

---

### T23. Admin CSV Export (프로그램 · 사용자 · 신청) — P1

**Pre-condition**: `sysadmin@youth-moa.test` 로그인.

**Steps**:
1. `/admin/programs` → 상단 "CSV 다운로드" → 파일 저장
2. Excel 에서 파일 열기 → UTF-8 BOM · 한글 정상 표시 · 컬럼 헤더 정합
3. `/admin/users` · `/admin/programs/{id}/applications` 도 동일 반복

**Expected**:
- CENTER_ADMIN 이 CSV 다운로드 시 본인 센터 데이터만 (403 방어)
- 필터 상태 그대로 CSV 반영

**커버 spec**: `admin-bulk-csv.spec.ts`, `qa-checklists/csv-export.md`

---

### T24. Admin 통계 대시보드 (성별/연령/방문자) — P1

**Pre-condition**: `sysadmin@youth-moa.test` 로그인.

**Steps**:
1. `/admin/stats` (또는 대시보드 상세 이동) 진입
2. 방문자 라인차트 (최근 30일) · 일자별 값 hover 시 tooltip 확인
3. 성별 도넛 · 연령대 도넛 (19-24, 25-29, 30-34, 35-39, 40+, UNKNOWN 6종)
4. 프로그램별 신청 수 막대 · 마감임박 리스트

**Expected**:
- 차트는 SVG 서버 렌더 (외부 CDN 없음)
- 방문자 데이터: 최근 24시간은 in-memory + 익일 02:00 flush 로 확정

**커버 spec**: `admin-stats.spec.ts`, `A6-admin-stats-qa-report.md`

---

### T25. Admin 동적 필드 · 자격요건 관리 — P1

**Pre-condition**: `sysadmin@youth-moa.test` 로그인.

**Steps**:
1. `/admin/apply-questions` (동적 필드) 목록 확인
2. "새 질문" → 타입 (text/select/checkbox) · 라벨 · 필수 여부 → 저장
3. 프로그램 편집으로 이동 → "적용 질문" 선택에 새 질문 노출
4. 사용자 브라우저에서 T6 시나리오 재현 → Step 3 동적 필드에 신규 질문 표시 확인
5. `/admin/programs/{id}/eligibility` → 자격요건 CRUD 도 동일 flow

**커버 spec**: `admin-dynamic-field-form.spec.ts`, `admin-eligibility-form.spec.ts`

---

### === 회귀 방어 특수 케이스 ===

---

### T26. A7-rate-limit: 5분 내 반복 신청 → 알림 병합 — P1 회귀

**Pre-condition**: 2 세션.
- [A] seed1
- [B] center1

**Steps**:
1. [A] center1 관할 프로그램 X 신청
2. [A] 즉시 마이페이지에서 취소
3. [A] 프로그램 X 재신청
4. [A] 다시 취소
5. [A] 재재신청 (5분 이내 총 3회)
6. [B] 벨 → 알림 리스트에 "NEW_APPLICATION [3건]" 처럼 병합 표기 확인 (개별 3건 스팸 아님)

**Expected**:
- 병합 창구는 5분 sliding window
- 5분 경과 후 신청은 별도 알림으로 분리

**커버 spec**: `admin-notification-rate-limit.spec.ts`

---

### T27. A6-viewCount: USER 조회는 카운트, admin 조회는 skip — P1 회귀

**Pre-condition**: 프로그램 X 의 현재 viewCount 확인 (DB 또는 admin 상세 화면).

**Steps**:
1. seed1 로 프로그램 X 상세 접근 → viewCount +1 확인 (다음 admin 조회 시)
2. 같은 세션에서 다시 X 상세 재접근 → 카운트 변화 없음 (dedup)
3. 로그아웃 · 다른 브라우저에서 seed2 로 X 접근 → +1
4. sysadmin 로그인 상태로 X 상세 접근 → 카운트 변화 없음

**Expected**:
- dedup 창구 = 30분 세션 기준 (기록 정책 확인 필요 · draft)

**커버 spec**: (A6-followup — 이월 항목)

---

### === P2 엣지 케이스 ===

---

### T28. SMS 인증 실패 · 재시도 (rate-limit) — P2

**Pre-condition**: 회원가입 진행 중 (T1 Step 4 지점).

**Steps**:
1. "인증번호 발송" 5회 연속 클릭 (같은 번호)
2. 5회 이후 "잠시 후 다시 시도하세요" 에러 확인
3. 잘못된 인증번호 입력 3회 → "인증번호가 다르거나 만료되었습니다"
4. 정확한 코드 입력 → 인증 성공

**Expected**:
- rate-limit 창구 = 분당 3회 / 시간당 10회 (draft, 서버 정책 확인 필요)

**커버 spec**: (없음 — 이월)

---

### T29. 프로필 편집 시 phone 변경 → 재인증 강제 — P2 이월

**Pre-condition**: seed1 로그인.

**Steps**:
1. `/mypage/profile/edit` 진입 → Step1 재인증 (현재 비밀번호)
2. Step2 편집 화면 → phone 을 새 번호로 변경
3. "저장" 클릭

**Expected (현재 상태)**:
- phone 변경 감지 시 재인증 (SMS) 강제 → 미구현 (이월 항목 R2)
- `resetPhoneVerified` 는 구현됨, UX 강제만 미결

**커버 spec**: `mypage-profile-edit` (deferred file L106~119), `A5-2` 이월

---

### T30. 회원 탈퇴 (hard delete + cascade) — P2

**Pre-condition**: 탈퇴 대상 유저 준비 (신청·즐겨찾기·알림 각 1건 이상 보유).

**Steps**:
1. 대상 유저 로그인 → 마이페이지 프로필 탭 하단 "회원 탈퇴" 클릭
2. 안내 페이지 확인 (탈퇴 시 삭제되는 데이터 목록)
3. 비밀번호 재확인 → 사유 선택/입력 → "탈퇴하기"
4. 로그아웃 상태로 홈 이동
5. admin 브라우저에서 `/admin/users` 검색 → 해당 계정 미노출 확인 (hard delete)
6. DB (또는 admin 신청 목록) 확인 → 대상 유저의 application/bookmark/notification/userAgreement 전건 삭제 확인

**Expected**:
- 차단 (T12 Step 6) 과 달리 완전 삭제
- 동일 이메일로 재가입 가능

**커버 spec**: `mypage-interactions.spec.ts` (일부), `docs/design-contracts/mypage.md`

---

## 커버리지 매트릭스

| 시나리오 | 우선순위 | 주요 Playwright spec | 계약 문서 |
|---|---|---|---|
| T1 회원가입 | P0 | signup.spec.ts, visual-signup.spec.ts | signup.md |
| T2 로그인 | P0 | login.spec.ts, visual-login.spec.ts | login.md |
| T3 홈 | P0 | home.spec.ts, home-hero-rotation.spec.ts | home.md |
| T4 프로그램 목록 | P0 | program-list.spec.ts, bookmark.spec.ts | programs.md |
| T5 프로그램 상세 | P0 | program-detail.spec.ts | program-detail.md |
| T6 신청 wizard | P0 | apply.spec.ts, apply-dynamic-response.spec.ts | apply.md |
| T7 마이페이지 | P0 | mypage-interactions.spec.ts | mypage.md |
| T8 admin 로그인 | P0 | admin-login.spec.ts | admin/login.md |
| T9 admin 대시보드 | P0 | admin-dashboard.spec.ts, admin-header-notifications.spec.ts | admin/dashboard.md |
| T10 프로그램 CRUD | P0 | admin-program-form.spec.ts, admin-programs-list.spec.ts | admin/program-form.md |
| T11 신청 관리 | P0 | (부분) admin-program-form.spec.ts | admin/program-applications.md |
| T12 사용자 관리 | P0 | admin-users.spec.ts, admin-users-actions.spec.ts | admin/users.md, user-detail.md |
| T13 공지 CRUD | P0 | admin-notice-form.spec.ts, admin-notice-upload.spec.ts | admin/notice-management.md |
| T14 관리자 발급 | P0 | admin-staff-management.spec.ts | admin/login.md (일부) |
| T15 신청→승인 크로스 | P0 | admin-notification-fanout.spec.ts | admin/header-notifications.md |
| T16 소셜 로그인 | P1 | (없음) | (이월) |
| T17 검색 | P1 | (없음) | (이월) |
| T18 청년센터 | P1 | visual-centers.spec.ts | centers.md |
| T19 공지 상세 | P1 | notices.spec.ts | notices.md |
| T20 사용자 벨 | P1 | notification-bell.spec.ts | notifications.md |
| T21 취소→admin | P1 | admin-notification-fanout.spec.ts | admin/header-notifications.md |
| T22 대량 승인 | P1 | admin-bulk-csv.spec.ts | admin/application-bulk.md |
| T23 CSV export | P1 | admin-bulk-csv.spec.ts | admin/csv-export.md |
| T24 통계 대시보드 | P1 | admin-stats.spec.ts | admin/stats.md |
| T25 동적 필드·자격요건 | P1 | admin-dynamic-field-form.spec.ts, admin-eligibility-form.spec.ts | admin/program-dynamic-fields.md, program-eligibility.md |
| T26 rate-limit | P1 회귀 | admin-notification-rate-limit.spec.ts | (P1 특수) |
| T27 viewCount | P1 회귀 | (이월 A6-followup) | (이월) |
| T28 SMS 재시도 | P2 | (없음) | (이월) |
| T29 phone 재인증 | P2 이월 | (deferred) | mypage-profile-edit.md §deferred |
| T30 회원 탈퇴 | P2 | (부분) mypage-interactions.spec.ts | mypage.md |

---

## 이월 항목 (별도 티켓)

| 항목 | 상세 | 담당 |
|---|---|---|
| **T16 소셜 로그인 자동화** | Google/Kakao OAuth Playwright spec 신설 | 미할당 |
| **T17 검색 결과 spec** | 프로그램·공지 통합 검색 자동화 | 미할당 |
| **T27 viewCount 자동화** | A6-followup 티켓 (Program.viewCount 엔티티 + admin-stats.spec.ts) | admin 트랙 |
| **T28 SMS rate-limit 정책 확정** | 분당·시간당 상한 서버 정책 문서화 후 spec | 미할당 |
| **T29 phone 재인증 UX** | A5-2 이월 R2 · resetPhoneVerified 트리거 통합 | mypage 트랙 |
| **문서 draft 확정** | 각 시나리오의 "?" 표기 (재신청 정책 · dedup 창구 · rate-limit 상한) 사용자 결정 필요 | 사용자 검토 |

---

## 검토 요청 사항

1. **시드 계정 비밀번호** — 실 운영 환경에서 `admin.seed.password.*` override 를 문서에 명시할지 (혹은 QA 환경 전용으로 유지)
2. **draft 표시 항목** — T28 rate-limit 수치 · T27 dedup 창구 · T21 재신청 정책 확정
3. **T16 소셜 로그인** — provider 목록 (Google · Kakao · Naver 여부) 확정
4. **크로스 flow 2 세션 실행 가이드** — 시크릿 창 사용 규칙 · 세션 격리 여부 (Redis 미공유 환경 등) 추가 안내 필요 여부

검토 완료 후 확정본 커밋 지시 부탁드립니다.
