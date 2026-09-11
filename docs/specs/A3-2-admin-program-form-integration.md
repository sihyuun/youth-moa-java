# 작업 명세: A3-2 — admin-program-form-integration (파생 큐 인라인 통합 · Course · ProgramAttachment · 이미지 파일 업로드)

| 메타 | 값 |
|---|---|
| 상태 | **`spec_confirmed`** (2026-09-11 사용자 결정: **Qn-A/B/C/D + Δ 13종 모두 권장안 A**. §11 그대로 이행) |
| 브랜치 | `feature/A3-2-admin-program-form-integration` |
| 스코프 | F4/F0c 인라인 통합 · **Course 엔티티 신설 (V13)** · **ProgramAttachment 엔티티 신설 (V14)** · **이미지 파일 업로드 (P0-3 FileStorage 재사용)** |
| 선행 | ✅ A3-1 admin-program-form (#212) — V12 · 3탭 폼 · 소프트 삭제 · 이미지 URL 입력 · F4/F0c 별도 페이지 링크만 · main 기준 `9847250` |
| 후행 | A4 admin-program-detail (신청 현황) · A6 조회수 · A8 카드/캘린더 · A9 Program-Center FK |
| ADR | `docs/adr/admin-track-roadmap-2026-09.md` §Q4-A "F0c/F4 인라인 통합" — A3 분할로 A3-2 에서 실현 |
| 마스터 지시서 | `ADMIN-00-master-directive.md` §5-A3 · §3-1 (Course · ProgramAttachment) · Q3-A |
| 상위 spec | `docs/specs/A3-admin-program-form.md` §12 A3-2 이월 · §11 Qn-C-Course/Attachment |
| prototype | `admin/prototype.html` L2510~2578 (썸네일·상세·첨부·강좌·질문) + L2582~2609 (약관) |
| main 기준 | `9847250` (V12 · A3-1 impl_done · A3-2 미착수) |
| 예상 규모 | **XL — 파일 30~40개 · 순증 3,000~4,000 LOC**. 리스크 상 (2 엔티티 신설 · 2 마이그레이션 · multipart 3종 인프라 동시 도입 · 인라인 트랜잭션 통합) |

---

## 0. 계약 확인 (0단계)

- **본 화면 계약**: A3-1 에서 `docs/design-contracts/admin/program-form.md` + `e2e/contracts/admin-program-form.ts` **신설 완료**. A3-2 는 **계약 확장** (인라인 F4/F0c 섹션 · Course row 렌더 · 첨부 리스트 · 이미지 업로드 UI) 으로 진행
- **admin 공통 정책**: `docs/design-contracts/admin/POLICY.md` (다크 헤더 · 인디고 primary · 존댓말) 승계
- **재활용 계약**: `admin-eligibility.ts` · `admin-dynamic-field.ts` · `admin-notice-attachment` 계약 (첨부 정책 · 셀렉터)
- **prototype 우선순위**: L2510~2578 (신청 정보 탭 안 강좌 · 질문 인라인) · L2522~2529 (첨부) · L2442~2448 (이미지 업로드)

---

## 1. 디자인 출처 (3자산 병렬 정독)

| 자산 | 인용 | 결론 |
|---|---|---|
| `admin/prototype.html` L2442~2448 | 190×190 dashed card + primary "이미지 업로드" 버튼 | **실 파일 업로드 UI 신설** — A3-1 은 imageUrl 텍스트 입력만 유지, 이번 티켓에서 multipart 로 전환 |
| `admin/prototype.html` L2522~2529 | dashed button "첨부파일 업로드 (PDF, HWP)" | **ProgramAttachment 엔티티 신설** (NoticeAttachment 패턴 복제) |
| `admin/prototype.html` L2549~2568 | 강좌 제공 radio (예/아니오) → "예" 시 조건부 `showCourses` 카드 `{{ coursesEl }}` 다중 row | **Course 엔티티 신설** · `Program.has_courses` 컬럼 신설 (V13) |
| `admin/prototype.html` L2570~2577 | 신청 질문 관리 — "+ 주관식 질문" · "+ 객관식 질문" 버튼 + `{{ questionsEl }}` 리스트 | **F0c 인라인 통합** — `AdminApplyQuestionService` 재사용, 편집 폼 안에서 생성/수정/삭제 |
| `admin/prototype.html` L2429 (탭1 자격요건 영역) | prototype 폼엔 없으나 F4 요구사항 (age/region/etc) 존재 | **F4 인라인 통합** — 정보 탭 하단에 `ProgramEligibility` 3필드 직접 편집 |
| `NoticeAttachment.java` L34~90 | `@Lob byte[] data` + `fileName` · `storedName` · `fileSize` · `contentType` · `sortOrder` · `@Basic(LAZY)` | 복제 대상 — `ProgramAttachment` 는 동일 컬럼 스키마 + `program_id FK` |
| `common/storage/FileStorage.java` (P0-3) | `save(...)` · `delete(...)` · `LocalFileStorage` (dev) / `SupabaseFileStorage` (prod) | 이미지 업로드 · 첨부 업로드 모두 재사용 |
| ADMIN-00 §3-1 | Course(program_id · name · schedule · capacity · sortOrder) · ProgramAttachment(program_id · filename · path · size · sortOrder) | V13 · V14 스키마 근거 |

### 1-A. 자산 간 갭 (형태 vs 존재 축)

| 항목 | wireframe | prototype | HANDOFF/ADMIN-00 | 축 | 채택 |
|---|---|---|---|---|---|
| 이미지 파일 업로드 | 없음 | 실 업로드 버튼 명시 (L2448) | ADMIN-00 §3-1 "thumbnail 실업로드" | 존재 | prototype 채택 → FileStorage 재사용 |
| Course 다중 row 편집 | 언급 | 조건부 UI (L2563~2568) | ADMIN-00 §3-1 Course 엔티티 | 존재+형태 | 통합 채택 → V13 신설 |
| 첨부파일 (PDF/HWP) | 없음 | L2525~2528 | ADMIN-00 §3-1 ProgramAttachment | 존재 | prototype+ADMIN-00 채택 → V14 신설 |
| F0c 인라인 CRUD | 없음 | L2570~2577 명시 | ADMIN-00 §5-A3 인라인 | 형태+존재 | 인라인 통합 (별도 페이지 병존) |
| F4 인라인 | 없음 | 명시 없음 (프로토는 신청 정보 탭에 자격요건 구획 없음) | ADMIN-00 §5-A3 "자격요건 인라인" | 존재 | ⚠️ Qn-A-F4 — prototype 미명시. ADMIN-00 지시대로 정보 탭 하단 포함 vs 제외 |
| 별도 페이지 (F4/F0c) 처리 | — | — | A3-1 spec §2-2 옵션 A (병존) 확정 | 형태 | Qn-A 재확인 필요 |

### 1-B. 데이터 모델 gap 표 (필수)

A3-1 완료 후 스키마 기준. **A3-2 신설 대상만**.

| prototype 필드 | 현재 스키마 (V12 기준) | 조치 |
|---|---|---|
| 이미지 (썸네일 + 파일 업로드) | `Program.imageUrl VARCHAR(500)` (URL 문자열만) | **필드 유지 · 저장 값 소스만 변경** — multipart 업로드 → FileStorage → URL 반환 → imageUrl 저장 |
| 강좌 제공 여부 | ❌ 없음 | **`program.has_courses BOOLEAN NOT NULL DEFAULT false`** 신설 (V13) |
| Course 다중 row (name · schedule · capacity · sortOrder) | ❌ 없음 | **`course` 테이블 신설** (V13) — 5-1 참조 |
| 첨부파일 (PDF/HWP/DOCX/XLSX) | ❌ 없음 | **`program_attachment` 테이블 신설** (V14) — 5-2 참조 · NoticeAttachment 복제 |
| F0c ApplyQuestion 인라인 CRUD | `apply_question` 테이블 존재 (V11 · F0c 완결) | 스키마 변경 없음 · 컨트롤러 재사용 (2-3 참조) |
| F4 Eligibility 인라인 | `program.eligibility_*` 임베디드 컬럼 (F4 완결) | 스키마 변경 없음 · 폼 파라미터 매핑만 확장 |

**신설 마이그레이션 종합**:
1. **V13** — `program.has_courses BOOLEAN` 컬럼 추가 + `course` 테이블 신설
2. **V14** — `program_attachment` 테이블 신설 (NoticeAttachment 컬럼 복제)

### 1-C. 데이터 소비 지점 (필수)

| 소비 지점 | 위치 | 이번 티켓 영향 | 회귀 방어 |
|---|---|---|---|
| A3 편집 폼 정보 탭 | `AdminProgramController.editForm` (A3-1) | 이미지 파일 업로드 위젯 + F4 인라인 추가 · 첨부 리스트 추가 | A3-1 회귀 필수 |
| A3 편집 폼 신청 탭 | 동 | 강좌 제공 radio · Course 다중 row · 질문 인라인 CRUD 추가 | 동 |
| A3 상세 카드 (편집 폼 기본 렌더) | `AdminProgramController.detail` | 신설 필드 (courses · attachments) 렌더 | E2E `admin-program-form.spec.ts` 확장 |
| A2 목록 | `AdminProgramController.list` | 강좌 개수 컬럼 필요성 판단 (Qn-Δ-list) | A2 spec 갱신 여부 |
| 사용자 상세 `/programs/{id}` | `ProgramController.detail` | 첨부 · 강좌 노출 여부 결정 필요 (Qn-Δ-user-render) | **이번 티켓은 admin 만** · 사용자 노출은 후행 |
| 사용자 신청 폼 | `ApplicationController.applyForm` | Course 선택 필드 추가 여부 (강좌 있는 프로그램일 때) — Qn-Δ-user-apply | **후행 A4 로 이월** |
| F4 별도 페이지 | `AdminProgramEligibilityController` | 유지 (병존) · 무회귀 | E2E `admin-eligibility-*.spec.ts` |
| F0c 별도 페이지 | `AdminApplyQuestionController` | 유지 (병존) · 무회귀 | E2E `admin-dynamic-field-*.spec.ts` |
| 신청 사이드 (신청 시 첨부 다운로드) | 미착수 | 관리자만 조회 · 다운로드 가능하도록 초기 스코프 | 후행 |
| P0-3 FileStorage bucket | `LocalFileStorage` / `SupabaseFileStorage` | bucket 신설 (`program-thumbnails` · `program-attachments`) or 공용 bucket + prefix | 회사 PC dev 는 local, prod 는 supabase |
| reset endpoint | `TestResetController` (기존) | Course · ProgramAttachment cascade 정리 확장 | Qn-Δ-reset |

### 1-D. write→read 왕복 통합 시나리오 (필수)

**시나리오 1 — 이미지 파일 업로드**:
- `/admin/programs/{id}` 편집 진입 → 정보 탭 이미지 카드에 기존 URL 렌더 or 빈 카드 → 파일 선택 (jpg 300KB) → submit (multipart) → Controller 가 FileStorage.save → 반환 URL 을 `Program.imageUrl` 에 저장 → PRG redirect → 상세에 새 썸네일 렌더 → A2 목록 썸네일에도 반영

**시나리오 2 — Course 등록 왕복**:
- 편집 진입 → 신청 탭 → "강좌 제공: 예" 선택 → 조건부 카드 노출 → "+ 강좌 추가" 3회 클릭 → 각 row 에 name/schedule/capacity 입력 → submit → 서버가 `List<CourseForm>` 을 upsert (기존 삭제 · 신규 추가) → 재로드 시 3개 row 프리필

**시나리오 3 — 첨부 업로드 · 삭제**:
- 편집 진입 → 정보 탭 첨부 영역 → 파일 3개 첨부 (PDF/HWP/DOCX) → submit → 3 row 저장 · FileStorage 에 실 저장 → 상세 재로드 시 리스트 렌더 → 개별 삭제 버튼 → POST `/admin/programs/{id}/attachments/{aid}/delete` → row + FileStorage 파일 모두 삭제 → 재로드 시 2개 남음

**시나리오 4 — F4 인라인 저장**:
- 편집 폼 정보 탭 하단 자격요건 3필드 (age/region/etc) 입력 → submit (Program POST 통합) → `AdminProgramService.updateWithSubResources` 가 Program + Eligibility 동시 저장 → **F4 별도 페이지 (`/admin/programs/{id}/eligibility`) 접근 시 동일 값 확인** (병존 회귀 방어)

**시나리오 5 — F0c 인라인 CRUD**:
- 신청 탭 "+ 주관식 질문" 클릭 → 클라이언트 JS 로 row DOM 추가 (label · required · sortOrder input) → "+ 객관식 질문" → options 필드 추가 → submit → 서버가 `List<ApplyQuestionForm>` upsert (id 없는 row = 신규, 기존 row 삭제됐으면 soft delete) → **F0c 별도 페이지 `/admin/programs/{id}/dynamic-fields` 에서 동일 값 노출**

**시나리오 6 — 회귀**:
- A3-1 편집 폼 (이미지 URL 입력만 사용하던 프로그램) → 재로드 시 imageUrl 그대로 유지 (파일 업로드 하지 않으면 무변경) → 사용자 사이드 `/programs/{id}` 상세 무회귀

---

## 2. 배경 · 스코프 · 인라인 통합 방식

### 2-1. 포함

| URL | 메서드 | RBAC | 목적 |
|---|---|---|---|
| `/admin/programs/{id}` | GET | SYSTEM_ADMIN (A3-1 승계) | 편집 폼 · Course · 첨부 · 인라인 F4/F0c 포함 |
| `/admin/programs/{id}` | POST (multipart) | 동 | 통합 저장 — Program + Eligibility + Course upsert + ApplyQuestion upsert + Term + 이미지·첨부 파일 |
| `/admin/programs/{id}/attachments/{aid}/delete` | POST | 동 | 첨부 단건 삭제 (row + FileStorage) |
| `/admin/programs/{id}/attachments/{aid}/download` | GET | 동 (관리자 다운로드) | 첨부 다운로드 (NoticeAttachment 패턴) |
| `/admin/programs/new` | GET/POST | 동 | 신규 등록 확장 — 이미지 업로드 · Course · 첨부 · 인라인 F4/F0c |

**신설 · 확장 대상 컨트롤러**: `AdminProgramController` (기존 확장) · `AdminProgramAttachmentController` (신설 or 병합) · `AdminProgramService` 확장 (트랜잭션 통합)

### 2-2. F4 · F0c 별도 페이지 처리 (Qn-A · 핵심)

- **옵션 A (권장 · A3-1 spec §2-2 승계)**: **별도 페이지 유지 + 인라인 신설 (병존)**
  - `/admin/programs/{id}/eligibility` · `/dynamic-fields` URL 그대로 · 딥링크 · 부분 편집
  - 편집 폼 안에도 동일 필드 인라인 · 통합 저장 시 함께 저장
  - E2E `admin-eligibility-*.spec.ts` · `admin-dynamic-field-*.spec.ts` 무회귀
- 옵션 B: 별도 페이지 제거 · 인라인만 (URL 폐기 · 마이그레이션 필요)
- 옵션 C: 별도 페이지 read-only 로 축소 · 편집은 인라인만

**권장**: **옵션 A** — 회귀 최소 · 딥링크 유지 · 이중 진입 허용. Qn-A 사용자 결정 대기.

### 2-3. 트랜잭션 통합 전략

**단일 POST · 단일 `@Transactional`**:
```
AdminProgramService.updateWithSubResources(id, ProgramForm form, MultipartFile image, List<MultipartFile> attachments):
  1. Program 필드 갱신 (A3-1 로직)
  2. Eligibility 임베디드 갱신 (F4 재활용)
  3. image != null 이면 FileStorage.save → Program.imageUrl 갱신
  4. Course 리스트 upsert (form.courses 로 기존 전량 재작성 or diff)
  5. ApplyQuestion 리스트 upsert (form.questions · id 없으면 신규 · 기존 id 없어졌으면 soft delete)
  6. attachments (multipart) 존재분만 append (기존 첨부는 별도 delete endpoint)
```

- **회귀 방어**: 서비스 실패 시 전체 롤백 (FileStorage 실 저장은 성공 후 후처리 — 실패 시 orphan file 은 이월 청소 스크립트로 대응)
- 대안: 각 sub-resource 를 별도 POST endpoint 로 분리 (HTMX partial save · Qn-Δ-htmx). 초기 스코프는 통합 POST 권장

### 2-4. 제외 (이월)

| 항목 | 이월처 | 근거 |
|---|---|---|
| 신청 현황 테이블 · 상태 변경 | **A4** | ADMIN-00 §5-A4 |
| 조회수 | **A6** | 통계 트랙 |
| 카드/캘린더/CSV | **A8** | ADMIN-00 §5-A8 |
| Program-Center FK 승격 | **A9** | ADMIN-00 Q2 |
| 자동 승인 로직 실행 | **A4** | 신청 상태 변경 로직 |
| 사용자 신청 폼 Course 선택 | **A4** or 후행 | 강좌 있는 프로그램은 신청 시 특정 강좌 선택 필요 여부 판단 필요 |
| 사용자 사이드 첨부 다운로드 | 후행 | 이번 티켓은 admin 만 다운로드 · 사용자 노출은 후행 |
| 리치 에디터 (Toast UI Editor) | A3-3 or 후행 | A3-1 툴바 flavor 유지 |
| 드래그·드롭 sortOrder | A3-3 or 후행 | 이번 티켓은 숫자 sortOrder 입력 |

---

## 3. Course 엔티티 (Qn-B)

### 3-1. 필드 · 상한 (Qn-B 결정)

**권장 A안** (prototype + ADMIN-00 §3-1 준수):

| 필드 | 타입 | 필수 | 검증 | 비고 |
|---|---|---|---|---|
| `id` | Long | — | — | PK |
| `program` | Program (FK, LAZY) | ✓ | | `ON DELETE CASCADE` |
| `name` | VARCHAR(200) | ✓ | `@NotBlank @Size(max=200)` | 강좌명 |
| `schedule` | VARCHAR(200) | — | `@Size(max=200)` | 요일·시간 설명 문자열 |
| `capacity` | Integer | — | `@Positive` | 강좌별 정원 (nullable 허용) |
| `sortOrder` | int | ✓ | | 기본 1, 서버에서 리스트 순서로 재부여 |
| `isActive` | boolean | ✓ | | 기본 true (Program 소프트 삭제 시 함께 SUSPENDED 상태로) |
| `createdAt`/`updatedAt` | LocalDateTime | ✓ | | `BaseTimeEntity` |

- Qn-B-max: 강좌 개수 상한 → **권장 20개** (prototype 은 상한 없으나 UX/성능 보호). deviation 시 산문 규칙 대신 `@Size(max=20)` 로 폼에 강제
- Qn-B-schedule: schedule 필드 자유 문자열 vs 구조화 (요일 enum · 시작/종료 시각) → **권장 A안 자유 문자열** (prototype 준수 · 스코프 최소)
- Qn-B-sortOrder: 숫자 입력 vs 드래그앤드롭 → **권장 A안 숫자 입력** (Qn-Δ-드래그 B안은 후행)

### 3-2. V13 마이그레이션

```sql
-- V13__create_course_and_has_courses.sql
ALTER TABLE program ADD COLUMN has_courses BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE course (
  id BIGSERIAL PRIMARY KEY,
  program_id BIGINT NOT NULL REFERENCES program(id) ON DELETE CASCADE,
  name VARCHAR(200) NOT NULL,
  schedule VARCHAR(200),
  capacity INT,
  sort_order INT NOT NULL DEFAULT 1,
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_course_program ON course(program_id, is_active, sort_order);
```

- **관계**: `Course.@ManyToOne Program` 단방향. Program 에 `@OneToMany` 컬렉션 미추가 (CLAUDE.md 엔티티 규칙)
- 조회: `CourseRepository.findAllByProgramIdAndIsActiveTrueOrderBySortOrderAsc(programId)`

### 3-3. 도메인 메서드

```java
Course.rename(String name)
Course.updateSchedule(String schedule)
Course.updateCapacity(Integer capacity)
Course.reorder(int sortOrder)
Course.deactivate()   // 강좌 삭제 = 소프트 (isActive=false)
```

---

## 4. ProgramAttachment 엔티티 (Qn-C)

### 4-1. 필드 (NoticeAttachment 복제)

| 필드 | 타입 | 필수 | 검증 | 비고 |
|---|---|---|---|---|
| `id` | Long | — | — | PK |
| `program` | Program (FK, LAZY) | ✓ | | `ON DELETE CASCADE` |
| `fileName` | VARCHAR(255) | ✓ | `@NotBlank @Size(max=255)` | 원본 파일명 |
| `storedName` | VARCHAR(255) | — | | FileStorage 저장 경로/키 |
| `fileSize` | long | ✓ | | 바이트 |
| `contentType` | VARCHAR(100) | — | | MIME |
| `sortOrder` | int | ✓ | | 기본 0 |
| `data` | `bytea` LAZY | — | | NoticeAttachment 승계 (P0-3 인프라 유예 정책). FileStorage 이관 완료 시 null 허용 · storageUrl 대체 |
| `createdAt` | LocalDateTime | ✓ | | `@CreatedDate` |

**Qn-C-max**: 최대 첨부 개수 → **권장 A안 10개** (F0c ATTACHMENT 정책 승계 or Notice 정책)
**Qn-C-ext**: 확장자 → **권장 A안 pdf · hwp · docx · xlsx** (prototype "PDF, HWP" 및 F0c 정책 승계)
**Qn-C-size**: 파일 크기 상한 → **권장 A안 5MB** (F0c · NoticeAttachment 승계)
**Qn-C-dup**: 파일명 중복 처리 → **권장 A안 storedName 은 UUID 로 유일화 · fileName 은 원본 유지** (NoticeAttachment 패턴)

### 4-2. V14 마이그레이션

```sql
-- V14__create_program_attachment.sql
CREATE TABLE program_attachment (
  id BIGSERIAL PRIMARY KEY,
  program_id BIGINT NOT NULL REFERENCES program(id) ON DELETE CASCADE,
  file_name VARCHAR(255) NOT NULL,
  stored_name VARCHAR(255),
  file_size BIGINT NOT NULL,
  content_type VARCHAR(100),
  sort_order INT NOT NULL DEFAULT 0,
  data BYTEA,
  created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_program_attachment_program ON program_attachment(program_id, sort_order);
```

### 4-3. FileStorage 연동

- Bucket: `program-attachments` (Qn-C-bucket A안) or 공용 bucket + prefix (`programs/{id}/attachments/`) — 권장 A안: **전용 bucket 분리** (권한 정책 별도)
- 저장: `FileStorage.save(inputStream, fileName, contentType)` → `StoredFile { url, storedName }` 반환
- 삭제: 개별 `POST /admin/programs/{id}/attachments/{aid}/delete` → `FileStorage.delete(storedName)` + row 삭제
- 다운로드: `GET .../download` — NoticeAttachment 다운로드 패턴 복제 (Content-Disposition + Content-Type)

---

## 5. 이미지 파일 업로드 (Qn-D)

### 5-1. UI · 저장 흐름

**Qn-D-mode**: URL 수동 입력 vs 파일 업로드 배타 여부
- **권장 A안 (배타)**: 이미지 카드 영역에 "파일 업로드" 버튼만. URL 수동 입력 필드 제거 → 항상 FileStorage 경유
- 옵션 B: 둘 다 지원 (URL 입력 필드 유지 + 파일 업로드 버튼 · 둘 중 하나 선택)
- 옵션 C: 파일 업로드만 · 기존 A3-1 imageUrl 은 편집 시 파일 재업로드 요구 (마이그레이션 부담)

**권장 이유**: prototype L2448 "이미지 업로드" 버튼만 존재 · URL 입력 필드 없음. 배타가 UX 단순.
**주의**: A3-1 시드 프로그램 (12+건) 은 이미 imageUrl 을 텍스트로 보유 → **마이그레이션 무필요** (기존 imageUrl 은 그대로 렌더 · 편집 시 새 파일 업로드하면 덮어씀). 옵션 A 채택해도 기존 데이터 무회귀.

### 5-2. 정책

| 항목 | 권장 A안 | 대안 |
|---|---|---|
| 확장자 | `jpg · jpeg · png · webp` | + gif (Qn-D-ext) |
| 크기 상한 | **2MB** | 5MB (Qn-D-size) |
| bucket | `program-thumbnails` | 공용 bucket + prefix |
| 프리뷰 | submit 전 클라이언트 JS 로 파일 선택 시 즉시 프리뷰 (FileReader) | 서버 왕복 후 프리뷰 |
| 삭제 | 파일 재업로드 시 이전 파일 FileStorage 에서 삭제 | orphan file 후처리로 대응 |

### 5-3. 검증

- `@Size` 대신 서비스 계층에서 `MultipartFile.getSize()` 체크
- MIME 타입 화이트리스트 · magic bytes 검사 (스코프 최소 시 확장자만)
- 400 시 flash + 입력값 보존 (파일 필드는 재선택 필요 — HTTP 특성)

---

## 6. 화면 · 라우팅

### 6-1. 정보 탭 확장 (prototype L2440~2530)

1. **이미지 카드** — 190×190 dashed · "파일 업로드" 버튼 (multipart · Qn-D)
2. 프로그램 제목 · 프로그램 설명 (A3-1 승계)
3. 진행 기간 · 신청 기간 (A3-1 승계)
4. 진행 장소 · 문의처 · 청년센터 · 모집 인원 (A3-1 승계)
5. **자격요건 3필드 인라인** (F4 재활용 · Qn-A A안 채택 시)
6. 상세 내용 (A3-1 승계 · 툴바 flavor)
7. **첨부파일** — dashed 버튼 · 다중 파일 · 리스트 렌더 · 개별 삭제 버튼 (Qn-C)

### 6-2. 신청 탭 확장 (prototype L2534~2578)

1. 승인 여부 radio (A3-1 승계)
2. **강좌 제공 radio** (예/아니오) → "예" 조건부 강좌 구성 카드 노출
3. **강좌 구성 카드** (조건부) — 다중 row (name · schedule · capacity · sortOrder · 삭제) + "+ 강좌 추가" 버튼 (Qn-B)
4. **신청 질문 관리** — "+ 주관식 질문" / "+ 객관식 질문" 버튼 + 리스트 (F0c 인라인 · Qn-A)
   - Row 필드: label · required · sortOrder · options (객관식만 · 쉼표구분 or 다중 row)
   - 삭제: row soft delete (isActive=false)

### 6-3. 약관 탭 (A3-1 승계 · 변경 없음)

### 6-4. 인라인 편집 UI 상세

**Qn-E-form-encoding**: multi-row 저장 방식
- **권장 A안 (Spring 표준)**: repeated form fields `courses[0].name` · `questions[1].label` · `questions[1].options[0]` — Bean 바인딩 자연스러움
- 옵션 B: JSON hidden field (courses 를 JSON 문자열로 한 필드에)
- 옵션 C: HTMX partial save 각 row 마다 별도 endpoint (Qn-Δ-htmx)

**Qn-E-empty-row**: 저장 시 완전 빈 row 처리 → **권장 A안 자동 스킵** (필수값 없으면 삭제된 것으로 간주)

**Qn-E-sortOrder**: **숫자 sortOrder 필드 직접 입력** vs 서버 자동 재부여 (제출 순서대로)
- **권장 A안 · 서버 자동 재부여** (row 순서대로 1,2,3... 부여 · UX 단순)
- 드래그·드롭 후행 시 별도 필드로 승격 가능

### 6-5. 삭제 흐름 (A3-1 승계)

- 이번 티켓 신설 삭제:
  - 첨부 개별 삭제 (POST · confirm 모달)
  - Course 개별 삭제 (form 내 row 삭제 · 저장 시 반영)
  - 질문 개별 삭제 (form 내 row 삭제 · 저장 시 soft delete)

---

## 7. RBAC

A3-1 승계 — **SYSTEM_ADMIN only** (`@PreAuthorize("hasRole('SYSTEM_ADMIN')")`). Qn-F 확인 필요 (A3-1 이 SYSTEM only 로 확정됐는지, 아니면 CENTER 확장 결정됐는지 재확인 필요).

---

## 8. 회귀 방지 (최우선)

### 8-1. A3-1 회귀

| 대상 | 검증 |
|---|---|
| A3-1 편집 폼 3탭 렌더 | `admin-program-form.spec.ts` 재실행 |
| A3-1 소프트 삭제 | `admin-program-form-delete.spec.ts` (or A3-1 spec 내) 재실행 |
| A3-1 imageUrl 텍스트 입력 (기존 방식) | Qn-D-mode A안 (배타) 채택 시 → 기존 imageUrl 은 유지 · 편집만 파일 업로드로 강제. 회귀 시나리오 명시 필요 |

### 8-2. 파생 큐 무회귀

| 대상 | 검증 |
|---|---|
| F0c 별도 페이지 (신청 질문 관리) | `admin-dynamic-field-*.spec.ts` 전수 무회귀 (Qn-A A안 · 병존) |
| F4 별도 페이지 (자격요건) | `admin-eligibility-*.spec.ts` 전수 무회귀 |
| A2 목록 | `admin-programs-list.spec.ts` — 강좌 컬럼 노출 여부 결정 필요 (Qn-Δ-list) |
| A2 상세 (A3-1 통합됐다면) | 관련 spec |

### 8-3. 사용자 사이드 무회귀

| 대상 | 검증 |
|---|---|
| `/programs` 목록 | `programs.spec.ts` — 신설 컬럼 SELECT 포함되지만 UI 렌더 X → 무회귀 |
| `/programs/{id}` 상세 | `program-detail.spec.ts` — Course · Attachment 사용자 노출은 후행 → 무회귀 |
| `/apply/{programId}` | `apply.spec.ts` — Course 선택은 A4 이월 → 무회귀 |

### 8-4. 시드 · Program 엔티티 원칙

- 기존 12+건 프로그램: `has_courses = false` default 로 무회귀 (강좌 없는 상태 유지). Course row 미생성
- 신규 샘플 (Qn-Δ-seed): 강좌 있는 시드 프로그램 1건 + 첨부 있는 시드 1건 추가 여부 → **권장 A안 미추가** (수동 확인용). 옵션 B: 시드 확장으로 회귀 방어 강화
- Program 에 `@OneToMany` 미추가 원칙 (CLAUDE.md)

---

## 9. 마이그레이션

| V | 파일 | 내용 |
|---|---|---|
| V13 | `V13__create_course_and_has_courses.sql` | `program.has_courses` + `course` 테이블 |
| V14 | `V14__create_program_attachment.sql` | `program_attachment` 테이블 |

**한 번 머지된 V 파일 절대 수정 금지 규칙** — Qn 결정 후 V13/V14 확정. 병렬 브랜치가 V13 를 선점했으면 rebase 필요.

**시드 확장 (Qn-Δ-seed)**: DataInitializer 에 강좌 있는 프로그램 1건 · 첨부 있는 프로그램 1건 부착 여부 결정. **권장 A안 미추가** (스코프 최소 · A3-2 QA 는 수동 or E2E 로 생성/삭제 왕복).

---

## 10. 테스트

### 10-1. 정적 (JVM)

- `AdminProgramCourseServiceTest` — Course upsert · 순서 재부여 · soft delete · 상한 20개 검증
- `AdminProgramAttachmentServiceTest` — multipart upload · 확장자·크기 검증 · FileStorage 호출 · 개별 삭제
- `AdminProgramImageUploadServiceTest` — 이미지 업로드 · FileStorage 저장 · 이전 파일 삭제
- `AdminProgramFormIntegrationServiceTest` — 단일 트랜잭션 통합 (Program + Eligibility + Course + Question + Term + 이미지 + 첨부) · 롤백
- `CourseRepositoryTest` (@DataJpaTest) — sortOrder 정렬 · isActive 필터
- `ProgramAttachmentRepositoryTest` — 파일 메타 · sortOrder · LAZY data fetch
- `AdminProgramFormRenderTest` — 3탭 렌더 (인라인 F4/F0c/Course/Attachment 확인 · 조건부 강좌 카드)
- `JpaMappingTest` — Course · ProgramAttachment 매핑
- **회귀 (필수)**: A3-1 `AdminProgramFormServiceTest` · F4/F0c 서비스 · Notice attachment 렌더

### 10-2. 동적 (curl · 8091)

```bash
# 편집 폼 렌더 (인라인 F4/F0c/Course/Attachment 섹션 확인)
curl -s -b jsession http://localhost:8091/admin/programs/1 | grep -E "강좌 구성|첨부파일|주관식 질문|자격요건"

# multipart 통합 저장
curl -s -X POST -b jsession \
  -F "_csrf=..." \
  -F "title=..." \
  -F "hasCourses=true" \
  -F "courses[0].name=1강좌" -F "courses[0].capacity=20" \
  -F "questions[0].label=지원동기" -F "questions[0].required=true" \
  -F "image=@sample.jpg" \
  -F "attachments=@guide.pdf" \
  http://localhost:8091/admin/programs/1
# → 302

# 첨부 삭제
curl -s -X POST -b jsession -F "_csrf=..." http://localhost:8091/admin/programs/1/attachments/5/delete
# → 302

# 첨부 다운로드
curl -s -o /tmp/dl.pdf -w "%{http_code}\n" -b jsession http://localhost:8091/admin/programs/1/attachments/5/download
# → 200 · Content-Disposition: attachment; filename="..."
```

### 10-3. 계약 확장

- `docs/design-contracts/admin/program-form.md` 갱신 — 인라인 F4/F0c 섹션 · Course row 렌더 · 첨부 리스트 · 이미지 업로드 UI 셀렉터 추가
- `e2e/contracts/admin-program-form.ts` — proto: 인용 L2510~2578 추가
- `npx playwright test --project=contracts` → 갭 0

### 10-4. 기능 E2E (Playwright)

| spec | 검증 |
|---|---|
| `admin-program-form-image-upload.spec.ts` | 파일 선택 → submit → 저장 → 재로드 시 새 썸네일 |
| `admin-program-form-course.spec.ts` | 강좌 제공 예 → 조건부 카드 노출 → 3 row 추가 → 저장 → 재로드 시 3 row 프리필 → 1 row 삭제 → 저장 → 2 row 남음 |
| `admin-program-form-attachment.spec.ts` | 다중 파일 첨부 · 리스트 · 개별 삭제 · 5MB 상한 초과 400 · 잘못된 확장자 400 · 다운로드 |
| `admin-program-form-inline-eligibility.spec.ts` | 정보 탭 자격요건 저장 → F4 별도 페이지에서 동일 값 확인 |
| `admin-program-form-inline-questions.spec.ts` | 신청 탭 질문 인라인 CRUD → F0c 별도 페이지에서 동일 값 확인 (역방향도 동일) |
| `admin-program-form-integration.spec.ts` | 통합 저장 (Program + Course + Question + Eligibility + Term + 이미지 + 첨부) 단일 POST · 부분 검증 실패 시 전체 롤백 |
| **회귀** | A3-1 `admin-program-form.spec.ts` + `admin-programs-list.spec.ts` + `admin-programs-detail.spec.ts` + F4/F0c 별도 페이지 spec 전수 + 사용자 사이드 spec 전수 |

### 10-5. reset endpoint (Qn-Δ-reset)

- **권장 A안 · 확장**: `POST /__test__/reset-programs-extended` 를 Course · ProgramAttachment cascade 삭제 + FileStorage 정리 (dev bucket) 로 확장. FileStorage 정리는 LocalFileStorage 만 (Supabase 는 prod 라 미개입)
- 대안: 별도 endpoint `reset-program-sub-resources` 신설

---

## 11. 결정 필요 항목 (Qn — 대규모)

**핵심 3**:

### Qn-A · F4/F0c 별도 페이지 처리
- **A (권장 · A3-1 spec §2-2 승계 · 회귀 최소)**: 별도 페이지 유지 + 인라인 신설 (병존)
- B: 별도 페이지 제거 · 인라인만
- C: 별도 페이지 read-only 로 축소 · 편집은 인라인만

### Qn-B · Course 필드 · 상한 · sortOrder
- **A (권장)**: 필수 name / 선택 schedule · capacity · isActive soft delete · 상한 20 · 서버 자동 sortOrder 재부여
- B: schedule 필수화
- C: 상한 없음 · 성능 리스크 감수
- D: 드래그·드롭 sortOrder UI (Qn-Δ-드래그 B안 후행)

### Qn-C · ProgramAttachment 정책
- **A (권장 · NoticeAttachment/F0c 승계)**: 확장자 pdf/hwp/docx/xlsx · 5MB · 상한 10개 · storedName UUID · bucket `program-attachments`
- B: 프로그램 자체 정책 (10MB · 상한 5개 · 확장자 확장 등)
- C: NoticeAttachment 완전 재활용 (공용 attachment 테이블로 통합 · 스코프 대폭 확장)

### Qn-D · 이미지 업로드 (URL vs 파일)
- **A (권장 · prototype 준수 · UX 단순)**: 파일 업로드만 (URL 수동 입력 필드 제거 · 기존 imageUrl 은 유지 · 편집 시 파일 재업로드하면 덮어씀)
- B: 둘 다 (URL 필드 유지 + 파일 업로드 버튼 · 배타 선택)
- C: 파일 업로드만 + 기존 imageUrl 은 편집 시 파일 재업로드 요구 (마이그레이션 부담)

**세부 · Qn-Δ**:

### Qn-Δ-D-ext · 이미지 확장자
- **A (권장)**: jpg / jpeg / png / webp
- B: + gif

### Qn-Δ-D-size · 이미지 크기 상한
- **A (권장)**: 2MB
- B: 5MB (첨부와 동일)

### Qn-Δ-B-max · Course 개수 상한
- **A (권장)**: 20개
- B: 상한 없음
- C: 10개

### Qn-Δ-C-bucket · Attachment bucket 분리
- **A (권장)**: 전용 bucket `program-attachments`
- B: 공용 bucket + prefix `programs/{id}/attachments/`

### Qn-Δ-encoding · Multi-row 저장 encoding
- **A (권장 · Spring 표준)**: repeated form fields `courses[0].name`
- B: JSON hidden field
- C: HTMX partial save (Qn-Δ-htmx B 채택 시)

### Qn-Δ-sortOrder · 서버 자동 재부여
- **A (권장)**: 서버가 제출 순서대로 1,2,3 재부여 · 클라이언트는 sortOrder 필드 미노출
- B: 클라이언트에 sortOrder 숫자 필드 노출 (F0c 별도 페이지 동일)
- C: 드래그·드롭 (Qn-Δ-드래그 B · 후행)

### Qn-Δ-htmx · 저장 방식
- **A (권장 · admin 일관성)**: 통합 form submit (단일 POST · PRG)
- B: HTMX partial save (각 sub-resource 별 endpoint) — 미검증 · 스코프 확장

### Qn-Δ-list · A2 목록 강좌 컬럼
- **A (권장 · 스코프 최소)**: A2 목록 무변경 · 강좌 개수 미노출
- B: 강좌 개수 컬럼 추가 (A2 spec 갱신 필요)

### Qn-Δ-user-render · 사용자 상세에 Course/Attachment 노출
- **A (권장 · 스코프 최소)**: 이번 티켓 이월 (후행) · admin 만 조회 가능
- B: 사용자 상세에도 렌더 · prototype.tsx 사용자 상세 재확인 필요

### Qn-Δ-reset · reset endpoint 확장
- **A (권장)**: `reset-programs-extended` 확장 · Course·Attachment cascade + LocalFileStorage 정리
- B: 별도 endpoint 신설

### Qn-Δ-seed · 시드 확장
- **A (권장 · 회귀 최소)**: 기존 시드 12+건 무변경 · 신규 샘플 미추가
- B: 강좌 있는 시드 1건 + 첨부 있는 시드 1건 추가

### Qn-Δ-drag · 드래그 sortOrder
- **A (권장 · 스코프 최소)**: 이번 티켓 미도입 · 서버 자동 재부여 또는 숫자 입력
- B: SortableJS webjar 도입 (후행)

### Qn-Δ-contract · 계약 확장 vs 신설
- **A (권장)**: A3-1 계약 확장 (`program-form.md` 갱신 · 새 파일 미신설)
- B: A3-2 전용 계약 신설 (분리 유지)

**Qn 총합**: 3 (핵심) + 13 (Δ) = **16개** (사용자 결정 필요)

---

## 12. 스코프 예상 규모

| 항목 | 규모 |
|---|---|
| 신규 Java 파일 | 10~15 (Entity 2 · Repository 2 · Service 2~3 · Controller 확장 · DTO 4~6) |
| 수정 Java 파일 | 6~8 (`AdminProgramController` · `AdminProgramService` · `Program` · `DataInitializer` · `TestResetController` · security 등) |
| 신규 템플릿 fragment | 3~5 (course-row · attachment-row · image-upload · inline-eligibility · inline-question-row) |
| 수정 템플릿 | 3 (form/tab-info · form/tab-apply · admin/program/detail) |
| 신규 테스트 (JVM) | 8~10 |
| 신규 E2E spec | 6~8 |
| 계약 갱신 | 1 (form.md · admin-program-form.ts 확장) |
| Flyway V | 2 (V13 · V14) |
| **총 diff** | **3,000~4,000 라인** |
| **리스크** | **상** — 2 엔티티 신설 · 2 마이그레이션 · multipart 3종 · 트랜잭션 통합 · A3-1 회귀 방어 · F4/F0c 병존 |

---

## 13. deferred / deviation

### deferred

| 항목 | 이월처 |
|---|---|
| 사용자 신청 폼 Course 선택 (강좌 있는 프로그램) | **A4** or 후행 |
| 사용자 상세 Course · Attachment 노출 | 후행 소형 티켓 (Qn-Δ-user-render) |
| A2 목록 강좌 컬럼 | 후행 (Qn-Δ-list) |
| 리치 에디터 (Toast UI Editor) | A3-3 or 후행 |
| 드래그·드롭 sortOrder | A3-3 or 후행 (Qn-Δ-drag) |
| 신청 마감 vs 진행 시작 검증 로직 | A3-1 spec §5-A3 에서 이월된 항목. 이번 티켓 스코프 밖 |
| **orphan file cleanup** (verify fix 2026-09-11 신설) | **후속 티켓** — Controller `@Transactional(rollbackFor=Exception.class)` 도입으로 DB 는 전체 롤백되지만, FileStorage 물리 저장 이후 후행 예외 (예: Course validation 실패) 발생 시 물리 파일이 남을 수 있음. 스케줄러 or CLI 로 `program-images`/`program-attachments` bucket 을 DB 상 imageUrl · ProgramAttachment 참조와 대조해 미참조 파일 삭제 |
| Program-Center FK 승격 | **A9** |
| 조회수 | **A6** |
| 카드·캘린더·CSV | **A8** |

### deviation

| 항목 | 사유 |
|---|---|
| Program 엔티티에 `@OneToMany` 컬렉션 미추가 | CLAUDE.md 엔티티 규칙 (단방향 `@ManyToOne` 원칙) |
| F4/F0c 별도 페이지 병존 (Qn-A A안) | 회귀 방어 · 딥링크 유지 |
| ProgramAttachment 에 `@Lob byte[] data` 컬럼 유지 (NoticeAttachment 승계) | P0-3 인프라 결정 유예 정책 승계. Supabase Storage 이관 시 nullable + storageUrl 로 승격 |
| Qn-Δ-seed A안 (시드 미추가) | 스코프 최소 · 회귀 방어는 기존 시드로 수행 |
| Qn-Δ-drag A안 (숫자/자동 재부여) | 스코프 최소 · 드래그·드롭은 후행 |
| Qn-Δ-user-render A안 (사용자 렌더 이월) | 이번 티켓은 admin 만 |
| Qn-D A안 (파일 업로드만 · URL 필드 제거) | prototype 준수 · A3-1 기존 imageUrl 은 유지되므로 회귀 없음 |

---

## 14. 작업 큐 메타

- 작업 ID: **A3-2**
- 우선순위: **높음** (A3-1 후속 · admin 트랙 인라인 통합 완결)
- 추정 단위: 1 PR (~3,500 LOC · 리스크 상) or 2 분할 (A3-2a 이미지+첨부 · A3-2b 인라인 통합+Course)
- 상태: **`spec_done`** (Qn 결정 대기)
- 다음 단계: 사용자 Qn-A~D + Qn-Δ-* (총 16개) 결정 → `spec_confirmed` → ym-impl 인계

---

## 다음 단계 인계

명세 산출 완료. 결정 필요 항목은 **핵심 4 (Qn-A/B/C/D) + Δ 13 = 총 17개**. 대규모 티켓이므로 다음 순서로 결정 흐름을 잡을 것을 권장합니다:

1. **Qn-A (F4/F0c 별도 페이지 처리)** — 스코프 결정. A3-1 spec §2-2 A안 승계 여부만 확인
2. **Qn-D (이미지 업로드 방식)** — imageUrl 필드 존치 여부 결정 → 기존 시드 회귀 계산 확정
3. **Qn-B (Course)** · **Qn-C (Attachment)** — 신설 엔티티 세부 정책
4. **Qn-Δ-*** 일괄 결정 (13개)

권장 세트: **"모두 A안"** — prototype 준수 · A3-1 회귀 최소 · 스코프 계단화.

사용자 결정 이후 spec 상태를 `spec_confirmed` 로 갱신하고 각 Qn 오른쪽에 채택 안 표기.
