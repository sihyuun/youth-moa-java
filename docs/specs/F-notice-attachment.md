# F-notice-attachment — 공지사항 첨부파일 다운로드 (사용자 사이드)

> 상태: `impl_done` (2026-09-03 갱신 — 사용자 사이드 다운로드 PR #130. admin 업로드 UI 는 후속 티켓)
> 브랜치 후보: `feature/notice-attachment`
> 산출: 2026-07-31 (ym-spec)
> 선행: 없음 (V3 다음 V4 신설)
> 후속: admin 트랙에서 업로드 UI (`POST /admin/notices/{id}/attachments`) 별도 PR

---

## 1. 배경

`docs/wireframe-policy/notice.md` WF-6-002 공지사항 상세 정책:

- **첨부파일 있을 경우, 공지사항 하단에 파일명 노출**
- **파일명 클릭 시 다운로드**
- 업로드 UI 언급 없음 (admin 트랙에서 처리)

현재 상태 (2026-07-31 실측):

| 항목 | 현재 |
|---|---|
| `Notice` 엔티티 | 첨부 필드 0개 (본문 `@Lob content` 만) |
| `NoticeAttachment` 엔티티 | **이미 존재** (F0g). 단 `data` (byte[]) 필드는 **없음** — 메타데이터만 |
| `NoticeAttachmentRepository` | 존재. `findByNoticeIdOrderBySortOrderAscIdAsc(Long)` |
| `notice_attachment` 테이블 V 파일 | **부재** — 엔티티만 존재하고 마이그레이션은 누락 상태 |
| detail.html 첨부 목록 | 이미 렌더링. 클릭 시 `alert('첨부파일 다운로드는 준비 중입니다.')` stub |
| 스토리지 인프라 | 미도입 |
| SecurityConfig | `/notices/**` permitAll |
| DB 최신 마이그레이션 | V3 (terms + user_agreements) |
| 시드 첨부 | 이미 4건 존재 (앞 3개 공지에 1~2건씩) |

이번 PR 은 stub 을 걷어내고 **DB `@Lob bytea` 로 실제 바이트를 저장·다운로드**하도록 완성한다. 업로드 UI 는 스코프 밖.

---

## 1-A. 확정된 정책 결정 (2026-07-31 사용자 확정)

| # | 항목 | 결정 |
|---|---|---|
| D1 | 스토리지 | **DB `@Lob byte[]` (PG bytea)** — 학습 단계 인프라 결정 유예. 배포 시 Supabase Storage 로 이관 예정 |
| D2 | 업로드 스코프 | 이번 PR 은 **다운로드만**. 업로드 UI 는 admin 트랙 별도 PR |
| D3 | 첨부 개수 | `Notice` 1 : N `NoticeAttachment` (단방향 `@ManyToOne`, CLAUDE.md 엔티티 규칙) |
| D4 | 파일 크기 상한 | **5MB** (`5 * 1024 * 1024` bytes) |
| D5 | 허용 확장자 | `pdf`, `hwp`, `docx`, `xlsx` |
| D6 | 시드 첨부 실제 바이트 | 시드 1건 이상은 **실 바이트로 교체**하여 다운로드 왕복 검증 가능하게 함 (기존 4건 중 최소 1건 리소스 파일 로드) |
| D7 | 다운로드 경로 | `GET /notices/{noticeId}/attachments/{attachmentId}/download` |
| D8 | 다른 notice 소속 attachment 접근 | 404 (경로 검증 필수) |
| D9 | 다운로드 인증 | 불필요 (공지사항은 public — 현행 SecurityConfig 유지) |

**이월 (파생 큐)**

| 작업 | 티켓 후보 | 내용 |
|---|---|---|
| admin 업로드 UI | `feature/admin-notice-attachment-upload` | `POST /admin/notices/{id}/attachments` (multipart) — 5MB·확장자 서버 검증 |
| Supabase Storage 이관 | `refactor/notice-attachment-storage` | 배포 단계 진입 시 `@Lob` → 스토리지 URL 로 마이그레이션 |

---

## 2. 디자인 출처

| 자산 | 위치 |
|---|---|
| wireframe 정책 | `docs/wireframe-policy/notice.md` WF-6-002 "[첨부파일]" 절 |
| prototype | 사용자 사이드 첨부 다운로드는 prototype 에 별도 컴포넌트 없음 — wireframe 정책 텍스트가 유일한 출처 |
| 현재 렌더 | `src/main/resources/templates/notice/detail.html:46~56` (이미 목록 렌더 중, 클릭 stub) |

## 2-A. 자산 간 갭

wireframe 정책만 존재 (prototype 미포함) → 임의 결정 사항 (파일 크기·확장자·경로) 는 §1-A 에 사용자 확정으로 고정.

## 2-B. 데이터 모델 gap 표

| 필드 (필요) | 현재 `NoticeAttachment` | 조치 |
|---|---|---|
| id | ✅ Long IDENTITY | 유지 |
| notice (ManyToOne LAZY) | ✅ | 유지 |
| fileName (VARCHAR 255) | ✅ `fileName` | 유지. spec 원안의 `filename` (VARCHAR 200) 대신 **현행 필드명·길이 유지** — 이미 컬럼 있음, 재명명 시 마이그레이션 복잡도 증가 |
| contentType (VARCHAR 100) | ✅ `contentType` | 유지 |
| fileSize (BIGINT) | ✅ `fileSize` | 유지. spec 원안의 `size` 대신 현행 이름 유지 (Java 예약어 아님, 명시성 우수) |
| **data (@Lob byte[])** | ❌ 없음 | **신규 추가** — 실 바이트 저장 |
| uploadedAt (LocalDateTime) | ✅ `createdAt` (@CreatedDate) | 유지. spec 원안의 `uploadedAt` 은 의미 동일 → 현행 이름 유지 |
| storedName | ✅ (legacy) | 유지 — 향후 Supabase Storage 이관 시 오브젝트 key 저장용. 이번 PR 은 read-only |
| sortOrder | ✅ | 유지 |

**핵심 신설 필드는 `data` (byte[]) 1개**. 다른 필드는 이미 존재하므로 재명명 대신 현행 유지 (마이그레이션·기존 시드 코드 영향 최소화).

## 2-C. 데이터 소비 지점

| 소비 지점 | 현재 | 이번 PR 조치 |
|---|---|---|
| `notice/detail.html` 첨부 목록 렌더 | fileName + humanFileSize 노출, 클릭 시 alert stub | 클릭 링크를 `/notices/{nid}/attachments/{aid}/download` 로 교체 |
| `NoticeService.findAttachments(Long)` | 이미 구현 | 유지 |
| admin 화면 | 없음 | 이번 스코프 외 (파생 큐) |
| home 카드 등 다른 화면의 첨부 노출 | 없음 | — |

## 2-D. write → read 왕복

이번 PR 은 사용자 사이드 read (다운로드) 만. write (업로드) 는 admin 트랙 이월.
대신 **시드 → 다운로드 왕복** 을 통합 시나리오로 검증 (D6, §5 참조).

---

## 3. 데이터 모델

### 3-1. `NoticeAttachment` 엔티티 (수정)

기존 `NoticeAttachment.java` 에 아래 **필드 1개만 추가**:

```java
@Lob
@Column(nullable = true)
private byte[] data;
```

- `nullable = true`: 기존 시드 4건 중 실 바이트를 넣지 않는 항목은 null 유지 가능 (호환). 다운로드 컨트롤러는 `data == null` 이면 404.
- Builder 파라미터 추가: `byte[] data`.

정책 상수는 **엔티티가 아닌** 서비스 계층 (`NoticeAttachmentPolicy` 또는 `NoticeService`) 에 정의:

```java
public static final long MAX_ATTACHMENT_SIZE_BYTES = 5L * 1024 * 1024; // 5MB
public static final java.util.Set<String> ALLOWED_EXTENSIONS =
    java.util.Set.of("pdf", "hwp", "docx", "xlsx");
```

> 이 상수는 이번 PR 에서는 **참조처가 서비스 계층 유닛 테스트 뿐** (admin 업로드 컨트롤러가 없으므로). admin 트랙에서 실 검증 로직이 붙는다.

### 3-2. `Notice` 엔티티

**변경 없음.** `@OneToMany` 컬렉션 도입 금지 (CLAUDE.md 규칙 — 단방향 유지). 조회는 `NoticeAttachmentRepository.findByNoticeIdOrderBySortOrderAscIdAsc(Long)`.

### 3-3. `NoticeAttachmentRepository`

기존 메서드에 **다운로드 조회용 1건 추가**:

```java
Optional<NoticeAttachment> findByIdAndNoticeId(Long id, Long noticeId);
```

- 다른 notice 소속 attachment 접근 → `Optional.empty()` → 404 매핑 (D8).

---

## 4. 마이그레이션 — `V4__add_notice_attachment_and_data.sql`

### 4-1. 배경

`notice_attachment` 테이블 자체가 V 파일에 등록되지 않은 상태 (엔티티만 있고 baseline V1 이후 추가된 스키마는 V 파일 부재). 이번에 **테이블 정식 등록 + `data` 컬럼 추가** 를 한 V 파일에서 처리.

### 4-2. SQL

```sql
-- F-notice-attachment: 공지사항 첨부파일 실 바이트 저장 도입.
--
-- 배경:
--   1) NoticeAttachment 엔티티는 F0g 에서 신설됐으나 V 파일 등록 누락 상태였다.
--      baseline(V1) 스냅샷에도 포함되지 않아 신규 환경에서 부팅 시 validate 실패 위험.
--   2) 사용자 사이드 다운로드 도입에 맞춰 @Lob byte[] data 컬럼을 추가한다.
--
-- 스토리지 결정: 학습 단계 DB bytea. 배포 시 Supabase Storage 로 이관 예정.
-- 상한: 5MB (서비스 계층 정책 상수 — DB 제약 아님).
--
-- 한 번 머지된 V 파일은 수정 금지 (CLAUDE.md DB 규칙).

CREATE TABLE IF NOT EXISTS notice_attachment (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    notice_id bigint NOT NULL,
    file_name varchar(255) NOT NULL,
    stored_name varchar(255),
    file_size bigint NOT NULL,
    content_type varchar(100),
    sort_order integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT fk_notice_attachment_notice FOREIGN KEY (notice_id) REFERENCES notice(id)
);

CREATE INDEX IF NOT EXISTS idx_notice_attachment_notice
    ON notice_attachment(notice_id, sort_order, id);

-- 실 바이트 저장 컬럼. NULL 허용 (legacy 시드 호환).
ALTER TABLE notice_attachment
    ADD COLUMN IF NOT EXISTS data bytea;
```

> `IF NOT EXISTS` 사용: baseline 이후 개발 환경에서 ddl-auto=update 로 이미 테이블이 만들어져 있을 수 있음. 신규 환경에서는 정식 생성.

### 4-3. 검증

- `YouthMoaApplicationTests` (Testcontainers) 가 빈 PG 에 V1~V4 적용 후 validate 통과 확인
- e2e / @DataJpaTest 프로파일 (H2 `create-drop`) 은 V 파일 무시하고 엔티티로 스키마 생성 — `data` 컬럼 매핑 동작 확인

---

## 5. 컨트롤러

### 5-1. 다운로드 엔드포인트

`NoticeController` 에 신규 메서드 추가 (또는 `NoticeAttachmentController` 분리 — 권장):

```java
@GetMapping("/notices/{noticeId}/attachments/{attachmentId}/download")
public ResponseEntity<byte[]> download(
    @PathVariable Long noticeId,
    @PathVariable Long attachmentId) {
  NoticeAttachment att = noticeService.findAttachmentForDownload(noticeId, attachmentId);
  // att.data == null 인 legacy 시드는 서비스에서 IllegalStateException → 404
  String encoded = URLEncoder.encode(att.getFileName(), StandardCharsets.UTF_8)
      .replace("+", "%20");
  return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION,
          "attachment; filename*=UTF-8''" + encoded)
      .header(HttpHeaders.CONTENT_TYPE,
          att.getContentType() != null ? att.getContentType() : "application/octet-stream")
      .contentLength(att.getFileSize())
      .body(att.getData());
}
```

- 실패 → `ResponseStatusException(NOT_FOUND)` (기존 `detail()` 패턴과 동일)

### 5-2. 서비스

`NoticeService.findAttachmentForDownload(Long noticeId, Long attachmentId)`:

- `findByIdAndNoticeId` 로 조회 → 없거나 다른 notice 소속이면 `IllegalArgumentException` → 404 매핑
- `data == null` 이면 `IllegalStateException` → 404 (또는 410 Gone 도 고려 가능. 이번 PR 은 404 로 통일)

### 5-3. detail.html 수정

기존 stub (`onclick="alert(...)"`) 을 실 링크로 교체:

```html
<div th:if="${!attachments.isEmpty()}" class="notice-attachments">
    <a th:each="a : ${attachments}" class="notice-attachment"
       th:href="@{/notices/{nid}/attachments/{aid}/download(nid=${notice.id}, aid=${a.id})}">
        <span class="notice-attachment-icon">⬇</span>
        <span class="notice-attachment-name" th:text="${a.fileName}">파일명.pdf</span>
        <span class="notice-attachment-size" th:text="${a.humanFileSize}">1.2MB</span>
    </a>
</div>
```

- `role="button" tabindex="0" onclick/onkeydown` 제거 → 순수 `<a>` 로 시맨틱 정합. `download` 속성은 붙이지 않는다 (서버가 `Content-Disposition: attachment` 로 강제)

### 5-4. SecurityConfig

**변경 없음.** `/notices/**` permitAll 이 다운로드 경로도 커버 (D9).

---

## 6. 시드 — DataInitializer.seedNotices()

### 6-1. 실 바이트 시드 1건 (D6)

`src/main/resources/data/notice-attachments/sample.pdf` (신규 리소스) 를 클래스패스로 로드해 첫 시드 첨부에 주입:

```java
byte[] sampleBytes;
try (var is = new ClassPathResource("data/notice-attachments/sample.pdf").getInputStream()) {
  sampleBytes = is.readAllBytes();
}

attachments.add(
    NoticeAttachment.builder()
        .notice(notices.get(0))
        .fileName("청년의날_축제_안내문.pdf")
        .storedName("dummy-1.pdf")
        .fileSize(sampleBytes.length)     // 실 크기로 교체 (기존 1_258_291L → 실 파일 크기)
        .contentType("application/pdf")
        .sortOrder(0)
        .data(sampleBytes)
        .build());
```

- 나머지 3건 (hwp/pdf/xlsx) 은 `data = null` 유지 → 클릭 시 404 (프론트는 링크 렌더, 실 다운로드 실패는 학습 단계 허용)
- 대안: 4건 모두 실 바이트 채우기. 리소스 파일이 커지므로 **최소 1건만 실 바이트, 나머지는 stub 유지**로 결정 (§1-A D6)

### 6-2. 리소스 파일

- `src/main/resources/data/notice-attachments/sample.pdf` — 임의의 작은 (< 100KB) PDF. 저작권 없는 자체 생성 PDF 또는 텍스트만 든 최소 PDF 사용. 실 사용자 표시 파일명(`청년의날_축제_안내문.pdf`) 과 별개
- 크기 상한 (5MB) 훨씬 아래로 유지 → git 저장소 부담 없음

---

## 7. 변경 범위 (파일 단위)

- [ ] `src/main/resources/db/migration/V4__add_notice_attachment_and_data.sql` — **신규**
- [ ] `src/main/java/io/github/sihyuuun/youthmoa/notice/NoticeAttachment.java` — `data` 필드 + Builder 파라미터 추가
- [ ] `src/main/java/io/github/sihyuuun/youthmoa/notice/NoticeAttachmentRepository.java` — `findByIdAndNoticeId` 추가
- [ ] `src/main/java/io/github/sihyuuun/youthmoa/notice/NoticeService.java` — `findAttachmentForDownload` + 정책 상수 (MAX_ATTACHMENT_SIZE_BYTES, ALLOWED_EXTENSIONS)
- [ ] `src/main/java/io/github/sihyuuun/youthmoa/notice/NoticeController.java` (또는 `NoticeAttachmentController.java` 신설) — `GET .../download`
- [ ] `src/main/resources/templates/notice/detail.html` — stub → 실 링크
- [ ] `src/main/java/io/github/sihyuuun/youthmoa/common/DataInitializer.java` — 시드 첨부 1건에 실 바이트 주입
- [ ] `src/main/resources/data/notice-attachments/sample.pdf` — **신규 리소스**
- [ ] `src/test/java/io/github/sihyuuun/youthmoa/notice/NoticeControllerTest.java` — 다운로드 TC 추가 (또는 `NoticeAttachmentDownloadTest` 신설)

---

## 8. 검증 시나리오

### 8-1. 정적 검증

- `./gradlew compileJava` 통과
- `./gradlew test --tests JpaMappingTest` — `NoticeAttachment.data` 매핑 검증
- `./gradlew test --tests YouthMoaApplicationTests` — Testcontainers 로 V1~V4 마이그레이션 + validate 통과
- `./gradlew test --tests NoticeControllerTest` — 아래 동적 TC 포함
- `./gradlew test --tests NoticeAttachmentDownloadTest` (신설 시)

### 8-2. 단위 테스트 (MockMvc / @DataJpaTest)

정책 강제:

| # | 시나리오 | 기대 |
|---|---|---|
| TC-1 | 서비스 계층 상한 상수 (5MB) 존재·값 검증 | `NoticeService.MAX_ATTACHMENT_SIZE_BYTES == 5 * 1024 * 1024` |
| TC-2 | 허용 확장자 상수 검증 | `Set.of("pdf","hwp","docx","xlsx")` |
| TC-3 | `findByIdAndNoticeId` — 정상 조회 | Optional 값 존재 |
| TC-4 | `findByIdAndNoticeId` — 다른 notice 소속 attachmentId 조회 | Optional.empty |

**주의**: 5MB 초과·비허용 확장자 검증은 admin 업로드 컨트롤러가 담당. 이번 PR 은 상수 존재·값만 확인 (서비스에 write 경로 없음).

### 8-3. 동적 검증 (curl / MockMvc)

시드 실 바이트 첨부 (attachments[0]) 대상:

| # | 요청 | 기대 |
|---|---|---|
| DC-1 | `GET /notices/1/attachments/1/download` | 200 OK |
| DC-2 | 위 응답 헤더 `Content-Disposition` | `attachment; filename*=UTF-8''<encoded 청년의날_축제_안내문.pdf>` |
| DC-3 | 위 응답 헤더 `Content-Type` | `application/pdf` |
| DC-4 | 위 응답 헤더 `Content-Length` | 시드된 실 파일 크기와 일치 |
| DC-5 | 위 응답 바디 크기 | `Content-Length` 와 일치 (파일 무결성) |
| DC-6 | `GET /notices/2/attachments/1/download` (attachment 1 은 notice 1 소속) | 404 |
| DC-7 | `GET /notices/999/attachments/999/download` | 404 |
| DC-8 | `GET /notices/1/attachments/2/download` (data==null legacy 시드) | 404 |
| DC-9 | 정적 리소스 회귀: `GET /notices/1` HTML 응답에 `href="/notices/1/attachments/1/download"` 포함 | 렌더 확인 |

**bootRun 로컬 검증 (회사 PC — e2e 프로파일 8090)**:

```bash
curl -s -o /dev/null -w "%{http_code} %{size_download}\n" \
  http://localhost:8090/notices/1/attachments/1/download

curl -sI http://localhost:8090/notices/1/attachments/1/download | \
  grep -iE "content-(disposition|type|length)"
```

### 8-4. 시각 검증 (사용자 영역)

- 브라우저에서 `/notices/1` 접속 → 첨부 목록 표시
- 파일명 클릭 시 브라우저 다운로드 다이얼로그 또는 자동 다운로드
- 다운로드된 파일 열기 → 정상 PDF (시드 리소스 내용)
- 다른 3건 (data==null) 클릭 시 404 페이지 (학습 단계 허용, admin 트랙에서 실 바이트 채워지면 해소)

---

## 9. admin 연계

- 본 스펙은 admin 트랙의 **선행 조건**만 처리:
  - 엔티티 `data` 필드
  - 마이그레이션 V4
  - 정책 상수 (5MB · 확장자 화이트리스트)
  - 다운로드 컨트롤러
  - 리포지토리 조회 메서드
- admin 트랙에서 신설할 항목:
  - `POST /admin/notices/{id}/attachments` (multipart)
  - 5MB 초과 → 413 or 400
  - 비허용 확장자 → 400
  - `DELETE /admin/notices/{nid}/attachments/{aid}`
  - admin prototype 화면 매핑 확인

---

## 10. 의존성 / 선행 작업

- 없음 (V3 다음 V4 신설, 코드 변경 파일 상호 독립)
- **주의**: 다른 브랜치가 V4 를 먼저 선점하면 rebase 후 V5 로 재부여 (CLAUDE.md 마이그레이션 규칙)

---

## 11. 작업 큐 메타

- 작업 ID: `F-notice-attachment`
- 우선순위: 중 (wireframe 정책 반영 · admin 트랙 선행)
- 추정 단위: 1 PR
- 상태: `spec_confirmed`
- 브랜치 후보: `feature/notice-attachment`
- 커밋 컨벤션 예: `260731_notice_attachment - 공지 첨부 다운로드 도입`

---

## 12. 리스크 / 결정 근거

| 리스크 | 완화 |
|---|---|
| `@Lob bytea` 로 대용량 저장 시 DB 부담 | 5MB 상한 (D4) + 배포 시 Supabase Storage 이관 (D1) |
| 기존 시드 3건 `data==null` → 사용자가 클릭 시 404 | 학습 단계 허용. admin 트랙에서 실 바이트 업로드로 해소 |
| `notice_attachment` V 파일 부재로 기존 개발 환경 이미 update 로 테이블 생성됨 | `CREATE TABLE IF NOT EXISTS` + `ADD COLUMN IF NOT EXISTS` 로 idempotent |
| filename URL 인코딩 (한글 파일명) | `filename*=UTF-8''` RFC 5987 표기 준수 |
| `@Lob` byte[] 지연 로딩 | Hibernate 기본은 즉시 로딩. 상세 페이지에서 목록 조회 시 `data` 도 함께 로드되지 않도록 **DTO projection 또는 별도 조회 메서드 분리 검토** — 이번 PR 은 다운로드 경로에서만 엔티티 전체 로드하므로 목록 조회는 영향 없음 (`findByNoticeIdOrderBySortOrderAscIdAsc` 는 상세 렌더 시 호출되나, `data` 필드는 프록시 로딩 대상이 아닌 즉시 로딩) → **후속 최적화**: `@Basic(fetch=LAZY)` 를 `data` 에 부착하는 방안을 admin 트랙에서 검토 |
