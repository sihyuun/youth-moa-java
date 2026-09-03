# 작업 명세: A-admin-notice-attachment — 관리자 공지사항 첨부파일 업로드/삭제 + P0-3 파일 저장소 인프라

| 메타 | 값 |
|---|---|
| 상태 | `impl_done` (2026-09-03 · brancn feature/admin-notice-attachment · Qn-1 **C** · Qn-2 A · Qn-3 A · Qn-4 A · Qn-5 **A** · Qn-6 **B** · Qn-7 A · Qn-8 **Custom (작성자 기반, 아래 §8-후속)** · Qn-9 A) |
| 브랜치 후보 | `feature/admin-notice-attachment` |
| 착수 조건 | **A1 admin-shell (PR #205) 머지 후**. AdminScope · admin 헤더 fragment · `/admin/**` SecurityFilterChain · SYSTEM_ADMIN/CENTER_ADMIN 시드가 A1 산출물이며 이 티켓의 필수 인프라 |
| 관련 문서 | `docs/specs/F-notice-attachment.md` (사용자 다운로드 완료 · PR #130) · `docs/specs/ADMIN-00-master-directive.md` §P0-3/§Q4 · `docs/adr/admin-track-roadmap-2026-09.md` §Q5 |
| 파생 큐 위치 | ADR 로드맵 §2 "admin 공지사항 첨부(소형·P0-3 병행)" — A1 후, A2 전 삽입 |

---

## 0. 계약 상태 (0단계)

- **계약 없음** — 관리자 공지사항 관리 화면은 admin prototype.tsx/html 에 **부재** (grep 실증: `admin.*notice|notice.*admin` 매치 0건). ADMIN-00 §1-A 표에도 없다
- 따라서 이 티켓은 prototype 원문에 의존하지 않고 **wireframe-policy + 사용자 사이드 detail 화면 톤 + admin 공통 계약(POLICY/dashboard/shell)** 을 근거로 구현하고, **계약을 신설**한다:
  - `docs/design-contracts/admin/notice-management.md` (신설)
  - `e2e/contracts/admin-notice.ts` (신설)
- 준수 공통 정책: `docs/design-contracts/admin/README.md` · admin POLICY (다크 헤더 · 인디고 primary · 존댓말 "…했어요/됐어요" 톤 · 삭제 confirm 모달 1단계)

---

## 1. 배경 · 스코프

### 완료된 선행
- **F-notice-attachment (PR #130)** — 사용자 다운로드 완비. `NoticeAttachment` 엔티티에 `@Lob byte[] data (bytea)` 컬럼 + `V4__notice_attachment_data.sql` + 정책 상수 (`NoticeService.MAX_ATTACHMENT_SIZE_BYTES = 5MB`, `ALLOWED_EXTENSIONS = {pdf, hwp, docx, xlsx}`) + 다운로드 컨트롤러 `GET /notices/{nid}/attachments/{aid}/download`
- **A1 admin-shell (PR #205, CI 대기)** — `AdminScope.effectiveCenterName()`, admin 헤더 fragment, `/admin/**` chain, `SYSTEM_ADMIN`/`CENTER_ADMIN` 시드
- **F-notice-attachment 결정 재확인**: 저장 백엔드는 **학습 단계 DB `@Lob bytea`**. 이 티켓은 그 결정을 승계하되, **P0-3 저장소 추상화 인터페이스만 신설**하여 향후 Supabase Storage 이관 시의 갈아끼움 지점을 확보 (§2)

### 이번 티켓 스코프
1. **`FileStorage` 인터페이스 + `DbLobFileStorage` 구현** — P0-3 최소 실체화. `spring.servlet.multipart` 설정 신설
2. **admin 공지사항 관리 화면**:
   - `GET /admin/notices` — 목록
   - `GET /admin/notices/{id}` — 상세 편집 (첨부 관리 포함)
   - `POST /admin/notices/{id}/attachments` — multipart 업로드
   - `DELETE /admin/notices/{id}/attachments/{attachmentId}` — 삭제
3. **AdminScope 적용** — SYSTEM_ADMIN 전체 / CENTER_ADMIN 는 자기 센터 공지만. 단, `Notice` 엔티티에는 **현재 center FK 가 없음** — §3 참조 (**Qn-8**)
4. **계약 신설** — admin/notice-management.md + admin-notice.ts
5. **기능 E2E** — `admin-notice-upload.spec.ts` (multipart form submission)

### 이번 티켓 제외 (이월)
- **신규 공지 작성/삭제 화면** — CRUD 전체는 첨부 스코프에서 벗어남. Qn-5 에서 결정
- **Supabase Storage 구현체** — 실제 배포 준비 시점 별도 티켓 (`refactor/notice-attachment-supabase-storage`)
- **파일 미리보기, 재정렬(sortOrder drag)** — 추후

---

## 2. 파일 저장소 정책 (ADR Q5 상세화)

### 2-1. 결정 재확인 (F-notice-attachment D1 승계)

F-notice-attachment §1-A D1: **"학습 단계 DB `@Lob byte[]`(PG bytea). 배포 시 Supabase Storage 로 이관 예정."** — 이번 티켓은 이 결정을 그대로 승계한다.

### 2-2. 추상화만 신설 (P0-3 최소 실체화)

배포 시점 이관을 매끄럽게 하기 위해 인터페이스 layer 만 이번에 도입한다. 구현체는 **`DbLobFileStorage` 하나만**. Supabase 구현체는 이월.

```java
public interface FileStorage {
  /** 저장. 반환값은 후속 다운로드/삭제 시 사용할 opaque handle. DB LOB 구현은 null 반환 */
  StorageHandle store(String logicalName, String contentType, byte[] bytes);
  /** 삭제. DB LOB 은 no-op (엔티티 삭제로 함께 지워짐) */
  void delete(StorageHandle handle);
}

public record StorageHandle(String storedName, String storageUrl) {}
```

- **dev / e2e / local / prod (현재)**: `DbLobFileStorage` → `NoticeAttachment.data` 로 직접 저장. `storedName` 은 UUID, `storageUrl` = null
- **prod (배포 준비 완료 후)**: `SupabaseFileStorage` — **이번 티켓 미구현**. Qn-1 에서 결정된 대로 이월
- Bean 정의: `@ConditionalOnProperty(name="youthmoa.storage.backend", havingValue="db", matchIfMissing=true)` — 기본값 db LOB

### 2-3. multipart 설정 (P0-3)

`application.yml` 신설 (Qn-2 결정 반영):
```yaml
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 5MB     # 파일당 (NoticeService.MAX_ATTACHMENT_SIZE_BYTES 와 일치)
      max-request-size: 6MB  # 파일 5MB + 폼 필드 여유
```

- 상한을 F-notice-attachment 정책 상수 (5MB) 와 **정확히 일치**시켜 Servlet-level rejection 이 우선 발동하도록 함. 서비스 계층은 이중 검증 (defense in depth)

### 2-4. 확장자·MIME 검증

- **화이트리스트 (F-notice-attachment 승계)**: `NoticeService.ALLOWED_EXTENSIONS = {pdf, hwp, docx, xlsx}`
- **검증 방식**: **파일명 확장자 + Content-Type 헤더 병행 검증** (Qn-4)
  - 파일명 확장자 = `NoticeService.extensionOf()` 이미 존재 → whitelist 매칭
  - Content-Type = 확장자별 허용 목록 매칭 (예: `pdf` → `application/pdf`)
  - **magic number 검증은 이번 스코프 아님** — 학습 단계 오버스펙. Qn-4 에서 확정
- 실패 시 사용자 메시지: `"허용되지 않는 파일 형식이에요. pdf, hwp, docx, xlsx 만 업로드할 수 있어요."` (admin POLICY 존댓말 톤)

---

## 3. 엔티티 · 마이그레이션

### 3-1. 기존 자산 활용
- `NoticeAttachment` — 이미 완비. 이 티켓에서 **엔티티 변경 없음**
- `V4__notice_attachment_data.sql` — 이미 존재
- `NoticeAttachmentRepository.findByIdAndNoticeId` — 이미 존재. 삭제 시 재활용

### 3-2. 신규 마이그레이션
- **없음** — 첨부 CRUD 는 기존 스키마로 충분

### 3-3. Notice · Center 관계 (⚠️ 결정 필요)

**현재 상태**: `Notice` 엔티티는 `center` FK 가 없다. 시스템 공지 성격에 맞으나 **CENTER_ADMIN 의 스코프 필터가 애매**해진다.

| 옵션 | 내용 | 트레이드오프 |
|---|---|---|
| A | Notice 에 `Center center` (nullable) FK 추가 — null 이면 시스템 공지 (SYSTEM_ADMIN 전용 관리) | 마이그레이션 1건 추가. 스코프 명확 |
| B | 이번 티켓은 **SYSTEM_ADMIN 만 관리 가능** 으로 제한. CENTER_ADMIN 은 403 | 인프라 변경 없음. admin 파일럿 성격 부합 |
| C | Notice 는 전 관리자 공유 — CENTER_ADMIN 도 편집 가능 | RBAC 상 위험 (남의 공지 삭제 가능) |

**권장**: **옵션 B** (Qn-8). Notice 는 청년모아 전체 시스템 공지 성격이 강하고, 센터별 공지 개념은 admin 트랙 후반에 별도 재정의하는 것이 자연스럽다.

---

## 4. 데이터 모델 gap 표

| 필드 (필요) | 현재 상태 | 조치 |
|---|---|---|
| `NoticeAttachment` 전체 | ✅ 완비 (F-notice-attachment) | 없음 |
| `Notice.center` FK | ❌ | Qn-8 결정 필요 (권장 B: 유지) |
| 업로더 user_id | ❌ | 이월 (감사 로그) — 이번 티켓 미포함 |
| 업로드 시각 | ✅ `createdAt` (@CreatedDate) | 유지 |

---

## 5. 데이터 소비 지점

| 소비 지점 | 현재 | 이번 티켓 조치 |
|---|---|---|
| 사용자 `notice/detail.html` 첨부 목록 렌더 | 완비 (파일명 · 크기 · 다운로드 링크) | 변경 없음 — 업로드 후 자동 노출 확인 (E2E 왕복) |
| 사용자 `GET /notices/{nid}/attachments/{aid}/download` | 완비 | 변경 없음 — 업로드 → 다운로드 왕복 검증 |
| admin `GET /admin/notices` | ❌ | 신설 |
| admin `GET /admin/notices/{id}` | ❌ | 신설 (첨부 목록 · 업로드 UI · 삭제 버튼) |
| admin `POST /admin/notices/{id}/attachments` | ❌ | 신설 |
| admin `DELETE /admin/notices/{id}/attachments/{aid}` | ❌ | 신설 |

---

## 6. 변경 범위 (파일 단위)

### 신규
- [ ] `src/main/java/.../common/storage/FileStorage.java` — 인터페이스
- [ ] `src/main/java/.../common/storage/StorageHandle.java` — record
- [ ] `src/main/java/.../common/storage/DbLobFileStorage.java` — 구현체
- [ ] `src/main/java/.../admin/AdminNoticeController.java` — 목록·상세·첨부 CRUD
- [ ] `src/main/java/.../notice/NoticeAttachmentService.java` (또는 `NoticeService` 확장) — `uploadAttachment(noticeId, MultipartFile)`, `deleteAttachment(noticeId, attachmentId)`
- [ ] `src/main/resources/templates/admin/notice/list.html` — 목록
- [ ] `src/main/resources/templates/admin/notice/edit.html` — 상세 편집 (첨부 관리 섹션 포함)
- [ ] `src/main/resources/templates/admin/notice/_attachments-fragment.html` — HTMX 업로드/삭제 후 부분 갱신 target
- [ ] `src/test/java/.../admin/AdminNoticeControllerTest.java` — RBAC · 성공/실패 · CENTER_ADMIN 403 (옵션 B 채택 시)
- [ ] `src/test/java/.../notice/NoticeAttachmentServiceTest.java` — 상한 · 확장자 · MIME 검증
- [ ] `docs/design-contracts/admin/notice-management.md` — 계약 문서
- [ ] `e2e/contracts/admin-notice.ts` — 기계 계약 (헤더 · 목록 컬럼 · 첨부 UI 개수)
- [ ] `e2e/tests/admin-notice-upload.spec.ts` — 기능 E2E (업로드 → 목록 노출 → 삭제 → 미노출 왕복)

### 수정
- [ ] `application.yml` — `spring.servlet.multipart` 설정 (§2-3)
- [ ] `src/main/resources/templates/admin/fragments/header.html` — GNB "공지사항 관리" 링크 활성화 (A1 에서 placeholder 였다면)
- [ ] `SecurityConfig` — CSRF 토큰이 multipart form 에도 전달되는지 확인. 필요 시 `hx-headers` (HTMX) 설정. (A1 이 CSRF 활성 이후이므로 신규 form 은 반드시 `_csrf` hidden 포함)

---

## 7. 검증 시나리오

### 7-1. 정적 검증
- `./gradlew compileJava`
- `./gradlew test --tests AdminNoticeControllerTest` — MockMvc + `@WithMockUser(roles="SYSTEM_ADMIN")`
- `./gradlew test --tests NoticeAttachmentServiceTest` — 상한(5MB+1B) 400 · 확장자 화이트리스트 위반 400 · MIME 불일치 400 · 정상 케이스 저장 확인
- `./gradlew test --tests AdminNoticeRenderTest` — Thymeleaf 실 렌더 · 잔존 표현식 0건

### 7-2. 동적 검증 (curl)
- `preview_start(name: "youth-moa-e2e")` 로 bootRun (H2)
- `POST /admin/notices/1/attachments` multipart with sample.pdf (< 5MB) → 302 redirect (또는 200 fragment 반환 시 HTMX swap)
- `GET /notices/1` → 첨부 목록에 방금 업로드한 파일 노출
- `GET /notices/1/attachments/{aid}/download` → 200 OK + bytes 일치
- `DELETE /admin/notices/1/attachments/{aid}` → 204/302 + 재조회 시 미노출
- **RBAC**: USER 로그인 세션으로 위 admin endpoint 호출 → 403/302

### 7-3. 인터랙션 검증 (기능 E2E)
- `admin-notice-upload.spec.ts` (chromium project) — CSRF · multipart form · DOM 변화 · HTMX swap 시나리오
  - Playwright `page.setInputFiles()` 로 multipart 업로드
  - 업로드 후 첨부 목록 fragment 에 파일명 노출 확인
  - 삭제 버튼 → confirm 모달 → 확인 → 목록 미노출 확인

### 7-4. 계약 검사
- `npx playwright test --project=contracts` — admin-notice 계약 갭 0 확인

### 7-5. 시각 확인 (사용자)
- admin POLICY 톤 (다크 헤더 · 인디고 · 존댓말) 정합
- 업로드 실패 메시지 표시 (5MB 초과 · 확장자 위반) 시 색·라벨

---

## 8. 결정 필요 항목 (Qn)

원샷 결정용. 각 항목 권장안 있음.

| # | 질문 | 옵션 | 권장 |
|---|---|---|---|
| **Qn-1** | 파일 저장소 이번 스코프 | A: 인터페이스 신설 + `DbLobFileStorage` 만 (Supabase 는 이월) / B: 인터페이스 없이 서비스 계층에 직접 저장 / C: Supabase Storage 구현체까지 이번에 포함 | **A** — 인프라 학습 목표 + 이월 안전 |
| **Qn-2** | 파일 크기 상한 | A: 5MB (F-notice-attachment 승계) / B: 10MB (ADMIN-00 §P0-3 예시) | **A** — 정책 일관성. `max-request-size = 6MB` |
| **Qn-3** | 확장자 화이트리스트 | A: pdf/hwp/docx/xlsx (승계) / B: + jpg/png (이미지 추가) | **A** — 승계. 이미지는 공지 본문 imageUrl 로 별도 |
| **Qn-4** | MIME 검증 방식 | A: 확장자 + Content-Type 헤더 매칭 / B: magic number(Apache Tika) / C: 확장자만 | **A** — 학습 단계 적정. B 는 오버스펙 |
| **Qn-5** | 신규 공지 작성/수정/삭제 CRUD | A: 이번 스코프 (전체 CRUD) / B: 첨부 관리만 (기존 공지 편집만 · 신규 등록 UI 없음) | **B** — 스코프 격리. 신규 공지 CRUD 는 별도 티켓 `A-admin-notice-crud` |
| **Qn-6** | Supabase Storage 클라이언트 (참고용 — Qn-1=A 채택 시 이번 티켓 미해당) | A: Supabase Java SDK / B: REST + OkHttp | (이월 티켓에서 결정) |
| **Qn-7** | 업로드 후 응답 방식 | A: HTMX fragment 반환 + `hx-swap="outerHTML"` (부분 갱신) / B: PRG 패턴 302 redirect | **A** — admin UX 자연스러움 · CLAUDE.md 인터랙션 검증 규칙과 자연 정합 |
| **Qn-8** | CENTER_ADMIN 접근 정책 | A: Notice.center FK 추가 · CENTER_ADMIN 자기 센터만 / B: SYSTEM_ADMIN 만 관리 · CENTER_ADMIN 403 / C: 전 관리자 공유 | **B** — 마이그레이션 없음, Notice 는 전역 공지 성격, admin 파일럿 적정 |
| **Qn-9** | HTMX 삭제 confirm | A: 커스텀 admin 공통 confirm 모달 (A1 도입 예정 · 확인 필요) / B: 브라우저 `confirm()` 임시 | **A** — POLICY 준수 (HANDOFF: 파괴적 액션 1단계 커스텀 모달) |

---

## 8-후속. Qn-8 Custom (작성자 기반 RBAC) — 사용자 확정 (2026-09-03)

사용자 트랙 Notice 는 원래 전역 공지 (센터 소유 없음). 이 티켓에서 **작성자(User) 기반 RBAC** 도입.

### 매트릭스

| 액션 | SYSTEM_ADMIN | CENTER_ADMIN |
|---|---|---|
| Create | ✅ 모두 | ✅ 모두 |
| Read | ✅ 모두 | ✅ 모두 |
| Update | ✅ 모두 | ⚠️ **본인 작성 공지만** |
| Delete | ✅ 모두 | ⚠️ **본인 작성 공지만** |

### 구현 요구

- **엔티티**: `Notice.createdBy: User` (`@ManyToOne`, NOT NULL) 신규 필드
- **마이그레이션 V*.sql** (신규):
  1. `ALTER TABLE notice ADD COLUMN created_by BIGINT REFERENCES users(id)`
  2. `UPDATE notice SET created_by = (SELECT id FROM users WHERE email = 'sysadmin@youth-moa.test')` — 기존 시드 공지 전량 백필
  3. `ALTER TABLE notice ALTER COLUMN created_by SET NOT NULL`
- **RBAC 검증 로직**: Update/Delete 진입 시 SYSTEM_ADMIN 아니면 `notice.createdBy.id == currentUser.id` 확인, 아니면 403
- **DataInitializer**: sysadmin 계정 (`sysadmin@youth-moa.test`) 이 Notice 시드 생성 시점에 이미 존재해야 함 (A1 에서 이미 seed order 상단으로 이동 완료)

### 파생 결정 (사용자 확정)

- **파생 Q1**: 기존 시드 공지 백필 → **sysadmin 계정 소유로 백필** (createdBy NOT NULL 유지, null 미허용)
- **파생 Q2**: Supabase Storage 클라이언트 → **B (REST + OkHttp)** — 커뮤니티 SDK 대신 표준 REST API 직접 호출 (학습 목적 + 의존성 최소)

## 8-후속. Qn-1 = C (Supabase Storage 구현체 포함)

`FileStorage` 인터페이스 + 두 구현체 병행:

| 프로파일 | 구현체 | 저장 위치 |
|---|---|---|
| `dev`, `local`, `e2e`, `test-guard` | `LocalFileStorage` (또는 `DbLobFileStorage`) | 로컬 fs `./storage/notice-attachments/{id}/{filename}` |
| `prod` | `SupabaseFileStorage` | Supabase Storage bucket `notice-attachments` |

**REST 사양**:
- Upload: `POST ${SUPABASE_URL}/storage/v1/object/notice-attachments/{path}` (multipart)
- Download: `GET ${SUPABASE_URL}/storage/v1/object/notice-attachments/{path}` (Bearer)
- Delete: `DELETE ${SUPABASE_URL}/storage/v1/object/notice-attachments/{path}` (Bearer)
- Auth: `Authorization: Bearer ${SUPABASE_SERVICE_ROLE_KEY}` (env)
- 버킷 초기화: 앱 부팅 시 존재 확인 + 없으면 REST `POST /storage/v1/bucket` 로 생성 (idempotent)

**환경변수** (application.yml + application-prod.yml):
- `SUPABASE_URL` (기존 재활용)
- `SUPABASE_SERVICE_ROLE_KEY` (신규, secret)

## 8-후속. Qn-5 = A (신규 공지 CRUD 포함)

이번 스코프에 **관리자 공지 전체 CRUD** 포함:

- `/admin/notices` — 관리자 공지 목록 (검색·페이징)
- `/admin/notices/new` — 신규 작성 폼
- `POST /admin/notices` — Create
- `/admin/notices/{id}` — 편집 폼 + 첨부 관리 (기존 계획)
- `POST /admin/notices/{id}` — Update
- `DELETE /admin/notices/{id}` — Delete
- 사용자 헤더 fragment 및 admin GNB 에 "공지 관리" 링크 추가

## 스코프 확대 반영 (사용자 결정 후)

Qn-1=C + Qn-5=A + Qn-8=Custom 조합으로 원래 예상보다 대폭 확대. 다음 섹션 §9 은 이 확대분 반영한 규모 재추정.

---

## 9. 스코프 예상 규모 (수정)

| 항목 | 예상 |
|---|---|
| 신규 파일 | ~28개 (java 10 + template 5 + migration 1 + test 6 + contract 4 + doc 2) |
| 수정 파일 | ~6개 (application.yml/-prod.yml, header/auth-header fragment, SecurityConfig, User/Notice 관련) |
| PR 라인 수 | +2500 ~ +3500 (Supabase 클라이언트 + CRUD 4화면 + RBAC + 계약·테스트) |
| assertion 개수 | 정적 25 (controller + service + storage) + 계약 40 + E2E 시나리오 8 |
| 예상 세션 | 2~3 (impl 1~2 · qa/verify 1) |
| 리스크 | 중 — Supabase Storage 실 연동 셋업 (bucket 생성·auth) + createdBy 마이그레이션 백필 정확성 필요. **prod SUPABASE_SERVICE_ROLE_KEY env 배포 시점에 준비 필수** |

---

## 10. deferred / deviation 후보

| 유형 | 항목 | 사유 |
|---|---|---|
| deferred | Supabase Storage 구현체 | 배포 준비 시점 티켓 `refactor/notice-attachment-supabase-storage` |
| deferred | 신규 공지 CRUD 전체 | 별도 티켓 `A-admin-notice-crud` |
| deferred | 업로더 감사 로그 (uploaded_by_user_id) | 감사 트랙에서 일괄 처리 |
| deferred | 첨부 sortOrder drag reorder | UX 확장 티켓 |
| deferred | Notice.center FK (Qn-8=A 채택 시) | 이번 티켓 권장은 B, A 채택 시 마이그레이션 별도 |
| deviation | magic number MIME 검증 미도입 | 학습 단계 오버스펙 (Qn-4=A) |

---

## 11. 인계

Qn-1~9 결정 후 `ym-impl` 인계. 브랜치 `feature/admin-notice-attachment`.

---

## 12. 구현 매핑 (2026-09-03 · ym-impl)

### 명세 §6 변경 범위 → 파일

| 명세 항목 | 구현 파일 |
|---|---|
| Notice.createdBy 신설 | `src/main/java/.../notice/Notice.java` (필드 + Builder) |
| V9 마이그레이션 (3단계 백필) | `src/main/resources/db/migration/V9__add_notice_created_by.sql` |
| DataInitializer 시드 순서 (admins → notices) + createdBy 부착 | `src/main/java/.../common/DataInitializer.java` |
| `FileStorage` 인터페이스 | `src/main/java/.../common/storage/FileStorage.java` |
| `StoredFile` record | `src/main/java/.../common/storage/StoredFile.java` |
| `LocalFileStorage` (@Profile !prod) | `src/main/java/.../common/storage/LocalFileStorage.java` |
| `SupabaseFileStorage` (@Profile prod, REST + OkHttp) | `src/main/java/.../common/storage/SupabaseFileStorage.java` |
| multipart 5MB / 6MB 설정 | `src/main/resources/application.yml:36-42` |
| storage backend 설정 | 동일 파일 `youthmoa.storage.*` |
| OkHttp 의존성 | `build.gradle.kts` (com.squareup.okhttp3:okhttp:4.12.0) |
| `AdminNoticeService` (RBAC + CRUD + 업로드 검증) | `src/main/java/.../admin/AdminNoticeService.java` |
| `AdminNoticeController` (7 endpoint) | `src/main/java/.../admin/AdminNoticeController.java` |
| 목록 화면 | `src/main/resources/templates/admin/notice/list.html` |
| 신규/편집 폼 (mode 분기) | `src/main/resources/templates/admin/notice/form.html` |
| 첨부 fragment (HTMX target) | `src/main/resources/templates/admin/notice/_attachments-fragment.html` |
| GNB "공지 관리" 링크 | `src/main/resources/templates/admin/fragments/header.html:44-45` |
| CSS (버튼 · 폼 · 첨부 · 모달) | `src/main/resources/static/css/admin.css` (append) |
| 계약 문서 | `docs/design-contracts/admin/notice-management.md` |
| 계약 스펙 (list/form/edit) | `e2e/contracts/admin-notice.ts` |

### 테스트

| 대상 | 위치 | 통과 |
|---|---|---|
| `AdminNoticeServiceTest` (RBAC 5 + 업로드 검증 8) | `src/test/java/.../admin/AdminNoticeServiceTest.java` | ✅ |
| `LocalFileStorageTest` (round-trip 5) | `src/test/java/.../common/storage/LocalFileStorageTest.java` | ✅ |
| 기존 `NoticeAttachmentRepositoryTest` (createdBy 추가 반영) | 갱신 후 ✅ | ✅ |
| 기존 `JpaMappingTest` (createdBy 반영) | 갱신 후 ✅ | ✅ |
| 기존 `HomeServiceTest` (createdBy 반영) | 갱신 후 ✅ | ✅ |

### RBAC 매트릭스 (§8-후속) 이행

| 액션 | SYSTEM_ADMIN | CENTER_ADMIN 본인 | CENTER_ADMIN 타인 | 구현 |
|---|---|---|---|---|
| Create | ✅ | ✅ | ✅ | Controller `create()` |
| Read (목록·편집 폼) | ✅ | ✅ | ✅ | `list()`, `editForm()` |
| Update | ✅ | ✅ | ❌ 403 | Service `assertCanEdit()` |
| Delete | ✅ | ✅ | ❌ 403 | 동일 |
| 첨부 업로드/삭제 | ✅ | ✅ | ❌ 403 | 동일 |

### 미완 / 후속

- **동적 검증**: bootRun 8091 curl round-trip · 계약 스캔 (`--project=contracts admin-notice-*`) · 기능 E2E (`admin-notice-*.spec.ts` 신설) 는 ym-qa 세션에서 진행
- **prod SUPABASE_SERVICE_ROLE_KEY**: fly.toml secret 등록은 배포 세션에서
- **A3 프로그램 첨부·썸네일** 은 동일 `FileStorage` 인터페이스 재활용 예정
