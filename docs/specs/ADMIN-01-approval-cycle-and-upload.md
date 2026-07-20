# 작업 명세: ADMIN-01 — 관리자 승인 사이클 + 파일 업로드 (실행 spec)

> **성격**: `ADMIN-00-master-directive.md` (마스터 플랜, `spec_confirmed`) 의 **실행 오버레이**.
> ADMIN-00 의 아키텍처 결정 (§2)·데이터 모델 gap (§3)·Phase 정의 (§5)·결정 기록 Q1~Q10 (§8) 은 **그대로 유효하며 재작성하지 않는다**.
> 본 문서는 "전체 admin 을 한 번에" 가 아니라 **포트폴리오 시연 사이클 우선**의 단계 분할·실행 순서·신규 발견 갭·신규 Q(Q11~) 만 정의한다.

| 메타 | 값 |
|---|---|
| 작업 ID | ADMIN-01 |
| 상태 | **`spec_done`** — Q11~Q19 사용자 결정 대기 |
| 트랙 | admin (포트폴리오 강화) |
| 선행 | P0-2 완료 (PR #89 — `/admin/**` RBAC + CSRF + 관리자 시드 3계정). P0-1 Flyway **준비 완료·비활성** (`chore/flyway-activation` 별도 티켓 — §6 의존 관계 참조) |
| 마스터 | [ADMIN-00-master-directive.md](ADMIN-00-master-directive.md) |
| 라이프사이클 | 본 spec 컨펌 → Phase 별 ym-impl → ym-qa → ym-verify → 머지 |

---

## 0. ADMIN-00 대비 이번 실행 범위 (핵심 재정의)

### 0-1. 왜 순서를 바꾸는가

ADMIN-00 권장 순서는 `A1 → A2 → A3 → A4 → …` (화면 완성도 순). 그러나 포트폴리오 관점에서 가장 먼저 완성해야 하는 것은 **"admin 이 신청을 승인 → 도메인 이벤트 → 사용자 알림 도착"** 이라는 **엔드투엔드 사이클**이다. 근거:

1. **백엔드가 이미 준비됨** — `ApplicationService.approve/reject` (이벤트 발행 + idempotent), `Application.approve/reject(User admin)` (processedBy/processedAt 기록), `ApplicationNotificationListener` (AFTER_COMMIT + REQUIRES_NEW) 가 전부 구현·머지 상태. **admin 화면만 없어서 이 사이클이 시연 불가능**한 상태다.
2. A2 풀스펙 (CSV·일괄선택·3뷰) 과 A3 (3탭 폼·드래그앤드롭) 은 데이터 모델 신설 (Course·ApplyQuestion 등) 이 커서 사이클 완성을 몇 주 뒤로 미룬다.
3. 도메인 이벤트 → 트랜잭션 경계 (`@TransactionalEventListener`) → 감사 이력은 GMP 실무 (ALCOA+, 감사 추적) 경험과 직결되는 어필 포인트다.

### 0-2. Phase 분할 (ADMIN-00 A-티켓과의 매핑)

| Phase | 내용 | ADMIN-00 매핑 | 브랜치 후보 | PR 추정 |
|---|---|---|---|---|
| **Phase 1** | **승인 사이클 완성** — admin shell(경량) + 신청 관리 + 승인/반려 + 감사 이력 | A1 **부분** (레이아웃·로그인·상호 진입 동선·대시보드 간이) + A4 **부분** (신청 현황 테이블·상태 변경·신청 상세 모달) | PR-1 `feature/A1-admin-shell` (경량 스코프)<br>PR-2 `feature/A4-admin-approvals` | 2 PR |
| **Phase 2** | **프로그램 CRUD + 파일 업로드** — 업로드 인프라 + 프로그램 목록/등록/수정 폼 (동적 질문 제외) | P0-3 전체 + A2 **핵심** (목록형, CSV·일괄은 후순위) + A3 **부분** (프로그램 정보 탭 + 약관 탭. 신청 정보 탭의 동적 질문·강좌는 제외 — Q16) | PR-3 `chore/P0-3-file-upload`<br>PR-4 `feature/A2-admin-programs-list`<br>PR-5 `feature/A3-admin-program-form` | 3 PR |
| **Phase 3** | **공지/SiteImage/센터 CRUD** — NoticeAttachment 실파일 승격 포함 | A9 (센터 — Q1 확정) + 백로그 ⑨ SiteImage CRUD + 공지 CRUD (prototype 화면 부재 — Q14) | PR-6 `feature/ADMIN-notices-crud`<br>PR-7 `feature/ADMIN-siteimage-crud`<br>PR-8 `feature/A9-admin-centers` | 3 PR |
| (후속) | A1 잔여 (통계 카드 실데이터·알림 벨) / A4 잔여 (course-detail·대기자 Q7) / A5·A6·A7·A8 / F0c-dynamic-fields | ADMIN-00 §5 순서 그대로 재개 | ADMIN-00 §9 큐 | — |

**Phase 1 이 끝나면 시연 가능한 스토리**: 관리자 로그인 → 프로그램 신청 현황 → 대기 건 승인(의견 입력) → 사용자 계정으로 알림 벨에서 "신청이 승인되었습니다" 확인 → 마이페이지 신청 내역 상태 "승인". 여기에 감사 이력 (누가·언제·무엇을·왜) 조회까지.

### 0-3. Phase 1 에서 의도적으로 뺀 것 (ADMIN-00 잔여로 이월)

| 항목 | prototype 근거 | 이월 위치 |
|---|---|---|
| 대기 전체 승인 / 선택 일괄 승인·반려 바 | prototype.html L1611~1636 | Phase 1b 또는 A4 잔여 (Q18) |
| 신청자 CSV 내보내기 | prototype.html L1615~1618 | A2 CSV 인프라와 함께 |
| 대기자 배너 + 자동 승인 토글 | prototype.html L1580~1592 | Q7 결정대로 A8 이후 별도 티켓 |
| 신청 답변 (answers) 모달 섹션 | prototype.html L2699~2715 | ApplyAnswer 엔티티 (F0c-dynamic-fields) 이후 — Q16 |
| course-detail 신청 현황 | prototype.html L1370~1441 | Course 엔티티 (A3-full) 이후 |
| 대시보드 통계 카드 실데이터·알림 벨·글로벌 검색 | ADMIN-00 A1/A6/A7 | 해당 티켓 |

---

## 1. 디자인 출처 (3자산)

3자산 전수 정독은 **ADMIN-00 §1 에서 완료** (HANDOFF·tsx·wireframe.png 대조, §1-A wireframe↔hifi 채택표 포함). 본 spec 은 이번 범위 화면에 대한 **추가 정독·라인 인용**만 기록한다.

| 자산 | 이번 범위 해당 위치 |
|---|---|
| `docs/00_assets/admin/prototype.html` | 신청 현황 테이블 L1594~1666 / 신청 상세 모달 L2677~2749 / 삭제(파괴적 액션) 확인 모달 L2751~2768 / 프로그램 상세 타이틀·수정·⋯메뉴 L1446~1488 / program-form 업로드 영역 L2442~2448(썸네일)·L2522~2529(첨부 PDF,HWP) / 승인 여부 radio L2537~2547 |
| `docs/00_assets/admin/prototype.tsx` | `Applicant` 인터페이스 L154~164 / `ApplicantStatus`("승인"\|"대기"\|"반려"\|"취소") + `applicantBadge` 색상 L69~74 / `AppState` L201~251 (confirmDialog·toast·loading) / `Screen` 라우팅 L180~188 / `CENTERS` L256~259 |
| `docs/00_assets/admin/HANDOFF.md` | §신청자 상태 뱃지 (L110~) / §최신 기능 1 폼 검증·2 삭제 확인 모달·3 접근성·7 마이크로카피 톤 (L160~202) / §테이블 공통 패턴 (L218~) / §센터 데이터 격리 (L286~) |
| `docs/00_assets/admin/wireframe.png` | ADMIN-00 §1-A 3행 — 신청 상세 모달의 "신청 이력 + 담당자 의견" 은 wireframe 에만 존재 → **Q6 확정: 병합** (adminComment + 상태 변경 이력). 본 spec Phase 1 이 이 결정의 구현체 |

### 1-A. 자산 간 갭 — 이번 정독에서 **신규 발견** (ADMIN-00 §1-A/§3 에 없던 것)

| # | 항목 | prototype 근거 | ADMIN-00 상태 | 조치 |
|---|---|---|---|---|
| G1 | **승인 여부 radio (자동 승인 / 수동 승인)** — program-form 신청 정보 탭 | prototype.html L2537~2547 | §3-1 gap 표 **누락** | `Program.approvalMode` 컬럼 필요. 자동 승인 시 `apply()` 에서 즉시 APPROVED + 승인 알림 → **Q13** |
| G2 | 담당자 의견 textarea 의 마이크로카피 — "담당자 의견을 입력해주세요. **사용자에게 노출됩니다.**" | prototype.html L2739 | Q6 은 병합만 결정, 노출 정책 미기술 | 의견은 상태 무관 사용자 노출 전제 → 저장 위치 설계 **Q12** |
| G3 | 신청 상태 select 는 **승인/대기/반려 3종만** — "취소" 는 옵션에 없음 (사용자 전용 상태) | prototype.html L2731~2735 (동일 패턴 user-detail L1891·L1910) | A5 비고에 취소=뱃지 언급 | CANCELLED 신청은 상태 변경 UI 비활성 (뱃지 고정). 상태 머신 §3-2 반영 |
| G4 | "대기" 로 **되돌리기** 가능 (select 에 대기 존재) | prototype.html L2733 | 도메인 메서드 없음 (`approve`/`reject` 만) | `Application.resetToPending(User admin)` 신설. 사용자 알림은 미발행 (알림 타입 없음) — §3-2 |
| G5 | 승인/반려 일시 표시 블록 (`hasStatusDate`) | prototype.html L2717~2725 | — | 기존 `processedAt` 바인딩으로 충족 (컬럼 신설 불필요) |

### 1-B. 데이터 모델 gap 표 (이번 범위분 — ADMIN-00 §3 의 부분집합 + 신규)

> ADMIN-00 §3-1(Program)·§3-3(Applicant) 전량 열거는 마스터 참조. 아래는 **Phase 1·2 가 실제로 만드는 것만**.

#### Phase 1

| prototype 필드/기능 | 현재 엔티티 | 조치 |
|---|---|---|
| `Applicant.name/email/gender/phone` (tsx L154~159) | `Application.user` 경유 ✅ (User 에 name·email·gender·phone 존재) | `@EntityGraph` fetch join — 신규 repo 메서드 |
| `Applicant.appliedAt` (tsx L160) | `appliedAt` ✅ | 유지 |
| `Applicant.visits` 참여횟수 (tsx L161) | ❌ 컬럼 불필요 | 파생 — `ApplicationRepository` 에 user 별 APPROVED count 일괄 조회 쿼리 추가 (N+1 방지, 기존 `countByProgramIdsAndStatuses` 패턴) |
| `Applicant.status` (tsx L162) | `ApplicationStatus` 4종 ✅ | 라벨 매핑 (PENDING=대기, APPROVED=승인, REJECTED=반려, CANCELLED=취소) — enum 에 `getLabel()` 추가 |
| 담당자 의견 (html L2738~2739) | `rejectReason`(500) 만 존재 | **`Application.adminComment` 컬럼 추가 (length 500)** — Q6 확정분. 저장 정책 Q12 |
| 상태 변경 이력 (wireframe · Q6 병합) | ❌ | **`ApplicationStatusHistory` 엔티티 신설** — §5-3 감사 설계 |
| 신청 요약 (신청 N·승인 N·정원 N — html L1599~1607) | ❌ 컬럼 불필요 | 파생 — `countByProgramAndStatusIn` 재사용 |
| `Program.center` (센터 격리 기준) | ❌ `organization` 문자열만 | **Q2 확정분 조기 착수 (Q11)** — `@ManyToOne Center center` FK + 시드 organization→Center name 매칭 백필. 격리 E2E (ADMIN-00 §7-4 상시 규칙) 의 전제 |
| 관리자 계정·RBAC | ✅ P0-2 완료 (UserRole·User.center/centerScope·시드 3계정·`/admin/**` 매처) | 재사용 — 변경 없음 |

#### Phase 2 (P0-3 + A2/A3 부분)

| prototype 필드/기능 | 현재 엔티티 | 조치 |
|---|---|---|
| 썸네일 업로드 (html L2442~2448) | `Program.imageUrl` ✅ (URL 문자열) | 실 업로드 연동 — `FileStorageService` 결과 URL 저장 |
| 첨부파일 PDF/HWP (html L2522~2529) | ❌ | **`ProgramAttachment` 엔티티 신설** — `NoticeAttachment` 패턴 복제 (fileName/storedName/fileSize/contentType/sortOrder) |
| `applyPeriod` 신청 기간 (html L2474~2481) | ❌ | **`applyStartDate`/`applyEndDate` 컬럼 추가** (ADMIN-00 §3-1) + `getStatus()`·D-day 파생을 신청기간 기준으로 정정 |
| `venue` 진행 장소 (html L2485~2487) | ❌ | 컬럼 추가 |
| `contact` 문의처 (html L2489~2491) | ❌ | 컬럼 추가 |
| `views` 조회수 (tsx L137 인근 Program 모델) | ❌ | `viewCount` 컬럼 + 상세 진입 증가 (Notice 패턴) |
| 승인 여부 자동/수동 (html L2537~2547 — **신규 발견 G1**) | ❌ | **`approvalMode` enum 컬럼 (AUTO/MANUAL, 기본 MANUAL)** — Q13 |
| 약관 정보 탭 | ❌ | Q3 확정 — Program 컬럼 3개 (제공 여부·약관명·내용) |
| 자격요건 입력 | `ProgramEligibility` @Embeddable ✅ (F4) | admin 폼에 3필드 입력 추가 — prototype 폼에 없음 → 배치 Q17 |
| `NoticeAttachment` 실파일 | 메타데이터 stub ✅ | storedName 을 실 저장 키로 승격 + 다운로드 컨트롤러 (Phase 3 공지 CRUD 와 함께, 업로드 인프라는 Phase 2) |
| 신청 질문/답변/강좌 | ❌ | **이번 트랙 제외** (Q16) — F0c-dynamic-fields 후속 |

### 1-C. 데이터 소비 지점 (CLAUDE.md 2026-07-14 규칙 — Phase 1 의 쓰기 데이터가 표시되는 모든 화면)

| 소비 지점 | 참조 | 현재 상태 | 갭 |
|---|---|---|---|
| admin 신청 현황 테이블 상태 뱃지 | prototype.html L1662 `ap.statusCell` + tsx L69~74 색상 | 이번 신규 | — |
| admin 신청 상세 모달 (상태·의견·처리 일시) | prototype.html L2727~2741 | 이번 신규 | — |
| **사용자 알림 벨** — 승인/반려 메시지 | `ApplicationNotificationListener` L37·L54 (반려 메시지에 `rejectReason` 포함) | ✅ 기구현 | 반려 의견이 `rejectReason` 에 저장되어야 알림 문구에 노출 → Q12 |
| **사용자 마이페이지 신청 내역** — 상태 뱃지·반려 사유 | `templates/mypage/history.html` | ✅ 기구현 (rejectReason 노출) | 승인 시 담당자 의견 노출 여부 — prototype(사용자) 미확인 → Phase 1 은 미노출, 알림·반려 사유만 (Q12 비고) |
| 신청 완료 페이지 (`/apply/complete`) | 승인 알림 링크 타깃 (Listener L43) | ✅ 기구현 | — |
| 감사 이력 (ApplicationStatusHistory) | admin 모달 내 이력 리스트 (wireframe 유래, Q6) | 이번 신규 | 이력 표시는 모달 내 최소 리스트 (처리자·일시·전이·의견) |

---

## 2. 변경 범위 (파일 단위 — Phase 별 전체 목록)

### Phase 1 / PR-1 `feature/A1-admin-shell` (경량)

- [ ] `templates/admin/layout.html` + `templates/admin/fragments/header.html` — 다크 헤더 56px sticky `#111827`, GNB (통계·프로그램 관리·사용자 관리 — 미구현 메뉴는 자리+disabled), 유저 드롭다운 (내 정보/사용자 페이지/로그아웃)
- [ ] `templates/admin/fragments/icons.html` — prototype 인라인 SVG 이식 (이모지 금지 규칙)
- [ ] `templates/admin/login.html` — 밝은 헤더 + 420px 카드 (ADMIN-00 §2-2)
- [ ] `templates/admin/dashboard.html` — 간이 (스탯 카드: 프로그램 수·대기 신청 수 정도, 최근 프로그램 리스트)
- [ ] `src/main/java/...admin/AdminDashboardController.java` — `admin/` 패키지 신설 (ADMIN-00 §2-1)
- [ ] `static/css/admin.css` — `:root` 토큰 (tsx L24~55 실측치). **main.css 오염 금지**
- [ ] `SecurityConfig.java` — `/admin/login` 뷰 연결 + 로그인 성공 시 role 기반 타깃 분기 (successHandler). 단일 filter chain 유지 (학습 메모: dual `@Order` chain 대안 비교는 impl 시 주석으로)
- [ ] `templates/fragments/header.html` (사용자) — §8-1 확정분: `sec:authorize="hasAnyRole('CENTER_ADMIN','SYSTEM_ADMIN')"` 조건부 "관리자 페이지" 링크
- [ ] `src/test/java/...admin/AdminShellRenderTest.java` — 비인가 302/403, GNB 렌더, USER 계정 링크 미노출

### Phase 1 / PR-2 `feature/A4-admin-approvals`

- [ ] `application/Application.java` — `adminComment` 필드 + `resetToPending(User admin)` + approve/reject 에 comment 인자 반영
- [ ] `application/ApplicationStatusHistory.java` + `ApplicationStatusHistoryRepository.java` — 신설 (§5-3)
- [ ] `application/ApplicationService.java` — approve/reject 시그니처 확장 (comment) + `resetToPending` + 이력 append (동일 트랜잭션)
- [ ] `application/ApplicationRepository.java` — 프로그램별 신청자 목록 `@EntityGraph(user)` + user 별 참여횟수 일괄 count 쿼리
- [ ] `application/ApplicationStatus.java` — `getLabel()` (대기/승인/반려/취소)
- [ ] `program/Program.java` — `@ManyToOne Center center` FK (Q11 채택 시)
- [ ] `common/DataInitializer.java` — Program 시드에 center FK 연결 (organization 명 → Center name 매칭. **파생 시드 금지 규칙 준수** — 각 시드 row 에 명시 연결, forEach 파생 없음)
- [ ] `admin/AdminProgramController.java` — 간이 목록 `GET /admin/programs` (read-only 테이블: No./프로그램명/센터/신청현황/상태) + 상세 `GET /admin/programs/{id}` (신청 현황 테이블)
- [ ] `admin/AdminApplicationController.java` — `POST /admin/applications/{id}/status` (status+comment, CSRF 토큰 필수) — HTMX 부분 갱신 (`HX-Request` 분기, F0f 기구현 패턴)
- [ ] `admin/AdminProgramQueryService.java` — 센터 격리 강제 조회 (ADMIN-00 §2-4-3: Repository 파라미터로 유효 센터 강제)
- [ ] `templates/admin/program/list.html`, `templates/admin/program/detail.html`, `templates/admin/program/detail-fragments.html` (**fragment 별도 파일 규칙**), 신청 상세 모달 fragment
- [ ] `templates/admin/fragments/confirm-dialog.html` + `static/js/admin-confirm.js` — 파괴적/상태 변경 액션 확인 모달 (HANDOFF §최신 기능 2. `hx-confirm` 금지 — ADMIN-00 §1-B)
- [ ] `src/test/java/...` — `AdminApprovalFlowTest` (서비스: 상태 전이·이력·이벤트), `AdminProgramRenderTest`, 센터 격리 슬라이스 테스트 (`@WithMockUser(roles=...)`)
- [ ] `e2e/tests/admin-approval-cycle.spec.ts` — §5-2 왕복 시나리오

### Phase 2 / PR-3 `chore/P0-3-file-upload`

- [ ] `build.gradle.kts` — S3 호환 SDK (또는 Supabase Storage REST 클라이언트) 의존성
- [ ] `application.yml` — `spring.servlet.multipart` (max-file-size 10MB, max-request-size 20MB) + `storage.*` 설정 (버킷·엔드포인트·키는 환경변수)
- [ ] `common/storage/FileStorageService.java` (인터페이스) + `SupabaseStorageService` (prod) + `LocalDiskStorageService` (local/e2e 프로파일) — Q4 확정 구조
- [ ] `common/storage/FileValidator.java` — 확장자 화이트리스트·크기·MIME sniffing (§5-4)
- [ ] `src/test/java/...storage/` — Validator 단위 + LocalDisk 통합

### Phase 2 / PR-4 `feature/A2-admin-programs-list` (핵심만)

- [ ] PR-2 의 간이 목록을 A2 스펙으로 승격: 상태 필터 세그먼트·검색·페이지네이션 8건·빈 상태 (CSV·일괄선택·3뷰는 A2 잔여로 이월)
- [ ] `Program` — `applyStartDate/applyEndDate`·`venue`·`contact`·`viewCount` 컬럼 + `getStatus()` 신청기간 기준 정정 (**사용자 화면 D-day 표기 회귀 주의** — ProgramCardDto·detail 소비 지점 재검증)

### Phase 2 / PR-5 `feature/A3-admin-program-form` (부분)

- [ ] `templates/admin/program/form.html` — 탭 1 프로그램 정보 (썸네일 업로드 + 기간 2종 + 장소·문의처·센터·정원 + 상세 내용 + 첨부) / 탭 3 약관 정보. 탭 2 신청 정보는 **승인 여부 radio 만** (Q13) — 질문 빌더 제외 (Q16)
- [ ] `program/ProgramAttachment.java` + Repository — 신설
- [ ] `Program.approvalMode` + `ApplicationService.apply()` 자동 승인 분기 (Q13)
- [ ] `ProgramEligibility` 3필드 입력 (Q17)
- [ ] Bean Validation — HANDOFF §최신 기능 1 규칙 매핑 ("신청 마감 < 진행 시작, 시작 ≤ 종료", 양수 정원) + 인라인 에러 (`#EF4444` 보더 + 11px 텍스트, html L2454 패턴). 메시지 톤: admin 은 HANDOFF 존댓말 톤 (ADMIN-00 §1-B 주의 — 사용자 페이지 "~해야 합니다" 와 별도)

### Phase 3 (개요만 — 착수 시 Phase 별 상세 spec 갱신)

- PR-6 공지 CRUD: `admin/AdminNoticeController` + form (첨부 업로드 = NoticeAttachment 실파일 승격 + 다운로드 스트리밍) — 화면 디자인 Q14
- PR-7 SiteImage CRUD: slot 목록 + 이미지 교체 (업로드) + sortOrder/isActive — 백로그 ⑨. `SiteImage.update()` 기존 도메인 메서드 재사용
- PR-8 센터 CRUD: A9 (Q1 확정 — **착수 전 디자인 번들 갱신 확인** 조건 그대로)

---

## 3. 핵심 설계

### 3-1. 화면·컴포넌트 명세 (Phase 1 신청 현황 테이블 — prototype.html L1638~1664)

| 컬럼 | 폭 (grid) | 데이터 소스 | 비고 |
|---|---|---|---|
| (체크박스) | 44px | — | Phase 1 은 렌더만·기능 없음 (일괄 액션 Q18 이월). 네이티브 checkbox `accent-color:#3F30E9` |
| No. | 44px | 행 번호 | Inter 폰트 |
| 이름 | 100px | `application.user.name` | |
| 이메일 | 1fr | `user.email` | ellipsis |
| 성별 | 44px | `user.gender` 라벨 | |
| 핸드폰번호 | 120px | `user.phone` | |
| 접수일시 | 150px | `appliedAt` (`YYYY-MM-DD HH:mm`) | |
| 참여횟수 | 80px | user 별 APPROVED count (일괄 쿼리) | |
| 상태 | 90px | `status.getLabel()` + `applicantBadge` 색 (tsx L69~74: 승인 `#D1FAE5/#047857`, 대기 warning, 반려·취소 `#FEE2E2/#DC2626`) | 클릭 → 신청 상세 모달 |

테이블 헤더 요약: `신청 {PENDING+APPROVED}명 · 승인 {APPROVED}명 · 정원 {capacity}명` (html L1599~1607).

**신청 상세 모달** (html L2677~2749): 신청자 정보 그리드 6항목 → (Phase 1 생략: 답변) → 처리 일시 (`processedAt` 존재 시, L2717~2725) → 상태 select (승인/대기/반려 — 취소 제외 G3) → 담당자 의견 textarea (선택) → 취소/확인. 확인 클릭 시 **확인 모달 1단계** (반려는 파괴적 톤: "…반려할까요?") 후 POST.

### 3-2. ApplicationStatus 상태 머신 (admin 액션 관점)

```
                    ┌────────────────────────────────┐
                    │  (사용자) apply / reapply       │
                    ▼                                │
   ┌──────────► PENDING ◄──────────────┐             │
   │              │  │                 │ resetToPending (G4)
   │   approve    │  │  reject         │  이벤트 없음·이력만
   │   +이벤트     │  │  +이벤트(사유)    │
   │              ▼  ▼                 │
   │        APPROVED  REJECTED ────────┤
   │              │        │           │
   │  reject      │◄───────┘ approve   │   ※ APPROVED↔REJECTED 상호 전이 허용
   │              │                    │     (모달 select 3종 자유 선택)
   │              ▼                    │
   └───── (사용자) cancel ──► CANCELLED ── admin 변경 불가 (G3: select 미노출, 뱃지 고정)
                                       └─ (사용자) reapply → PENDING
```

- **이벤트 발행 지점**: `ApplicationService.approve()` L100~105 / `reject()` L119~125 — 커밋 전 `publishEvent`, `ApplicationNotificationListener` 가 AFTER_COMMIT 에 사용자 알림 생성. **기존 코드 그대로 재사용, 리스너 수정 없음**.
- idempotent 규칙 유지: 동일 상태 재요청 = no-op·이벤트 미발행 (ApplicationService L95·L114). `resetToPending` 도 동일 규칙.
- 이력 append 는 상태가 실제로 변한 경우에만, **상태 변경과 동일 트랜잭션** (원자성 — 이력 없는 상태 변경 금지).

### 3-3. 파일 업로드 설계 (Phase 2 — Q4 확정: Supabase Storage)

**저장소 비교 (학습 기록용 — 결정은 ADMIN-00 Q4 에서 완료)**:

| 기준 | 로컬 파일시스템 | Supabase Storage ✅ | AWS S3 |
|---|---|---|---|
| 비용 | 0 | Free tier 1GB (기존 프로젝트 재사용, 추가 비용 0) | 프리티어 후 과금 + 계정·IAM 신규 셋업 |
| Fly.io 배포 호환 | ❌ 디스크 ephemeral — 재배포 시 소실 (volume 붙이면 단일 리전 제약) | ✅ 외부 오브젝트 스토리지 | ✅ |
| 학습 가치 | 낮음 (그러나 Storage 추상화 인터페이스 학습에는 충분) | **S3 호환 API** — S3 SDK 로 접근 가능해 S3 학습 겸용 + RLS/공개 버킷 개념 | 표준이지만 셋업 비용 큼 |
| 결론 | local/e2e 프로파일 구현체 | **prod 구현체** | 채택 안 함 (Supabase 가 S3 호환이라 학습 목적 중복) |

**presigned URL 업로드 흐름 (개념 — 학습 규칙 §협업 1)**:

```
[서버 경유 방식 — Phase 2 1차 채택 권장 (Q15)]
브라우저 ──multipart/form-data──► Spring (MultipartFile)
        ── FileValidator 검증 ──► FileStorageService.store() ──► Supabase Storage
        ◄── 공개 URL + 메타데이터 저장 (ProgramAttachment row)

[presigned URL 방식 — 학습 확장 단계]
1. 브라우저 → 서버: "이 파일 올릴게요" (파일명·크기·MIME)
2. 서버: 검증 후 스토리지에 서명된 임시 업로드 URL 발급 요청 → 브라우저에 반환
   (서명 = 자격증명 없이도 '이 경로에 이 조건으로 N분간 PUT 가능' 을 증명하는 토큰)
3. 브라우저 → 스토리지에 직접 PUT (서버 대역폭·메모리 우회 — 대용량에 유리)
4. 브라우저 → 서버: 업로드 완료 통지 → 서버가 메타데이터 저장 (+존재 확인)
트레이드오프: 서버 부하↓ vs 흐름 복잡도↑(2왕복+완료 통지 정합성), 검증이 사전 신고 기반
  → 10MB 이하 소용량·학습 단계에서는 서버 경유가 단순·안전. presigned 는 이후 리팩터 티켓으로
```

**파일 검증 (`FileValidator`)**: ① 확장자 화이트리스트 (썸네일: jpg/jpeg/png/webp, 첨부: pdf/hwp/hwpx) ② 크기 (10MB) ③ **MIME sniffing** — `Content-Type` 헤더 신뢰 금지, 매직 바이트 검사 (Tika 도입 vs 수동 시그니처 비교는 impl 시 비교표) ④ 저장 파일명은 UUID 재생성 (`storedName`), 원본명은 `fileName` 에만 (경로 조작·XSS 차단) ⑤ Content-Disposition 다운로드 시 RFC 5987 인코딩 (한글 파일명).

**NoticeAttachment 연동**: 기존 엔티티 스키마 그대로 (`storedName` 을 스토리지 키로 승격 — 엔티티 주석의 예정 사항 이행). 다운로드는 `GET /notices/{id}/attachments/{attachmentId}` 스트리밍 or 공개 버킷 URL redirect — Phase 3 공지 CRUD 에서 확정.

### 3-4. 권한 모델 + 감사 이력 (GMP 어필 포인트)

**권한 (P0-2 기구현 재사용)**: `/admin/**` → `hasAnyRole('CENTER_ADMIN','SYSTEM_ADMIN')` (SecurityConfig L78~79) + 시드 3계정 (sysadmin / center1 / center2 — 센터 상이, 격리 테스트용). Phase 1 추가분은 **데이터 스코프**: `AdminProgramQueryService` 가 principal 의 유효 센터를 Repository 파라미터로 강제 (ADMIN-00 §2-4-3 — 컨트롤러에서 누락 불가능한 구조). SYSTEM_ADMIN 은 전체, CENTER_ADMIN 은 `user.center` 고정. 센터 스코프 셀렉터 (헤더) 는 A1 잔여로 이월 — Phase 1 은 SYSTEM_ADMIN=전체 고정.

**감사 이력 — `ApplicationStatusHistory`** (ALCOA+ 대응: Attributable·Contemporaneous·Original):

| 컬럼 | 타입 | 의미 (ALCOA) |
|---|---|---|
| `id` | PK | — |
| `application` | `@ManyToOne(LAZY)` FK | 대상 (Original) |
| `fromStatus` / `toStatus` | `@Enumerated(STRING)` | What |
| `comment` | varchar(500) | Why (담당자 의견 스냅샷 — Application.adminComment 는 '현재값', 이력은 '당시값') |
| `changedBy` | `@ManyToOne(LAZY)` User FK | Who (Attributable) |
| `changedAt` | `@CreatedDate` | When (Contemporaneous) |

- **append-only**: `@Setter` 금지는 물론 update/delete 도메인 메서드 자체를 두지 않음. 수정 = 새 row (GMP 감사 추적 원칙 — 지우지 않는다).
- `Application.processedBy/processedAt` 은 "최종 처리자" 요약 (기존 유지), History 는 전체 궤적. 모달의 이력 리스트·처리 일시 블록 (G5) 데이터 소스.
- 사용자 취소 (`cancel`) 도 이력 대상 (changedBy = 본인) — 승인→취소→재신청 흐름 추적 가능.

### 3-5. 관리자 CRUD 실효성 체크 (CLAUDE.md 규칙 — Phase 별 확인 항목)

| # | 항목 | 확인 방법 |
|---|---|---|
| 1 | **시드 덮어쓰기 방지**: admin 이 승인한 신청·수정한 프로그램이 재기동 후 유지되는가 | `DataInitializer` 는 `existsBy*` idempotent skip 구조 ✅ — Phase 1 에서 center FK 백필 추가 시에도 "이미 연결된 row skip" 조건 필수. 검증: 상태 변경 → 재기동 → 값 유지 assertion (동적 검증 절차에 포함) |
| 2 | **파생 로직 무력화 방지**: 편집값을 런타임 파생이 덮지 않는가 | `Program.getStatus()` 는 파생 (DB 컬럼 아님 — HANDOFF 확정) 이라 충돌 없음. Phase 2 `applyStartDate` 도입 시 status 파생 소스가 바뀌므로 **사용자 화면 D-day·CTA 5분기 (F0f-fix-1) 회귀 테스트 필수** |
| 3 | **파생 시드 금지**: center FK 백필이 forEach 파생 패턴이 아닌가 | 시드 각 Program.builder() 에 center 명시 연결 (기존 organization 문자열이 실제 센터명과 1:1 — DataInitializer L482~ 확인됨) |
| 4 | Flyway 활성 후 `ddl-auto: validate` 전환 시 신규 컬럼·테이블이 마이그레이션 파일로 존재하는가 | §6 의존 규칙 |

---

## 4. 갭 리스트 (현재 코드 vs 이번 범위 prototype)

| # | 항목 | 현재 상태 | prototype/목표 | Phase | 우선순위 |
|---|---|---|---|---|---|
| 1 | admin 화면 전무 (`templates/admin` 없음, admin 패키지 없음) | ❌ | shell + 신청 관리 | 1 | 높음 |
| 2 | `/admin/login` — 매처만 permitAll, 뷰·성공 분기 없음 (SecurityConfig L72~74 주석) | 부분 | 별도 로그인 페이지 + role 분기 | 1 | 높음 |
| 3 | 승인/반려 진입점 — 서비스는 있으나 호출 UI 없음 | 백엔드만 ✅ | 신청 현황 테이블 + 모달 | 1 | 높음 |
| 4 | 담당자 의견·상태 이력 (Q6 확정분) | ❌ | adminComment + History | 1 | 높음 |
| 5 | 센터 데이터 격리 — role 은 있으나 Program 에 center FK 없어 격리 불가 | ❌ | center FK + 스코프 강제 조회 | 1 (Q11) | 높음 |
| 6 | 사용자↔관리자 상호 진입 동선 (§8-1 확정) | ❌ | 헤더 드롭다운 조건부 링크 | 1 | 중간 |
| 7 | multipart 설정·스토리지 서비스 없음 | ❌ | FileStorageService 2구현체 | 2 | 높음 |
| 8 | `Program` 신청기간·장소·문의처·조회수·approvalMode 부재 | ❌ | §1-B Phase 2 표 | 2 | 중간 |
| 9 | `NoticeAttachment` 실파일 stub (엔티티 주석에 승격 예정 명시) | 메타만 ✅ | 실 저장·다운로드 | 2~3 | 중간 |
| 10 | SiteImage CRUD (백로그 ⑨ — "관리자 페이지 첫 진입 시 우선 대상") | 엔티티+update() ✅ | slot 관리 화면 | 3 | 중간 |
| 11 | 공지 CRUD 화면 | ❌ (prototype 에도 화면 없음) | Q14 | 3 | 낮음 |

---

## 5. 검증 시나리오 (정적 / 동적 / E2E 분리 — ym-qa 실행 항목)

### 5-1. 정적 (매 PR)

- `compileJava` + 신설 테스트 클래스 전체
- **Phase 1**: `AdminShellRenderTest`·`AdminProgramRenderTest` (Thymeleaf 실 렌더 — F0h-c2 규칙) / `AdminApprovalFlowTest`: PENDING→APPROVED 이벤트 1회 발행, 동일 상태 재요청 no-op·이벤트 0회, REJECTED→APPROVED 상호 전이, CANCELLED 변경 시도 거부, History row 생성 (from/to/changedBy/comment), resetToPending 이벤트 미발행·이력 생성
- RBAC 슬라이스: `@WithMockUser(roles="USER")` → `/admin/**` 403, CENTER_ADMIN → 타 센터 programId 접근 403/404
- **Phase 2**: `FileValidatorTest` (확장자 위조 — `.png` 명·PDF 매직바이트 → 거부, 10MB 초과 거부), 폼 Bean Validation 경계 (마감>시작 역전)

### 5-2. 동적 (bootrun-e2e 8090 + curl — 회사 PC 필수 경로)

- `GET /admin` 비인증 → 302 `/admin/login` (또는 로그인), USER 세션 → 403
- admin 세션 (cookie jar 로그인) → `GET /admin/programs/{id}` 200 + 신청 현황 테이블 마크업 (`접수일시`·상태 뱃지 class) + Thymeleaf 표현식 잔존 0건
- `POST /admin/applications/{id}/status` — CSRF 토큰 **없이 403**, 토큰 포함 정상 → 재조회 시 상태 반영 (**write→read 왕복**)
- 상태 변경 후 **사용자 계정 cookie jar** 로 `GET /notifications` 응답에 "신청이 승인되었습니다" 마크업 (왕복 규칙 — 알림 소비 지점)
- `/css/admin.css` 200 · 사용자 페이지 `/css/main.css` 회귀 200
- 재기동 후 상태·이력 유지 (실효성 체크 §3-5-1 — local 프로파일에서 1회)

### 5-3. E2E (Playwright — `e2e/tests/admin-approval-cycle.spec.ts` 신설)

**왕복 시나리오 (write→read)**:
1. 사용자 (기존 시드 계정) 로그인 → 프로그램 신청 (PENDING 생성 — 테스트가 자체 데이터 생성)
2. 로그아웃 → `center1@youth-moa.test` (또는 sysadmin) 로그인 → `/admin/programs/{id}` → 해당 신청 행 확인 (이름·접수일시·"대기" 뱃지)
3. 상태 모달: "승인" 선택 + 의견 입력 → 확인 모달 → 저장 → 뱃지 "승인"·처리 일시 표시
4. 로그아웃 → 사용자 재로그인 → 알림 벨 뱃지 +1 → 드롭다운 "신청이 승인되었습니다" → 마이페이지 신청 내역 상태 "승인"
5. (반려 변형) 반려 + 사유 → 사용자 알림 문구에 사유 포함 (Listener L54 포맷)

**센터 격리 시나리오 (ADMIN-00 §7-4 상시 규칙)**: `center2@youth-moa.test` 로그인 → 목록에 center1 프로그램 미노출 + center1 프로그램 상세 직접 URL → 403/404.

**prototype 시각 대조** (화면 신설 PR 규칙): Claude Preview snapshot ↔ `admin/prototype.html` 신청 현황·모달 — 컬럼 수(9)·요약 헤더 구성·모달 섹션 순서·CTA 위치 4항목 표.

### 5-4. Phase 2 업로드 검증

- 정적: Validator 단위 / 동적: multipart POST → 200 + `ProgramAttachment` row + 스토리지 객체 존재, 11MB → 에러 메시지 렌더 (413 아닌 폼 인라인 에러), `.exe` 위장 → 거부 / E2E: 폼에서 썸네일 업로드 → 저장 → **사용자 프로그램 상세에 이미지 렌더** (write→read: admin 쓰기 → 사용자 화면 읽기)

---

## 6. 의존성 / 병렬 티켓 충돌 분석

### 6-1. `chore/flyway-activation` (P0-1 활성화 — 별도 진행 중) 과의 관계

- **규칙**: flyway 활성화 티켓이 **먼저 머지되면** 본 트랙의 스키마 변경 (adminComment·history 테이블·center FK·Phase 2 컬럼들) 은 `V{n}__*.sql` 마이그레이션 파일로 작성 (`ddl-auto: validate` 전제). **아직이면** `ddl-auto: update` 가 처리하되, baseline dump 시점에 포함되도록 **flyway 티켓 담당 세션에 스키마 변경 사실 공유 필수** (STATE.md 경유).
- 충돌 파일: `application.yml` (flyway 블록 — 이번 트랙은 multipart 만 추가, 블록 상이하여 auto-merge 가능) / **`DataInitializer.java` (HIGH)** — flyway 티켓이 시드 재정리 예정 + Phase 1 이 center FK 백필 추가. **머지 순서 합의 필요: flyway 먼저 권장** (ADMIN-00 §9 순서와 일치). Phase 1 PR-2 착수 전 flyway 티켓 상태 확인을 절차에 포함.

### 6-2. 병렬 티켓 (SSE 알림 · 캐싱) 충돌 표

| 파일/영역 | ADMIN-01 Phase 1 | SSE 알림 티켓 (예상: notification 패키지·헤더 fragment·JS) | 캐싱 티켓 (예상: ProgramService/HomeService·CacheConfig·build.gradle) | 판정 |
|---|---|---|---|---|
| `notification/*` | **수정 없음** (Listener·Service 재사용만) | 수정 | — | ✅ 무충돌 — Phase 1 이 notification 패키지를 안 건드리는 것이 설계 의도 |
| `ApplicationService.java` | 시그니처 확장 | 이벤트 발행부 접점 가능성 낮음 | — | 낮음 — 착수 시 상호 diff 확인 |
| `SecurityConfig.java` | successHandler·/admin/login | SSE 엔드포인트 매처 추가 가능 | — | **중간** — 매처 블록 인접 수정. 머지 순차 + rebase 1회 |
| `ProgramService/ProgramController` | **수정 없음** (admin 전용 `AdminProgramQueryService` 신설 — ADMIN-00 §2-1 허용 구조) | — | 수정 | ✅ 무충돌 설계 |
| `templates/fragments/header.html` (사용자) | 드롭다운 링크 1행 추가 | 알림 벨 수정 가능성 | — | **중간** — 순차 머지 |
| `build.gradle.kts` | Phase 2 에서 스토리지 SDK | — | 캐시 라이브러리 | 낮음 (의존성 블록 병합 용이) |
| `main.css` | **수정 없음** (admin.css 분리) | 수정 가능 | — | ✅ |
| `DataInitializer.java` | center FK 백필 | — | — | flyway 티켓과만 충돌 (§6-1) |

### 6-3. 파생 큐 판단 (티켓 요청 항목)

| 파생 큐 | 판단 | 근거 |
|---|---|---|
| `F0c-dynamic-fields` (강좌·질문·첨부 — "admin 선행 필요") | **이번 트랙 제외, 첨부만 부분 소화** | 첨부 업로드 인프라 (P0-3) 와 ProgramAttachment 는 Phase 2 가 제공 → F0c 잔여는 ApplyQuestion/ApplyAnswer/Course 만 남아 A3-full 티켓으로 축소 (Q16) |
| F4 자격요건 admin 입력 | **Phase 2 포함** | `ProgramEligibility` 스키마 기성 — 폼 필드 3개 추가로 파생 큐 종결 (Q17) |

---

## 7. 작업 큐 메타

| 항목 | 값 |
|---|---|
| 작업 ID | ADMIN-01 (Phase 1 = PR-1·PR-2, Phase 2 = PR-3~5, Phase 3 = PR-6~8) |
| 우선순위 | Phase 1 > Phase 2 > Phase 3 (순차. Phase 내 PR 도 순차 — 데이터 모델 의존) |
| 추정 | Phase 1: 2 PR (M+L) / Phase 2: 3 PR (M+M+L) / Phase 3: 3 PR (각 M) |
| 상태 | `spec_done` |
| 갱신 규칙 | Phase 2·3 착수 시 본 문서 해당 절 상세화 (별도 spec 파일 신설 대신 본 문서 개정 — ADMIN-00 재작성 금지 유지) |

---

## 8. 사용자 결정 필요 항목 (Q11~ — ADMIN-00 Q1~Q10 과 번호 연속)

| # | 질문 | 제안 (권장안) |
|---|---|---|
| **Q11** | `Program.center` FK 마이그레이션을 Phase 1 에 포함? (Q2 로 방향은 확정, **시점**만 결정) | **포함 권장** — 센터 격리 E2E 는 ADMIN-00 §7-4 상시 규칙이라 FK 없이는 Phase 1 격리 검증 자체가 불가. 시드 organization 문자열이 센터명과 매칭되어 백필 비용 낮음. 대안: Phase 1 은 SYSTEM_ADMIN 전용으로 축소하고 FK 는 Phase 2 로 |
| **Q12** | 담당자 의견 저장 위치 — prototype 은 의견이 "사용자에게 노출됩니다" (L2739) | **`adminComment` 단일 컬럼 신설 + 반려 시 동일 값을 `rejectReason` 에도 복사** (기존 알림 문구·마이페이지 반려 사유 소비 지점 무수정 호환). 승인 시 의견의 사용자 노출은 Phase 1 미구현 (사용자 prototype 에 노출 위치 없음 — 알림 메시지 포함 여부만 추가 결정) |
| **Q13** | `Program.approvalMode` (자동/수동 승인 radio — **ADMIN-00 gap 표 누락분**, html L2537~2547) | Phase 2 폼에 포함 + 컬럼 추가 (기본 MANUAL). AUTO 시 `apply()` 에서 즉시 approve (승인 알림 발행). 기존 시드·신청 흐름은 MANUAL 유지라 회귀 없음 |
| **Q14** | 공지 CRUD — admin prototype 17개 screen 에 **공지 관리 화면 없음** | A9 센터와 동일 접근: 디자인 번들 갱신 확인 → 없으면 §6 공통 컴포넌트 (dataTable·formField·confirmDialog) 기반 자체 설계. Phase 3 유지 |
| **Q15** | 업로드 1차 흐름 | **서버 경유 multipart 우선** (10MB 이하·단순·검증 확실). presigned URL 은 학습 확장 티켓 (`refactor/presigned-upload`) 으로 분리 — §3-3 개념 정리 완료 |
| **Q16** | F0c-dynamic-fields (ApplyQuestion/ApplyAnswer/Course + 드래그앤드롭) 이번 트랙 제외 확인 | 제외 — A3-full 후속 티켓. Phase 1 모달은 답변 섹션 없이 (hasAnswers=false 경로), Phase 2 폼 탭 2 는 승인 여부 radio 만 |
| **Q17** | F4 자격요건 3필드 (age/region/etc) 의 admin 폼 배치 — prototype 폼에 없는 필드 | 프로그램 정보 탭, 상세 내용 에디터 아래 3열 grid (공통 formField fragment). prototype 부재 필드이므로 시각 대조 리포트에 "prototype 외 추가" 명시 |
| **Q18** | 일괄 액션 (대기 전체 승인·선택 승인/반려·CSV — html L1611~1636) Phase 1 포함? | **제외 권장** — Phase 1 은 개별 상태 변경 + 모달로 사이클 완성에 집중. 일괄은 A4 잔여 (확인 모달·부분 실패 처리 등 부피 큼) |
| **Q19** | Phase 1 admin 로그인 구조 — 단일 filter chain + successHandler role 분기 vs `@Order` dual chain | **단일 체인 권장** (세션 공유 — §8-1 상호 진입 동선과 정합, 구현 단순). dual chain 은 학습 메모로만 기록 |

---

## 9. 다음 단계

명세 산출 완료. Q11~Q19 결정 반영 후 상태를 `spec_confirmed` 로 갱신하고 Phase 1 PR-1 (`feature/A1-admin-shell`) 부터 ym-impl 인계.
