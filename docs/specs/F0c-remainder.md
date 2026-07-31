# 작업 명세: F0c-remainder — 신청 폼 prototype 잔여 갭

> 산출: ym-spec, 2026-07-07. 상태: **`impl_done` — PR #75 (`80f1dd3` 260707_F0c_apply_wizard) + PR #85 (E2E 이관)**
> 구현 위치: `templates/application/apply.html` (3단계 위저드 `.apply-step-card`). 후속 `F0c-dynamic-fields` 는 admin 트랙 파생 큐로 이관

## ✅ 결정 확정 (2026-07-07, 전부 권장안 채택)

| # | 결정 |
|---|---|
| Q1 | **A. 3단계 위저드 채택** (prototype 기준. 클라이언트 JS 전환 + 단일 form 유지, 검증 실패 시 에러 있는 스텝으로 초기 표시) |
| Q2 | **A. 지원 동기 선택으로 완화** — `@NotBlank`·`@Size(min=10)` 제거. 글자 수 카운터·maxlength 는 prototype 에 없으므로 제거 (max 한도는 `@Size(max=1000)` 서버 검증만 유지) |
| Q3 | **A. readonly 3필드 유지** (주소 필드 미추가) |
| Q4 | **A. 약관 4항목 하드코딩** + admin `SiteText` 도입은 추후 admin 트랙 티켓으로 분리 |
| Q5 | **A. 동적 추가 정보 범위 제외** — `F0c-dynamic-fields` 로 작업 큐 별도 등재 (admin 설정 기능 선행 필요) |
> 핵심 발견: 현재 apply.html 은 구버전 prototype.tsx (단일 페이지 + 가운데 220px 버튼) 기준 구현. 현행 prototype.html/tsx 는 "최신 UX/UI 8종 적용" 업데이트로 **3단계 위저드** 구조로 변경됨 (apply.html 주석의 "tsx line 962~1017" 은 더 이상 유효하지 않은 라인 참조).

## 1. 디자인 출처 (3자산 모두 명시)

| 자산 | 위치 |
|---|---|
| `docs/00_assets/prototype.html` | `ProgramApply` 함수, **line 1108~1197** (3단계 위저드) |
| `docs/00_assets/prototype.tsx` | `ProgramApply`, **line 1074~1163** — prototype.html 과 내용 동일 (충돌 없음) |
| `docs/00_assets/HANDOFF.md` | §5.4 프로그램 신청 (line 384~394) · §5-E.4 신청 폼 개선 (line 647~653) · line 1029 "신청 3단계 위저드(스텝 프로그레스)" · line 1112 "완료는 모달 아닌 /apply/complete 페이지" |
| `docs/00_assets/wireframe.png` | 프로그램 섹션 (원본 좌표 x≈25500~28350, y≈1700~7000) — "프로그램 신청" 화면 2종 (기본 + 첨부파일 업로드 후 상태) + 우측 Description 패널 |
| 비교 대상 | `src/main/resources/templates/application/apply.html` · `static/css/main.css` line 1081~1100, 1342~1577 · `application/ApplicationController.java` · `application/ApplyRequest.java` |

## 1-A. 자산 간 갭 (3자산 비교)

| 항목 | wireframe.png | prototype.html | prototype.tsx | 채택 |
|---|---|---|---|---|
| 화면 구조 | **단일 페이지** (5개 섹션 스택 + 하단 신청하기) | **3단계 위저드** (스텝 프로그레스, 단계별 카드 1장) | 위저드 (html 동일) | ⚠️ **Q1 — 사용자 질문** |
| 신청자 정보 편집 | **변경 가능** ("로그인한 사용자의 정보가 노출되며, 변경 가능함") + **주소 필드** (재검색 버튼 + 상세주소) | readonly 3필드 (이름·핸드폰·이메일), 주소 없음, "정보 수정은 마이페이지" 안내 | html 동일 | ⚠️ **Q3 — 사용자 질문** (HANDOFF §5.4 도 "주소" 포함이라 3자산 중 2:1) |
| 추가 정보 | **관리자 설정 동적** (주관식 0/100 · 객관식 dropdown · 첨부파일 업로드, 모두 필수) + 강좌 선택 dropdown | "지원 동기" textarea 1개 고정, placeholder "(선택)" | html 동일 | ⚠️ **Q2·Q5 — 사용자 질문** |
| 개인정보 약관 본문 | 관리자 화면에서 설정한 약관 노출 (수집 항목에 "촬영 사진 및 영상" 포함 버전) | 하드코딩 4항목 (수집 항목·목적·**보유기간 3년·거부 권리**) | html 동일 | prototype 본문 채택 + DB 관리 여부는 **Q4** |
| 미동의 제출 처리 | 헬프 텍스트 "필수 항목입니다." | 토스트 "개인정보 수집 동의가 필요합니다." + 버튼 disabled | html 동일 | prototype 채택 (disabled) + 서버 @AssertTrue 유지 — 현행 필드 에러 방식이 SSR 에 부합, 유지 |
| 완료 처리 | 토스트 "신청되었습니다." + 목록 이동 | `/apply/complete` 페이지 이동 | html 동일 | **prototype 채택 (이미 HANDOFF line 1112 로 확정, D1b 구현 완료)** — 질문 불필요 |
| 상단 이동 Floating Button | 있음 | 없음 | 없음 | prototype 채택 → 미구현 유지 |

## 2. 변경 범위 (파일 단위)

- [ ] `templates/application/apply.html` — 3단계 위저드 마크업 (스텝 프로그레스 + 단계별 show/hide) + 문구·뱃지 갭 반영 + 구버전 주석 (line 14~24) 갱신
- [ ] `static/css/main.css` — `.apply-*` 블록: 스텝 프로그레스 신규, 단계 카드 스타일, Summary 카드 수치 보정, 섹션 타이틀 스타일 교체, 네비게이션 버튼 레이아웃
- [ ] `application/ApplyRequest.java` — **Q2 결정 종속**: applyReason 필수→선택 전환 시 `@NotBlank`·`@Size(min=10)` 제거 + javadoc 의 구버전 라인 참조 수정
- [ ] `ApplicationController.java` — 변경 없음 예상 (위저드는 클라이언트 전환, 제출은 기존 단일 POST 유지)
- [ ] `application.yml` — 변경 없음

## 3. 필드 / 컴포넌트 명세 (prototype.html line 1108~1197 기준)

### 공통 프레임
| 컴포넌트 | 스펙 | 현재 |
|---|---|---|
| 컨테이너 | max-width 700 중앙, padding 36px 0 48px | ✅ (apply-screen) |
| 뒤로가기 | **38x38 아이콘 전용 버튼** (border 1px `--color-border`, radius 9, ← 아이콘 18px). step>1 이전 단계 / step1 상세로 | ❌ "← 이전으로" 텍스트 링크 |
| 타이틀 | h2 "프로그램 신청" 26/700 center, mb 24 | ✅ (mb 28 — 미세차) |
| 스텝 프로그레스 | 3단계 `['신청자 정보','추가 정보','약관 동의']`. 원 34px (done·active: primary bg + 흰 글자, done 은 체크 아이콘 / 미도달: `--color-border-light` bg + textTri). 라벨 12.5px (active: primary·700). 커넥터 2px, maxWidth 80, 통과 시 primary | ❌ 없음 |
| Summary 카드 | 항상 노출. 이미지 **64x64** radius 10 · padding **16** · border `--color-border-light` + `--shadow-sm` · 뱃지 "진행중" · 제목 **15**/600 · sub "센터 · 기간" **12.5** textSec | ⚠️ 구조 ✅ / 수치 상이 (80px·padding 18·border 진함·shadow 없음·16px·13px) |
| 단계 카드 | 흰 카드 1장 (surface, radius-lg, border-light, shadow-sm, padding 24px 26px) 안에 현재 단계 콘텐츠만 표시 | ❌ 3섹션 전부 스택 + "2px 하단 보더" 섹션 타이틀 (prototype 에 없는 스타일) |

### Step 1 — 신청자 정보
| 라벨 | 타입 | 필수 | 검증 | 비고 |
|---|---|---|---|---|
| (섹션 타이틀 옆) 회원정보 자동입력 | badge | - | - | ❌ 미구현. primaryBg/primary, 11.5/600, padding 2px 9px, pill |
| 이름 / 핸드폰 번호 / 이메일 | readonly box | - | - | ✅ 완료 (46px, #F3F4F6, 이메일 note 포함) |
| 하단 안내 | text | - | - | ❌ "정보 수정은 마이페이지 › 개인 정보 수정에서 가능합니다." (12.5, textTri, mt 12) |

### Step 2 — 추가 정보
| 라벨 | 타입 | 필수 | 검증 | 비고 |
|---|---|---|---|---|
| 지원 동기 | textarea | **Q2** (proto: 선택) | 현행 @NotBlank+@Size(10~1000) ↔ proto "(선택)" 충돌 | height **120** (현행 88) · placeholder **"지원 동기를 입력해주세요. (선택)"** (현행 상이) · border 1.5px, 값 있으면 primary (CSS 유사 구현 ✅) · **counter 0/500 는 proto 에 없음** + maxlength 500 ↔ @Size max 1000 내부 불일치 |

### Step 3 — 약관 동의 (스텝 라벨: "약관 동의" / 카드 내 타이틀: "개인정보 수집 동의")
| 라벨 | 타입 | 필수 | 검증 | 비고 |
|---|---|---|---|---|
| 약관 박스 | scroll box | - | - | ⚠️ 항목 **3 (보유기간: 프로그램 종료 후 3년)·4 (거부 권리)** 누락. maxHeight 130→**160**, overflow hidden→**auto**, 배경 `--color-bg` (현행 surface) |
| 동의 토글 | 원형 라디오형 | ✓ | @AssertTrue ✅ | 라벨 텍스트 **"개인정보 수집 및 이용에 동의합니다"** (현행 "동의합니다") |

### 네비게이션 (Q1 종속)
| 버튼 | 스펙 |
|---|---|
| 이전 | step≥2 에만 노출, secondary, flex:1 |
| 다음 | step 1·2, primary, flex:2 |
| 신청 완료 | step 3, primary, flex:2, **미동의 시 disabled** — 라벨 "신청하기"→"신청 완료" (현행 가운데 220px 단일 버튼) |

## 4. 갭 리스트 (현재 코드 vs prototype)

| # | 항목 | 현재 상태 | prototype 명세 | 우선순위 |
|---|---|---|---|---|
| 1 | 3단계 위저드 + 스텝 프로그레스 | 단일 페이지 | 위저드 (클라이언트 JS 전환 + 단일 form 유지 권장 — 서버 왕복 불필요, 검증 실패 시 에러 있는 스텝으로 초기 표시) | 높음 (Q1) |
| 2 | 단계 카드 래핑 + 섹션 타이틀 스타일 | 2px 보더 타이틀 + 스택 | 흰 카드 1장 + 16/700 카드 내 타이틀 | 높음 (Q1 종속) |
| 3 | 네비게이션 버튼 (이전/다음/신청 완료 + disabled) | 신청하기 220px 가운데 | flex 1:2 레이아웃 | 높음 (Q1 종속) |
| 4 | "회원정보 자동입력" 뱃지 | 없음 | 있음 | 중간 |
| 5 | "정보 수정은 마이페이지…" 안내 문구 | 없음 | 있음 | 중간 |
| 6 | 약관 본문 3·4 항목 (보유기간·거부 권리) | 1·2 만 | 4개 전체 + overflow auto | **높음** (법적 고지 누락) |
| 7 | 동의 라벨 문구 | "동의합니다" | "개인정보 수집 및 이용에 동의합니다" | 중간 |
| 8 | 지원 동기 placeholder·height | 자체 문구·88px | "(선택)" 문구·120px | 중간 (Q2 종속) |
| 9 | applyReason 검증 정책 | 필수 10~1000자 + maxlength 500 (내부 불일치) | 선택 | 높음 (Q2) |
| 10 | 뒤로가기 아이콘 버튼 | 텍스트 링크 | 38x38 아이콘 버튼 | 낮음 |
| 11 | Summary 카드 수치 (64px·padding 16·border-light·shadow·15/12.5px) | 80px·18·border·no-shadow·16/13px | 좌측 | 낮음 |
| 12 | 글자 수 카운터 | 있음 (0/500) | **없음** — prototype 에 없음 (제거·유지 검토, Q2 와 함께 결정) | 낮음 |
| 13 | 핸드폰 미등록 시 "— (마이페이지에서 등록)" | 있음 | **없음** — 자체 정책 (유지 권장, 명세상 잉여 표기) | 낮음 |

### 완료 확인 (이미 구현 — 이번 범위 제외)
- max-width 700 중앙 레이아웃 + 헤더/푸터
- Summary 카드 구조 (썸네일 + 상태 뱃지 + 제목 + 기관·기간) 및 status 동적 뱃지
- 신청자 정보 readonly 3필드 (46px, #F3F4F6) + 이메일 안내 note
- 개인정보 동의 원형 라디오형 토글 CSS + `@AssertTrue` 서버 검증 + 필드 에러 노출
- 지원 동기 값 존재 시 primary 보더 (`:not(:placeholder-shown)`)
- 완료 페이지 `/apply/complete` (D1b, HANDOFF line 1112 정합) + 권한 위반 404 + OSIV 대응 fetch join
- 도메인 에러 flash (`applyError`) 재표시

## 5. 검증 시나리오 (ym-qa 실행 항목)

### 정적 검증
- `.\gradlew.bat compileJava` 통과
- `ApplicationControllerTest` (@WebMvcTest) — 기존 TC 회귀 + Q2 결정 반영 시 applyReason 검증 TC 수정 (선택 전환 시: 빈 값 제출 → 성공)

### 동적 검증 (curl / preview)
- `GET /programs/{id}/apply` 200 OK (로그인 세션) — 스텝 프로그레스 마크업 (`apply-step` 류 클래스) + 약관 3·4 항목 텍스트 grep
- `POST /programs/{id}/apply` privacyAgreed=false → 200 재표시 + "개인정보 수집 동의가 필요합니다." + 입력값 보존 (applyReason 잔존)
- `POST` 정상 → 302 `/apply/complete?applicationId=` 리다이렉트
- `/css/main.css` 200 + 변경 클래스 grep (processResources 선행 — CLAUDE.md CSS 반영 규칙)

### 시각 검증 (사용자 영역)
1. 스텝 1→2→3 전환 시 프로그레스 원 색·체크 아이콘·커넥터 변화, Summary 카드 항상 노출
2. 스텝 3 미동의 상태에서 "신청 완료" 버튼 disabled → 동의 시 활성
3. "이전" 버튼 스텝 2·3 에서만 노출, 스텝 1 뒤로가기 아이콘 버튼 → 상세 복귀
4. 약관 박스 내부 스크롤 (overflow auto) 동작

## 6. 의존성 / 선행 작업
- 없음 — D1b (`/apply/complete`) 머지 완료 상태 확인. 다만 **Q1~Q5 답변이 구현 범위를 좌우하므로 사용자 결정이 선행 조건**.

## 7. 작업 큐 메타
- 작업 ID: F0c-remainder / 우선순위: 2 (독립 화면, 병행 가능) / 추정: 1 PR (Q5 포함 시 2~3 PR 분리) / 상태: spec_done

## 사용자 결정 필요 질문 (Q-번호)

| # | 질문 | 선택지 |
|---|---|---|
| **Q1** | **3단계 위저드 도입 여부** — wireframe 원본은 단일 페이지, prototype.html 최신판은 위저드 (HANDOFF line 1029 "최신 UX/UI 8종"). 원칙상 prototype 우선이지만 화면 구조가 통째로 바뀌는 변경. | A. 위저드 채택 (prototype 기준, 권장) / B. 단일 페이지 유지 + 문구·스타일 갭만 반영 |
| **Q2** | **지원 동기 필수 여부** — prototype placeholder 는 "(선택)", 현재 구현은 필수 10~1000자 (+ maxlength 500 내부 불일치). | A. 선택으로 완화 (prototype) / B. 필수 유지 (한도는 500 또는 1000 으로 통일 지정 필요). 카운터 유지 여부도 함께 |
| **Q3** | **주소 필드·편집 가능 여부** — wireframe + HANDOFF §5.4 는 "주소 포함 + 변경 가능", prototype 은 "readonly 3필드 (주소 없음)". | A. prototype 대로 readonly 3필드 유지 (권장, 현행 일치) / B. 주소 필드 추가 |
| **Q4** | **개인정보 약관 본문의 DB 관리 여부** — wireframe 은 "관리자 설정 약관 노출" (확장성 원칙의 하드코딩 금지 후보). | A. 이번엔 prototype 4항목 하드코딩 + 추후 admin 티켓 분리 (권장) / B. 지금부터 엔티티 (예: `SiteText` slot) 도입 |
| **Q5** | **동적 추가 정보 (강좌 dropdown · 주관식/객관식 질문 · 첨부파일)** — wireframe·HANDOFF §5.4 원 기획이나 admin 설정 기능 선행 필요. | A. 이번 범위 제외, 별도 작업 ID (F0c-dynamic-fields) 큐 등록 (권장) / B. 이번에 포함 |
