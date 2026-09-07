# A-admin-terms-crud — 관리자 약관 CRUD

| 메타 | 값 |
|---|---|
| 작업 ID | `A-admin-terms-crud` |
| 상태 | **spec_confirmed** (2026-09-04 사용자 결정: Qn-1 **B** · Qn-2 **B** · Qn-3~9 권장안 그대로 A/A/A/A/A/A/A) |
| 트랙 | admin (파생 큐 두 번째) |
| 선행 | ✅ A1 admin-shell (PR #205) · ✅ A-admin-notice-attachment (PR #206) · ✅ F-signup-terms-agreement (PR #125 · Term/UserAgreement 엔티티 및 시드 완료) |
| 후행 | F0c-dynamic-fields (파생 큐 3번째) → F4 → A3 |
| ADR | `docs/adr/admin-track-roadmap-2026-09.md` §2 P2 "admin 약관 CRUD (별도 소형 PR)" · Q4 A |
| 예상 규모 | 1 PR · 파일 12~14개 · 순증 ≈ 850~1,000 LOC (테스트 포함) |

---

## 0. 계약 확인 (0단계)

- **본 화면 계약**: **없음** — 이번 티켓에서 함께 신설한다 (`e2e/contracts/admin-term.ts` + `docs/design-contracts/admin/term-management.md`)
- **admin 공통 정책**: `docs/design-contracts/admin/POLICY.md` (다크 헤더 · 인디고 primary · 존댓말 톤 "…했어요/됐어요") 준수
- **재활용 계약**: `admin-notice.ts` (list/form/edit 3계약) — 셀렉터 네이밍·확인 지점 그대로 이식
- **admin/prototype.html 부재**: notice 와 마찬가지로 admin prototype 에 약관 관리 화면이 **없음** → notice 계약과 동일한 근거(=admin POLICY + 사용자 CRUD 패턴)로 신설

---

## 1. 디자인 출처 (3자산 + 재활용)

| 자산 | 인용 |
|---|---|
| admin prototype.html | 약관 관리 화면 **부재** — notice 와 동일 상황 |
| admin prototype.tsx | 화면 없음 |
| admin/HANDOFF.md | §7 마이크로카피 존댓말 톤 · §5 삭제 확인 모달(파괴적 액션 1단계 · `hx-confirm` 금지, 커스텀 모달) |
| wireframe.png | 없음 |
| **재활용 근거** | `AdminNoticeController` / `AdminNoticeService` (PR #206) — 목록·신규·편집·삭제 4 endpoint + confirm 모달 + AdminExceptionHandler 400 매핑 |

### 1-A. 자산 간 갭
표 자체가 성립 안 함(원본 없음). 계약을 이번 티켓에서 **정의한다**.

### 1-B. 데이터 모델 gap 표
Term 엔티티(`user/Term.java`, PR #125)가 이미 존재. 각 필드가 admin CRUD 폼에 어떻게 매핑되는지 실측 기반으로 매핑.

| Term 필드 | 컬럼 | 폼 매핑 | 조치 |
|---|---|---|---|
| `id` | `bigint PK` | 편집 URL 세그먼트 | 그대로 |
| `code` | `varchar(50) UNIQUE` | 신규 등록 시 필수 입력, 편집 시 **readonly** (Qn-9) | UNIQUE 위반 시 400 |
| `title` | `varchar(100)` | 입력 필드 | `@Size(max=100)` 검증 |
| `contentPath` | `varchar(200)` | 입력 필드 (예: `/terms`, `/privacy`) | `@Size(max=200)` |
| `required` | `boolean` | 체크박스 | 기본 true |
| `version` | `int` | 편집 폼에 노출하되 **읽기 전용** (Qn-5 규칙에 따라 서비스가 증가) | 서비스가 자동 증가 |
| `sortOrder` | `int` | 숫자 입력 | 중복 허용 (동률 시 id asc) |
| `isActive` | `boolean` | 체크박스 | 목록 상태 표시 소스 |
| `createdAt / updatedAt` | BaseTimeEntity | 목록 표시 | 그대로 |

**필드 신설 없음**. 이번 티켓은 **기존 스키마 위 CRUD 만 얹는다**. 마이그레이션 V10 은 발생하지 않는다.

### 1-C. 데이터 소비 지점
`Term` 데이터를 사용하는 모든 소비 지점을 열거하고 관리자 CRUD 후 영향도를 확인.

| 소비 지점 | 현재 코드 위치 | 이번 티켓 영향 |
|---|---|---|
| 회원가입 폼 활성 약관 렌더 | `UserController.signUpPage` L72 · `termRepository.findByIsActiveTrueOrderBySortOrderAsc()` | 관리자가 `isActive=false` 전환 시 폼에서 자동 제외 — Qn-4 정책 필요 |
| 회원가입 검증 (필수 약관 누락) | `UserService.findMissingRequiredTermCodes` L215~232 | 관리자가 `required=true` 신규 약관 등록 시 즉시 검증에 편입 → **기존 회원가입 flow 회귀 위험** |
| 회원가입 저장 (UserAgreement 이력) | `UserService.signUp` L200~211 | `agreedVersion = term.getVersion()` 스냅샷 저장. 관리자가 version 을 증가시켜도 과거 이력 보존 |
| 탈퇴 시 이력 삭제 | `UserService.withdraw` L146 · `userAgreementRepository.deleteAllByUser` | 무관 |
| DataInitializer 시드 | `DataInitializer.seedTerms` L107~132 | 재기동 시 `count() > 0` 이면 skip → 관리자 편집값 보존 (실효성 OK) |

### 1-D. write→read 왕복 통합 시나리오
- 관리자가 신규 약관(required=true) 등록 → 회원가입 페이지 GET → 새 약관이 필수 체크박스로 렌더 → 미체크 시 400 → 체크 시 UserAgreement 이력에 `agreedVersion` 스냅샷 저장
- 관리자가 SERVICE 편집(title 변경 · version 증가) → 회원가입 페이지 GET → 새 title 렌더, 새 가입자는 `agreedVersion=새 version` 저장, 기존 회원 이력은 이전 version 그대로 (재동의 판정용)
- 관리자가 PRIVACY `isActive=false` 전환 → 회원가입 페이지 GET → PRIVACY 미노출, 남은 필수 약관만 검증. Qn-4 정책이 "활성 필수 약관 0건 허용" 여부를 결정

---

## 2. 배경 · 스코프

### 포함
| URL | 메서드 | 목적 |
|---|---|---|
| `/admin/terms` | GET | 목록 (버전 · 활성 상태 · 필수 · 정렬순서) |
| `/admin/terms/new` | GET | 신규 약관 폼 |
| `/admin/terms` | POST | Create |
| `/admin/terms/{id}` | GET | 편집 폼 (기존값 prefilled) |
| `/admin/terms/{id}` | POST | Update |
| `/admin/terms/{id}/delete` | POST | Delete (커스텀 confirm 모달 후) |

- **약관 versioning**: 활성 → 비활성 전환 시나리오 (개정판 등록). Qn-5 참조
- **회원가입 flow 회귀 방지**: 사용자 signup 화면이 활성 약관만 로드하는지 SignupControllerTest 로 회귀 assert
- 파생 큐 위치: 공지첨부(완료) → **약관 CRUD(지금)** → F0c → F4

### 제외
- 회원별 동의 이력 관리 화면 (별도 admin 티켓 이월)
- 약관 미동의 회원 알림/통계
- 개정 시 회원에게 자동 재동의 요청 UX (별도 후속 티켓 — `agreedVersion` 스냅샷은 이번 티켓에 이미 준비됨)
- 약관 본문(WYSIWYG) 편집기 — `contentPath` 는 정적 템플릿 경로 문자열만 관리 (`/terms`, `/privacy`). 본문 자체는 `templates/policy/*.html` 에서 관리 (F-signup-terms-agreement Q2 결정 유지)

---

## 3. RBAC 정책

**정책: SYSTEM_ADMIN 전용** (Qn-1 권장안).

근거:
- 약관은 개별 센터 스코프가 아닌 **전 서비스 회원가입 flow 근간**
- CENTER_ADMIN 이 편집해 회원가입 flow 를 깨는 사고 방지
- `AdminNoticeService.canEdit` 의 "본인 작성" 패턴은 부적합 (약관에는 작성자가 무의미)
- 구현: `AdminTermController` 각 endpoint 진입 시 `AdminScope.isSystemAdmin()` false 면 `AccessDeniedException` throw → Spring Security 가 403

`AdminScope` 재활용:
- `centerScopeLabel()` 은 목록 상단 배지에 그대로 표시 (SYSTEM_ADMIN 이면 "전체")
- `isSystemAdmin()` 이 이번 티켓의 게이트 함수

---

## 4. Versioning 정책

### Qn-5 권장안: **같은 row update + version++ (in-place)**

| 접근 | 설명 | 트레이드오프 |
|---|---|---|
| **A (권장)** | 같은 Term row 를 update. `Term.updateContent(...)` 도메인 메서드에서 version 을 +1. 과거 UserAgreement 는 `agreedVersion` 스냅샷으로 이전 값 보존 | 이력 테이블 조회로 과거 문안 파악 어려움 (그러나 spec 은 후속 티켓) |
| B | 새 row 를 `code=SERVICE`, `version=n+1` 로 삽입. UNIQUE 위반이므로 `code` UNIQUE 를 (code, version) UNIQUE 로 재정의 필요 → 스키마 변경 발생 | code 유일성 회원가입 조회 로직 `findByCode` 가 깨짐. 마이그레이션 부담 |

**권장 A 근거**:
- V3 스키마의 `uk_terms_code` UNIQUE 를 그대로 유지하려면 A 만 가능
- `UserAgreement.agreedVersion` 스냅샷 컬럼이 이미 있어 "동의 당시 문안"을 재구성 가능 (V3 comment L45 명시)
- 개정 UX 는 편집 폼 "저장 시 version 을 자동으로 1 증가" 체크박스로 노출 (Qn-6)

### 활성 상태 정책
- `isActive=true` 인 Term 은 카테고리(code) 별 1개여야 하는가? — **아니오**. UNIQUE 는 code 이므로 물리적으로 1개
- **경계 시나리오**: SERVICE 를 `isActive=false` 로 전환하고 새 SERVICE 를 만들 수 없음 (code UNIQUE). 따라서 **isActive 전환은 "폼에서 제외" 목적일 뿐이고 실질적 개정은 in-place update**

---

## 5. 삭제 정책

### Qn-3 권장안: **hard delete (제약 있음)**

Term 이 어떤 UserAgreement 라도 참조되면 FK 제약으로 삭제 실패. AdminTermService.delete 는 이 경우 400 반환.

```
canDelete(term) = userAgreementRepository.countByTerm(term) == 0
```

- **사용 사례**: 잘못 만든 약관을 즉시 제거 (아직 아무도 동의 안 함)
- **일반 사례**: 이미 동의 이력이 있는 약관은 삭제 대신 `isActive=false` 처리

**대안 (soft delete)**: `isDeleted` 컬럼 신설. 이번 티켓에서는 채택하지 않음 — Term 엔티티에 이미 `isActive` 가 있어 기능 중복. Qn-3 사용자 결정 시 재검토.

---

## 6. 활성 약관 0건 상태 정책 (핵심 안전장치)

### Qn-4 권장안: **활성 필수 약관 0건 허용 (회원가입 폼은 "약관 없음" empty state 렌더)**

시나리오별 회원가입 flow 동작:

| 상태 | 렌더 | POST 검증 |
|---|---|---|
| 활성 필수 2건 (현재) | 체크박스 2개 | 미체크 시 400 |
| 활성 필수 1건 | 체크박스 1개 | 미체크 시 400 |
| 활성 필수 0건 · 선택 N건 | 선택 체크박스만 | 통과 (필수 없음) |
| **활성 0건 (전체 비활성)** | 약관 섹션 자체 렌더 안 함 | 통과 (`findMissingRequiredTermCodes` 는 빈 리스트 반환) |

**근거**:
- `UserService.signUp` L179 `if (!missing.isEmpty())` 는 활성 필수가 0건이면 자연스럽게 통과
- `UserController.signUpPage` L72 는 `activeTerms` 가 빈 리스트여도 500 안 남 (Thymeleaf `th:each` 가 빈 컬렉션 렌더 스킵)
- signup.html 이 실제로 이 empty state 를 그레이스풀하게 렌더하는지 회귀 테스트로 확인 필요

**보완**: admin 목록에 "활성 필수 약관 0건이에요. 회원가입 시 약관 동의 절차가 노출되지 않아요." 경고 배너 표시 (Qn-4 채택 시).

---

## 7. 변경 범위 (파일 단위)

### 신규 (Java)
- [ ] `src/main/java/io/github/sihyuuun/youthmoa/admin/AdminTermController.java` — 6 endpoint (list/new/create/edit/update/delete)
- [ ] `src/main/java/io/github/sihyuuun/youthmoa/admin/AdminTermService.java` — CRUD + version 증가 + FK 검사
- [ ] `src/main/java/io/github/sihyuuun/youthmoa/user/Term.java` — `updateContent(title, contentPath, required, sortOrder, isActive, bumpVersion)` 도메인 메서드 추가

### 신규 (Repository 확장)
- [ ] `src/main/java/io/github/sihyuuun/youthmoa/user/TermRepository.java` — `findAllByOrderBySortOrderAscIdAsc()` 추가 (관리 목록은 비활성 포함)
- [ ] `src/main/java/io/github/sihyuuun/youthmoa/user/UserAgreementRepository.java` — `countByTerm(Term term)` 추가 (delete 가능성 판정)

### 신규 (Template)
- [ ] `src/main/resources/templates/admin/term/list.html`
- [ ] `src/main/resources/templates/admin/term/form.html`

### 수정 (Template)
- [ ] `src/main/resources/templates/admin/fragments/header.html` — GNB 에 "약관 관리" 링크 추가 (currentPage='terms')

### 수정 (Test fixture)
- [ ] `src/main/java/io/github/sihyuuun/youthmoa/test/TestFixtureController.java` — `POST /__test__/reset-terms` 추가 (Qn-8)
- [ ] `src/main/java/io/github/sihyuuun/youthmoa/common/DataInitializer.java` — `SEED_TERM_COUNT = 2L` 상수 추가

### 신규 (테스트)
- [ ] `src/test/java/.../admin/AdminTermControllerTest.java` — RBAC + CRUD + version bump + FK 위반 시 400
- [ ] `src/test/java/.../admin/AdminTermFormRenderTest.java` — 목록·신규·편집 Thymeleaf 렌더
- [ ] `src/test/java/.../user/SignupControllerTermsRegressionTest.java` — 활성 약관 0건/1건/N건 시나리오

### 신규 (계약 · E2E)
- [ ] `docs/design-contracts/admin/term-management.md`
- [ ] `e2e/contracts/admin-term.ts` — list/form/edit 3 계약 (admin-notice 셀렉터 네이밍 승계)
- [ ] `e2e/contracts/runner.ts` 등록
- [ ] `e2e/tests/admin-term-crud.spec.ts` — CRUD 왕복
- [ ] `e2e/tests/admin-term-rbac.spec.ts` — SYSTEM_ADMIN OK · CENTER_ADMIN 403 · USER 403

### 신규 (CSS)
- [ ] `src/main/resources/static/css/admin.css` — `.admin-term-*` 클래스 (notice 클래스 네이밍 승계, 스타일은 재사용 or `@apply` 방식 없이 개별 정의)

---

## 8. 필드 / 컴포넌트 명세

### 목록 (`/admin/terms`)

| 컬럼 | 소스 | 표시 |
|---|---|---|
| ID | `t.id` | `#1` |
| Code | `t.code` | 태그 pill (예: `SERVICE`, `PRIVACY`) |
| 제목 | `t.title` | 편집 링크 |
| 필수 | `t.required` | ✓ / 공란 |
| 정렬 | `t.sortOrder` | 숫자 |
| 버전 | `t.version` | `v2` |
| 상태 | `t.isActive` | "활성" / "비활성" 뱃지 (`.admin-term-status-pill--active` 등) |
| 등록일 | `t.createdAt` | `yyyy-MM-dd` |
| 관리 | — | [편집] |

상단 우측 `[+ 신규 등록]` 버튼. 상단에 "활성 필수 약관 N건" 요약 배지. **활성 필수 0건이면 경고 배너 노출**.

### 신규/편집 폼 (`admin/term/form.html`)

| 라벨 | 필드 | 검증 | 편집 모드 |
|---|---|---|---|
| Code | `code` (text) | `@NotBlank`, `@Size(max=50)`, `@Pattern("^[A-Z_]+$")` | **readonly** (Qn-9) |
| 제목 | `title` (text) | `@NotBlank`, `@Size(max=100)` | 편집 가능 |
| 본문 경로 | `contentPath` (text) | `@NotBlank`, `@Size(max=200)`, `@Pattern("^/.+")` | 편집 가능 |
| 필수 여부 | `required` (checkbox) | — | 편집 가능 |
| 정렬 순서 | `sortOrder` (number) | `@Min(1)`, `@Max(999)` | 편집 가능 |
| 활성 여부 | `isActive` (checkbox) | — | 편집 가능 |
| 버전 자동 증가 | `bumpVersion` (checkbox) | 편집 모드에서만 노출 | 편집 시 "본문/필수 여부를 실질적으로 변경한 경우 체크" 도움말 |
| 현재 버전 | `version` (readonly text) | — | 표시만 |

**저장 시 검증 실패**: 폼 재렌더 + `AdminExceptionHandler` 는 GET 폼 렌더 실패 시엔 관여 안 함 (Controller 가 BindingResult 로 직접 처리). POST 시 `IllegalArgumentException` (예: code UNIQUE 위반, FK 참조로 삭제 불가) 은 handler 가 400 매핑.

### 삭제 확인 모달
`admin/notice/form.html` 패턴 승계. `#term-delete-modal` id + "약관을 삭제할까요? 삭제 후 복구할 수 없어요." 문구. FK 참조 있으면 삭제 버튼 disabled + "이미 동의한 회원이 있어 삭제할 수 없어요. 비활성 처리해주세요." 대체 안내.

---

## 9. 갭 리스트 (현재 코드 vs 이번 티켓)

| # | 항목 | 현재 상태 | 이번 티켓 | 비고 |
|---|---|---|---|---|
| 1 | 관리자 약관 CRUD UI | 없음 (Term 엔티티는 있음) | 6 endpoint + 2 template | — |
| 2 | GNB "약관 관리" 링크 | 없음 | header.html 수정 | currentPage='terms' |
| 3 | Term.updateContent 도메인 메서드 | 없음 (Getter 만) | 신설 | version bump 로직 캡슐화 |
| 4 | UserAgreementRepository.countByTerm | 없음 | 신설 | delete 판정 |
| 5 | TermRepository 관리 목록 조회 | `findByIsActiveTrueOrderBySortOrderAsc` 만 | `findAllByOrderBySortOrderAscIdAsc` 추가 | 비활성 포함 |
| 6 | `POST /__test__/reset-terms` | 없음 | 신설 | Qn-8 |
| 7 | DataInitializer `SEED_TERM_COUNT` 상수 | 없음 | `2L` | reset 기준 |
| 8 | admin-term 계약 3건 | 없음 | 신설 | admin-notice 승계 |
| 9 | signup 회귀 테스트 | 없음 | 신설 | 활성 0/1/N 3시나리오 |

---

## 10. 검증 시나리오

### 정적 검증
- `./gradlew compileJava` PASS
- `./gradlew test --tests JpaMappingTest` PASS (스키마 무변경이라 회귀만)
- `./gradlew test --tests AdminTermControllerTest` — @WebMvcTest 슬라이스, RBAC 6 케이스 + CRUD 6 케이스 + FK 위반 1 케이스
- `./gradlew test --tests AdminTermFormRenderTest` — 목록·신규·편집 3 렌더
- `./gradlew test --tests SignupControllerTermsRegressionTest` — signup GET 렌더 3 시나리오 (활성 0/1/N)

### 동적 검증 (curl · e2e 프로파일 · port 8090)
```bash
# 정상 진입
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/admin/terms
# → 302 (로그인 필요)

# 로그인 세션 확보 후
curl -s -o /dev/null -w "%{http_code}\n" -b jar.txt http://localhost:8090/admin/terms  # 200
curl -s -o /dev/null -w "%{http_code}\n" -b jar.txt http://localhost:8090/admin/terms/new  # 200
curl -s -o /dev/null -w "%{http_code}\n" -b jar.txt http://localhost:8090/admin/terms/1  # 200

# CENTER_ADMIN 로그인 후
curl -s -o /dev/null -w "%{http_code}\n" -b jar.txt http://localhost:8090/admin/terms  # 403
```

### 인터랙션 검증 (Playwright)
- `admin-term-crud.spec.ts`: 등록 → 목록 노출 → 편집 → 목록 반영 → 삭제 확인 모달 → 삭제 → 목록 미노출
- `admin-term-rbac.spec.ts`: SYSTEM_ADMIN 200 · CENTER_ADMIN 403 · USER 403
- **회원가입 회귀** `signup.spec.ts` (기존) 실행 재확인 — pass 유지

### 계약 검사
```bash
cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=contracts
```
- `admin-term-list` · `admin-term-form` · `admin-term-edit` 갭 0 확인

### 시각 검증 (사용자 영역)
- 목록 상단 "활성 필수 약관 2건" 배지 렌더
- 편집 폼 "버전 자동 증가" 체크박스 도움말 톤 확인
- 삭제 모달 존댓말 카피

---

## 11. 회원가입 회귀 방지 (필수)

**규칙**: admin 트랙 어떤 조작도 signup flow 500 을 일으키면 안 됨.

`SignupControllerTermsRegressionTest` 케이스:
1. **활성 약관 2건 (기본)** → `GET /signup` 200 + 체크박스 2개 렌더
2. **PRIVACY 를 비활성으로 전환 → 활성 1건** → `GET /signup` 200 + 체크박스 1개
3. **SERVICE, PRIVACY 둘 다 비활성 → 활성 0건** → `GET /signup` 200 + 약관 섹션 스킵 렌더 (`th:each` 빈 컬렉션)
4. **활성 0건 상태에서 POST /signup** → 통과 (필수 약관 없음)
5. **새 필수 약관 등록 (예: MARKETING required=true)** → `GET /signup` 렌더에 즉시 등장 + 미체크 POST → 400

### admin 조작으로 signup 이 깨지는 케이스 (테스트로 방어)
- 관리자가 활성 SERVICE 의 `code` 를 변경 → **막는다**: 편집 폼에서 code readonly
- 관리자가 활성 약관을 delete 시도 (동의 이력 있는 경우) → 400 반환 + UI 안내
- 관리자가 activate 상태를 급격히 뒤엎어도 signup 은 항상 200 (empty state 허용)

---

## 12. 결정 필요 항목 (Qn)

원샷 답변 가능한 9개.

### Qn-1: RBAC 정책
| 안 | 내용 |
|---|---|
| **A (권장)** | SYSTEM_ADMIN 전용 |
| B | SYSTEM_ADMIN + CENTER_ADMIN (notice 패턴 승계) |

### Qn-2: content 저장 방식
| 안 | 내용 |
|---|---|
| **A (권장)** | `contentPath` 문자열만 관리 (기존 스키마 유지). 본문은 `templates/policy/*.html` |
| B | Term 에 `contentHtml` LOB 컬럼 신설 (마이그레이션 V10) |
| C | Term 에 `contentMarkdown` LOB 컬럼 + 렌더 시 변환 |

### Qn-3: 삭제 방식
| 안 | 내용 |
|---|---|
| **A (권장)** | hard delete. FK(UserAgreement) 참조 있으면 400 + "비활성 처리하세요" 안내 |
| B | soft delete — Term 에 `isDeleted` 컬럼 신설 |
| C | delete 자체 미지원 (`isActive` 로만 제어) |

### Qn-4: 활성 약관 0건 상태 허용 여부
| 안 | 내용 |
|---|---|
| **A (권장)** | 허용. signup 폼은 empty state 렌더 (약관 섹션 스킵). admin 목록에 경고 배너 |
| B | 최소 1건 필수 약관 유지 강제 (마지막 활성 필수를 비활성화하려 하면 400) |

### Qn-5: versioning 방식
| 안 | 내용 |
|---|---|
| **A (권장)** | 같은 row update + version++ (in-place). UNIQUE(code) 유지. `agreedVersion` 스냅샷으로 재동의 판정 |
| B | 새 row + `isActive=false` 로 이전 (스키마 변경 필요) |

### Qn-6: version bump UX
| 안 | 내용 |
|---|---|
| **A (권장)** | 편집 폼에 "버전 자동 증가" 체크박스. 체크 시 서비스가 version+1. 일반 편집(오타 수정 등)은 미체크 |
| B | 항상 자동 증가 (편집 = 개정) |
| C | 수동 입력 필드 노출 (오작동 위험) |

### Qn-7: HTMX fragment 응답 vs PRG redirect
| 안 | 내용 |
|---|---|
| **A (권장)** | PRG redirect (list · create · update · delete). admin-notice CRUD 도 PRG 사용 (첨부만 fragment) |
| B | 목록 갱신을 HTMX outerHTML fragment 로 |

### Qn-8: `POST /__test__/reset-terms` endpoint
| 안 | 내용 |
|---|---|
| **A (권장)** | 신설. E2E 가 신규 약관 생성 시 다음 test 오염 방지. `SEED_TERM_COUNT=2L` 기준 초과 row 삭제 + 시드 2건 원본 UPSERT |
| B | 미신설. E2E 는 read-only 시나리오만 |

### Qn-9: 편집 시 code 변경 허용 여부
| 안 | 내용 |
|---|---|
| **A (권장)** | 편집 모드에서 **readonly**. code 변경은 UserAgreement 참조 무결성 위험 |
| B | 편집 가능 (UNIQUE 위반만 400) |

---

## 12-후속. Qn-1 = B (SYSTEM/CENTER 모두 조회 · SYSTEM UD only) — 사용자 확정 (2026-09-04)

- 목록 (`GET /admin/terms`), 상세 (`GET /admin/terms/{id}`) 는 `SYSTEM_ADMIN` + `CENTER_ADMIN` 모두 접근 가능
- Create/Update/Delete 는 `SYSTEM_ADMIN` 만 (`AdminScope` 불필요, role check 만)
- `AdminExceptionHandler` 재활용으로 CENTER_ADMIN 이 UD 시도 시 403 반환
- SecurityConfig `/admin/terms/**` 는 기존 `hasAnyRole` 매처로 커버, 컨트롤러 레벨 `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` 이나 서비스 계층 role 검증 추가

## 12-후속. Qn-2 = B (content DB TEXT 저장, 웹 편집) — 사용자 확정 (2026-09-04)

**당초 권장 A (contentPath 유지) 에서 B 로 재판정**. admin CRUD 실사용성 확보 우선.

### 스키마 변경 (V10 마이그레이션 신규)

```sql
-- src/main/resources/db/migration/V10__add_term_content.sql
-- Step 1: content 컬럼 추가 (nullable)
ALTER TABLE term ADD COLUMN content TEXT;

-- Step 2: 기존 시드 (`terms/service-v1.html` 등) HTML 을 백필
-- (DataInitializer 에서 재시드 or SQL 하드코딩 · 시드 2건뿐이라 SQL 로 처리 권장)
UPDATE term SET content = '<p>서비스 이용약관 기본 내용...</p>' WHERE code = 'service';
UPDATE term SET content = '<p>개인정보처리방침 기본 내용...</p>' WHERE code = 'privacy';

-- Step 3: content NOT NULL 승격
ALTER TABLE term ALTER COLUMN content SET NOT NULL;
```

### 엔티티 변경

`Term.java`:
- `content` 필드 추가 (`@Column(columnDefinition="TEXT", nullable=false)`)
- 기존 `contentPath` 필드는 유지 (deprecated 표기 · 사용자 signup flow 는 `content` 우선 로드하고 `contentPath` 는 미사용)

### 관리자 편집 UI (임시 · prototype 준비 전)

- `templates/admin/term/form.html` 의 content 필드는 **`<textarea rows="20" cols="80">`** 로 임시 구현
- prototype 완성 후 WYSIWYG (Trix / TinyMCE / Quill) 로 교체 별도 티켓
- ym-impl 은 textarea 로만 구현, deferred 로 기록

### XSS 방어

- **의존성 추가** (`build.gradle.kts`): `implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20240325.1")`
- `AdminTermService.save(...)` 진입 시 content 를 sanitize 후 저장
  ```java
  static final PolicyFactory POLICY = Sanitizers.FORMATTING
      .and(Sanitizers.LINKS).and(Sanitizers.BLOCKS)
      .and(Sanitizers.STYLES).and(Sanitizers.TABLES);
  String safeContent = POLICY.sanitize(rawContent);
  ```
- signup 렌더는 `th:utext="${term.content}"` (이미 sanitize 된 상태 신뢰)
- `AdminTermServiceTest` 에 XSS 시나리오 TC 추가 (`<script>alert('xss')</script>` 저장 → 저장값 확인)

### 사용자 signup flow 호환

- `TermService.findActiveTerms()` 반환 순서 유지
- signup 화면 fragment 가 `term.contentPath` 에서 파일 로드하던 코드를 `term.content` 로 직접 렌더로 변경
- **signup 회귀 방지 테스트 필수** (spec §11)

### Prototype 이월 사항 (사용자가 준비 예정 명시)

사용자가 관리자 약관 편집 화면 (특히 content 편집 UX) prototype 을 별도로 준비할 예정. 이번 티켓은 임시 textarea 로 진행, 추후 별도 UX 개선 티켓 (`A-admin-terms-crud-ux-polish`) 에서 교체.

## 12-후속. Qn-3~Qn-9 사용자 확정 요약

- Qn-3 A: hard delete + FK 참조 시 400 (`AdminExceptionHandler` 재활용)
- Qn-4 A: 활성 약관 0건 허용 (signup 화면은 empty state 렌더)
- Qn-5 A: in-place update + version++ (UNIQUE code 유지)
- Qn-6 A: "이번 수정은 개정입니다" 체크박스 옵션 (미체크 시 version 유지)
- Qn-7 A: PRG redirect (약관은 화면 이동 자연스러움, HTMX fragment 미필요)
- Qn-8 A: `POST /__test__/reset-terms` 신설 (`SEED_TERM_COUNT=2` 상수 노출, PR #206 패턴 재활용)
- Qn-9 A: 편집 모드 code readonly

---

## 13. 스코프 예상 규모

| 항목 | 수량 |
|---|---|
| 신규 Java 파일 | 2 (Controller, Service) |
| 수정 Java 파일 | 4 (Term, TermRepository, UserAgreementRepository, DataInitializer, TestFixtureController) |
| 신규 Template | 2 |
| 수정 Template | 1 |
| 신규 CSS 클래스 | ~15 (`.admin-term-*`) |
| 신규 테스트 | 3 (Controller · RenderTest · SignupRegression) |
| 신규 E2E spec | 2 |
| 신규 계약 | 3 (list · form · edit) |
| 예상 순증 LOC | 850~1,000 (테스트 포함) |
| 예상 assertion (계약) | ~24 (list 8 · form 8 · edit 8) |

### 리스크
- **낮음** — 스키마 무변경, notice CRUD 패턴 재활용, RBAC 단순
- 회원가입 회귀 위험은 SignupRegressionTest 3 케이스로 방어
- version bump 로직이 미묘함 (버전 감소는 금지) — Term.updateContent 도메인 메서드에서 assert

---

## 14. deferred / deviation 후보

| 항목 | 사유 | 이월 위치 |
|---|---|---|
| 개정 시 회원 자동 재동의 요청 UX | 이번 스코프 초과 | 후속 admin 티켓 (`A-terms-re-agreement`) |
| 약관 본문 WYSIWYG 편집기 | Qn-2 A 유지 시 불필요. B/C 채택 시에도 별도 티켓 | `A-terms-content-editor` (조건부) |
| 회원별 동의 이력 관리 화면 | admin 마이페이지 계열 | A6 이후 |
| 미동의 회원 통계 | stats 화면 | A5 admin-stats |
| soft delete 정책 | Qn-3 A 유지 시 불필요 | — |

---

## 15. 다음 단계

이 명세는 **spec_done — Qn 결정 대기** 상태.

**결정 요청**: 위 Qn-1~9 를 원샷 답변으로 확정해주세요.
- **"모두 권장 OK"** — 전 9개 권장안 채택
- **"권장 OK, Qn-X 만 B/C"** — 일부만 다른 안
- **개별 답변** — 각 Q 마다

결정 후 ym-impl 로 인계 → 브랜치 `feature/A-admin-terms-crud` 생성 → 구현 → ym-qa → ym-verify → 머지.
