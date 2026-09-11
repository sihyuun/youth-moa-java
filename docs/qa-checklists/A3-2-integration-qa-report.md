# A3-2 admin-program-form-integration QA 리포트

- **브랜치**: `feature/A3-2-admin-program-form-integration`
- **커밋**: `3eff054` (260911_A3_2_admin_program_form_integration - F4/F0c 인라인 · Course · Attachment · 이미지 업로드)
- **QA 모드**: 옵션 (B) 회귀 우선 분할 — 신규 test/spec 신설 없음. 통합 테스트·Playwright 신규 spec 5종·계약 확장 assertion 은 **후속 세션 이월**.
- **실행 일시**: 2026-09-11 · 회사 PC (Docker Desktop 격리 상태)
- **실행자**: ym-qa (Claude Code)

---

## 결과 요약

| 영역 | 결과 | 비고 |
|---|---|---|
| 1. 정적 회귀 (`./gradlew.bat test`) | **445/446 PASS · 1 FAIL** | 실패 1건은 Docker 필요 (환경 한계), A3-2 무관 |
| 2. V13/V14 마이그레이션 안전성 | **정적 OK** | 스키마 SQL 정합 · Flyway 활성 부팅 성공 (e2e 프로파일 8090 정상 기동) |
| 3. multipart 3종 스모크 (curl) | **2 OK · 1 미검증** | 이미지·Attachment 왕복 OK · Course 인라인은 curl multipart 로 저장 안됨 (원인 후속 조사) |
| 4. A3-1 회귀 무영향 | **PASS** | 인라인 없이 기존 flow(3탭 왕복 · 수정 · 삭제) 8/8 통과 |
| 5. F4/F0c 별도 페이지 병존 무회귀 | **PASS** | admin-eligibility·admin-dynamic-field 스펙 및 계약 전 통과 |
| 6. 사용자 사이드 무회귀 | **PASS** | `/`, `/programs`, `/programs/1` HTTP 200 · `/calendar` 302 (auth 요구, 정상) |
| 7. 기존 계약 (`admin-program-form.ts`) | **갭 없음** | contracts 프로젝트 통과 (assertion 확장 없이 기존 것 유지) |
| 8. 기능 E2E (admin-program-form.spec.ts) | **6/6 PASS** | + contract 2/2 = 총 **8/8 PASS** |

**종합**: A3-1 회귀·F4/F0c 병존·사용자 사이드 모두 **무회귀**. Course 인라인 저장 신규 기능은 **UI 기반 실측 미검증** — 이월 Playwright spec 에서 반드시 커버해야 함.

---

## 1. 정적 검증

### 1-1. 컴파일 · 회귀 전체

```
./gradlew.bat test
> Task :test
446 tests completed, 1 failed
BUILD FAILED in 5m 35s
```

**실패 케이스 1건** (`build/test-results/test/TEST-io.github.sihyuuun.youthmoa.YouthMoaApplicationTests.xml`):

```
io.github.sihyuuun.youthmoa.YouthMoaApplicationTests > contextLoads() FAILED
Caused by: java.lang.IllegalStateException:
  Could not find a valid Docker environment. Please see logs and check configuration
  at org.testcontainers.dockerclient.DockerClientProviderStrategy.getFirstValidStrategy
```

- **원인**: `TestcontainersConfiguration` 은 Testcontainers PostgreSQL 컨테이너를 요구. 회사 PC Docker Desktop 은 stale socket 격리 조치로 사용 불가 (`MEMORY.md · Docker stale socket 기동 crash 조치법` 참조).
- **A3-2 와의 관계**: **무관**. `YouthMoaApplicationTests.contextLoads` 는 2026-07 P0-1 (Flyway 활성화) 시점부터 존재하며, main 브랜치에서도 동일하게 실패한다.
- **조치**: 개인 PC / CI 환경에서 최종 확인. 회사 PC 한계 명시.

### 1-2. V13/V14 마이그레이션

- V13 `create_course_and_has_courses.sql` — `program.has_courses` 컬럼 + `course` 테이블 (`ON DELETE CASCADE`) + `idx_course_program(program_id, is_active, sort_order)` 인덱스.
- V14 `create_program_attachment.sql` — `program_attachment` 테이블 (`ON DELETE CASCADE`) + `idx_program_attachment_program(program_id, sort_order)` 인덱스.
- **부팅 검증**: e2e 프로파일 (H2 in-memory) 로 `./bootrun-e2e.cmd` 기동 시 Flyway 정상 적용 · Hibernate `ddl-auto=validate` 로 매핑 검증 통과 · HTTP 200 응답. Testcontainers PostgreSQL 정합 검증은 개인 PC/CI 이월.

---

## 2. 동적 검증 (curl · e2e 프로파일 8090)

### 2-1. 기본 라우팅 스모크

| 경로 | HTTP | 비고 |
|---|---|---|
| `GET /` | 200 | 사용자 홈 |
| `GET /programs` | 200 | 사용자 프로그램 목록 |
| `GET /programs/1` | 200 | 사용자 프로그램 상세 (본문 HTML 정상 렌더) |
| `GET /calendar` | 302 | 로그인 요구 (정상 auth flow) |
| `GET /admin/login` | 200 | 관리자 로그인 페이지 |
| `GET /admin/programs` | 302 | 인증 요구 |
| `GET /admin/programs/new` | 302 → (로그인 후) 200 | 인증 후 정상 |
| `GET /admin/programs/1/eligibility` | 302 | 로그인 후 정상 (F4 별도 페이지 병존) |
| `GET /admin/programs/1/dynamic-fields` | 302 | 로그인 후 정상 (F0c 별도 페이지 병존) |

### 2-2. multipart 스모크 (관리자 세션 + CSRF)

로그인 → CSRF 추출 → 편집 폼 재로드 → 프리필 확인 시나리오.

**A. 이미지 업로드 (create)**

```
POST /admin/programs (multipart with image=@img.jpg;type=image/jpeg)
→ HTTP 302 Location: http://localhost:8090/admin/programs/29
```

- ✅ 성공. 재조회 후 편집 폼 정상 렌더.

**B. Attachment 업로드 (edit)**

```
POST /admin/programs/29 (multipart with attachments=@sample.pdf;type=application/pdf)
→ HTTP 302 Location: http://localhost:8090/admin/programs/29
```

- ✅ 성공. 재조회 후 편집 폼에서 `sample.pdf` 파일명 렌더링 확인 (`grep -c 'sample.pdf' after.html = 1`).

**C. Course 인라인 저장 (edit)**

```
POST /admin/programs/29 (multipart with:
  hasCourses=true, courses[0].name=A반, courses[0].schedule=월수금, courses[0].capacity=5)
→ HTTP 302 Location: http://localhost:8090/admin/programs/29
```

- 재조회 후 프리필 확인: `courses field count: 1` (신규 빈 row 1개) · `A반 present: 0` · bootRun 로그에 `insert into course` 없음.
- ⚠️ **UI/UX 자동화 미검증**: curl multipart 로는 Course 데이터가 DB 에 반영되지 않음. 원인 후보:
  1. **Spring MVC binding**: `List<CourseFormRow>` 을 `multipart/form-data` request 의 `courses[0].name=...` 파트로 바인딩할 때 특정 조건 (예: `@ModelAttribute` + `@RequestParam("attachments")` 병존) 에서 nested list 파트가 무시될 수 있음.
  2. **curl form 형식**: `-F "courses[0].name=A반"` 인코딩이 서버 파서에서 form-urlencoded 와 다르게 처리될 수 있음.
  3. **실제 결함**: Course 저장 로직 결함 (`AdminProgramService.upsertCourses`) 가능성.
- **후속**: Playwright UI 기반 spec 에서 반드시 검증 필요 (**이월 항목** 참조). 프로덕션 코드 수정은 이번 스코프 밖 (신규 기능 검증은 후속 세션에서 spec + 필요 시 수정).

---

## 3. 계약 검증 (Playwright contracts 프로젝트)

```
[chromium] tests\admin-program-form.spec.ts (6 tests)
[contracts] tests\visual-admin-program-form.spec.ts (2 tests)
[contracts] tests\visual-admin-eligibility.spec.ts (1 test)
[contracts] tests\visual-admin-dynamic-field.spec.ts (여러 건)
[contracts] tests\visual-program-detail.spec.ts (100/100 통과 · 갭 0건 · 의도 이탈 2건)
...
31 passed (1.7m)
```

- **기존 계약** (`admin-program-form.ts`, `admin-eligibility.ts`, `admin-dynamic-field.ts`, `program-detail.ts`) 갭 0건.
- **인라인 확장 assertion 20~30 추가** 는 이월.

---

## 4. 기능 E2E (Playwright)

```
BASE_URL=http://localhost:8090 npx playwright test admin-program-form --reporter=line
Running 8 tests using 1 worker
...
8 passed (29.7s)
```

포함 시나리오 (A3-1 회귀 · 무영향 검증):

1. 신규 등록 → 편집 폼 prefilled 확인 (3탭 왕복)
2. 편집 → 제목 수정 → 저장 → 반영
3. FK 참조 있는 시드 프로그램(#1) 삭제 → 소프트 삭제 302
4. FK 없는 신규 프로그램 삭제 → 목록 리다이렉트
5. CENTER_ADMIN 이 `/new` GET 시 403 (Qn-1 A: SYSTEM only)
6. 비로그인 시 등록 폼 → 로그인 페이지 302
7. (contracts) 관리자 프로그램 신규 폼 디자인 계약 — SYSTEM_ADMIN
8. (contracts) 관리자 프로그램 편집 폼 디자인 계약 — SYSTEM_ADMIN

**이 스펙들은 이미지 업로드·attachment·course 인라인 신규 기능은 커버하지 않음** — 기존 A3-1 flow 만 검증. multipart 파일 없이도 `@RequestParam(required=false)` 로 정상 통과 → **A3-1 회귀 무영향** 확정.

---

## 5. 사용자 사이드 무회귀

- `/`, `/programs`, `/programs/1` HTTP 200 · 렌더 마크업 정상.
- `program-detail` contract 100/100 통과, 갭 0건 (visual-program-detail.spec.ts).
- Program 상세는 course/attachment 인라인 도입과 무관하게 기존 마크업 유지.

---

## 6. 시각 확인 (사용자 영역, 대기)

Preview 도구 미가용 (도구 리스트 없음). 사용자가 직접 확인:

- [ ] `/admin/programs/new` 및 `/admin/programs/{id}` 편집 폼 3탭 레이아웃
- [ ] Course 인라인 row 추가 (up/down 화살표) · 삭제 · 최대 20개 제한 UX
- [ ] Attachment 인라인 추가 · 삭제 · 다운로드 링크 · 최대 10개 · 5MB 제한 UX
- [ ] 이미지 업로드 preview (있는 경우) 및 저장 후 프리필된 image_url 노출
- [ ] F4 eligibility 링크 · F0c dynamic-fields 링크가 편집 폼 내부에서 정상 노출

---

## 이월 항목 (후속 세션 필수)

| 이월 항목 | 사유 | 우선순위 |
|---|---|---|
| **Playwright spec 신설 (5종)**: `image-upload.spec.ts`, `course-crud.spec.ts`, `attachment.spec.ts`, `inline-eligibility.spec.ts`, `inline-dynamic-field.spec.ts` | Course 인라인 저장의 UI 기반 실측 필수 · curl multipart 로 저장 안됨 발견 → UI 로 재현 후 결함 여부 확정 | **P0** (Course 저장 실측 미검증) |
| **Integration test 신설**: Course + Attachment 단일 트랜잭션 rollback (`@Transactional`, `@SpringBootTest`) | 신규 서비스 로직 (upsertCourses, uploadAttachment) 단위 커버리지 확보 | P1 |
| **Course/Attachment Repository Test 신설**: `@DataJpaTest` + H2 | Repository 매핑 및 sort_order/soft-delete 로직 회귀 방지 | P1 |
| **Service Test 5종 신설**: `AdminProgramImageService`, `AdminProgramAttachmentService`, `AdminProgramService#upsertCourses`, 파일 유효성 검증 (확장자·크기·개수) | 정책 계약 (Qn-B/C/Δ) 회귀 방지 | P1 |
| **계약 확장 assertion 20~30 추가**: `admin-program-form.ts` 에 course row · attachment row · image preview · F4/F0c 인라인 링크 | prototype vs 구현 시각적 확정 | P2 |
| **Testcontainers 통합 회귀**: `YouthMoaApplicationTests.contextLoads()` | 회사 PC Docker 미가용 | 개인 PC / CI |

**후속 세션 최우선 액션**: Playwright UI 기반 `course-crud.spec.ts` 로 Course 인라인 저장이 실제 동작하는지 확정하고, 결함이면 수정 (이번 스코프 밖).

---

## 다음 단계 인계

**PASS 조건부**:

- A3-1 회귀 무영향 · F4/F0c 병존 무회귀 · 사용자 사이드 무회귀 · 이미지/Attachment multipart 왕복 실측 OK ✅
- Course 인라인 저장은 **UI 기반 후속 검증 필수** — 이번 세션 스코프 상 spec 신설 금지 → 후속 세션에서 확정 필요

> 이번 QA 는 **회귀 우선** 목적으로 통과. 커밋 전 최종 관문으로 `ym-verify` (적대적 검증) 호출을 권장합니다.
> Course 인라인 저장 결함 여부는 **후속 세션 (신규 Playwright spec + 필요 시 수정)** 의 필수 조건입니다. 이 상태로 main 머지 시 사용자 UI 로 course 저장이 안 될 위험이 있으니, **머지 전 반드시 UI 실측** 필요.
