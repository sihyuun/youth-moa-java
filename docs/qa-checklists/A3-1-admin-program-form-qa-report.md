# QA 리포트 — A3-1 admin-program-form (재검증)

| 메타 | 값 |
|---|---|
| 브랜치 | `feature/A3-1-admin-program-form` |
| 초기 commit | `1bacda0` (반려) |
| fix commit | `64e2ef0` (260910_a3_1_fix_qa_rejects) |
| spec | `docs/specs/A3-admin-program-form.md` (spec_confirmed · Qn 모두 A) |
| QA 일시 | 2026-09-10 (재검증 2회차) |
| **최종 판정** | ✅ **PASS — ym-verify 인계 대상** |

---

## 반려 3건 fix 재현 결과 (최우선 확인)

### #1 [BLOCKER] 날짜 prefill 로케일 렌더 → **RESOLVED**

**Before (`1bacda0`)**: Windows 한국 로케일에서 Thymeleaf `th:field` 가 `value="26. 8. 31."` 렌더 → HTML5 `input[type=date]` 가 value 무시 → 편집 시 전 date 필드 공백

**After (`64e2ef0`)**: `form.html` L112~170 `th:value="${#temporals.format(...,'yyyy-MM-dd')}"` + null-guard, `ProgramFormRequest` DTO 에 `@DateTimeFormat(pattern="yyyy-MM-dd")` (submit 파싱 안전화)

**실측 원본** (`curl -b /tmp/cookies.txt http://localhost:8090/admin/programs/1`):

```html
<input type="date" id="startDate" name="startDate"
       value="2026-08-31"
       class="admin-program-form-input"/>
<input type="date" id="endDate" name="endDate"
       value="2026-09-13"
       class="admin-program-form-input"/>
<input type="date" id="applyStartDate" name="applyStartDate" required
       value=""
       class="admin-program-form-input"/>
<input type="date" id="applyEndDate" name="applyEndDate" required
       value=""
       class="admin-program-form-input"/>
```

- `startDate` · `endDate` = `2026-08-31` / `2026-09-13` (yyyy-MM-dd 포맷 · HTML5 date 브라우저 파싱 통과)
- `applyStartDate` · `applyEndDate` 는 시드 null (Qn-8 A "기존 시드 편집 없이 유지" 준수) → `null`-guard 로 빈 문자열 출력 ✅

### #2 [회귀] admin-programs-detail.spec.ts → **RESOLVED**

- `ls e2e/tests/admin-programs-detail.spec.ts` → **파일 없음** (fix commit diff: 41줄 -)
- admin 회귀 실행 시 spec 참조 오류 0건 · admin 전체 64/64 PASS

### #3 [계약 spec 버그] visual-admin-program-form.spec.ts goto 누락 → **RESOLVED**

- fix commit diff (L15 · L27 각 `await page.goto(contract.path)` 삽입) 확인
- `--project=contracts admin-program-form` 재실행 → **2 passed** (new 32/32, edit 11/11, 갭 0)

---

## 재검증 6영역

### 1. 정적 검증

```
gradlew.bat test --tests "*AdminProgram*"
BUILD SUCCESSFUL in 1m 59s
```

- AdminProgramFormServiceTest · AdminProgramFormRenderTest · AdminProgramDetailRenderTest 갱신분 · A2 서비스 회귀 포함 전수 PASS
- ProgramFormRequest DTO `@DateTimeFormat` 신설이 기존 서비스 테스트 회귀 없음 확인

**판정**: ✅ PASS

### 2. 동적 검증 (curl · http://localhost:8090 · e2e 프로파일)

sysadmin@youth-moa.test / Admin!234 세션. bootRun `.claude/scripts/bootrun-e2e.cmd` 로직 재현 (Start-Process + JDK 17 + `--args="..."`).

| 시나리오 | 결과 | 원본 |
|---|---|---|
| `GET /admin/programs/1` 편집 폼 | ✅ 200 | 위 §반려 #1 실측 인용 |
| date input `startDate` / `endDate` prefill | ✅ `value="2026-08-31"` / `"2026-09-13"` | 위 §반려 #1 인용 |
| date input `applyStartDate/End` null 처리 | ✅ `value=""` (Qn-8 A 준수) | 위 §반려 #1 인용 |
| POST /admin/login sysadmin | ✅ 302 → `/admin` | `login:302 redirect:http://localhost:8090/admin` |

**판정**: ✅ PASS (실측 원본 확보)

### 3. 계약 검증 (`--project=contracts admin-program-form`)

```
BASE_URL=http://localhost:8090 npx playwright test --project=contracts admin-program-form
Running 2 tests using 1 worker
[1/2] 관리자 프로그램 신규 폼 디자인 계약 — SYSTEM_ADMIN
[2/2] 관리자 프로그램 편집 폼 디자인 계약 — SYSTEM_ADMIN
  2 passed (6.3s)
```

**갭 리포트 원본**:

```
gap-admin-program-form-new.md
> ## 비로그인 — 32/32 통과
> 갭 없음.
> **합계: 32/32 통과 · 갭 0건**

gap-admin-program-form-edit.md
> ## 비로그인 — 11/11 통과
> 갭 없음.
> **합계: 11/11 통과 · 갭 0건**
```

**판정**: ✅ PASS (갭 0 · new 32/32 · edit 11/11)

### 4. 기능 E2E (`--project=chromium admin-program-form`)

```
Running 6 tests using 1 worker
[1/6] 신규 등록 → 편집 폼 prefilled 확인 (3탭 왕복)
[2/6] 편집 → 제목 수정 → 저장 → 반영
[3/6] FK 참조가 있는 시드 프로그램(#1) 삭제 시 400
[4/6] FK 없는 신규 프로그램 삭제 → 목록 리다이렉트
[5/6] CENTER_ADMIN 이 /new GET 시 403 (Qn-1 A: SYSTEM only)
[6/6] 비로그인 시 등록 폼 접근 → 로그인 페이지로
  6 passed (14.5s)
```

**핵심**: 이전 반려 시 FAIL 이었던 `[1/6] 신규 등록 → 편집 폼 prefilled 확인 (3탭 왕복)` 이 이번엔 **PASS** — 날짜 필드 prefill 회복 확인의 결정적 증거.

**판정**: ✅ PASS (6/6)

### 5. 회귀 검증

**5-1. Admin 전체 (`-g admin`)**:
```
[62/64] qa-dynamic-admin-term.spec.ts:131:5 · Qn-3 A
[63/64] qa-dynamic-admin-term.spec.ts:153:5 · Qn-4 A empty state
[64/64] qa-dynamic-admin-term.spec.ts:193:5 · Qn-8 A reset-terms
  64 passed (2.2m)
```

Admin 전체 64/64 PASS. admin-programs-detail spec 참조 오류 0건. reset endpoint 4종 (`reset-programs` · `reset-terms` · `reset-notices` · `reset-apply-questions`) 상호 파괴 없음 (spec 별 reset 204 확인).

**5-2. 사용자 사이드 (`-g "program|apply|signup"`)**:
```
[65/67] signup.spec.ts:106:5 · 서버 측 비밀번호 정책 위반
[66/67] signup.spec.ts:136:5 · FormatCheck 그룹 내 다중 @AssertTrue
[67/67] signup.spec.ts:161:5 · 중복확인 안 누르고 제출
  67 passed (2.0m)
```

사용자측 program/apply/signup 67/67 PASS. Nullable 컬럼 8종 (V12 스키마) 확장 후에도 사용자 SELECT 무회귀.

**판정**: ✅ PASS (admin 64/64 + 사용자 67/67 = 131/131, 회귀 0건)

### 6. 시각 확인 (사용자 영역, 대기)

curl 로 확인 완료:
- 편집 폼 date input 4종 렌더 형식 ✅ (`yyyy-MM-dd` 또는 빈값)
- 3탭 마크업 (`#tab-info / #tab-apply / #tab-terms`) ✅
- 삭제 버튼 노출 (편집 모드) ✅

**사용자 눈 확인 잔여 항목** (Preview 도구 없음):
- 편집 폼 진입 시 브라우저 date picker 가 `value="2026-08-31"` 정상 표시
- 3탭 JS 전환 애니메이션·인터랙션 감각
- V12 마이그레이션 신설 컬럼 (venue · contact · description · terms_*) 편집 폼 라벨/스페이싱
- confirm 모달 오픈·닫기 타이포

**판정**: ⚠️ 사용자 육안 확인 대기 (버그 없이 렌더 마크업 정상)

---

## 7. 미실행 (환경 한계)

- Testcontainers 통합 테스트 (`YouthMoaApplicationTests`) — 회사 PC Docker 미기동 시 회피, CI ubuntu 러너에서 매 PR 검증

---

## 8. 최종 판정 및 인계

| 영역 | 판정 |
|---|---|
| 반려 #1 (날짜 prefill) | ✅ RESOLVED — `value="2026-08-31"` 실측 확인 |
| 반려 #2 (detail spec 삭제) | ✅ RESOLVED — 파일 없음 · 회귀 64/64 |
| 반려 #3 (계약 goto 삽입) | ✅ RESOLVED — 계약 2/2 · 갭 0 |
| 정적 | ✅ PASS |
| 동적 (curl) | ✅ PASS (실측 원본 인용) |
| 계약 | ✅ PASS (new 32/32 · edit 11/11 · 갭 0) |
| 기능 E2E | ✅ PASS (6/6, prefilled 시나리오 포함) |
| 회귀 | ✅ PASS (admin 64/64 · 사용자 67/67) |
| 시각 | ⚠️ 사용자 대기 (마크업은 정상, 감성 확인만 필요) |

**결론**: **PASS**. ym-impl 반려 3건 전수 해소 · 회귀 0건 · 계약 갭 0 · 기능 E2E 6/6.

---

## 9. 다음 단계

> 검증 완료. 커밋 전 최종 관문으로 **`ym-verify`** (적대적 검증) 호출을 권장합니다.
> 시각 확인 (사용자 영역, 감성 요소) 통과 시 머지 진행 가능합니다.

## 10. 작업 큐 메타

- 상태: **qa_done**
- 다음: ym-verify → 시각 사용자 컨펌 → 머지
