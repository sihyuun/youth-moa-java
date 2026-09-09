# F4-admin-eligibility — 관리자 프로그램 자격요건 CRUD

| 메타 | 값 |
|---|---|
| 작업 ID | `F4-admin-eligibility` |
| 상태 | **`spec_confirmed`** (2026-09-09 사용자 결정: Qn-1~8 **모두 권장안 A**. §11 그대로 이행) |
| 트랙 | admin 파생 큐 **4번째 (마지막 · 파생 큐 완결 티켓)** |
| 선행 | ✅ A1 admin-shell (#205) · ✅ admin-notice (#206) · ✅ admin-terms (#207) · ✅ F0c-dynamic-fields (#208) · ✅ F4-detail-requirements-grid (#73 — `ProgramEligibility` 신설) |
| 후행 | 파생 큐 완결 → **A2 admin-programs-list · A3 admin-program-form** 착수 가능 |
| ADR | `docs/adr/admin-track-roadmap-2026-09.md` §Q4 파생 큐 순서 (공지첨부 → 약관 → 동적필드 → **F4** → 완결) |
| main 기준 | `4466252` (V11 최신 · `ProgramEligibility` @Embeddable 존재) |
| 사용자 사이드 spec | `docs/specs/F4-detail-requirements-grid.md` (impl_done PR #73) |
| 예상 규모 | 1 PR — 파일 12~14 개 · 순증 ≈ 700~900 LOC (F0c 대비 축소: 파일 업로드 없음 · 스키마 신규 없음 · 사용자 flow 회귀 없음) |

---

## 0. 계약 확인 (0단계)

- **본 화면 계약**: **없음** — 이번 티켓에서 함께 신설한다
  - `docs/design-contracts/admin/program-eligibility.md`
  - `e2e/contracts/admin-eligibility.ts`
- **admin 공통 정책**: `docs/design-contracts/admin/POLICY.md` (다크 헤더 · 인디고 primary · 존댓말 "…했어요/됐어요") 준수
- **사용자 사이드 계약**: 프로그램 상세 (`docs/design-contracts/program-detail.md` 존재 시) — 자격요건 3-grid 렌더 회귀 방지가 핵심
- **재활용 계약**: `admin-dynamic-field.ts` · `admin-notice.ts` · `admin-term.ts` (list/form/edit 셀렉터 네이밍 · confirm 모달 · 400 매핑 규칙)
- **admin/prototype.html 부재 근거**: `grep eligibility docs/00_assets/admin/` 결과 0 매치. notice·terms·dynamic-fields 와 동일 상황 → 계약 신설 근거 (admin POLICY + admin CRUD 패턴) 승계

---

## 1. 디자인 출처 (3자산 병렬 정독)

| 자산 | 인용 | 결론 |
|---|---|---|
| `docs/00_assets/prototype.tsx` `ProgramDetail` L992~1013 | 사용자 사이드 3-grid 카드 (연령 / 거주지 / 기타) | **사용자 사이드 UI 는 PR #73 로 이미 구현 완료.** F4 admin 티켓은 이 데이터의 **입력 UI 만** 다룸 |
| `docs/00_assets/prototype.html` L1026~1047 | tsx 와 동일 | 동일 |
| `docs/00_assets/HANDOFF.md` §5-E.8 (L683~687) | "지원 대상·자격요건 섹션, 필드: 연령/거주지/기타, **admin 등록 필드 필요**, `Program.eligibility = { age, region, etc }`" | HANDOFF 가 admin 입력 UI 명시. 이 티켓이 그 요구를 충족 |
| `docs/00_assets/admin/prototype.tsx` L469 `ProgramFormScreen` | stub (`/* 탭: 정보/신청/약관 */`) | admin prototype 부재 → 계약 신설 |
| `docs/00_assets/wireframe.png` | 사용자 상세에도 자격요건 섹션 **없음** (F4-detail spec L15 확인) | wireframe 은 이 스코프 밖 (HANDOFF 개선안 우선) |
| `src/main/java/.../program/ProgramEligibility.java` L11~34 | `@Embeddable` · 3 String 필드 (age/region/etc) · 각 length 100/100/200 · @Lob 금지 명시 | **엔티티 신설 불필요 · 마이그레이션 불필요** |
| `src/main/java/.../program/Program.java` L59~62 | `@Embedded ProgramEligibility eligibility` 편입 완료 | 조회·저장 접근 경로 확보 |
| `src/main/resources/templates/program/detail.html` L184~219 | `${program.eligibility.age/region/etc}` null-safe 바인딩 | 사용자 사이드 회귀 방지 대상 |

### 1-A. 자산 간 갭 (형태 vs 존재 축)

| 항목 | wireframe | prototype (사용자/admin) | HANDOFF | 축 | 채택 |
|---|---|---|---|---|---|
| 자격요건 admin 입력 UI | 없음 | admin prototype 없음 | "admin 등록 필드 필요" 명시 | **형태 없음 / 존재는 HANDOFF 인정** | ⚠️ POLICY 비대칭 3원칙 1 (prototype 누락 = 정책 폐기 아님). 형태는 이번 티켓에서 신설 · admin CRUD 패턴 재활용 |
| 필드 3종 (연령/거주지/기타) | 언급 없음 | 사용자 사이드 3 grid | HANDOFF §5-E.8 명시 | 존재 (규칙) | HANDOFF 채택 → 3필드 고정 |
| 필드 카테고리 확장 (연령·소득·거주지 별) | 없음 | 없음 | 없음 | 존재 (규칙) | ⚠️ Qn-3 — 3필드 고정 vs 확장 (권장: 고정) |
| CRUD 방식 | 없음 | 없음 | admin 등록/수정 | 형태 | ⚠️ Qn-5 — 프로그램 편집 인라인 vs 별도 페이지 (권장: 별도 페이지, A3 미착수 상태) |

### 1-B. 데이터 모델 gap 표 (필수)

**핵심 특징**: `ProgramEligibility` 는 F4-detail (PR #73) 에서 **이미 신설** → 이 티켓은 스키마 신규 없음.

| 필드 | 현재 스키마 (Program 테이블) | 조치 |
|---|---|---|
| `eligibility_age` VARCHAR(100) | ✓ 존재 (V1 baseline + PR #73 이후) | 없음 |
| `eligibility_region` VARCHAR(100) | ✓ 존재 | 없음 |
| `eligibility_etc` VARCHAR(200) | ✓ 존재 | 없음 |
| soft delete flag | ❌ 없음 | Qn-2 결정 — 3필드 통째 null 초기화가 "삭제" 역할 수행 가능. **별도 flag 불필요** (권장) |
| 감사 (updatedAt) | Program.updatedAt 존재 | 없음 (embed 값 변경 시 부모 엔티티 dirty 로 자동 반영) |

**V12 마이그레이션**: **불필요** — F0c 대비 최대 축소 지점.

**시드 데이터**: `DataInitializer.seedPrograms()` 가 이미 프로그램 시드에 `ProgramEligibility` 값을 주입 (L630~ 등 12+건). 별도 시드 조작 불필요.

### 1-C. 데이터 소비 지점 (필수)

`Program.eligibility` 를 소비하는 모든 지점.

| 소비 지점 | 위치 | 이번 티켓 영향 | prototype 매칭 확인 |
|---|---|---|---|
| 사용자 프로그램 상세 3-grid | `templates/program/detail.html` L184~219 | **회귀 방지 대상** (읽기 전용 소비) | prototype.tsx L992 매칭 (PR #73) |
| 관리자 자격요건 편집 페이지 | `AdminProgramEligibilityController` (신설) | **본 티켓 신설** | admin prototype 없음 → 계약 신설 (§0) |
| 관리자 프로그램 편집 폼 진입 지점 | A2/A3 미착수 → **`/admin/programs/{id}/eligibility` 별도 URL** (Qn-5 A) | 임시 화면. A3 착수 시 "정보" 탭 인라인 이식 | — |
| 관리자 프로그램 목록 검색·필터 | 현재 없음 (자격요건 기반 필터) | 이번 스코프 밖 | — |
| 프로그램 검색 자격요건 매칭 | 미구현 (해당 스코프는 별도 이월) | 이번 스코프 밖 | — |

### 1-D. write→read 왕복 통합 시나리오 (필수)

- 관리자가 프로그램 A `/admin/programs/A/eligibility` 진입 → 3필드 폼에 기존값 prefilled → 연령 "만 19세 이상" 으로 변경 → 저장 → PRG redirect (Qn-8 A) → `GET /programs/A` 사용자 상세 → 연령 카드에 "만 19세 이상" 노출
- 관리자가 3필드 모두 공란 저장 → Program.eligibility 값 3필드 모두 null → 사용자 상세 3-grid 는 F4-detail Q3-B 기본 문구 렌더 ("연령 제한 없음" 등)
- 관리자가 101자 연령 입력 → `@Size(max=100)` 검증 실패 → 400 flash + 폼 재표시 + 입력값 보존
- CENTER_ADMIN 이 `/admin/programs/A/eligibility` 접근 → Qn-1 결정에 따라 403 (권장 A) 또는 자기 센터만 허용 (B)
- 관리자가 자격요건 "완전 삭제" (Qn-2 A 채택 시) → 3필드 null 로 UPDATE → 별도 삭제 endpoint 불필요

---

## 2. 배경 · 스코프

### 포함

| URL | 메서드 | RBAC | 목적 |
|---|---|---|---|
| `/admin/programs/{programId}/eligibility` | GET | SYSTEM (Qn-1) | 편집 폼 (기존값 prefilled) |
| `/admin/programs/{programId}/eligibility` | POST | SYSTEM | Update (신규 = 3필드 채우기 · 삭제 = 3필드 비우기) |

**목록 화면 불필요** — 자격요건은 프로그램 1:1 값 객체 (@Embedded). 프로그램별 목록에 존재하는 다중 row 개념 없음.

### 제외 (이월)

| 항목 | 이월처 |
|---|---|
| 관리자 프로그램 편집 폼 "정보" 탭 통합 (인라인) | A3 `admin-program-form` |
| 자격요건 자동 매칭 (사용자 신청 시 자격 검증) | 별도 티켓 |
| 자격요건 카테고리 확장 (연령·소득·거주지·경력 별 다중 row) | 별도 티켓 (스코프 크게 확장) |
| 프로그램 목록 자격요건 기반 검색·필터 | A2/A3 이후 |
| CENTER_ADMIN 접근 확장 | A3 (Program-Center FK 도입 후) |

---

## 3. RBAC 정책 (Qn-1)

Qn-1 결정에 따름. F0c-dynamic-fields Qn-1 A 채택 (SYSTEM_ADMIN only) 과 동일 정책 승계 권장.

**권장안 (Qn-1-A)**: SYSTEM_ADMIN 만 허용. 근거:
1. Program-Center FK 가 아직 없어 CENTER_ADMIN 격리 로직이 임시적 (organization 문자열 매칭 = fragile)
2. F0c 와 동일 정책 유지로 admin 파생 큐 전체 일관성 확보
3. A3 착수 시 Center FK 도입 후 CENTER_ADMIN 확장 자연스러움

**대안 (Qn-1-B)**: SYSTEM_ADMIN + CENTER_ADMIN (organization 문자열 매칭). AdminNoticeService.canEdit 패턴 재활용

Controller `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` (Qn-1-A) 또는 Service `canEdit()` (Qn-1-B).

---

## 4. 엔티티 · 마이그레이션

**변경 없음** — 이 티켓의 최대 축소 지점.

- `ProgramEligibility` @Embeddable 이미 완비 (F4-detail PR #73)
- `Program.eligibility` @Embedded 이미 완비
- V12 마이그레이션 **불필요**
- 시드 12+건 이미 자격요건 값 주입 — 조작 불필요

**도메인 메서드 추가** (`Program.java`):
- `updateEligibility(ProgramEligibility)` — 값 객체 통째 교체 (@Embeddable 관례)
- 검증은 DTO 레벨 (@Size) 로 위임, 도메인은 replace 만

---

## 5. Controller / Service / DTO 상세

### admin

| 컴포넌트 | 신설 여부 | 참조 패턴 |
|---|---|---|
| `AdminProgramEligibilityController` | 신설 | `AdminProgramDynamicFieldController` (F0c) |
| `AdminProgramEligibilityService` | 신설 | 단순 (find + update 만) |
| `AdminEligibilityRequest` DTO | 신설 | `@Size(max=100) age` · `@Size(max=100) region` · `@Size(max=200) etc` (모두 nullable 허용) |
| `AdminExceptionHandler` | 재활용 | 400 flash + redirect 매핑 (기존) |

**Service 시그니처 (간단)**:
```java
public Program getProgram(Long programId);   // 진입 검증
public void updateEligibility(Long programId, AdminEligibilityRequest req);
```

### 사용자 사이드

**변경 없음** — `templates/program/detail.html` 및 `ProgramService` 는 그대로 소비. 회귀 방지가 유일한 관심사.

---

## 6. 화면 · 템플릿 (admin)

### 편집 폼 `/admin/programs/{programId}/eligibility` — `admin/program-eligibility/form.html` (신설)

- 상단: 페이지 타이틀 "자격요건 편집" + 프로그램명 표시 + "프로그램 상세로 돌아가기" 링크
- 폼 3필드:
  - `age` — `<input type="text" maxlength="100">` · 라벨 "연령" · placeholder "예: 만 19세 ~ 39세 청년" · 도움말 "공란 시 '연령 제한 없음' 으로 노출돼요"
  - `region` — `<input type="text" maxlength="100">` · 라벨 "거주지" · placeholder "예: 경기도 거주 또는 활동 중인 청년"
  - `etc` — `<textarea maxlength="200">` · 라벨 "기타 조건"
- 하단: "저장" 버튼 (primary) + "취소" 버튼 (secondary — 목록/상세로 이동)
- **삭제 버튼 없음** — Qn-2 A 채택 시 "3필드 공란 저장" 이 곧 삭제

### admin POLICY 준수

- 존댓말: "저장했어요" / "입력값을 확인해주세요"
- 다크 헤더 · 인디고 primary
- form encoding: `application/x-www-form-urlencoded` (파일 없음)

---

## 7. 회귀 방지 (핵심 · F0c 대비 리스크 낮음)

### 사용자 프로그램 상세 무회귀

| 기준 | 검증 |
|---|---|
| `templates/program/detail.html` L184~219 3-grid 렌더 유지 | 기존 `ProgramDetailRenderTest` (있으면) 재실행 |
| `eligibility` null-safe 바인딩 유지 | Qn-2 A (3필드 null = 삭제) 시 F4-detail Q3-B 기본 문구 렌더 확인 |
| 시드 12+건 자격요건 값 보존 | DataInitializer 재기동 시 덮어쓰지 않는지 확인 (Program 은 이미 idempotent) |
| 사용자 사이드 계약 존재 시 갭 0 유지 | `npx playwright test --project=contracts --grep program-detail` |

### admin CRUD 실효성

- 관리자가 저장한 값이 재기동 시 시드에 덮어씌워지지 않는가? (Program 은 `existsBy...` idempotent 시드라 OK)
- 3필드 공란 저장 후 재진입 시 폼이 빈 상태로 prefilled 되는가?

---

## 8. 테스트

### 정적

- `AdminProgramEligibilityControllerTest` (@WebMvcTest + @WithMockUser) — GET/POST · RBAC 403 · 400 검증 실패
- `AdminProgramEligibilityServiceTest` — update 라운드트립 · null 필드 허용
- `AdminEligibilityRequestValidationTest` — Bean Validation @Size TC (100/100/200)
- `JpaMappingTest` — 변경 없음 (기존 통과 재확인)
- **회귀**: 기존 `ProgramDetailRenderTest` (있으면) 재실행

### 동적 (curl)

- `GET /admin/programs/1/eligibility` 200 + prefilled
- `POST /admin/programs/1/eligibility` `age=만 20세 이상` 302 → `GET /programs/1` 응답에 "만 20세 이상" 포함
- `POST /admin/programs/1/eligibility` `age=<101자>` 400 flash + 폼 재표시
- CENTER_ADMIN 계정으로 접근 시 403 (Qn-1 A)
- 3필드 공란 저장 후 `GET /programs/1` 에 기본 문구 노출

### 계약 신설

- `docs/design-contracts/admin/program-eligibility.md` — 폼 정책 (admin POLICY 승계)
- `e2e/contracts/admin-eligibility.ts` — 셀렉터·정량값 (필드 3개 · maxlength · 라벨 텍스트 · 도움말 문구)

### 기능 E2E

- `e2e/tests/admin-eligibility-form.spec.ts` — CRUD 왕복 (편집 → 저장 → 사용자 상세 반영 확인)
- `e2e/tests/admin-eligibility-rbac.spec.ts` — CENTER_ADMIN 403 · USER 403
- **회귀**: 기존 `program-detail.spec.ts` (있으면) 무회귀

### reset endpoint (Qn-6)

- **불필요 권장** — Program 시드가 이미 자격요건 값 보유. eligibility 는 별도 row 가 아니라 Program 컬럼이므로 "id 초과 삭제" 개념 없음. Test 별도 setup 필요 시 각 테스트가 명시적으로 원복 (Program.updateEligibility 호출).
- 대안 (Qn-6 B): `POST /__test__/reset-eligibility` 신설 — 시드 Program 12건의 eligibility 를 원본 시드값으로 되돌리기. E2E 격리 강도 높음 but 시드값 하드코딩 중복 발생.

---

## 9. 결정 필요 항목 (Qn — 사용자 대기)

| # | 질문 | 선택지 | 권장 |
|---|---|---|---|
| **Qn-1** | RBAC 정책 | A. SYSTEM_ADMIN 만 (F0c 와 동일) / B. SYSTEM+CENTER (organization 문자열 매칭) | **A** — F0c 정책 일관성 · A3 에서 확장 |
| **Qn-2** | 삭제 정책 | A. 3필드 공란 저장 = 삭제 (권장 · 별도 endpoint 없음) / B. 명시적 DELETE endpoint + isActive flag 추가 (V12 마이그레이션 필요) | **A** — 스키마 변경 회피 · UX 자연스러움 |
| **Qn-3** | 필드 확장 | A. 3필드 고정 (연령/거주지/기타 · 권장) / B. 카테고리 다중 row 로 확장 (별도 엔티티 신설) | **A** — HANDOFF 계약 정합 · 스코프 최소 |
| **Qn-4** | 검증 상한 | A. 엔티티 컬럼 length 승계 (100/100/200 · 권장) / B. Bean Validation 별도 확장 | **A** |
| **Qn-5** | 관리자 화면 진입 지점 | A. `/admin/programs/{id}/eligibility` 별도 페이지 (권장 · F0c 와 동일 · A3 미착수) / B. 프로그램 편집 페이지 인라인 섹션 (A3 통합) | **A** — A3 착수 시 인라인 이식 |
| **Qn-6** | reset endpoint 신설 | A. **미신설** (권장 · Program 시드 재활용) / B. 신설 (`POST /__test__/reset-eligibility`) | **A** — 스키마 변경 없음이 성립하는 근거 |
| **Qn-7** | 폼 UI 문구 톤 | A. "…해주세요" (F0c 승계 · 권장) / B. "…하세요" | **A** |
| **Qn-8** | 응답 방식 | A. PRG redirect (권장 · admin CRUD 일관성) / B. HTMX fragment 부분 갱신 | **A** — F0c/notice/term 과 동일 |

---

## 10. 스코프 예상 규모

| 항목 | 규모 |
|---|---|
| 파일 수 | 12~14 개 (Controller 1 + Service 1 + DTO 1 + template 1 + 계약 2 + 테스트 6~8 + 미세 static asset) |
| 순증 LOC | 700~900 (테스트 포함) |
| Assertion | 정적 ≈ 20건 · 계약 ≈ 10건 · E2E 2 spec |
| 리스크 (낮음) | 스키마 변경 없음 · 사용자 flow 회귀 없음 (읽기만) · 파일 업로드 없음 |
| 리스크 (매우 낮음) | 3필드만 다루는 단순 CRUD. F0c 대비 리스크 대폭 축소 |

**F0c 대비 축소 지점**:
- 스키마 신규 X (F0c 는 V11 + 2 엔티티 신설)
- 파일 업로드 X (F0c 는 ATTACHMENT 필드 + FileStorage 통합)
- 사용자 flow 회귀 X (F0c 는 apply.html Step 2 병합 + multipart 전환)
- 목록 화면 X (@Embedded 라 1:1 · 다중 row 개념 없음)

---

## 11. deferred / deviation

### deferred

| 항목 | 이월처 |
|---|---|
| 관리자 프로그램 편집 폼 "정보" 탭 통합 (인라인) | A3 `admin-program-form` |
| CENTER_ADMIN 접근 확장 | A3 (Program-Center FK) |
| 자격요건 자동 매칭 (사용자 신청 자격 검증) | 별도 티켓 |
| 카테고리 다중 row 확장 | 별도 티켓 |
| 프로그램 목록 자격요건 기반 검색·필터 | A2/A3 이후 |

### deviation

| 항목 | 사유 |
|---|---|
| admin prototype 부재 → 계약 신설 | POLICY 비대칭 3원칙 1 (prototype 누락 = 정책 폐기 아님) |
| 목록 화면 미신설 | @Embedded 1:1 값 객체이므로 목록 개념 없음. F0c/notice/term 패턴에서 이탈이지만 도메인 성격상 필연 |
| reset endpoint 미신설 | Qn-6 A 결정 시 — 스키마 변경 없어 시드 재활용 가능 |

---

## 12. 파생 큐 완결 판정

이 티켓 완료 시 admin 파생 큐 4/4 완결:

| # | 파생 큐 티켓 | 상태 |
|---|---|---|
| 1 | admin-notice-attachment (#206) | ✅ impl_done |
| 2 | admin-terms-crud (#207) | ✅ impl_done |
| 3 | F0c-dynamic-fields (#208) | ✅ impl_done |
| 4 | **F4-admin-eligibility** | ⏳ 이 티켓 |

**후행 로드맵**: A2 admin-programs-list → A3 admin-program-form (F0c-dynamic-fields · F4-admin-eligibility 인라인 통합) → A4~A9

---

## 다음 단계 인계

명세 산출 완료. Qn-1 ~ Qn-8 (총 8개 결정 항목) 사용자 결정 이후 `ym-impl` 인계 가능.

**권장 세트**: 모두 A (§9 표 오른쪽 열). 사용자 "모두 권장 OK" 응답 시 spec 상태를 `spec_confirmed` 로 갱신하고 §9 표에 채택 안 표기.

**주의**:
- F0c 패턴 재활용 극대화 — Controller/Service/화면 구조 · 계약 신설 절차 모두 동일
- 스키마 변경 없음 · 파일 업로드 없음이 본 티켓의 최대 이점 — 그만큼 리스크 낮음
- 사용자 사이드 프로그램 상세 회귀 방지가 유일한 실질 위험 지점
