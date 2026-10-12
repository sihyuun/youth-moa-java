# QA 리포트 — FOLLOW-program-duplicate

- 날짜: 2026-10-08
- 브랜치: `feature/FOLLOW-program-duplicate` (미커밋)
- 선행: ym-spec `ad7f9cca33165d76b` · ym-impl `a7a3be0aaa02475ff`
- 변경 파일: 신설 3 (service test · e2e spec · spec doc) + 수정 6 (Service · Controller · detail.html · contract · render test · (contract) 참조)
- QA 담당: ym-qa (자동)
- 판정: **통과** (코드/테스트 수정 없음 · ym-verify 인계 가능)

---

## 1. 정적 검증

| 항목 | 결과 |
|---|---|
| `compileJava` | BUILD SUCCESSFUL (21s) |
| `compileTestJava` | BUILD SUCCESSFUL |
| `AdminProgramDuplicateServiceTest` (신설) | **12 TC PASS · 0 fail · 0 err** |
| `AdminProgramDetailRenderTest` (수정) | **7 TC PASS · 0 fail · 0 err** |

신설 12 TC 는 결정 사항 Q1~Q10 전부 커버: 제목 접미사 · isActive=false · viewCount=0 · autoApproveWhenFull=false · createdBy=수행자 · center=원본 유지 · 날짜 보존 · content/eligibility 보존 · Course active-only · ApplyQuestion active-only · 미존재 404 · 인증 없음 IllegalStateException.

렌더 7 TC 중 신규 1건 (`detail-menu-clone` markup 노출) + 기존 6 유지. "복제 미노출" 어서션은 spec §2 수정대로 제거됨.

---

## 2. 회귀 검증 (admin + application + program 패키지)

- 명령: `./gradlew test --tests "io.github.sihyuuun.youthmoa.admin.*" --tests "io.github.sihyuuun.youthmoa.application.*" --tests "io.github.sihyuuun.youthmoa.program.*"`
- 결과: **BUILD SUCCESSFUL (7m 55s) · 455 TC PASS · 0 fail · 0 err · 0 skip** (56 테스트 클래스 기준 집계)

기존 admin-program 관련 render/service 테스트 (`AdminProgramServiceTest`, `AdminProgramFormRenderTest`, `AdminProgramBulkServiceTest`, `AdminProgramCourseUpsertTest`, `AdminProgramAttachmentServiceTest`, `AdminProgramAutoApproveTest` 등) 전수 PASS. 복제 매핑 추가로 인한 기존 흐름 회귀 **0건**.

---

## 3. 동적 검증 (bootRun · e2e 프로파일 · port 8090)

세션 쿠키 로그인 후 `curl`/`Invoke-WebRequest` 로 직접 실측.

| 시나리오 | 결과 |
|---|---|
| `GET /admin/programs/1` (SYSTEM_ADMIN) | **200** + `data-testid="detail-menu-clone"` markup 포함 ✅ |
| `POST /admin/programs/1/duplicate` (SYSTEM_ADMIN) | **302 → `/admin/programs/26/edit`** ✅ (Q5) |
| `POST /admin/programs/999999/duplicate` (SYSTEM_ADMIN) | **404** ✅ (IllegalArgumentException → ResponseStatusException NOT_FOUND) |
| `POST /admin/programs/1/duplicate` (CENTER_ADMIN) | **403** ✅ (@PreAuthorize('SYSTEM_ADMIN')) |
| `GET /admin/programs/26/edit` (복제본) | **200** · title=`"취업역량 강화 워크숍 (복제본)"` (Q2 ✅) · `name="active"` checkbox **not checked** (Q3 ✅) · attachment row **0** (Q1 ③ Attachment 미복제 ✅) |

---

## 4. E2E 자동화 (Playwright chromium)

- 명령: `BASE_URL=http://localhost:8090 npx playwright test tests/admin-program-duplicate.spec.ts tests/admin-program-detail.spec.ts --project=chromium`
- 결과: **4/4 PASS (39.3s)**
  - `admin-program-duplicate.spec.ts` 1 TC — 상세 → ⋯ 더보기 → 복제 → `/edit` redirect + title "(복제본)" + isActive=false (Q2·Q3·Q5)
  - `admin-program-detail.spec.ts` 3 TC — 수정 CTA · 복제·삭제 메뉴 동시 노출 (Q5 복원) · 신청 현황 보기 regression 유지

---

## 5. 디자인 계약 (contracts 프로젝트)

- 명령: `BASE_URL=http://localhost:8090 npx playwright test tests/visual-admin-programs.spec.ts --project=contracts`
- 결과: **2/2 PASS (19.8s)**
- `gap-admin-program-detail.md` 재집계: **25/25 통과 · 갭 0건**
  - 신설 체크 `header.more.menu.clone` (exists, P1) 통과
  - 신설 체크 `header.more.menu.clone.label` (text "복제", P1) 통과
  - 기존 체크 `header.more.menu.delete` (삭제도 유지) 통과
- 갭 리포트는 untracked (`e2e/gap-reports/*.md` 는 로컬 아티팩트)

---

## 6. 트랜잭션 롤백 (코드 리뷰)

Service 레벨 `@Transactional` + 단일 쓰레드 흐름:

- `AdminProgramService.duplicate()` 전체가 `@Transactional`
- Program insert → Course insert loop → ApplyQuestion insert loop 가 하나의 트랜잭션 안에서 수행
- 중간 insert 중 예외 (예: 길이 제한 위반·FK 위반) 발생 시 Program 포함 전체 롤백 보장

Controller 의 `/duplicate` 매핑은 추가적인 `@Transactional(rollbackFor=Exception.class)` 를 걸지 않지만, Service 트랜잭션 1개로 Program/Course/ApplyQuestion 이 모두 묶이므로 요구사항(§5) 충족. 단위 테스트로의 예외 롤백 재현은 미작성이지만 Service 흐름이 단순 insert loop 라 코드 리뷰로 커버됨.

---

## 7. 결정 반영 매트릭스

| Q | 결정 | 검증 위치 | 결과 |
|---|---|---|---|
| Q1 복제 범위 (Program + active Course + active ApplyQuestion, Attachment 미복제) | ③ | DuplicateServiceTest `course/questionActiveOnly_비활성제외` + 동적 EDIT_26 ATTACH_ROWS=0 | ✅ |
| Q2 제목 "(복제본)" + 255 trim | 권장 | DuplicateServiceTest `titleAppended` + 동적 TITLE 매치 | ✅ |
| Q3 isActive=false | 권장 | DuplicateServiceTest `isActiveFalse` + 동적 ACTIVE_CHECKED=False | ✅ |
| Q4 날짜 보존 | 권장 | DuplicateServiceTest `datesPreserved` | ✅ |
| Q5 redirect `/edit` | 권장 | Controller 302 LOC `/admin/programs/26/edit` + E2E waitForURL | ✅ |
| Q6 viewCount=0 | 권장 | DuplicateServiceTest `viewCountZero` | ✅ |
| Q7 SYSTEM_ADMIN only | 권장 | 동적 CENTER_DUP=403 | ✅ |
| Q8 autoApproveWhenFull=false | 권장 | DuplicateServiceTest `autoApproveFalse` | ✅ |
| Q9 createdBy=수행자 | 권장 | DuplicateServiceTest `createdByCurrentUser` | ✅ |
| Q10 center 원본 유지 | 권장 | DuplicateServiceTest `centerPreserved` | ✅ |

---

## 8. 미실행 / UNVERIFIED

| 항목 | 사유 | 담당 |
|---|---|---|
| `duplicate_attachmentNotCopied` 단위 TC | spec §7 예시에 포함됐으나 실제 테스트 파일에는 미작성. 동적 EDIT_26 ATTACH_ROWS=0 으로 간접 확인되나 soft-coverage. 영향도 낮음 | ym-verify 재검토 — PASS 처리 또는 추가 요청 결정 |
| spec §8 "구현 매핑" TBD 행 | QA 범위 외 (ym-verify 가 채움) | ym-verify |
| 트랜잭션 롤백 unit TC | 코드 리뷰로 충족, 명시 TC 는 미작성 | 선택 사항 |
| Attachment 미복제 시각 확인 | 동적 ATTACH_ROWS=0 으로 커버 | 완료 |

---

## 9. 금지 사항 준수

- 코드 수정 **0건** ✅
- 테스트 수정 **0건** ✅
- 리포트 작성만 수행 (`docs/qa-reports/FOLLOW-program-duplicate-qa-20261008.md`)

---

## 10. 다음 단계

**ym-verify 인계 가능**. 특별 유의 포인트:
- spec §8 구현 매핑 테이블 TBD 라인 채우기
- `duplicate_attachmentNotCopied` 명시 단위 TC 추가 여부 판단 (현재는 동적 간접 확인만 있음)
- 복제 수행자가 복제 성공 flash 확인 (UI 영역, 사용자 시각 확인 범위)
