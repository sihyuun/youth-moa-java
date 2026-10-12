# QA 리포트 (3차): A5-2 승인 처리 페이지 — UNVERIFIED 4건 해소 재검증

- 작업 ID: A5-2 approval-bulk
- 선행: ym-verify 1차 (commit prefix `ace20e42`) → PASS 10 / UNVERIFIED 4
- ym-impl 3차 (commit prefix `a97d9e20`) 결과물 재검증
- 수행일: 2026-10-12
- 브랜치: `feature/A5-2-approval-bulk`
- 담당: ym-qa (3차)
- 상태: **qa_done — UNVERIFIED 4 → 0**

---

## 1. 입력 요약 (ym-impl 3차 산출물)

| 변경 분류 | 파일 | 비고 |
|---|---|---|
| E2E 추가 | `e2e/tests/admin-bulk-csv.spec.ts` | gender TC 1건 추가 (line 249~277) |
| Unit 신설 | `src/test/java/.../admin/AdminApplicationBulkControllerTest.java` | 7 TC (U2/U3/U4 커버) |
| 자체 셀프체크 | 23/23 PASS | ym-impl 보고 기준 |

## 2. 정적 검증 (Gradle unit test)

### 2-1. 타깃 테스트 재돌림 (23 TC)

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.14"
.\gradlew.bat test --tests "io.github.sihyuuun.youthmoa.admin.AdminApplicationBulk*" \
                   --tests "io.github.sihyuuun.youthmoa.admin.AdminCsvControllerTest"
```

**BUILD SUCCESSFUL in 4m 51s** — JaCoCo report 포함.

JUnit XML 집계 (`build/test-results/test/TEST-*.xml`):

| Test Class | tests | failures | errors | skipped | time (s) |
|---|---:|---:|---:|---:|---:|
| AdminApplicationBulkApproveAllPendingTest | 2 | 0 | 0 | 0 | 137.124 |
| AdminApplicationBulkControllerTest | **7** | **0** | **0** | **0** | 47.803 |
| AdminApplicationBulkRejectServiceTest | 5 | 0 | 0 | 0 | 0.681 |
| AdminApplicationBulkServiceTest | 4 | 0 | 0 | 0 | 0.902 |
| AdminCsvControllerTest | 5 | 0 | 0 | 0 | 1.011 |
| **합계** | **23** | **0** | **0** | **0** | — |

→ **23 / 23 PASS ✅**

### 2-2. admin.* 패키지 전수 회귀 (1차 PASS 자산 유지 확인)

```powershell
.\gradlew.bat test --tests "io.github.sihyuuun.youthmoa.admin.*"
```

**BUILD SUCCESSFUL in 6m 02s.**

| 지표 | 값 |
|---|---:|
| 테스트 클래스 수 | **46** |
| 테스트 메서드 수 | **374** |
| failures | **0** |
| errors | **0** |
| skipped | **0** |

→ admin 패키지 전역 regression **374 / 374 PASS ✅** · 신규 23 TC 포함, 기존 자산 회귀 0건.

## 3. UNVERIFIED → PASS 매핑

ym-verify 1차의 UNVERIFIED 4항목이 ym-impl 3차에서 실제로 어떤 TC로 커버됐는지 라인 번호 단위 매핑.

| UNVERIFIED 항목 | 해소 TC | 위치 | 결과 |
|---|---|---|---:|
| **U1** CSV 성별(gender) 컬럼 포함 + 값 집합 | `admin-bulk-csv.spec.ts` ::"CSV 내보내기 — 신청 목록 gender 컬럼 포함 + 값 MALE/FEMALE/빈값" | tests/admin-bulk-csv.spec.ts:249 | ✅ PASS |
| **U2** reason 200/201 경계 | `bulkReject_reason_200chars_success_flash` / `bulkReject_reason_201chars_rejects_with_length_flash` | AdminApplicationBulkControllerTest.java:54, :74 | ✅ PASS × 2 |
| **U3** ids 501 cap (approve + reject) | `bulkApprove_ids_501_rejects_with_cap_flash` / `bulkReject_ids_501_rejects_with_cap_flash` | AdminApplicationBulkControllerTest.java:94, :118 | ✅ PASS × 2 |
| **U4** CENTER_ADMIN cross-center 403 | `bulkApprove_centerAdmin_otherCenterProgram_forbidden` / `bulkReject_centerAdmin_otherCenterProgram_forbidden` / `bulkApproveAllPending_centerAdmin_otherCenterProgram_forbidden` | AdminApplicationBulkControllerTest.java:142, :166, :190 | ✅ PASS × 3 |

**UNVERIFIED 4 → 0 ✅** · U4는 bulkApproveAllPending 까지 포함해 요구 사항(3개 액션 모두) 완전 커버.

## 4. 동적 검증 (E2E — U1 해소 재돌림)

### bootRun 기동

- `.claude/scripts/bootrun-e2e.cmd` 로 e2e 프로파일 (H2 in-memory + 시드) 포트 **8090** 기동
- `/login` readiness 응답 확인 후 Playwright 투입

### admin-bulk-csv.spec.ts (chromium, 10 TC 전수)

```powershell
cd e2e
$env:BASE_URL = "http://localhost:8090"
npx playwright test admin-bulk-csv --project=chromium --reporter=line
```

| # | TC | 결과 |
|--:|---|---:|
| 1 | 선택 인터랙션 — 체크박스 선택 시 액션바 활성 + 개수 갱신 + 해제 | ✅ |
| 2 | 프로그램 일괄 운영중단 → 재활성화 — 모달 확인 후 302 + 상태 반영 (자기복원) | ✅ |
| 3 | 사용자 일괄 차단 — 사유 모달 입력 후 확인 → 302 + flash | ✅ |
| 4 | 사용자 권한 변경 모달 — 역할 select 노출 + 취소 (무변경 확인) | ✅ |
| 5 | 신청 일괄 승인 — PENDING 2건 선택 → 모달 확인 → 302 + APPROVED 반영 | ✅ |
| 6 | CSV 내보내기 — 프로그램 목록 다운로드 + 파일명·헤더 검증 | ✅ |
| 7 | CSV 내보내기 — 사용자 목록 (SYSTEM_ADMIN) 다운로드 + 헤더 검증 | ✅ |
| 8 | CSV 내보내기 — 신청 목록 다운로드 + 파일명 프로그램 스코프 검증 | ✅ |
| 9 | **CSV 내보내기 — 신청 목록 gender 컬럼 포함 + 값 MALE/FEMALE/빈값 (U1)** | ✅ |
| 10 | CSV RBAC — CENTER_ADMIN 은 사용자 CSV 내보내기 불가 (403) | ✅ |

→ **10 / 10 PASS (1.6m)** · gender TC 포함 전수 통과.

## 5. 회귀 검증

- admin.* 패키지 전수 **374 / 374 PASS** (§2-2)
- 신규 23 TC 추가 후에도 기존 PASS 자산 회귀 0건
- E2E admin-bulk-csv.spec.ts 10 TC 중 1차 PASS 자산 9건 유지 + U1 신설 1건 추가 모두 PASS

## 6. 미실행 (환경 한계)

- 없음. 회사 PC에서 정적·동적·회귀 전 영역 수행 완료.
- 시각 확인은 CSV·flash message 레벨이라 이번 UNVERIFIED 해소 범위에는 추가 사용자 영역 없음.

## 7. 결론

| 영역 | 결과 |
|---|---|
| 정적 (23 TC 타깃) | ✅ 23 / 23 PASS |
| 정적 (admin 전수 374 TC) | ✅ 374 / 374 PASS, 회귀 0 |
| 동적 (E2E 10 TC) | ✅ 10 / 10 PASS |
| UNVERIFIED 해소 | ✅ **4 → 0** (U1·U2·U3·U4 모두 TC로 박제) |

## 8. 작업 큐 메타

- 상태: **qa_done**
- UNVERIFIED: **0**
- 머지 가능 여부: ym-verify 2차 (적대적 재검증) 호출 후 PASS 받으면 머지 진행 가능
- 다음 단계: `ym-verify` 2차 (refute-first) — UNVERIFIED 해소가 TC 단위로 실제 박제됐는지 적대적 관점에서 재확인
