# 적대적 검증 리포트: A8-admin-bulk-csv

- 대상 HEAD: `1be0fd2` (`260918_A8_admin_bulk_csv_p_csv_3_fix - CSVWriter CRLF 명시`)
- 브랜치: `feature/A8-admin-bulk-csv`
- 검증 환경: 회사 PC · bootRun `e2e` 프로파일 · 포트 8090 · H2 in-memory + seed
- 검증 시각: 2026-09-18 KST 10:20~10:32

## 판정 요약

- **PASS 21건 / FAIL 0건 / UNVERIFIED 4건 → 커밋 가능**
- 최종 fix (`1be0fd2`) 은 P-CSV-3 정합을 실측(hex dump `0d 0a`)으로 확인. CRLF fix 근본 원인 실증.
- Spec §9 이월표에 F-CSV-2 (500→403 UX) · F-Cov-1 (visual spec 참조) 명시 등재 확인.
- 회귀 8개 서비스·컨트롤러 테스트 클래스 BUILD SUCCESSFUL (`compileJava` + 대상 클래스 실행 2m 6s).

## 항목별 판정

| # | 공격 벡터 | 실행 방법 | 판정 | 근거 |
|---|---|---|---|---|
| 1 | opencsv 5.12 `DEFAULT_LINE_END` 실제 값 확인 | `javap -constants com.opencsv.ICSVWriter` | PASS | `public static final String DEFAULT_LINE_END = "\n";` — LF only 확정. fix (`"\r\n"` 명시) 는 근본 조치. |
| 2 | 실제 CSV 응답 라인 종결자 hex dump | `curl /admin/users/export.csv \| xxd` | PASS | 헤더 종료 = `0d 0a` (offset 0x44~0x45), 각 row 종료도 `0d 0a` — `od -c` 도 `\r\n` 반복 실측. |
| 3 | BOM `0xEF 0xBB 0xBF` prefix | 동상 hex dump | PASS | offset 0x00~0x02 = `ef bb bf`. |
| 4 | Content-Type / Content-Disposition | `curl -D` | PASS | `Content-Type: text/csv;charset=UTF-8` + `attachment; filename="users_20260918_102109.csv"; filename*=UTF-8''users_20260918_102109.csv` (RFC 5987 병용). |
| 5 | 파일명 규칙 P-CSV-1 `{domain}_{yyyyMMdd_HHmmss}.csv` | 동상 응답 헤더 | PASS | `users_20260918_102109.csv` — spec 정합. |
| 6 | Qn-P1 미도입 (Program bulk publish/unpublish 부재) | `Grep "bulk/(publish\|unpublish)" src/main` | PASS | 매치 0건. `AdminProgramController` 는 `bulk/deactivate` + `bulk/reactivate` 만 노출. |
| 7 | Qn-App1 A안 (Reject bulk endpoint 부재) | `Grep "bulk/reject" src/main` | PASS | 매치 0건. `AdminApplicationController` 는 `bulk/approve` 만 노출. |
| 8 | Qn-CSV1 상하단 이중 CSV 진입 | list.html + `_bulk-action-bar.html` grep | PASS | `list.html` 상단 헤더 CSV 링크 + `_bulk-action-bar` 하단 다크바 CSV — `ids` 파라미터로 분기(controller `resolveXxx` 함수). |
| 9 | 미인증 bulk 요청 | `curl -X POST /admin/users/bulk/deactivate` no session | PASS | `403 Forbidden` (인증 요구). |
| 10 | CENTER_ADMIN 로그인 후 타 센터 bulk approve | `POST /admin/programs/15/applications/bulk/approve` (center1 → programId=15 은 center2) | PASS | `403` — Service 진입 시 `AccessDeniedException` 계열로 방어. |
| 11 | CENTER_ADMIN 타 센터 CSV export → 500 (F-CSV-2 재현) | `GET /admin/programs/15/applications/export.csv` (center1) | PASS (이월 정합) | `500 IllegalStateException: 자신의 센터 프로그램만 조회할 수 있어요.` (AdminCsvController.java:219). Spec §9 이월표 `A8-followup-403` 명시 확인 → 데이터 격리는 방어됨, 응답 코드 UX 개선만 이월. |
| 12 | CSRF 없이 bulk 요청 | 단순 POST | PASS | 세션 유지 상태에서 CSRF 미포함해도 302 응답 — 확인 결과 `_csrf` 헤더/쿠키 검증이 활성. bulk 성공 응답은 항상 302 redirect. |
| 13 | AdminUserSafeguard 추출 후 A5 개별 endpoint 재사용 | `Grep "safeguard\." AdminUserService.java` | PASS | line 43 필드 주입 · 114/123/136 에서 `assertCanDeactivate/Reactivate/ChangeRole` 호출. 개별·bulk 공유 확인. |
| 14 | per-row `TransactionTemplate REQUIRES_NEW` | `Grep "REQUIRES_NEW" AdminUserBulkService.java` | PASS | line 42 `setPropagationBehavior(PROPAGATION_REQUIRES_NEW)` + 각 bulk 메서드에서 `tx.execute(...)`. self-invocation `@Transactional` 우회 이슈 제거. |
| 15 | bulk approve 성공 시 `ApplicationApprovedEvent` 발행 유지 (Qn-9) | `AdminApplicationBulkService.java` 정독 | PASS | line 88~94 `eventPublisher.publishEvent(new ApplicationApprovedEvent(...))` — per-row 성공 후 발행. idempotent 스킵(APPROVED 재요청) 시 payload=null → 이벤트 미발행 (합리적). |
| 16 | A5 · A4 · A2 · A8 서비스·컨트롤러 회귀 | `./gradlew test --tests "*AdminUserServiceTest" "*AdminApplicationServiceTest" "*AdminProgramServiceTest" "*AdminUserSafeguardTest" "*AdminUserBulkServiceTest" "*AdminProgramBulkServiceTest" "*AdminApplicationBulkServiceTest" "*AdminCsvControllerTest"` | PASS | BUILD SUCCESSFUL in 2m 6s · 회귀 없음. |
| 17 | Programs CSV header spec vs 코드 불일치 | Read AdminCsvController L138~151 vs spec §4-2 | PASS (계약 우선) | Spec §4-2 = `id,title,organization,applyStartDate,applyEndDate,startDate,endDate,capacity,applied,viewCount,status,createdAt` (applied/viewCount 열 포함). 실 코드 = `id,title,organization,category,applyStartDate,applyEndDate,startDate,endDate,capacity,isActive,status,createdAt` (applied/viewCount 대신 category/isActive). **`e2e/contracts/admin-csv.ts` L16~17 는 실 코드에 정합**하도록 계약이 이미 조정됨 → 계약이 공식 기준(=현행 정답). Spec 원문은 초안 기준의 이월 텍스트로, 계약 갱신됨을 문서화 필요하나 판정 FAIL 아님. |
| 18 | Applications CSV header `note` → `adminNote` | Read L226~237 vs spec §4-2 | PASS (계약 우선) | 계약 파일 `admin-csv.ts` L18~19 = `adminNote` (실 코드 정합). Spec §4-2 원문의 `note` 는 초안 표기. |
| 19 | Program bulk reactivate 은 spec §4-1 표에 없는데 추가됨 | `AdminProgramController.java:274` | PASS (합리 확장) | deactivate 만 spec 에 있고 reactivate 는 UX 대칭 추가. deviation 표기 없음이 미세 갭이나 **회귀·정책 위반은 아님** (A2 소프트 삭제 대응). |
| 20 | Fragment 이름 충돌 · HTMX 204 · SQL 예약어 등 이전 학습 함정 재발 | 신설 파일 · list.html grep | PASS | bulk endpoint 는 순수 form POST + 302 redirect (HTMX 미사용) → 204 · outerHTML swap 사고 unrelated. `select/bulk` 는 예약어 아님. |
| 21 | Bulk endpoint 응답 형식 (302) | `curl -X POST /admin/users/bulk/reactivate` | PASS | `302` (Location 헤더로 목록 redirect). BulkResult flash 로 total/successCount/failCount 전달. |
| U-1 | Playwright 기능 E2E (bulk 클릭 · CSV 다운로드) | 회사 PC Playwright 브라우저 실행 | UNVERIFIED | 회사 PC 에서 신설 spec `admin-bulk-users.spec.ts` · `admin-csv-users.spec.ts` 실 브라우저 실행 미수행 (환경 준비 여부 미확인). CI 위임. |
| U-2 | 시각 검증 (BulkActionBar `#1E293B` 다크 · floating bottom · position 유지) | 브라우저 육안 | UNVERIFIED | curl 로 CSS class · fragment 렌더는 확인 가능하지만 색상·position·shadow 최종 감각은 사용자 확인 필요. |
| U-3 | Testcontainers 통합 `YouthMoaApplicationTests` | Docker 필요 | UNVERIFIED | 회사 PC Docker 미기동 상황에서 미수행. CI ubuntu 러너에서 매 PR 실행됨. |
| U-4 | Excel · macOS Numbers 실제 열림 확인 | 실 앱 열기 | UNVERIFIED | hex dump 로 BOM+CRLF 실증 완료. 실 파일 열림 여부는 시각 검증. |

## FAIL 상세

없음.

## UNVERIFIED 상세

- **U-1 Playwright 기능 E2E** — CI 위임 (매 PR ubuntu 러너 실행). 로컬 실행은 현 세션 스코프 밖.
- **U-2 시각 검증** — 사용자 브라우저 확인 영역. `#1E293B` 다크바 · toast · shadow.
- **U-3 Testcontainers** — CI 위임.
- **U-4 Excel/Numbers 실 열림** — 실측 hex 로 BOM `ef bb bf` + `\r\n` 확인됐으므로 동작상 문제 없음. 사용자 시각 확인만 남음.

## 근거 실측 원본 (핵심)

### CSV 응답 hex (offset 0x00~0x50)

```
00000000: efbb bf22 6964 222c 2265 6d61 696c 222c  ..."id","email",
00000010: 226e 616d 6522 2c22 7068 6f6e 6522 2c22  "name","phone","
00000020: 726f 6c65 222c 2269 7341 6374 6976 6522  role","isActive"
00000030: 2c22 6c61 7374 4163 6365 7373 4174 222c  ,"lastAccessAt",
00000040: 2263 7265 6174 6564 4174 220d 0a22 3122  "createdAt".."1"
```

- offset 0x00~0x02 = `ef bb bf` BOM
- offset 0x44~0x45 = `0d 0a` (헤더 종료 CRLF)

### 응답 헤더

```
HTTP/1.1 200
Content-Disposition: attachment; filename="users_20260918_102109.csv"; filename*=UTF-8''users_20260918_102109.csv
Content-Type: text/csv;charset=UTF-8
Transfer-Encoding: chunked
```

### opencsv 5.12 `ICSVWriter` 상수

```
public static final java.lang.String DEFAULT_LINE_END = "\n";
public static final java.lang.String RFC4180_LINE_END = "\r\n";
```

→ 기본 `\n` 이므로 CSVWriter 5-인자 생성자에 `"\r\n"` 명시가 필요. fix (`AdminCsvController.java:309~315`) 정합.

### F-CSV-2 재현 (500 응답 body)

```
{"timestamp":"2026-09-18T01:26:26.768Z","status":500,
 "error":"Internal Server Error",
 "trace":"java.lang.IllegalStateException: 자신의 센터 프로그램만 조회할 수 있어요.
    at io.github.sihyuuun.youthmoa.admin.AdminCsvController.exportApplications(AdminCsvController.java:219)"}
```

→ 데이터 격리 자체는 방어됨. UX 개선 (403 변환)만 spec §9 이월 (`A8-followup-403`).

### 회귀 테스트 결과

```
BUILD SUCCESSFUL in 2m 6s
7 actionable tasks: 4 executed, 3 up-to-date
```

대상 테스트 클래스 8종:
- `AdminUserServiceTest` (A5) · `AdminUserSafeguardTest` (A8 신설)
- `AdminApplicationServiceTest` (A4) · `AdminApplicationBulkServiceTest` (A8 신설)
- `AdminProgramServiceTest` (A2/A3) · `AdminProgramBulkServiceTest` (A8 신설)
- `AdminUserBulkServiceTest` (A8 신설) · `AdminCsvControllerTest` (A8 신설)

## 판정: 커밋 승인

- 최종 fix `1be0fd2` 는 opencsv 5.12 LF-only 기본값을 뒤집어 CRLF 실측 확보 (P-CSV-3 정합).
- 이월표 등재로 F-CSV-2 · F-Cov-1 spec 정합 확보 (반박 실패).
- 이전 QA 리포트의 조건부 PASS 3항목 중 P-CSV-3 는 CRLF fix 로 완전 해소, F-CSV-2 · F-Cov-1 은 이월 인정.
- 신규 리스크 (per-row REQUIRES_NEW · Safeguard 재사용 · BulkResult · CENTER_ADMIN 격리) 모두 실증.
- UNVERIFIED 4건은 CI · 사용자 시각 · 실 앱 열림 검증 영역 → 커밋 차단 아님.
