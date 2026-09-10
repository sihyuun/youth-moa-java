# QA 리포트 — A3-1 admin-program-form

| 메타 | 값 |
|---|---|
| 브랜치 | `feature/A3-1-admin-program-form` |
| commit | `1bacda0` (260910_A3_1_admin_program_form) |
| spec | `docs/specs/A3-admin-program-form.md` (spec_confirmed · Qn 모두 A) |
| QA 일시 | 2026-09-10 |
| 결과 | **반려 (impl fix 필요 3건)** — 동적/정적 대부분 pass, 그러나 회귀 3건 + 실사용 버그 1건 |

---

## 1. 정적 검증

- `./gradlew test --tests "*AdminProgram*"` → **BUILD SUCCESSFUL** (AdminProgramFormServiceTest 15 TC · AdminProgramFormRenderTest 4 TC · AdminProgramDetailRenderTest 갱신분 · A2 서비스 회귀 포함)
- ym-impl 커밋 본문 445/446 PASS (Testcontainers 1건 Docker 미기동 예외) 재확인 — 회사 PC 재실행 필요 시 별도, CI ubuntu 러너에서 감지 예정

**정적 판정**: ✅ PASS

---

## 2. 동적 검증 (curl · http://localhost:8090 · e2e 프로파일)

`.claude/scripts/bootrun-e2e.cmd` 로 V12 마이그레이션 반영 후 sysadmin 로그인 세션으로 실측.

| 시나리오 | 결과 | 원본 응답 |
|---|---|---|
| Qn-A A · `GET /admin/programs/1` 편집 폼 대체 | ✅ 200 · `<span>프로그램 편집</span>` · `input[name=title] value="취업역량 강화 워크숍"` · `tab-info/apply/terms` 3탭 | 위 검증 로그 |
| Qn-A A · `GET /admin/programs/new` | ✅ 200 · `<span>프로그램 신규 등록</span>` · 3탭 모두 렌더 | 동 |
| Qn-A CRUD · `POST /admin/programs` (create) | ✅ 302 → `/admin/programs/25` (신규 id 발급) | `create=302` |
| Qn-A CRUD · `POST /admin/programs/25` (update, approvalMode=AUTO) | ✅ 302 → `/admin/programs/25` · 재조회 `value="qa-curl-...-updated"` | `update=302` |
| Qn-3 A · `POST /admin/programs/1/delete` (FK 있음) | ✅ **400** — Application FK 참조 blocked | `delete_fk=400` |
| Qn-3 A · `POST /admin/programs/25/delete` (FK 없음) | ✅ 302 → `/admin/programs` | `delete_clean=302` |
| Qn-1 A · CENTER_ADMIN `GET /new` | ✅ **403** | `center_new=403` |
| Qn-1 A · CENTER_ADMIN `POST /admin/programs` | ✅ **403** | `center_post=403` |
| Qn-1 A · 비로그인 `GET /new` | ✅ 302 → `/admin/login` | `anon=302` |
| Qn-6 A · `POST /__test__/reset-programs` | ✅ 204 | `reset=204` |
| Qn-Δ4 A · 필수 validation (applyStart/End 미입력) | ✅ 400 · body `"신청 기간을 입력해주세요."` | `vfail=400` |

**동적 판정**: ✅ 전 시나리오 PASS. Qn 결정 반영 정확.

---

## 3. 계약 검증 (`--project=contracts`)

`BASE_URL=http://localhost:8090 npx playwright test --project=contracts admin-program-form`

**결과**: **2 failed** (신규 · 편집 두 계약 모두 갭 30건 이상)

**원인 분석**: 계약 자체·구현·러너 모두 정상. **spec 파일 `e2e/tests/visual-admin-program-form.spec.ts` 가 `page.goto(contract.path)` 를 누락**. 다른 visual 계약 spec (`visual-admin-dynamic-field.spec.ts` L17/L29/L41 · `visual-admin-eligibility.spec.ts` L13) 은 모두 `await page.goto(contract.path)` 를 loginAdmin 뒤에 명시하는데, program-form spec 만 이 호출이 빠져 있음.

결과적으로 계약 검증은 loginAdmin 직후 착지한 `/admin` (대시보드) 에서 program-form 셀렉터를 조회 → 전 checks (`admin-program-form-title` · `admin-program-form-tab` 등) 가 "요소 없음" 으로 실패. **오탐** 이지만 계약이 실제로 동작하지 않으므로 CI/wrap-up 에서 이 spec 은 기능적으로 무력화된 상태.

**필요 수정** (impl 반려 항목 ①):
```ts
// e2e/tests/visual-admin-program-form.spec.ts:15,26
await loginAdmin(page);
await page.goto(adminProgramFormNewContract.path);  // ← 추가
const anon = await runContract(page, adminProgramFormNewContract, 'anon');
```

**계약 갭 실측 판정**: 현 spec 로는 판정 불가 (goto 누락). goto 삽입 후 재실행 필요.

**계약 판정**: ⚠️ **UNVERIFIED — spec goto 누락**

---

## 4. 기능 E2E (`--project=chromium admin-program-form.spec.ts`)

**결과**: **5 passed / 1 failed** (총 6)

| # | 시나리오 | 결과 |
|---|---|---|
| 1 | 신규 등록 → 편집 폼 prefilled 확인 (3탭 왕복) | ❌ **FAIL** — `applyStartDate` prefill 값이 빈 문자열 |
| 2 | 편집 → 제목 수정 → 저장 → 반영 | ✅ PASS |
| 3 | FK 참조 시드 프로그램 삭제 시 400 | ✅ PASS |
| 4 | FK 없는 신규 프로그램 삭제 → 목록 리다이렉트 | ✅ PASS |
| 5 | CENTER_ADMIN `/new` 시 403 (Qn-1 A) | ✅ PASS |
| 6 | 비로그인 등록 폼 접근 → 로그인 페이지 | ✅ PASS |

**FAIL 원인 (impl 반려 항목 ② — 실사용 버그)**:

편집 폼 렌더 시 날짜 필드 value 가 locale-dependent 포맷으로 출력됨.

```html
<input type="date" id="startDate" name="startDate" value="26. 8. 31."/>
<input type="date" id="endDate"   name="endDate"   value="26. 9. 13."/>
<input type="date" id="applyStartDate" ... value=""/>  <!-- 시드는 null 이라 빈값 -->
```

- `LocalDate.toString()` 이 아닌 Thymeleaf 기본 `#temporals` 처리로 `y. M. d.` 시스템 로케일 포맷이 나옴 (Windows 한국 로케일)
- HTML5 `input[type=date]` 는 `yyyy-MM-dd` (RFC 3339) 만 허용 → 브라우저가 value 무시하고 빈 필드 렌더
- 결과: **관리자가 편집 진입 시 진행 시작/종료일 · 신청 시작/종료일이 모두 비어 보임 · 저장 시 기존 값 완전 소실 위험**

**수정 방법 (form.html)**:
```html
<input type="date" name="startDate"
       th:value="${form.startDate != null} ? ${#temporals.format(form.startDate, 'yyyy-MM-dd')} : ''"/>
```
또는 DTO 에 `@DateTimeFormat(pattern="yyyy-MM-dd")` + `th:field` 전용 컨버터 확인.

시드 프로그램 신설 컬럼(`applyStartDate`/`applyEndDate`) 이 null 인 것도 참고 사항이나, spec Qn-8 A ("기존 시드 편집 없이 유지") 준수이므로 이는 정책상 문제 없음. 단 편집 UX 상 null 필드에 안내 문구가 없어 관리자가 실수로 저장 시 400 유도 (신청기간 필수 validation) — 편집 진입 시 미리 default 값 (예: `startDate - 14일`) 을 프리필하는 UX 는 별도 판단 필요 (본 반려에는 미포함).

**기능 E2E 판정**: ❌ **1건 FAIL — 실사용 버그**

---

## 5. 회귀 검증

### 5-1. 사용자 사이드 무회귀 (`-g "program|apply|signup"`)

**결과**: **67 passed / 4 failed** — 사용자 측 프로그램/신청/회원가입 무회귀. 실패 4건 전부 admin (`admin-program-form.spec.ts` 1건 위 §4 · `admin-programs-detail.spec.ts` 3건 아래).

Nullable 컬럼 8종 추가로 인한 사용자 사이드 SELECT 확장 문제 없음 확인.

### 5-2. Admin 전체 무회귀 (`-g admin --grep-invert "program-form"`)

**결과**: **59 passed / 3 failed**

실패 3건 전부 `admin-programs-detail.spec.ts` (A2 상세 spec):

```
[chromium] › admin-programs-detail.spec.ts:8:5  › /admin/programs/1 진입 → 상세 카드 + 하위 관리 링크
[chromium] › admin-programs-detail.spec.ts:16:5 › 편집·삭제 버튼 disabled (A3 이월)
[chromium] › admin-programs-detail.spec.ts:32:5 › 목록 → 상세 이동
```

**원인 (impl 반려 항목 ③ — 회귀)**:

Qn-A A 결정 (`/admin/programs/{id}` = 편집 폼 대체) 을 A3-1 impl 이 반영하면서 **A2 detail 스펙 파일을 갱신하지 않음**. spec §1-C · §8-2 에서 "A2 spec §11 deviation 갱신 필요" 를 명시했으나 실 조치는 누락.

A2 detail spec 은 "편집·삭제 버튼 disabled (A3 이월)" 같은 이월 시점 검증이라 A3-1 완료 시점에는 반드시 정리되어야 함.

**필요 조치**:
- `e2e/tests/admin-programs-detail.spec.ts` 삭제 또는 편집 폼 검증으로 재작성 (Qn-A A 반영)
- `e2e/contracts/admin-programs.ts` 의 `adminProgramDetailContract` 는 impl 이 `admin-program-form.ts` 에서 재-export 로 처리했으므로 계약 자체 회귀는 없음

### 5-3. reset endpoint 4종 상호 파괴 없음

- `reset-programs` 204 ✅ (§2)
- `reset-terms` 204 (Qn-8 A 스펙 spec pass 확인)
- `reset-notices` · `reset-apply-questions` (다른 spec 회귀에서 재사용 확인, 별도 실행 없음)

**회귀 판정**: ❌ **admin-programs-detail 3건 회귀 (spec 갱신 누락)**

---

## 6. 시각 확인 (사용자 영역 대기)

curl 로 확인 완료:
- ① `.admin-program-form-title` 텍스트 (신규="프로그램 신규 등록" / 편집="프로그램 편집") ✅
- ② `#tab-info / #tab-apply / #tab-terms` 3탭 렌더 ✅
- ③ `admin-program-form-tab--active` class 로 tab-info 기본 활성 ✅
- ④ `admin-program-form-actions .admin-btn--danger` 삭제 버튼 노출 (편집 모드) ✅

**사용자 눈 확인 필요 (Preview 도구 없음)**:
- 3탭 JS 전환 애니메이션·인터랙션 감각
- confirm 모달 오픈·닫기·타이포
- '+ 프로그램 등록' 버튼의 SYSTEM_ADMIN 조건부 노출 (list.html)
- 이미지 URL 필드 placeholder / 도움말 문구
- V12 마이그레이션 컬럼 (venue · contact · description · terms_*) 편집 폼 UI 라벨/스페이싱

**시각 판정**: ⚠️ 계약 goto 수정 후 preview 로 재확인 권장. 육안 인터랙션 4항은 사용자 확인 대기.

---

## 7. 미실행 (환경 한계)

- Testcontainers 통합 테스트 (`YouthMoaApplicationTests`) — Docker 미기동 시 회사 PC 회피, CI ubuntu 러너에서 매 PR 검증

---

## 8. 총평 및 반려 사유

| 영역 | 판정 |
|---|---|
| 정적 | ✅ PASS |
| 동적 (curl) | ✅ PASS (11 시나리오 전수 통과) |
| 계약 | ⚠️ UNVERIFIED (spec goto 누락) |
| 기능 E2E | ❌ 1건 FAIL (날짜 prefill 실사용 버그) |
| 회귀 | ❌ 3건 FAIL (A2 detail spec 갱신 누락) |
| 시각 | ⚠️ 사용자 대기 |

**ym-impl 반려 항목**:

1. **[BLOCKER · 실사용 버그]** `admin/program/form.html` 날짜 필드 (startDate · endDate · applyStartDate · applyEndDate) prefill 을 `yyyy-MM-dd` 로 강제. `#temporals.format(...,'yyyy-MM-dd')` 또는 DTO `@DateTimeFormat` 적용. Windows 한국 로케일에서 브라우저가 value 무시 → 편집 시 모든 날짜 필드가 비어 보이는 심각 버그.

2. **[회귀]** `e2e/tests/admin-programs-detail.spec.ts` 3건 실패. Qn-A A (detail → form 대체) 반영으로 이 spec 은 삭제하거나 편집 폼 검증으로 재작성 필요. spec §1-C · §8-2 명시 조치 누락.

3. **[계약 spec 버그]** `e2e/tests/visual-admin-program-form.spec.ts` L15/L26 loginAdmin 다음에 `await page.goto(contract.path)` 삽입. dynamic-field · eligibility spec 패턴 참조. 미수정 시 계약 검증이 상시 오탐 → CI 무의미.

세 항목 모두 수정 후 `ym-qa` 재호출 요망. 특히 ① 은 관리자 실사용 시 데이터 소실 위험이라 최우선.

## 9. 작업 큐 메타

- 상태: **qa_failed**
- 다음 단계: ym-impl 재작업 (반려 3건) → ym-qa 재검증 → 통과 시 ym-verify → 머지
