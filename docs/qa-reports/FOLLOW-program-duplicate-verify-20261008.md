# 적대적 검증 리포트: FOLLOW-program-duplicate

- 일시: 2026-10-08
- 브랜치: `feature/FOLLOW-program-duplicate` (uncommitted)
- 선행: ym-spec `ad7f9cca33165d76b` · ym-impl `a7a3be0aaa02475ff` · ym-qa `a7d52593ef44a937e` (통과 보고)
- 검증자: ym-verify
- 범위: Java 소스 수정 2건 · Thymeleaf 템플릿 1건 · 테스트 자산 신설 2건 · 기존 테스트 수정 1건 · E2E 계약 수정 1건

---

## 판정 요약

- **PASS 9건 / FAIL 1건 / UNVERIFIED 4건**
- **머지 가능 여부: 조건부 가능** — FAIL 1건(spec §8 구현 매핑 TBD)은 **doc-only** 결함으로 소스 변경 없이 라인 번호 채우기로 해소 가능. UNVERIFIED 4건은 모두 "구현 자체는 올바르나 negative 테스트 자산 누락"으로 운영 리스크 Low. 재작업 수준 결함은 없음.

---

## 항목별 판정

| # | 공격 벡터 | 실행 방법 | 판정 | 근거 |
|---|---|---|---|---|
| 1 | Q1 복제 범위 — soft-deleted(isActive=false) Course/ApplyQuestion 복제 제외가 코드에 실제 반영되었나 | `AdminProgramService.java` L463-479 소스 확인 + Repository 메서드 시그니처 확인 | PASS | Service 가 `findByProgramIdAndIsActiveTrueOrderBySortOrderAscIdAsc` / `findByProgramIdAndIsActiveTrueOrderBySortOrderAsc` 호출 (L464·L478). Spring Data JPA 파생 쿼리로 `isActive=true` WHERE 절 자동 추가. 테스트 `duplicate_courseActiveOnly` / `duplicate_questionActiveOnly` 가 시드에 비활성 1건 포함 후 복제 결과 2건·`allMatch(Course::isActive)` 로 입증 |
| 2 | Q2 title 접미사 길이 초과 trim | L420-426 소스 확인 | PASS (로직) · UNVERIFIED (경계값 TC) | 구현 올바름 — `baseTitle + " (복제본)"` 가 255 초과 시 `baseTitle.substring(0, 255 - suffix.length)` + suffix. 그러나 길이 경계(254/255/256 등) 단위 TC **부재**. 운영에서 title 길이 상한에 도달하는 사례가 없다면 Low. spec §7 에도 trim TC 요구 없음 |
| 3 | Q3·Q8 명시 초기화 | `Program.java` 빌더 L195-240 + Service L450-456 | PASS | 빌더 null-safe default (`isActive != null ? isActive : true`, `autoApproveWhenFull != null && autoApproveWhenFull`) 가 존재하지만 Service 가 **명시적으로 `.isActive(false)` / `.autoApproveWhenFull(false)` 전달**. default 값에 의존하지 않음. `duplicate_isActiveFalse_수동활성화전제` / `duplicate_autoApproveFalse` TC 통과 |
| 4 | Q5 `/edit` redirect 경로 존재 | `AdminProgramController.java` L212 `@GetMapping("/{id}/edit")` 확인 | PASS | 선행 FOLLOW-admin-program-detail-readonly 에서 분리된 `/edit` 서브경로 유효. redirect 깨짐 없음 |
| 5 | Q7 CENTER_ADMIN 403 차단 | `AdminProgramController.java` L336 `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` + `SecurityConfig.java` L37 `@EnableMethodSecurity` | PASS (정적) · UNVERIFIED (실행) | `@EnableMethodSecurity` 활성화돼 어노테이션이 작동한다는 전제는 성립. 그러나 **CENTER_ADMIN 세션으로 POST /duplicate 호출 시 실제 403 반환** 을 입증하는 단위 TC·E2E 가 모두 **없음**. ym-qa 리포트도 "코드 레벨 단위 테스트 없다" 명시. 운영에서 다른 SYSTEM_ADMIN-only 엔드포인트(Delete·Create 등)와 동일 패턴이므로 회귀 가능성 Low |
| 6 | Q9 createdBy 가 Q7 과 모순 가능성 | `AdminProgramService.duplicate()` L411 `adminScope.currentUser()` + Q7 결정 확인 | PASS | Q7 이 CENTER_ADMIN 을 Controller 레벨에서 차단하므로 서비스에 도달하는 principal 은 반드시 SYSTEM_ADMIN. 따라서 `createdBy=복제 수행자` 는 항상 SYSTEM_ADMIN 이 되며 Q7·Q9 간 모순 없음 |
| 7 | 트랜잭션 롤백 — Course 저장 중 예외 발생 시 Program 롤백 | `@Transactional` 선언 + 롤백 테스트 유무 | UNVERIFIED | 메서드에 `@Transactional` 선언 (L408). Spring 기본 설정상 RuntimeException 발생 시 롤백. 단, Course validate 실패 유도 시나리오 등 **부정 TC 부재**. ym-qa 도 "코드 리뷰로만 확인" 명시. 실용 리스크 Low (JPA save 는 RuntimeException 계열만 발생) |
| 8 | Attachment 미복제 — 원본에 있을 때 실제 미복제 입증 | 소스 확인 + 테스트 자산 확인 | PASS (로직) · UNVERIFIED (TC) | Service `duplicate()` 는 Attachment 를 참조하는 코드가 아예 없어 로직상 미복제 보장. 단 **spec §7 에 요구된 `duplicate_attachmentNotCopied` 단위 TC 가 구현되지 않음** (`AdminProgramDuplicateServiceTest.java` 전체 11 TC 중 미포함). ym-qa 가 "ATTACH_ROWS=0" 로 보고한 동적 확인은 **원본에 Attachment 없는 상태의 복제 결과** 라 입증력 약함 |
| 9 | 중복 복제 시 title "(복제본) (복제본)" 중첩 | 소스 확인 (가드 없음) | PASS | 가드 없음 — 2회 복제 시 "(복제본) (복제본)" 로 중첩됨. spec 에 중복 가드 요구 없음, 자연스러운 동작으로 수용 가능. 운영에서는 사용자가 복제 후 즉시 `/edit` 로 이동해 제목을 수정하는 전제 |
| 10 | spec §8 "구현 매핑" TBD 라인 번호 | `docs/specs/FOLLOW-program-duplicate.md` L131-141 | **FAIL** | spec §8 전체 6행 모두 `TBD` 상태 그대로. ym-verify agent 정의 (`spec 표 행 단위 재대조` 조항) 및 ym-qa 리포트가 "verify 에서 라인 번호 채우라고 명시" 로 지적한 미결 항목. **doc-only 수정**으로 즉시 해소 가능 (아래 §재작업 지시 참조) |
| 11 | CSRF 토큰 자동 주입 | 템플릿 L59-66 + SecurityConfig L247 | PASS | CSRF 활성 상태 (`csrf.ignoringRequestMatchers("/__test__/**")` 로 테스트 경로만 예외). `th:action` 사용 form 은 Thymeleaf-Spring Security 통합이 hidden `_csrf` input 을 자동 주입. 동일 패턴의 기존 `/delete` form (L208) 이 운영 중이므로 회귀 없음 |
| 12 | Program 빌더 isActive 함정 | `Program.java` L220 `isActive != null ? isActive : true` default 가 Service 의 명시 false 를 덮어쓰는지 | PASS | `.isActive(false)` 전달 시 `Boolean.FALSE != null` 이므로 삼항 결과 false. 언박싱 후 primitive boolean false 로 세팅. 함정 없음 |
| 13 | Program 빌더 viewCount 함정 | 동일 — L237 default 0 | PASS | `.viewCount(0)` 명시 전달. default 와 동일한 값이지만 명시해도 결과 동일 |

---

## FAIL 상세 (ym-impl 반려 사항)

### F1. spec §8 "구현 매핑" TBD 미채움

**재현 방법**
```powershell
Select-String -Path "C:\Users\User\IdeaProjects\youth-moa-java\docs\specs\FOLLOW-program-duplicate.md" -Pattern "TBD"
```
L135~L140 6행 모두 `TBD`.

**기대 vs 실제**
- 기대: ym-verify agent 정의상 "spec 구현 매핑 섹션 매핑(라인 번호·상태)이 실제 코드와 불일치하면 FAIL". 2026-07-08 F0h-c4 인포윈도우 누락 머지 사고 재발 방지 조항
- 실제: TBD 라인 그대로 커밋 직전 상태

**재작업 지시 (doc-only)**
spec §8 를 아래 실측 라인 번호로 교체:

| spec 섹션 | 코드 파일 | 라인 |
|---|---|---|
| §4 Program 복제 | `src/main/java/io/github/sihyuuun/youthmoa/admin/AdminProgramService.java` | L428-461 |
| §4 Course 복제 | `src/main/java/io/github/sihyuuun/youthmoa/admin/AdminProgramService.java` | L463-475 |
| §4 ApplyQuestion 복제 | `src/main/java/io/github/sihyuuun/youthmoa/admin/AdminProgramService.java` | L477-491 |
| §2 ⋯ 더보기 복제 메뉴 | `src/main/resources/templates/admin/program/detail.html` | L58-66 |
| §2 Controller endpoint | `src/main/java/io/github/sihyuuun/youthmoa/admin/AdminProgramController.java` | L341-351 |
| §6 Flash message | `src/main/java/io/github/sihyuuun/youthmoa/admin/AdminProgramController.java` | L349 |

이 수정 반영 후 커밋 가능.

---

## UNVERIFIED 상세

### U1. Q7 CENTER_ADMIN 403 실행 입증 부재
- **판정 불가 사유**: `@PreAuthorize` 어노테이션이 실제로 403 응답을 생성하는지 검증한 단위/E2E TC 없음. 정적 분석(어노테이션 + `@EnableMethodSecurity` 활성)만으로는 Spring 버전·설정 변경 시 회귀 가능성 배제 못 함
- **입증 필요 조건**: CENTER_ADMIN 세션으로 `POST /admin/programs/1/duplicate` 호출 → HTTP 403 확인하는 `AdminProgramDuplicateControllerTest` 또는 Playwright E2E 1건 추가
- **운영 리스크**: Low — 동일 패턴의 Create/Delete 엔드포인트가 운영 중

### U2. 트랜잭션 롤백 negative TC 부재
- **판정 불가 사유**: Course 저장 중 예외 유도 시나리오 (예: NOT NULL 위반) 테스트 자산 없음. `@Transactional` 어노테이션의 실제 롤백 거동 입증 못 함
- **입증 필요 조건**: `@MockBean CourseRepository` 로 save 시 `DataIntegrityViolationException` 발생 유도 → 해당 Program 도 롤백됐는지 `programRepository.findById()` 로 확인하는 TC
- **운영 리스크**: Low — JPA save 는 RuntimeException 계열만 발생

### U3. Attachment 미복제 입증력 약함
- **판정 불가 사유**: ym-qa 가 보고한 동적 확인 ATTACH_ROWS=0 은 원본에 Attachment 없는 상태에서의 복제 결과라 "미복제" 입증이 아닌 "미존재" 확인에 불과. 원본에 Attachment 가 있는 상태의 복제 TC 가 spec §7 요구에도 불구 미구현
- **입증 필요 조건**: `AdminProgramDuplicateServiceTest.duplicate_attachmentNotCopied` 신설 — ProgramAttachment 1건 이상 시드 포함 Program 을 복제 후 `programAttachmentRepository.findByProgramId(copy.getId())` 가 empty 임을 확인
- **운영 리스크**: Minimal — 소스상 Attachment 를 참조하는 코드 자체가 없음

### U4. title 255 trim 경계값 TC 부재
- **판정 불가 사유**: 로직은 올바르나 경계값(길이 254/255/256) 단위 TC 없음. substring off-by-one 리스크 미검증
- **입증 필요 조건**: `baseTitle.length() == 255` 케이스에서 결과 title 이 정확히 255 자인지 확인하는 TC
- **운영 리스크**: Low — Program.title 컬럼 상한에 근접하는 운영 데이터가 흔치 않음

---

## 머지 가이드

1. **즉시 조치 필수**: F1 spec §8 라인 번호 채우기 (doc-only, 5분)
2. **선택 조치 (권장)**: U1·U3 negative TC 2건 추가 (약 60 LOC) — 운영 신뢰도 상승
3. **보류 가능**: U2·U4 — 운영 리스크 Minimal

F1 조치 후 커밋 가능으로 판단.
