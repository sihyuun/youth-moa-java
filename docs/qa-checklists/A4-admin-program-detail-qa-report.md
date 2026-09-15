# QA 리포트 — A4 admin-program-detail

| 메타 | 값 |
|---|---|
| 브랜치 | `feature/A4-admin-program-detail` |
| 최초 commit | `e5e86d9` (ym-impl 산출물) |
| fix commit | **`ced3e8c`** (LazyInit fetch join + spec deviation 3건 확정) |
| spec | [`docs/specs/A4-admin-program-detail.md`](../specs/A4-admin-program-detail.md) `spec_confirmed` · §13 deviation 3건 추가 |
| QA 실행 | 2026-09-15 (재검증 · 회사 PC · JDK 17 부트스트랩 · e2e 프로파일 H2 · port 8090) |
| **판정** | **✅ PASS — 반려 4건 모두 해소 (P0 RESOLVED · Δ 3건 DEVIATION 확정)** |

---

## 재검증 요약

- **P0 (LazyInitializationException)**: **RESOLVED** — `AdminApplicationService.list()` 에 fetch join 적용, `AdminApplicationListRenderTest` 4/4 PASS, curl 실측 500 → 200 회복
- **Δ1 PAGE_SIZE**: 실 코드 상수 `AdminApplicationService.PAGE_SIZE = 10` (line 49) 확인. verify 초기 관측 "20" 은 오독 — 이미 처음부터 10 이었음. spec §13 deviation 반영
- **Δ2 상세 UI 페이지 wrapper**: spec §13 deviation 반영
- **Δ3 endpoint 4개 분리**: spec §13 deviation 반영
- **회귀**: 전체 500 TC · 499 PASS / 1 FAIL (Testcontainers `contextLoads` — 회사 PC Docker 인프라 baseline · A4 무관 · 이전 QA 세션과 동일)

---

## 1. 정적 검증 ✅

### 1-A. 전체 회귀 (`./gradlew test`)

| 항목 | 결과 |
|---|---|
| 전체 TC | **500** (직전 469 → 신설 31 반영) |
| 성공 | **499** |
| 실패 | 1 (`YouthMoaApplicationTests.contextLoads` · Testcontainers Docker unavailable · **A4 무관 baseline**) |
| 성공률 | 99.8% |
| duration | 8m 12s |

### 1-B. A4 대상 3 클래스 (fix 재검증)

| 클래스 | TC | 결과 | 변동 |
|---|---|---|---|
| `AdminApplicationListRenderTest` | **4** | **✅ 4 PASS** | 직전 1 FAIL → 4 PASS (LazyInit 해소) |
| `AdminApplicationServiceTest` | **24** | **✅ 24 PASS** | 직전 22 → 24 (fix 세션에서 2 TC 추가된 것으로 파악, ym-impl 커밋 diff 확인) |
| `AdminApplicationDetailModalRenderTest` | **3** | **✅ 3 PASS** | 유지 |
| **합계** | **31** | **✅ 31 PASS** | |

### 1-C. Fix 코드 검토 (`AdminApplicationService.java:79~110`)

```java
Specification<Application> spec =
    (root, query, cb) -> {
      if (query != null && Long.class != query.getResultType()) {
        root.fetch("user");
      }
      return cb.equal(root.get("program").get("id"), programId);
    };
```

- ✅ `root.fetch("user")` 로 LazyInit 원천 차단 (spec §7 준수 · 옵션 A 채택)
- ✅ `Long.class != query.getResultType()` 가드로 count query 실행 시 fetch 미적용 (Hibernate 는 count 쿼리에 fetch join 걸면 예외 발생 → 필수 가드)
- ✅ 상태·q LIKE 필터도 spec 체이닝으로 이어져 동작 (Service Test `list_statusFilter_APPROVED_isolatesRows` PASS)

---

## 2. 동적 검증 ✅ (bootRun e2e · port 8090)

### 2-A. Endpoint 실측

| 시나리오 | 결과 |
|---|---|
| `GET /admin/programs/1/applications` (sysadmin) | **200 OK · 19,974 bytes** |
| `GET /admin/programs/1/applications?status=APPROVED` | **200 OK · 20,006 bytes** |
| `LazyInitializationException` 문자열 출현 | **0건** |
| 사용자명 렌더 (`app.user.name`) | `시드유저19` ~ `시드유저28` **10명 정상 렌더** |
| 페이지네이션 링크 | `page=0`, `page=1`, `page=2` — **3페이지** (총 28건 / PAGE_SIZE 10 → 3페이지 부합) |

### 2-B. 회사 PC 로그인 절차 (기록용)

- 로그인 URL: `/admin/login` (formLogin loginProcessingUrl)
- 파라미터: `username` (email 아님) + `password` + `_csrf`
- 시드 계정: `sysadmin@youth-moa.test` / `Admin!234` (`DataInitializer.java:173~180` · `@Value("${admin.seed.password.system:Admin!234}")` 기본값)

### 2-C. RBAC / scope 격리

- CENTER_ADMIN scope 격리는 Service 계층 `assertProgramInScope` 에서 검증 (미매칭 시 `IllegalAccessError`)
- `AdminApplicationServiceTest.assertProgramInScope_centerAdmin_otherCenterProgram_denied` PASS 로 커버 (정적 검증에서 검증됨)
- 익명·USER 403 는 SecurityConfig 의 `hasAnyRole("CENTER_ADMIN","SYSTEM_ADMIN")` 로 강제 (login redirect 302 재현 완료)

---

## 3. 회귀 검증 ✅

| 트랙 | 결과 |
|---|---|
| 사용자 apply flow · mypage · notification listener | 정적 회귀 499/500 안에 포함 · A4 관련 회귀 **0건** |
| admin 전체 (dashboard·programs·notice·term·eligibility·dynamic-fields) | 동 |
| V15 마이그레이션 (`admin_note`) | e2e 프로파일 H2 재기동 시 매핑 검증 통과 |
| 사전 QA session baseline | 동일 (Testcontainers 1건 FAIL 유지) |

---

## 4. 계약 신설 확인

| 파일 | 상태 |
|---|---|
| `docs/design-contracts/admin/program-applications.md` (117 lines) | ✅ 신설 · deviation 4건 · deferred 8건 명시 |
| `e2e/contracts/admin-program-applications.ts` (355 lines · 35 checks: exists 17 + text 15 + count 3) | ✅ 신설 |
| Playwright runner `tests/visual-admin-program-applications.spec.ts` | ⚠️ **미신설** — ym-verify 단계에서 신설 · 갭 0 최종 확인 이월 |

---

## 5. 시각 확인 (사용자 영역)

- 신청 목록 9열 grid 렌더·상세 페이지 · 상태 변경 후 배지 색상·요약 배지 카운트 갱신 등 시각 감성 요소는 **사용자 브라우저 확인 이월**
- 이번 QA 재검증은 커맨드라인 curl 렌더까지만 실측. 계약 Playwright runner spec 신설 시 자동 커버 예정

---

## 6. Δ deviation 로그 (spec §13 반영 완료)

| 항목 | spec 원안 | 구현 | 판정 | 근거 |
|---|---|---|---|---|
| `PAGE_SIZE` | Qn-6 A = 20 | **10** (`AdminApplicationService.PAGE_SIZE`) | **DEVIATION 확정** (spec §13 반영) | admin-notice/term/programs 페이지네이션 일관성 (A2 `ADMIN_PAGE_SIZE=10`) · 사용자 컨펌 |
| 상세 UI | Qn-B A = 480px HTMX 모달 | **별도 페이지 wrapper** (`_application-detail-modal.html` fragment 를 host page 로 감쌈) | **DEVIATION 확정** (spec §13 반영) | 신청 정보량 (신청자 정보 6칸 + F0c 답변 + 처리 이력 + adminNote 등) 이 많아 페이지가 자연스러움 · 사용자 컨펌 |
| 상태 변경 원자성 | Qn-Δ9 A = 원자적 단일 POST | **4 endpoint 분리** (approve/reject/cancel/note) | **DEVIATION 확정** (spec §13 반영) | REST 관례 · 상태 변경 명확성 · UX 상 큰 차이 없음 · 사용자 컨펌 |

verify 초기 관측 "PAGE_SIZE=20" 은 **오독** — grep 상수 확인 결과 처음부터 `10` 이었음. 이번 QA 세션에서 실 코드 재확인 완료.

---

## 7. 반려 4건 최종 상태

| 반려 항목 | 상태 | 근거 |
|---|---|---|
| **P0 LazyInitializationException** | **RESOLVED** | `AdminApplicationService.java:82~90` fetch join · `AdminApplicationListRenderTest` 4/4 PASS · curl 실측 200 |
| **Δ PAGE_SIZE** | **DEVIATION 확정** | spec §13 반영 · 실 코드 원래 10 (verify 오독) |
| **Δ 상세 UI 페이지** | **DEVIATION 확정** | spec §13 반영 · 사용자 결정 |
| **Δ endpoint 4분리** | **DEVIATION 확정** | spec §13 반영 · 사용자 결정 |

---

## 8. 판정 및 인계

- **판정: ✅ PASS**
- **머지 조건 (직전 단계)**:
  1. ✅ ym-impl 재실행 → fetch join 적용 (`ced3e8c`)
  2. ✅ `AdminApplicationListRenderTest.기본()` PASS 재현
  3. ✅ bootRun 실측 `GET /admin/programs/1/applications` 200 재현 (19,974 bytes · 시드유저28 등 10건 렌더)
  4. ✅ PAGE_SIZE 실 코드 확인 (10) + spec deviation 반영
- **다음 단계**: **`ym-verify` (적대적 검증) 호출 권장** — spec ↔ 구현 매핑 행 단위 재대조 · 계약 Playwright runner spec 신설 · 갭 0 확인 · PASS 시 머지 가능

---

## 부록 A · 실측 curl 응답 (LazyInit 완치 확인)

```
list: 200 bytes=19974
filter APPROVED: 200 bytes=20006
--- LazyInit search ---
0
--- user name render ---
시드유저19
시드유저20
...
시드유저28
--- pagination links ---
page=0
page=1
page=2
```

## 부록 B · Fix diff 요약

```diff
- Specification<Application> spec =
-     (root, query, cb) -> cb.equal(root.get("program").get("id"), programId);
+ Specification<Application> spec =
+     (root, query, cb) -> {
+       if (query != null && Long.class != query.getResultType()) {
+         root.fetch("user");
+       }
+       return cb.equal(root.get("program").get("id"), programId);
+     };
```

- 파일: `src/main/java/io/github/sihyuuun/youthmoa/admin/AdminApplicationService.java:79~90`
- spec §7 옵션 A 원안 (fetch join in Specification) 그대로 구현
- count query 가드 필수 (Hibernate)
