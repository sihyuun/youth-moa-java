# FOLLOW-program-duplicate — 관리자 프로그램 복제 (Clone)

- 상태: `spec_confirmed` → `impl_in_progress` (2026-10-08)
- 산출: ym-spec (ad7f9cca33165d76b)
- 결정: Q1~Q10 모두 권장안 적용
- 선행: FOLLOW-admin-program-detail-readonly (2026-10-07, Q5 "복제 이월") 의 이월 항목을 해소

---

## 1. 배경

FOLLOW-admin-program-detail-readonly 에서 상세 헤더 ⋯ 더보기 메뉴에 "삭제" 1개만 두고 "복제" 를 이월 처리했다 (Q5 A). admin prototype.html L1470 에 "복제" 메뉴 아이템이 명시되어 있고, 시즌성 프로그램의 재개설 UX (`"OO 교실 2026년 봄"` → `"OO 교실 2026년 가을"`) 를 위해 자주 쓰이는 액션이므로 본 티켓에서 추가한다.

---

## 2. 변경 범위

### 신설
- `admin/program/detail.html` — ⋯ 더보기 메뉴에 "복제" 아이템 추가 (삭제 위)
- `AdminProgramController.duplicate(Long id)` — `POST /admin/programs/{id}/duplicate` 신규 매핑 (SYSTEM_ADMIN only)
- `AdminProgramService.duplicate(Long id)` — 트랜잭션. Program + Course(isActive=true) + ApplyQuestion(isActive=true) 복제
- `src/test/.../AdminProgramDuplicateServiceTest.java` — 복제 범위·속성 초기화 검증
- `src/test/.../AdminProgramDetailRenderTest.java` 에 복제 메뉴 노출 TC 1건 추가
- `e2e/tests/admin-program-duplicate.spec.ts` — 상세 → 복제 → `/edit` redirect → 제목·isActive 확인 흐름

### 수정
- `e2e/contracts/admin-program-detail.ts` — `header.more.menu.clone.missing` → `header.more.menu.clone` (exists + 라벨 "복제")
- `AdminProgramDetailRenderTest`: "복제 미노출" assertion 제거

### DB
- 마이그레이션 불필요 (기존 테이블·컬럼 재사용)

---

## 3. 결정 요약 (Q1~Q10)

| Q | 결정 | 비고 |
|---|---|---|
| Q1 복제 범위 | **③** Program + Course(isActive=true) + ApplyQuestion(isActive=true) | ProgramAttachment 는 미복제 (물리 파일 복사 비용 + orphan 위험) |
| Q2 제목 | `"원본제목 (복제본)"` | 길이가 255 를 초과하면 trim 후 접미사 부착 |
| Q3 복제본 isActive | **false** | 수동 활성화 전제. SUSPENDED 상태로 목록 노출 |
| Q4 날짜 | **원본 그대로** 복제 | 담당자가 편집 폼에서 수정 전제 |
| Q5 redirect | **복제본 편집폼** `/admin/programs/{newId}/edit` | 바로 편집할 수 있도록 |
| Q6 applied/viewCount | **0 초기화** | applied 는 Application row 자체 미복제라 자연 0, viewCount 는 명시적 0 |
| Q7 권한 | **SYSTEM_ADMIN only** | CENTER_ADMIN 제외 (등록/편집/삭제 패턴과 동일) |
| Q8 autoApproveWhenFull | **false 초기화** | FOLLOW-waitlist-auto-approve 플래그는 명시적 OFF |
| Q9 createdBy | **복제 수행자** (현재 세션 User) | 원본 createdBy 무시. A7 알림 fan-out 기준 |
| Q10 center | **원본 유지** | CENTER_ADMIN 미지원이라 cross-center 복제 고려 불필요 |

---

## 4. 복제 매핑 상세

### Program (신규 insert)
| 필드 | 값 |
|---|---|
| id | 신규 생성 |
| title | `원본.title + " (복제본)"` (255 넘으면 원본 trim 후) |
| center | 원본 center (Q10) |
| category / region / imageUrl / content / description | 원본 그대로 |
| eligibility (@Embedded) | 원본 값 복사 — age/region/etc 각 필드 |
| startDate / endDate / applyStartDate / applyEndDate | 원본 그대로 (Q4) |
| applyUrl / venue / contact | 원본 그대로 |
| capacity | 원본 그대로 |
| approvalMode | 원본 그대로 |
| termsService / termsPrivacy / termsMarketing | 원본 그대로 |
| hasCourses | 원본 그대로 |
| isActive | **false** (Q3) |
| viewCount | **0** (Q6) |
| autoApproveWhenFull | **false** (Q8) |
| createdBy | **복제 수행자** (Q9) |

### Course (isActive=true 만 복제)
각 행에 대해:
- 신규 Course insert — `program` = 신규 Program
- name / schedule / capacity / sortOrder 원본 복사
- isActive = true

### ApplyQuestion (isActive=true 만 복제)
각 행에 대해:
- 신규 ApplyQuestion insert — `program` = 신규 Program
- fieldType / label / isRequired / sortOrder / options / maxLength 원본 복사
- isActive = true

### ProgramAttachment
- **복제하지 않음** (Q1 ③). 담당자가 편집 폼에서 재업로드

### Application / Bookmark
- 복제하지 않음 (당연)

---

## 5. 트랜잭션·에러 처리

- `@Transactional`: Program → Course → ApplyQuestion 전체 롤백
- 미존재 id → `IllegalArgumentException` → Controller 에서 404 매핑 (admin-notice 패턴 승계)
- 세션 principal 없음 → `IllegalStateException` ("인증된 사용자만 프로그램을 복제할 수 있어요")
- CENTER_ADMIN 요청 → `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` 로 403 (Spring Security)

---

## 6. 메시지

- Flash message: `"프로그램을 복제했어요. 내용을 수정한 뒤 저장해주세요."`
- 복제 버튼 라벨 (⋯ 더보기 메뉴): `"복제"` (prototype L1470)
- 삭제 메뉴 아이템과 동일한 스타일, 단 `--danger` modifier 는 적용하지 않음

---

## 7. 테스트

### 단위 (AdminProgramDuplicateServiceTest)
- `duplicate_titleAppended` — "원본 (복제본)" 포함
- `duplicate_isActiveFalse` — 복제본 isActive=false
- `duplicate_autoApproveFalse` — 복제본 autoApproveWhenFull=false
- `duplicate_viewCountZero` — viewCount=0
- `duplicate_courseActiveOnly` — isActive=true Course 만 복제, inactive 제외
- `duplicate_questionActiveOnly` — isActive=true ApplyQuestion 만 복제
- `duplicate_attachmentNotCopied` — ProgramAttachment 복제 안 됨
- `duplicate_createdByCurrentUser` — createdBy = 복제 수행자
- `duplicate_notFound_throws` — 미존재 id 예외

### 렌더 (AdminProgramDetailRenderTest 신규 TC 1건)
- `GET_admin_program_detail_복제_메뉴_노출` — `data-testid="detail-menu-clone"` 포함

### E2E (admin-program-duplicate.spec.ts)
- 상세 → ⋯ 더보기 → 복제 클릭 → 302 → `/edit` → 제목 "(복제본)" 포함 확인

---

## 8. 구현 매핑 (완료 후 채움)

| spec 섹션 | 코드 파일 | 라인 |
|---|---|---|
| §4 Program 복제 | `AdminProgramService.duplicate()` | L428-461 |
| §4 Course 복제 | `AdminProgramService.duplicate()` | L463-475 |
| §4 ApplyQuestion 복제 | `AdminProgramService.duplicate()` | L477-491 |
| §2 ⋯ 더보기 복제 메뉴 | `admin/program/detail.html` | L58-66 |
| §2 Controller endpoint | `AdminProgramController.duplicate()` | L341-351 |
| §6 Flash message | `AdminProgramController.duplicate()` | L349 |
