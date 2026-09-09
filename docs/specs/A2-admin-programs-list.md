# 작업 명세: A2 — admin-programs-list (관리자 프로그램 목록 · 상세 조회)

| 메타 | 값 |
|---|---|
| 상태 | **`spec_confirmed`** (2026-09-09 사용자 결정: Qn-1~9 **모두 권장안 A**. §11 그대로 이행) |
| 브랜치 (예정) | `feature/A2-admin-programs-list` |
| 선행 | P0-1 Flyway (PR #109 완료) · P0-2 매처/CSRF (PR #89 완료) · A1 admin-shell (완료) · 파생 큐 4/4 완결 (#206 · #207 · #208 · #210) |
| 관련 문서 | 마스터 `docs/specs/ADMIN-00-master-directive.md` §5-A2 · ADR `docs/adr/admin-track-roadmap-2026-09.md` · A1 spec `docs/specs/A1-admin-shell.md` |
| 후속 | **A3** (프로그램 편집 폼 — 등록/수정/삭제, F0c/F4/공지·약관 인라인 통합) · **A8** (카드·캘린더 뷰 + 일괄선택·CSV) |

---

## 0. 배경 · 스코프

A2 는 **파생 큐 진입점**이자 A3 (편집 폼) 착수 전 조회 뼈대. 관리자가 프로그램 목록을 탐색하고 상세를 열람하여, F4 (자격요건) · F0c (동적 필드) · 공지 · 약관 등 하위 관리 화면으로 진입할 수 있게 한다. **편집·등록·삭제는 A3 소관** — 본 티켓은 read-only.

### 포함 (A2)

1. **`/admin/programs`** — 목록형 뷰 하나. 상태 필터(전체/진행중/마감/진행예정/운영중단) · 검색(프로그램명·센터) · 페이징
2. **`/admin/programs/{id}`** — 관리자 전용 상세 페이지. 정보 카드 + 하위 관리 진입 링크 (자격요건 · 동적 필드 · 상세 정보 · 신청 현황 미리보기 링크만) + "편집" 버튼 (A3 라우팅만, disabled)
3. **RBAC** — SYSTEM_ADMIN = 전체 / CENTER_ADMIN = `AdminScope.effectiveCenterName()` 매칭 프로그램만
4. **하위 관리 링크** — 상세 페이지에서 `/admin/programs/{id}/eligibility` (F4) · `/admin/programs/{id}/dynamic-fields` (F0c) 진입 (이미 구현됨)
5. **admin 공지 목록 필터 링크** — `/admin/notices?programId={id}` 는 A2 범위 밖 (공지-프로그램 FK 없음). 상세에는 링크 미노출

### 제외 (A3 이월 · A8 이월)

- **A3**: 프로그램 CRUD (`POST /admin/programs`, `POST /admin/programs/{id}`, `POST /admin/programs/{id}/delete`), 편집 폼 UI, 신청 정보 탭, 약관 탭, 강좌(Course), 신청 질문 (ApplyQuestion 신설), 첨부 (ProgramAttachment 신설)
- **A8**: 카드 뷰 (`?view=card`), 캘린더 뷰 (`?view=calendar` — admin 전용. 사용자 사이드 `/programs?view=calendar` 와 별개), 다크 액션바 + 체크박스 일괄 선택, CSV 내보내기, 반응형 정규화 (≤1024/≤680), 스켈레톤/에러 폴리시
- **A4**: 신청 현황 테이블 · 신청 상세 모달 · 상태 변경 드롭다운 · 담당자 의견
- **A6**: 조회수 (viewCount) 컬럼 (통계 트랙과 함께 처리) — A2 상세 페이지에서 노출은 하되 값은 항상 0

---

## 1. 디자인 출처 (3자산)

| 자산 | 위치 | 비고 |
|---|---|---|
| **prototype.html** | L902~1069 (PROGRAMS screen) — 액션바 L907~945 · 목록형 그리드 L960~1033 · 카드형 L1035~1069 · 캘린더 L1070~ | **유일한 실 마크업 근거** |
| prototype.tsx | L441 (`case "programs"` 라우팅) · L467 (`function ProgramsScreen(_: any) { return <div /> }` **스텁**) | 스캐폴드만 — 대조 실행 불가 |
| HANDOFF.md | 전역 레이아웃 · 마이크로카피 톤 "…했어요/됐어요" · dataTable 패턴 (그리드 · #F0EFF3 헤더 · 11px 폰트) | A1 에서 이미 흡수됨 |

### 1-A. 자산 간 갭

prototype.tsx 는 스텁이라 갭 표 실행 불가. wireframe 은 hifi 로 갈음 (ADMIN-00 §1-A). **prototype.html 이 유일한 근거**.

### 1-B. 데이터 모델 gap 표

| prototype 필드 (L960~1033 목록형 헤더 · L4195 CSV 헤더) | 현재 엔티티 | 조치 (A2 범위) |
|---|---|---|
| `No.` | (파생 — page × size + index) | 유지 |
| 프로그램명 (썸네일 + 이름) | `Program.title` + `imageUrl` ✅ | 유지 |
| 센터 | `Program.organization` (문자열) | 유지 — **Center FK 도입은 A3/A9 이월** (Qn-1 A안) |
| 지역 | `Program.region` ✅ | 유지 |
| 진행기간 | `Program.startDate` / `endDate` ✅ | 유지 |
| 신청기간 (applyPeriod) | ❌ 없음 | **A3 도입 (마스터 §3-1)** — A2 는 미노출 or 진행기간 재사용 (Qn-3) |
| 신청현황 (applied/capacity) | `Program.capacity` ✅ + `ApplicationRepository.countByProgramAndStatusIn(PENDING,APPROVED)` 파생 | 유지 — 목록에서 N+1 회피 위해 배치 조회 |
| 조회수 (views) | ❌ 없음 | **A6 이월** — 목록/상세 모두 값 노출은 스킵 or 항상 "-" (Qn-3) |
| 상태 뱃지 | `Program.getStatus()` 런타임 파생 ✅ (OPEN/UPCOMING/ENDED/SUSPENDED) | 유지 — DB 컬럼 추가 금지 (HANDOFF: 일괄 상태변경 없음) |
| 관리 (복제·삭제) | — | **A3 이월** — A2 는 "편집" 링크 하나만 |

**결론**: A2 범위는 엔티티 변경 없음. Flyway V 마이그레이션 불필요. `applyPeriod` · `viewCount` · Center FK · Course · Attachment 등 신설은 **모두 A3 · A6 · A9 담당**.

### 1-C. 데이터 소비 지점

A2 는 조회 전용이라 write→read 왕복 없음. 다음 지점이 Program 데이터를 읽는다:

| 소비 지점 | 위치 | A2 무회귀 확인 |
|---|---|---|
| 사용자 목록 `/programs` | `ProgramController.list` L36 | A2 는 admin 조회만 추가 — 무회귀 |
| 사용자 상세 `/programs/{id}` | `ProgramController.detail` L140 | 동 |
| 홈 최근/마감 임박 | `HomeController` + `ProgramRepository.findTop4By...` | 동 |
| 캘린더 뷰 | `ProgramCalendarService` | 동 |
| A1 대시보드 스탯 카드 | `AdminDashboardService` | 동 |
| F4 자격요건 편집 | `AdminProgramEligibilityController` | 동 |
| F0c 동적 필드 | `AdminApplyQuestionController` | 동 |

---

## 2. 재활용 인프라 (검증 완료)

| 인프라 | 위치 | A2 재활용 방식 |
|---|---|---|
| `AdminScope.effectiveCenterName()` | `admin/AdminScope.java:39` | `null` → 전체, 문자열 → `program.organization = ?` 매칭 |
| `@PreAuthorize` + `@EnableMethodSecurity` | `common/config/SecurityConfig.java` | `@PreAuthorize("hasAnyRole('CENTER_ADMIN','SYSTEM_ADMIN')")` 클래스 레벨 |
| `AdminExceptionHandler` | `admin/AdminExceptionHandler.java` | 그대로 상속 |
| `AdminNoticeController.list` 페이지네이션 5-단위 그룹 | `AdminNoticeController.java:56~65` | 동일 패턴 복제 |
| `templates/admin/layout.html` + fragments | A1 완료 | `currentPage="programs"` 로 활성 GNB |
| `admin.css` 다크 헤더 · dataTable · badge | A1 완료 | 그대로 사용 |
| **`ProgramRepository.JpaSpecificationExecutor`** | `program/ProgramRepository.java:11` | 이미 상속됨 — admin 필터 조합용 Spec 재사용 (사용자 사이드 `ProgramSpec` 확장) |
| `ProgramSpec` | `program/ProgramSpec.java` | organization eq / title like / status in / active in — 사용자 필터가 커버하지 않는 admin 전용 조합은 `AdminProgramSpec` 신설 |
| `applicationRepository.countByProgramAndStatusIn` | `ProgramController.detail:149` | 동 |

**결정**: admin 전용 서비스 `AdminProgramService` + `AdminProgramController` **신설**. 사용자 사이드 `ProgramService.search()` 는 status 파라미터 형태 (`upcoming|active|ended`) 가 admin 필터 (`전체|진행중|마감|진행예정|운영중단`) 와 어긋나므로 **직접 재활용 금지**. `AdminScope` 통합 필터를 반드시 강제해야 하므로 `AdminProgramService` 가 Repository 를 직접 호출.

---

## 3. 필드 · 화면 명세

### 3-1. `/admin/programs` 목록형 뷰

**상단 액션바** (prototype L907~945)

- **좌측 필터 세그먼트**: 전체 / 진행중 / 마감 / 진행 예정 / 운영중단 (5종 — SUSPENDED 포함. prototype 은 4종이나 `Program.getStatus()` 가 SUSPENDED 를 노출하므로 필터에도 포함. Qn-3)
- **우측**: 검색 인풋 (`프로그램명, 센터 검색` placeholder, 175px, prototype L919) · 뷰 토글 (목록/카드/캘린더 — **카드·캘린더는 disabled 스타일 + A8 안내 툴팁**) · CSV 내보내기 (**disabled + A8 안내**) · 프로그램 등록 버튼 (`goToProgramForm` → **`/admin/programs/new` = A3, A2 에서는 disabled + A3 안내**)

**목록 그리드** (실 구현 반영 · 2026-09-09 ym-verify 후 갱신)

| # | 컬럼 | 소스 | 비고 |
|---|---|---|---|
| 1 | 프로그램명 (썸네일 32×32 + 제목 · 조직명) | `title` · `organization` · `imageUrl` | — |
| 2 | 카테고리 | `Program.category` (i18n label) | prototype 이탈 · 관리자 UX 개선 (deviation) |
| 3 | 청년센터 | `Program.organization` | — |
| 4 | 상태 뱃지 | `Program.getStatus()` 파생 | OPEN/UPCOMING/ENDED/SUSPENDED |
| 5 | 신청기간 | **A3 이월** — 값 "-" | applyPeriod 컬럼 미도입 |
| 6 | 신청현황 | `applied/capacity` (배치 조회) | 배치 조회로 N+1 회피 |
| 7 | 조회수 | **A6 이월** — 값 "-" | viewCount 컬럼 미도입 |
| 8 | 등록일 | `Program.createdAt` | prototype 이탈 · 관리자 UX 개선 (deviation) |
| 9 | 관리 | "편집" 링크 → `/admin/programs/{id}` 상세 페이지 (A3 편집 진입은 상세에서) | — |

**Prototype 이탈** (§9 deviation 도 참조):
- **제거**: 체크박스 (A8 이월 · 일괄 선택 없음) · No. (페이지네이션으로 대체) · 진행기간 (신청기간과 겹침, 신청기간이 A3 완료 후 유의미해질 예정)
- **추가**: 카테고리 · 등록일 (관리자 실무 유용성)
- 컬럼 개수는 9종으로 동일 유지

**하단 페이지네이션**: 10건/페이지 (Qn-4). 5-그룹 단위 (AdminNoticeController 패턴)

**정렬**: 최신순 기본 (`createdAt DESC` — Qn-5 A안). 정렬 옵션 UI 는 **A8 이월** (prototype 없음)

**빈 상태**: "등록된 프로그램이 없어요" (필터·검색 미적용 시) / "검색 결과가 없어요" (적용 시). HANDOFF 마이크로카피 톤

### 3-2. `/admin/programs/{id}` 상세 조회

**prototype 원문 없음** — prototype 의 `program-detail` 화면은 A4 (신청 현황 테이블 포함) 이 실체. A2 는 **A4 로부터 조회 부분만 분리한 축소판**을 신설. Qn-2 결정 필요.

**권장 (Qn-2 A안 · 관리자 전용 신설)**:

- 상단 헤더: 제목 + 상태 뱃지 + "편집" 버튼 (**A3 라우팅 준비 — A2 에서는 disabled + "A3 에서 열립니다" 툴팁**)
- 정보 카드 (기본 정보):
  - 썸네일 (imageUrl)
  - 센터 (organization) · 지역 (region)
  - 진행기간 (startDate ~ endDate) · D-day (getDdayLabel)
  - 모집인원 (capacity) · 신청 (파생 count)
  - 자격요건 요약 (`ProgramEligibility.age/region/etc` — F4 요약 3줄)
- 설명 카드 (`content` — 그대로 렌더)
- **하위 관리 진입** 카드:
  - `[자격요건 관리]` → `/admin/programs/{id}/eligibility` (F4 완료)
  - `[동적 필드 관리]` → `/admin/programs/{id}/dynamic-fields` (F0c 완료)
  - `[신청 현황 보기]` → A4 (disabled + 안내)
  - `[사용자 화면 미리보기]` → `/programs/{id}` (새 창)
- 하단: "삭제" 버튼 (**A3 이월 — disabled**)

**Qn-2 B안 (사용자 뷰 재활용)**: `/programs/{id}` 를 iframe 또는 redirect 로 재활용. 편집·삭제 등 관리자 액션 배치 위치가 없어 UX 불편 → **A안 권장**.

### 3-3. RBAC 정책

`AdminScope.effectiveCenterName()` 을 모든 조회에 강제:

| 관리자 | 목록 조회 | 상세 조회 |
|---|---|---|
| SYSTEM_ADMIN | 전체 | 전체 |
| CENTER_ADMIN | `program.organization = self.center.name` 매칭만 | 동. 미매칭 시 **403** (`AccessDeniedException`) |

`AdminProgramService` 진입점 한 곳에서 스코프 필터를 반드시 적용. 컨트롤러에서 누락할 수 없는 구조 (마스터 §2-4 원칙).

**Program-Center FK 도입 여부** (Qn-1):
- 현재: `Program.organization` 문자열만 존재 (실측 L34)
- `AdminScope` 는 이미 organization 문자열 매칭 방식으로 확립됨 (L24~25 코멘트에 "A9 착수 시 FK 관계로 승격 예정" 명시)
- **권장 A안**: A2 에서 FK 도입 안 함 — organization 매칭 유지. A9 (센터 CRUD) 에서 일괄 정비
- B안: A2 에서 FK 도입 — V12 마이그레이션 + Program 엔티티 변경 + 시드 재정비. 스코프가 A3 만큼 커짐

---

## 4. 변경 범위 (파일 단위)

### Java

- **신규**: `admin/AdminProgramController.java` — 목록 · 상세
- **신규**: `admin/AdminProgramService.java` — 스코프 강제 조회
- **신규 (조건부)**: `admin/AdminProgramSpec.java` — admin 전용 필터 (organization eq / title like / status in java-side filter)
- **수정 없음**: `program/Program.java`, `program/ProgramRepository.java`, `program/ProgramService.java`, `program/ProgramController.java` — 무회귀

### 템플릿

- **신규**: `templates/admin/program/list.html`
- **신규**: `templates/admin/program/detail.html`
- **수정**: `templates/admin/fragments/header.html` — GNB "프로그램 관리" 활성화 (현재 `#`)
- **수정**: `templates/admin/dashboard.html` — "최근 프로그램" 행 클릭 시 `/admin/programs/{id}` 이동 (현재 disabled)

### CSS

- 수정 없음 — A1 admin.css 재활용

### 테스트

- **신규**: `AdminProgramListRenderTest` — @WebMvcTest + RBAC 슬라이스
- **신규**: `AdminProgramDetailRenderTest`
- **신규**: `AdminProgramServiceTest` — 스코프 필터 단위
- **신규 E2E**: `e2e/tests/admin-programs-list.spec.ts` · `admin-programs-detail.spec.ts` · `admin-programs-rbac.spec.ts`
- **신규 계약**: `e2e/contracts/admin-programs-list.ts` · `docs/design-contracts/admin/programs-list.md` (Qn-8)

### 마이그레이션

- **없음** — 엔티티 변경 없음

---

## 5. 갭 리스트 (현재 vs 목표)

| # | 항목 | 현재 | 목표 (A2) | 우선순위 |
|---|---|---|---|---|
| 1 | `/admin/programs` 경로 | 미존재 (헤더 GNB 비활성) | 목록형 뷰 · 필터 · 검색 · 페이징 | 높음 |
| 2 | `/admin/programs/{id}` 경로 | 미존재 | 관리자 상세 조회 + 하위 진입 링크 | 높음 |
| 3 | admin 헤더 GNB "프로그램 관리" | disabled `#` | 링크 활성 + `currentPage="programs"` 하이라이트 | 높음 |
| 4 | 대시보드 최근 프로그램 행 링크 | disabled | `/admin/programs/{id}` 이동 | 중간 |
| 5 | 상태 뱃지 렌더 fragment | 미존재 (A1 은 대시보드 텍스트만) | `admin/fragments/badge.html` 신설 or 인라인 | 중간 |
| 6 | 신청현황 배치 조회 | detail 에서만 파생 | 목록 N행 IN-쿼리 1회 | 중간 |
| 7 | 카드/캘린더 뷰 토글 | 미존재 | disabled 렌더만 (A8 예고 툴팁) | 낮음 |
| 8 | CSV · 일괄선택 | 미존재 | 동 (A8 이월) | 낮음 |

---

## 6. 검증 시나리오

### 정적

- `compileJava` + `AdminProgramListRenderTest` + `AdminProgramDetailRenderTest` + `AdminProgramServiceTest` PASS
- `JpaMappingTest` (엔티티 변경 없으므로 무회귀 확인만)

### 동적 (curl · 8091)

```bash
# 비인가
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8091/admin/programs
# → 302 /admin/login

# SYSTEM_ADMIN 로그인 후
curl -s -o /dev/null -w "%{http_code}\n" -b jsessionid http://localhost:8091/admin/programs
# → 200 · 전체 프로그램

curl -s http://localhost:8091/admin/programs?status=active | grep "진행중"
# → 필터 활성

curl -s http://localhost:8091/admin/programs?q=디자인 | grep "디자인"
# → 검색 hit

curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8091/admin/programs/1
# → 200 (상세)

# CENTER_ADMIN 다른 센터 프로그램 직접 접근
curl -s -o /dev/null -w "%{http_code}\n" -b centeradmin_jsessionid http://localhost:8091/admin/programs/{타센터_id}
# → 403
```

### 계약 (Qn-8 A안)

- `e2e/contracts/admin-programs-list.ts` 신설. 액션바 필터 5종 · 그리드 컬럼 9종 · 페이지네이션 · badge 색상. `proto:` 라인 인용 필수 (prototype.html L907~1033)
- `docs/design-contracts/admin/programs-list.md` — 아키텍처 (SSR + 페이지네이션), 상태머신 (필터 → URL 쿼리스트링), CTA 라우팅 표
- `npx playwright test --project=contracts` → 갭 0

### 기능 E2E (Playwright)

- `admin-programs-list.spec.ts` — 로그인 · 목록 렌더 · 필터 클릭 → URL 변경 → 결과 변경 · 검색 · 페이징
- `admin-programs-detail.spec.ts` — 목록 → 상세 이동 · 하위 링크 (F4/F0c) 클릭 → 정상 이동 · 편집 버튼 disabled 확인
- `admin-programs-rbac.spec.ts` — SYSTEM_ADMIN 전체 조회 · CENTER_ADMIN 자기 센터만 · 타 센터 직접 URL 403

### 무회귀 (사용자 사이드 · 파생 큐)

- `/programs` 목록 · `/programs/{id}` 상세 정상 (기존 E2E 재실행)
- `/admin/programs/{id}/eligibility` (F4) · `/admin/programs/{id}/dynamic-fields` (F0c) 접근 그대로

### 시각 (사용자 확인)

- 액션바 · 필터 세그먼트 · 상태 뱃지 색상 prototype 매칭 (계약 자동화됨)
- 상세 페이지 카드 배치 감성

---

## 7. 결정 필요 항목 (Qn)

### Qn-1 · Program-Center FK 도입 여부 및 RBAC 확장

- **A안 (권장)** — 이번 티켓 미도입. `AdminScope.effectiveCenterName()` organization 문자열 매칭 유지 (이미 확립됨). A9 (센터 CRUD) 에서 일괄 승격
  - 이유: A2 스코프 최소화. FK 도입 = 시드 재정비 + 다른 소비 지점 (사용자 목록·홈·캘린더·A1 대시보드) 전수 회귀 발생
- B안 — 본 티켓에서 FK 도입 (V12 마이그레이션). 이후 티켓은 FK 전제
- **RBAC 는 무조건 SYSTEM_ADMIN + CENTER_ADMIN 두 role 모두 허용**. USER 접근 403

### Qn-2 · 상세 페이지

- **A안 (권장)** — 관리자 전용 `templates/admin/program/detail.html` 신설. 정보 카드 + 하위 관리 진입 카드
- B안 — 사용자 `/programs/{id}` 재활용 (iframe / redirect)
  - 이유: 관리자 액션 (편집·삭제·하위 관리 진입) 배치 자리가 없음. UX 및 A3 확장성 취약

### Qn-3 · 필터·컬럼 범위

- **A안 (권장)** — prototype 액션바 필터 5종 (SUSPENDED 추가) · 검색 (title + organization) · 컬럼 9종. `applyPeriod` · `views` 컬럼은 "-" 표시 (A3/A6 예고)
- B안 — `applyPeriod` · `views` 컬럼 자체 미노출 → A3/A6 착수 시 목록 UI 재편 필요
- C안 — `applyPeriod` 를 A2 에서 함께 도입 (V12 + Program 컬럼 추가). Qn-1 B 와 함께 채택 시 스코프 대폭 증가

### Qn-4 · 페이지당 개수

- **A안 (권장)** — 10건 (`AdminNoticeController` 와 일관)
- B안 — 20건 (SEED 규모가 크면 페이지 이동 잦음). SEED 규모 확인 후 조정 여지

### Qn-5 · 정렬 기본값

- **A안 (권장)** — 최신순 (`createdAt DESC`)
- B안 — 신청 마감 임박순 (`endDate ASC`, ENDED 후순위)
- C안 — 인기순 (`applied DESC`) — 배치 조회 정렬이라 페이지네이션 비용
- 정렬 옵션 UI 는 **A8 이월** (prototype 없음)

### Qn-6 · 검색 방식

- **A안 (권장)** — LIKE `%q%` (title · organization OR)
- B안 — Elasticsearch-like fuzzy (오버스펙, 이번 범위 아님)
- Case-insensitive: PG `ILIKE` vs `lower(title) like lower(?)` — Hibernate 표준화 위해 후자

### Qn-7 · 액션 컬럼

- **A안 (권장)** — "편집" 링크 하나 (실 편집은 상세 페이지 → A3). prototype 은 복제·삭제 아이콘 병기이나 A3 이월
- B안 — 자격요건 · 동적 필드 · 편집 3개 병기 (하위 관리 바로 진입)
- C안 — "관리" 드롭다운 메뉴 (편집/자격요건/동적필드/삭제 - A3)

### Qn-8 · 계약 신설 여부

- **A안 (권장)** — 신설 (전 화면 정책 P0 · `admin-notice` · `admin-term` 관례). 목록형 뷰 하나만 계약. 카드/캘린더는 A8 계약
- B안 — 미신설, A3 완료 후 편집 폼과 함께 신설
  - 이유: 계약 없으면 향후 재해석 시 판단 흔들림 (POLICY.md 근거)

### Qn-9 (신규 제안) · admin 헤더 GNB "프로그램 관리" 링크

- 현재 A1 완료 상태에서 GNB "프로그램 관리" 는 `#` disabled. A2 완료 시 활성화하되, 활성화 시점은 **본 PR 병합 직후** (Qn-9 A안) vs **A3 완료 후** (B안)
- **A안 권장** — 조회만 있어도 진입 가치 충분

---

## 8. 스코프 예상 규모

| 항목 | 예상 |
|---|---|
| 신규 Java 파일 | 2~3 (Controller · Service · Spec 조건부) |
| 수정 Java 파일 | 0 |
| 신규 템플릿 | 2 (list · detail) |
| 수정 템플릿 | 2 (fragments/header · dashboard) |
| 신규 테스트 (JVM) | 3 (List/Detail Render + Service) — 각 ~150 라인 |
| 신규 E2E spec | 3 (list · detail · rbac) |
| 신규 계약 | 2 (ts + md) |
| Flyway V | **0** |
| 총 diff | 약 1,200~1,600 라인 (F0c 규모의 60~70%. F0c: ~2,000 라인) |
| 리스크 | **중하** — 엔티티 변경 없음, 재활용 인프라 완비. 리스크는 (1) `AdminScope` organization 매칭이 A9 FK 도입 시 재작성 필요 (2) 필터 5종 · 상태 뱃지 렌더 fragment 부재로 A8/A4 와 중복 신설 가능성 |

---

## 9. deferred / deviation

### deferred (본 PR 범위 아님 · 후속 티켓 명시)

| 항목 | 담당 티켓 | 근거 |
|---|---|---|
| 프로그램 CRUD (등록·수정·삭제) | **A3** | 마스터 §5-A3 |
| 신청 정보 탭 (ApplyQuestion 신설) | A3 (F0c 승계) | 마스터 §3 |
| 약관 정보 탭 (Program 컬럼 3개) | A3 | 마스터 §3-1 |
| 강좌 (Course 엔티티) | A3 | 마스터 §3-1 |
| 첨부 (ProgramAttachment) | A3 (P0-3 인프라 재사용) | 마스터 §3-1 |
| 카드 뷰 (`?view=card`) | **A8** | 마스터 §5-A8 |
| 캘린더 뷰 (`?view=calendar`) | A8 | 동 |
| 체크박스 일괄 선택 · 다크 액션바 | A8 | 동 |
| CSV 내보내기 | A8 | prototype L4195 헤더 참조 |
| 반응형 정규화 (≤1024/≤680) | A8 | 동 |
| 스켈레톤 · 에러/재시도 폴리시 | A8 | 동 |
| 신청 현황 테이블 · 신청 상세 모달 · 상태 변경 드롭다운 · 담당자 의견 | **A4** | 마스터 §5-A4 · Q6 |
| 강좌 상세 (course-detail) | A4 | 마스터 §5-A4 |
| `Program.viewCount` 컬럼 + 조회수 증가 · 노출 | **A6** | 마스터 §5-A6 (통계 트랙과 함께) |
| `Program.applyStartDate` / `applyEndDate` 컬럼 | **A3** | 마스터 §3-1 (A1 spec 에서도 마감임박 = endDate 임시 파생) |
| Program-Center FK 승격 | **A9** | 마스터 §5-A9 · `AdminScope` L24~25 코멘트 |
| 소프트 삭제 (Q10) | A3 | 마스터 §8 Q10 |
| 프로그램 복제 (`bulkClone`) | A3 | prototype L4098 |

### deviation (본 PR 에서 영구 이탈)

| 항목 | 사유 |
|---|---|
| 액션바 필터 5종 (prototype 4종 → SUSPENDED 추가) | `Program.getStatus()` 가 SUSPENDED 를 노출하므로 필터에서도 커버해야 조회 가능 (POLICY 관련 없음 · A2 신규 결정) |
| 뷰 토글 3개를 렌더만 하고 카드/캘린더 disabled | prototype 은 3뷰 동작이나 A8 이월. 접근성 위해 렌더 유지 + `aria-disabled="true"` |
| 목록 컬럼 재편 — **체크박스/No/진행기간 제거, 카테고리/등록일 추가** | 2026-09-09 ym-verify FAIL #4 후속 spec 정정. 체크박스는 A8 일괄 선택과 함께 도입 · No 는 페이지네이션이 대체 · 진행기간·신청기간 겹침(신청기간은 A3 완료 후 유의미) · 카테고리·등록일은 관리자 실무 유용성. 컬럼 개수 9종 동일 |

---

## 10. 작업 큐 메타

- 작업 ID: **A2**
- 우선순위: 높음 (A3 진입점)
- 추정 단위: 1 PR
- 상태: **`impl_done`** (2026-09-09) — ym-qa 인계 대기
- 다음 단계: 사용자 Qn-1~Qn-9 결정 → `spec_confirmed` → ym-impl 인계

---

## 11. 구현 매핑 (2026-09-09 ym-impl 산출)

### 백엔드

| 스펙 항목 | 파일 | 라인 |
|---|---|---|
| §2 재활용 인프라 검증 | `admin/AdminProgramService.java` | 전체 |
| §3-1 목록 조회 (Qn-3 A · Qn-4 A · Qn-5 A · Qn-6 A) | `AdminProgramService.list()` | L51~62 |
| §3-1 스코프 강제 (Qn-1 A) | `AdminProgramService.scopeSpec()` | L96~100 |
| §3-1 검색 (Qn-6 A, LIKE lower title+organization OR) | `AdminProgramService.keywordSpec()` | L102~110 |
| §3-1 필터 5종 (Qn-3 A, OPEN/UPCOMING/ENDED/SUSPENDED/전체) | `AdminProgramService.statusSpec()` | L122~152 |
| §3-1 신청수 배치 조회 (N+1 방지) | `AdminProgramService.countAppliedByProgramIds()` | L82~92 |
| §3-2 상세 조회 (Qn-2 A) | `AdminProgramService.find()` | L67~77 |
| §3-3 RBAC (Qn-1 A: SYSTEM_ADMIN + CENTER_ADMIN) | `admin/AdminProgramController.java` @PreAuthorize | L37 |
| §3-1 목록 컨트롤러 + 페이지네이션 5-그룹 | `AdminProgramController.list()` | L43~74 |
| §3-2 상세 컨트롤러 + 403 승격 | `AdminProgramController.detail()` | L77~91 |

### 화면 (Thymeleaf)

| 스펙 항목 | 파일 | 라인 |
|---|---|---|
| §3-1 목록 액션바 (필터 5종 + 검색) | `templates/admin/program/list.html` | L34~63 |
| §3-1 컬럼 9종 헤더 | `templates/admin/program/list.html` | L68~78 |
| §3-1 프로그램명 (썸네일 32×32 + 텍스트) | `templates/admin/program/list.html` | L86~100 |
| §3-1 상태 뱃지 | `templates/admin/program/list.html` | L104~107 |
| §3-1 신청기간 "-" (A3 이월) | `templates/admin/program/list.html` | L108~109 |
| §3-1 신청현황 (배치 count / capacity) | `templates/admin/program/list.html` | L110~111 |
| §3-1 조회수 "-" (A6 이월) | `templates/admin/program/list.html` | L112~113 |
| §3-1 액션 컬럼 "편집" (Qn-7 A · 상세 페이지로 이동) | `templates/admin/program/list.html` | L118~121 |
| §3-1 페이지네이션 5-그룹 | `templates/admin/program/list.html` | L127~140 |
| §3-2 상세 헤더 + 브레드크럼 + 상태 뱃지 | `templates/admin/program/detail.html` | L25~48 |
| §3-2 편집·삭제 disabled (A3 이월) | `templates/admin/program/detail.html` | L41~44 |
| §3-2 기본 정보 카드 | `templates/admin/program/detail.html` | L52~89 |
| §3-2 하위 관리 진입 카드 (F4 · F0c · A4 disabled) | `templates/admin/program/detail.html` | L92~110 |
| §3-2 자격요건 요약 | `templates/admin/program/detail.html` | L112~128 |
| §5-3 GNB "프로그램 관리" 활성화 (Qn-9 A) | `templates/admin/fragments/header.html` | L44~45 |
| §5-4 대시보드 "최근 프로그램" 링크 활성화 | `templates/admin/dashboard.html` | L128, L138~149 |

### 계약 · 테스트

| 스펙 항목 | 파일 |
|---|---|
| §6 계약 (Qn-8 A) — 스펙 | `docs/design-contracts/admin/programs-list.md` |
| §6 계약 — 검사 | `e2e/contracts/admin-programs.ts` |
| §6 계약 실행 spec | `e2e/tests/visual-admin-programs.spec.ts` |
| §6 기능 E2E 목록 | `e2e/tests/admin-programs-list.spec.ts` |
| §6 기능 E2E 상세 | `e2e/tests/admin-programs-detail.spec.ts` |
| §6 기능 E2E RBAC | `e2e/tests/admin-programs-rbac.spec.ts` |
| §6 정적: 목록 렌더 | `src/test/java/io/github/sihyuuun/youthmoa/admin/AdminProgramListRenderTest.java` |
| §6 정적: 상세 렌더 | `src/test/java/io/github/sihyuuun/youthmoa/admin/AdminProgramDetailRenderTest.java` |
| §6 정적: 서비스 스코프/필터 통합 | `src/test/java/io/github/sihyuuun/youthmoa/admin/AdminProgramServiceTest.java` |
| §6 CSS 신규 | `src/main/resources/static/css/admin.css` A2 섹션 (파일 하단) |
