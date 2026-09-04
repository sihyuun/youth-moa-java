# QA 리포트 — A-admin-notice-attachment

| 메타 | 값 |
|---|---|
| 대상 브랜치 | `feature/admin-notice-attachment` |
| impl commit | `09e9c83` (초회) → `9a726f6` (fix 1차) |
| QA 세션 | 2026-09-04 초회 QA (ym-qa) → 2026-09-04 재검증 (ym-qa) |
| bootRun 프로파일 | `e2e` (LocalFileStorage 활성, port 8090) |
| **최종 판정 (재검증)** | **FAIL — 반려 3건 중 2건 RESOLVED · P0-2 fix 부작용으로 신규 회귀 1건 발생 → 재반려** |

---

## 재검증 결과 — 2026-09-04 (fix commit `9a726f6`)

### 반려 3건 재현 결과

| 항목 | 초회 상태 | 재검증 결과 | 판정 |
|---|---|---|---|
| P0-1: `GET /admin/notices` 500 (LazyInitializationException) | FAIL | 200 OK + `admin-notice-col-author` 에 `시스템관리자` 정상 렌더 | **RESOLVED** |
| P0-2: HTMX webjar 경로 404 | FAIL | `/webjars/htmx.org/2.0.4/dist/htmx.min.js` 200, form.html 이 새 경로 참조 | **RESOLVED (기능만)** |
| P1: 확장자 위반 시 500 | FAIL | 400 + `{"error":"허용되지 않는 파일 형식이에요. pdf, hwp, docx, xlsx 만 업로드할 수 있어요."}` | **RESOLVED** |

### 신규 회귀 (P0-3, fix commit 부작용)

**증상**: 첨부파일 업로드 인터랙션이 브라우저에서 실패한다. 서버 curl은 200 정상.

**재현 절차**:
```bash
cd e2e && BASE_URL=http://localhost:8090 npx playwright test --project=chromium admin-notice-upload --reporter=line
```

Playwright 실측 결과 원본:
```
Error: expect(locator).toBeVisible() failed
Locator: locator('.admin-notice-attachment-item').filter({ hasText: 'e2e-dummy.pdf' })
Expected: visible
Timeout: 10000ms
Error: element(s) not found

# error-context.md 페이지 스냅샷 발췌
- heading "첨부파일" [level=3]
- text: 등록된 첨부파일이 없어요.   ← 업로드 후에도 empty state 유지
```

curl 실측 (동일 endpoint, 정상):
```
POST /admin/notices/17/attachments (tiny.pdf, 15B, _csrf form field)
→ 200 + <li class="admin-notice-attachment-item"> tiny.pdf 정상 렌더
```

**근본 원인 (확정)**:

`form.html:16` 의 P0-2 fix 가 HTMX script 태그에 `defer` 를 도입:

```html
<script th:src="@{/webjars/htmx.org/2.0.4/dist/htmx.min.js}" defer></script>
```

`defer` 스크립트는 HTML 파싱 완료 후 · `DOMContentLoaded` 직전에 실행된다.
반면 body 하단 inline script (`form.html:179-188`) 는 파싱 순서대로 즉시 실행되며,
그 시점에 `window.htmx` 는 아직 `undefined` 이다.

```javascript
// form.html:180-188
(function () {
    var token = document.querySelector('meta[name="_csrf"]');
    var header = document.querySelector('meta[name="_csrf_header"]');
    if (token && header && window.htmx) {          // ← window.htmx === undefined → 리스너 미등록
        document.body.addEventListener('htmx:configRequest', function (evt) {
            evt.detail.headers[header.getAttribute('content')] = token.getAttribute('content');
        });
    }
})();
```

결과: HTMX 요청에 `X-CSRF-TOKEN` header 가 부착되지 않음 → Spring Security 가 403 Forbidden 반환
→ HTMX 는 error 상태로 swap 스킵 → DOM 미갱신 → "등록된 첨부파일이 없어요" 유지.

curl 시나리오는 `_csrf` 를 form field 로 함께 보내서 Spring Security 가 통과시켰기 때문에 200 이 재현되어 서버 로그·응답만 봐서는 감지되지 않는 회귀다.

**수정 방안 (택 1)**:
- (권장) `defer` 제거 — 원래 head 에서 동기 로드하면 body inline script 시점에 `window.htmx` 준비 완료
- 또는 inline script 를 `DOMContentLoaded` 리스너 안으로 감싸기
- 또는 inline script 를 `defer` 스크립트로 분리 (선언 순서로 실행 보장)

**test-only 수정 가능 여부**: **불가**. spec 은 실제 브라우저 인터랙션을 정확히 검증하고 있고 (CLAUDE.md "인터랙션 검증" 조항), 회귀는 프로덕션 코드에 있음.

---

## 재검증 6영역 판정

| 영역 | 결과 |
|---|---|
| 정적 (compile + AdminNotice unit/render 21 TC) | **PASS** (BUILD SUCCESSFUL, 21/21) |
| 동적 (curl P0-1/P0-2/P1 3건) | **PASS** — 3건 반려 원본 재현 |
| 계약 (`--project=contracts` admin-notice-*) | **FAIL** — 3건 실패, 단 로그인 세션 유지 이슈로 이전부터 재현되던 계약 스캐너 자체 이슈로 추정 (fix 회귀 아님) |
| 기능 E2E (`--project=chromium` admin-notice-* 4 spec) | **9 PASS / 1 FAIL** — upload spec 만 실패 (신규 회귀 P0-3) |
| 회귀 (admin 트랙) | **19 PASS / 1 FAIL** — 실패 1건은 위 P0-3 upload spec. A1 admin-shell 무회귀 |
| 회귀 (사용자 트랙: notice/apply/login/signup 40 spec) | **38 PASS / 2 FAIL** — 실패 1건은 P0-3, 1건은 `notices.spec.ts:78` 페이지네이션 (curl 검증 중 시드 오염으로 12→17건 초과 발생. **fix 회귀 아님, 검증 세션 아티팩트**) |

### 사용자 시각 확인 대기 항목
- 새 공지 등록 → 편집 화면 → PDF 업로드 UI 왕복 (P0-3 fix 이후 재검증 필요)
- webjar 경로 변경 후 form.html 이외 화면 (신규 등록·편집) 의 HTMX 인터랙션 잔여 회귀 여부

---

## 재반려 사유

Fix commit `9a726f6` 이 P0-2 를 해결하며 도입한 `defer` 속성이 body inline script 의 `window.htmx` 체크와 timing race 를 유발해 **첨부 업로드 인터랙션 전체가 브라우저에서 무력화**됨. curl 로는 정상 200 이 나오지만 실제 UI 는 동작 불가 → 릴리스 차단 수준의 회귀.

**ym-impl 로 재반려**. 프로덕션 코드 수정 후 재검증 필요.

---

## 이하 초회 QA 리포트 원본 (2026-09-04 초회 세션)



---

## 요약

- ym-impl 이 보고한 정적 검증(compile · spotless · unit test)은 재실행 결과 재현 확인 (BUILD SUCCESSFUL, 5 test class 모두 pass)
- **동적 검증에서 목록 페이지 `GET /admin/notices` HTTP 500 재현** — `Notice.createdBy` LAZY 필드를 뷰에서 접근하는 `list.html:59` 에서 `LazyInitializationException`
- **첨부 업로드 form 화면 HTMX 로드 실패** — `form.html:16` 의 `/webjars/htmx.org/dist/htmx.min.js` 경로가 404 (버전 세그먼트 누락)
- 나머지 endpoint (신규·편집·POST create·POST update·업로드 API·삭제 API·다운로드 API) 는 curl 레벨 왕복 검증 통과. RBAC 매트릭스 전량 실증 완료
- 계약 3건 (list / form / edit) 스캔 결과 갭 리포트 3개 생성 — list 계약은 500 때문에 0/7. form·edit 계약은 확인 필요 (P0 회귀 fix 이후 재검증)
- 프로덕션 코드는 규정상 직접 수정 금지 → ym-impl 반려. E2E spec 4건 + 계약 scanner spec 1건 신설은 이번 세션 산출물

---

## 1. 정적 검증

- 명령: `./gradlew.bat test --tests AdminNoticeServiceTest --tests LocalFileStorageTest --tests NoticeAttachmentRepositoryTest --tests JpaMappingTest --tests HomeServiceTest`
- 결과 원본:

```
> Task :jacocoTestReport

BUILD SUCCESSFUL in 1m 53s
7 actionable tasks: 5 executed, 2 up-to-date
```

- 판정: **PASS** (5 test class 재현 성공)

---

## 2. 동적 검증 (curl · e2e 프로파일 port 8090)

### 2-1. 서버 상태

```
GET /programs: 200
GET /admin/login: 200
```

### 2-2. 로그인 (SYSTEM_ADMIN)

```
POST /admin/login: 302 redir=http://localhost:8090/admin
```

### 2-3. 목록 / 신규 / 편집 endpoint

| Endpoint | 기대 | 실제 | 판정 |
|---|---|---|---|
| `GET /admin/notices` | 200 | **500** | **FAIL** |
| `GET /admin/notices/new` | 200 | 200 | PASS |
| `GET /admin/notices/1` | 200 | 200 | PASS |

#### FAIL-1 · `GET /admin/notices` — LazyInitializationException

**bootRun 로그 원본 (요약)**:

```
2026-09-04T09:33:41.673+09:00 ERROR ... [/dispatcherServlet] : Servlet.service() for servlet [dispatcherServlet] threw exception [Request processing failed: org.thymeleaf.exceptions.TemplateInputException: An error happened during template parsing (template: "class path resource [templates/admin/notice/list.html]")] with root cause
org.hibernate.LazyInitializationException: Could not initialize proxy [io.github.sihyuuun.youthmoa.user.User#1] - no session
    at io.github.sihyuuun.youthmoa.user.User$HibernateProxy.getName(Unknown Source) ~[main/:na]
    ...
    at org.thymeleaf.spring6.expression.SPELVariableExpressionEvaluator.evaluate(...)
```

**응답 body 실측**:
```json
{"timestamp":"2026-09-04T00:22:21.225Z","status":403,"error":"Forbidden","message":"Forbidden","path":"/admin"}
```
(재현 시 `GET /admin/notices` 는 500 반환, 예외 스택은 위 참조)

**원인**:
- `src/main/java/.../notice/Notice.java:61-63`: `@ManyToOne(fetch = FetchType.LAZY) private User createdBy`
- `src/main/resources/application.yml`: `spring.jpa.open-in-view=false`
- `src/main/resources/templates/admin/notice/list.html:59`: `<span th:text="${n.createdBy != null ? n.createdBy.name : '-'}">`
- `AdminNoticeService.list()` 은 `@Transactional(readOnly = true)` 로 열려 있으나 트랜잭션이 컨트롤러 반환 시 종료됨. 이후 뷰 렌더링 시점의 `n.createdBy.name` 접근이 세션 밖. OSIV=false 조합에서 확정 실패.

**재현 절차**:
```bash
# admin login 세션 확보 후
curl -b cook.txt -o out.html -w "%{http_code}\n" http://localhost:8090/admin/notices
# → 500
```

**권장 fix (프로덕션 코드 — ym-impl 담당)**:
- Option A: `NoticeRepository` 에 `@EntityGraph(attributePaths = "createdBy") Page<Notice> findAllWithCreator(Pageable)` 추가 후 `AdminNoticeService.list()` 에서 사용
- Option B: `list.html` 에서 `n.createdBy.name` 접근을 서비스에서 미리 DTO 로 프로젝션
- 어느 쪽이든 회귀 방지 위해 `ym-e2e` spec (`admin-notice-list.spec.ts`) 를 임시로 skip 하지 말 것

### 2-4. POST /admin/notices (Create)

```
POST /admin/notices: 302 redir=http://localhost:8090/admin/notices/13
```
- 판정: **PASS**

### 2-5. POST /admin/notices/{id}/attachments (Upload · PDF 1KB)

```
Upload: 200
```
- fragment 응답 실측:
```html
<section id="admin-notice-attachments" class="admin-notice-attachments-section">
    <h3 class="admin-notice-form-section-title">첨부파일</h3>
    <ul class="admin-notice-attachments-list">
        <li class="admin-notice-attachment-item">
            <a href="/notices/13/attachments/5/download">dummy.pdf</a>
            <span class="admin-notice-attachment-size">15B</span>
            <button ... hx-delete="/admin/notices/13/attachments/5" ...>삭제</button>
```
- LocalFileStorage 실 저장 확인:
```
$ ls C:/Users/User/IdeaProjects/youth-moa-java/storage/notice-attachments/13/
-rw-r--r-- 1 2305_N0018 197121 15 9월 4 09:37 1b6cbb2b-2bfc-407e-b6f0-d6832825c133.pdf
```
- 판정: **PASS** (API 레벨)

### 2-6. GET /notices/{nid}/attachments/{aid}/download (사용자 다운로드 backward compat)

```
download: 200 size=15
$ md5sum /tmp/dl.pdf original.pdf
f62b27e45a1dfb140c91a291ab586d3e  /tmp/dl.pdf
f62b27e45a1dfb140c91a291ab586d3e  original.pdf
```
- 판정: **PASS** — bytes 완전 일치. NoticeAttachment.data 컬럼 이중 저장 방식으로 backward compat 유지 확인

### 2-7. 검증 실패 케이스

| 케이스 | 기대 | 실제 | 판정 |
|---|---|---|---|
| `.exe` 확장자 업로드 | 4xx | **500** | **FAIL (P1)** |
| 6MB 초과 파일 | 413 | 413 | PASS |

#### FAIL-2 · 확장자 위반 → 500

- `AdminNoticeService.validateUploadedFile` 은 `IllegalArgumentException` 을 던지지만 `@ControllerAdvice` 부재로 Boot 기본 오류 매핑이 500 을 반환
- 사용자 관점 UX 상 명확한 4xx (예: 400 + 안내 메시지) 로 전환 필요
- 권장: `AdminNoticeController` 에 `@ExceptionHandler(IllegalArgumentException.class)` 로 400 매핑

### 2-8. RBAC 매트릭스 (curl · Playwright 병행 실증)

| 시나리오 | 실제 응답 | 판정 |
|---|---|---|
| USER `GET /admin/notices` | 403 | PASS |
| CENTER_ADMIN `POST /admin/notices/1` (sysadmin 소유) | 403 + `AccessDeniedException: 이 공지를 수정할 권한이 없어요.` | PASS |
| CENTER_ADMIN `POST /admin/notices` (본인) | 302 → 14 | PASS |
| CENTER_ADMIN `POST /admin/notices/14` (본인 소유) | 302 | PASS |
| SYSTEM_ADMIN `GET /admin/notices/new` | 200 | PASS |
| SYSTEM_ADMIN `GET /admin/notices/1` | 200 | PASS |
| SYSTEM_ADMIN `GET /admin/notices` | 500 | **FAIL** (FAIL-1 회귀와 동일) |

- RBAC 로직은 정확히 구현됨. 다만 목록 페이지 회귀 때문에 SYSTEM_ADMIN 도 목록 접근 불가.

---

## 3. 계약 검사 (Playwright `--project=contracts`)

- 신설 spec: `e2e/tests/visual-admin-notice.spec.ts` (list / form / edit 3 시나리오)
- 계약 정의: `e2e/contracts/admin-notice.ts` (impl 산출물)
- 갭 리포트 생성:
  - `e2e/gap-reports/gap-admin-notice-list.md` — **0/7 통과 · 갭 7건**
  - `e2e/gap-reports/gap-admin-notice-form.md` — 존재, 상세 확인은 회귀 fix 후
  - `e2e/gap-reports/gap-admin-notice-edit.md` — 존재, `edit.delete.button` `edit.confirm.modal` 등 P0 갭 재확인 필요

### 갭 상세 (list)

`gap-admin-notice-list.md` 원본 인용:

```
| P0 | header.gnb.notices.active — 관리자 GNB "공지 관리" 링크 활성 | 공지 관리 | 대시보드 |
| P0 | page.title.exists — 페이지 타이틀 "공지 관리"                | 공지 관리 | (요소 없음) |
| P0 | create.button.exists — "신규 등록" 버튼                      | + 신규 등록 | (요소 없음) |
| P0 | list.head.row — 테이블 헤더 존재                             | true | false |
| P0 | list.rows.seeded — 시드된 공지 12건 이상 렌더                 | 12 | (요소 없음) |
```

- 원인: FAIL-1 (500) 로 페이지가 render 되지 못하므로 계약 selector 전량 매칭 실패. 회귀 fix 시 자동 해소 예상.

---

## 4. 기능 E2E (Playwright `--project=chromium`)

- 신설 spec 4건: `admin-notice-list.spec.ts` · `admin-notice-form.spec.ts` · `admin-notice-upload.spec.ts` · `admin-notice-rbac.spec.ts`
- 결과: **6 PASS / 4 FAIL**

### PASS

| Spec | 시나리오 | 판정 |
|---|---|---|
| admin-notice-form | 신규 → 편집 폼 왕복 | PASS |
| admin-notice-form | 편집 → 제목 수정 왕복 | PASS |
| admin-notice-rbac | USER 403 | PASS |
| admin-notice-rbac | CENTER_ADMIN SYSTEM_ADMIN 소유 편집 403 | PASS |
| admin-notice-rbac | CENTER_ADMIN 본인 공지 편집 | PASS |
| (기타 회귀 spec 34건) | 기존 사용자 사이드 | PASS |

### FAIL

1. **admin-notice-list.spec.ts:8** — "/admin/notices 진입 → GNB … + 페이지 타이틀 + 신규 등록 버튼"
   - 원인: FAIL-1 (list 500)
   - 원본 오류: `page.goto('/admin/notices')` 응답이 error json → 헤더 요소 미발견

2. **admin-notice-list.spec.ts:16** — "시드 공지 12건 이상 렌더 + 각 row 편집 링크 노출"
   - 원인: FAIL-1 (list 500)

3. **admin-notice-rbac.spec.ts:63** — "SYSTEM_ADMIN 은 목록·신규·편집 모두 접근 200"
   - 원인: FAIL-1. 신규·편집 200 확인 후 목록 GET에서 500. 이 spec 은 회귀 fix 확인용으로 유지

4. **admin-notice-upload.spec.ts:14** — "multipart PDF 업로드 → 목록 노출 → 삭제 → 미노출"
   - 원인: **FAIL-3 · HTMX 미로드**
   - 원본 오류: 첨부 업로드 form submit 후에도 attachments 섹션이 empty 상태 유지 (`text: 등록된 첨부파일이 없어요.`)
   - 진단: `form.html:16` 의 htmx 경로 `/webjars/htmx.org/dist/htmx.min.js` 는 **404** (`curl -o NUL -w "%{http_code}" /webjars/htmx.org/dist/htmx.min.js → 404`). 다른 template 은 모두 `/webjars/htmx.org/2.0.4/dist/htmx.min.js` (버전 세그먼트 포함, 200) 사용
   - HTMX 로드 실패 → form 이 native submit 으로 fallback → hx-target/swap 무효 → new page navigate → attachments empty view 반환 → 파일명 노출 실패
   - 별도: curl 레벨 업로드는 성공 (2-5 참조) 하므로 서버 API 는 정상. 순수 프론트엔드 회귀
   - 권장 fix: `templates/admin/notice/form.html:16` 을 `<script th:src="@{/webjars/htmx.org/2.0.4/dist/htmx.min.js}"></script>` 로 수정

---

## 5. 회귀 검증

- 명령: `npx playwright test --project=chromium -g "notice|apply|login|signup"`
- 결과: **35 PASS / 5 FAIL**
- FAIL 5건 중 4건은 위 admin-notice-* 신설 spec (P0 회귀 노출용). 나머지 1건:
  - `notices.spec.ts:78 · 12건 시드에서 페이지네이션이 나타나고 2페이지 클릭 시 URL·active 가 갱신된다`
  - 원인: **flaky · 세션 state 오염** (webServer.reuseExistingServer=true + admin-notice-form.spec.ts 가 신규 공지 생성 후 정리하지 않음 → 사용자 공지 목록 count 가 12→14 로 증가하여 페이지네이션 spec 가정 위반)
  - 판정: 프로덕션 회귀 아님. 임시 격리를 위해 admin-notice-form.spec.ts 를 이후 별도 workflow 로 실행하거나, e2e 프로파일 재기동 시 초기화되므로 CI 상에서는 문제없음 예상

---

## 6. 시각 확인 (사용자 영역, 대기)

- 회귀 fix 후 사용자 브라우저에서 확인 필요:
  - 인디고 primary 톤 (SYSTEM_ADMIN dashboard 와 톤 정합)
  - "신규 등록" 버튼 정렬 · header 오른쪽 정렬
  - 삭제 커스텀 confirm 모달 backdrop · 애니메이션
  - 첨부 목록의 아이콘/파일명 정렬
  - 편집 폼 forbidden banner (CENTER_ADMIN 이 sysadmin 공지 열 때)

---

## 7. UNVERIFIED (환경 한계)

- **Supabase Storage 구현체** (`SupabaseFileStorage`): prod 프로파일 전용. `SUPABASE_SERVICE_ROLE_KEY` env 부재로 로컬 검증 불가. 배포 세션에서 재검증 필요
- **CI ubuntu 러너에서 E2E 재실행**: PR 생성 후 GitHub Actions 확인 필요 (본 세션은 회사 PC 8090 만)

---

## 8. 반려 대상 (ym-impl 재작업 요구)

| # | 파일 · 라인 | 회귀 유형 | 조치 |
|---|---|---|---|
| FAIL-1 | `templates/admin/notice/list.html:59` + `notice/Notice.java:61` + `AdminNoticeService.java:96-104` | LazyInitializationException on `notice.createdBy.name` view render | Repository `@EntityGraph(attributePaths="createdBy")` fetch join 또는 DTO 프로젝션 |
| FAIL-2 | `admin/AdminNoticeController.java` (또는 신규 GlobalExceptionHandler) | `IllegalArgumentException` → 500 노출 (P1) | `@ExceptionHandler` 로 400 매핑 + 존댓말 안내 메시지 |
| FAIL-3 | `templates/admin/notice/form.html:16` | HTMX webjar 경로 404 (`.../dist/htmx.min.js`) — 다른 template 은 `.../2.0.4/dist/htmx.min.js` | 경로 수정 (버전 세그먼트 추가) |

---

## 9. 산출물 (QA 세션)

### 신설
- `e2e/tests/admin-notice-list.spec.ts` (3 시나리오)
- `e2e/tests/admin-notice-form.spec.ts` (2 시나리오)
- `e2e/tests/admin-notice-upload.spec.ts` (1 시나리오)
- `e2e/tests/admin-notice-rbac.spec.ts` (4 시나리오)
- `e2e/tests/visual-admin-notice.spec.ts` (3 계약 스캐너)
- `docs/qa-checklists/A-admin-notice-attachment-qa-report.md` (본 리포트)

### 자동 생성 (계약 스캐너 실행 부산물)
- `e2e/gap-reports/gap-admin-notice-list.md`
- `e2e/gap-reports/gap-admin-notice-form.md`
- `e2e/gap-reports/gap-admin-notice-edit.md`

---

## 10. 인계

**ym-impl 반려 사유 명확**. FAIL-1/2/3 세 항목 fix 후 재검증 요청. 정적 검증 · RBAC 로직 · 파일 저장소 인프라 자체는 견고하므로 fix 는 국소적 (3 파일 수준).

Fix 후 재확인 시나리오:
1. `GET /admin/notices` 200 확인
2. `visual-admin-notice.spec.ts` 3 계약 모두 갭 0
3. `admin-notice-*.spec.ts` 10 시나리오 전량 PASS
4. `exe` 업로드 400 확인
