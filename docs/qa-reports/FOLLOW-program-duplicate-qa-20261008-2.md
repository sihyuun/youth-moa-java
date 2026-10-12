# 검증 리포트 (2차): FOLLOW-program-duplicate

- 일시: 2026-10-08
- 브랜치: `feature/FOLLOW-program-duplicate`
- 선행
  - ym-verify 1차: commit `aa09250f093116485` → PASS 9 / FAIL 1 / UNVERIFIED 4
  - FAIL 1 (spec §8 TBD) → 사용자 doc edit 로 해소 완료
  - ym-impl 2차: commit `aef5d547304ff8ff9` → 테스트 자산 3건 신규/수정, 신규 9 TC 추가, duplicate 전수 22/22 PASS 보고
- 검증자: ym-qa (2차)
- 목적: UNVERIFIED 4건 해소 재검증 + 회귀 확인

---

## 1차 vs 2차 결과 비교

| 항목 | 1차 (ym-verify) | 2차 (ym-qa) |
|---|---|---|
| 판정 요약 | PASS 9 / **FAIL 1** / **UNVERIFIED 4** | **PASS 13 / FAIL 0 / UNVERIFIED 0** |
| spec §8 라인 번호 | TBD (FAIL) | 사용자 doc edit 반영 (해소) |
| U1 CENTER_ADMIN 403 | 코드 레벨 TC 없음 | TC 신설 → PASS |
| U2 Transactional 롤백 | negative TC 없음 | TC 신설 → PASS |
| U3 Attachment 미복제 | 입증력 약함 | TC 신설 → PASS |
| U4 title 255 trim 경계값 | 경계값 TC 없음 | TC 3건 신설 → PASS |
| 머지 가능 여부 | 조건부 (F1 해소 전제) | **가능** |

---

## 정적 검증

### A. AdminProgramDuplicate* 전수 재돌림

명령: `./gradlew test --tests "AdminProgramDuplicate*"`

결과: **BUILD SUCCESSFUL (8m 45s) · 22/22 PASS · failures=0 · errors=0**

| 테스트 클래스 | TC 수 | 결과 |
|---|---|---|
| `AdminProgramDuplicateControllerRbacTest` | 5 | 5 PASS |
| `AdminProgramDuplicateServiceRollbackTest` | 1 | 1 PASS |
| `AdminProgramDuplicateServiceTest` | 16 | 16 PASS |
| **합계** | **22** | **22 PASS** |

### B. UNVERIFIED 4건 전수 해소 매핑

| # | 1차 UNVERIFIED 항목 | 2차 해소 TC | 결과 |
|---|---|---|---|
| U1 | CENTER_ADMIN 403 실행 입증 부재 | `AdminProgramDuplicateControllerRbacTest.POST_duplicate_CENTER_ADMIN_forbidden()` | PASS (0.102s) |
| U2 | 트랜잭션 롤백 negative TC 부재 | `AdminProgramDuplicateServiceRollbackTest.duplicate_courseSaveFails_entireTransactionRollback()` | PASS (3.421s) |
| U3 | Attachment 미복제 입증력 약함 | `AdminProgramDuplicateServiceTest.duplicate_attachmentsNotCopied_실입증()` | PASS (0.140s) |
| U4 | title 255 trim 경계값 TC 부재 | `AdminProgramDuplicateServiceTest.duplicate_title249_fits_exactly255_접미사그대로()` · `duplicate_title250_overflow_오리지널trim()` · `duplicate_title255_overflow_접미사만큼trim()` (3건) | 3/3 PASS |

→ **UNVERIFIED 4건 전수 해소 확인 (해소율 100%)**

### C. 1차 PASS 자산 유지 (ym-verify 1차 승인 범위)

RBAC 전수 (5건), service 핵심 로직 (16건 중 신규 4건 외) 모두 재실행에서 동일 PASS. 코드 추가는 테스트 자산만 신설·보강이라 기존 자산 영향 없음.

---

## 회귀 검증

### D. admin 패키지 전체 회귀

명령: `./gradlew test --tests "io.github.sihyuuun.youthmoa.admin.*"`

결과: **BUILD SUCCESSFUL (9m 52s) · 360/360 PASS · failures=0 · errors=0**

- Duplicate 트랙 22 + 기타 admin 트랙 338 = **총 360 TC 모두 PASS**
- 1차 시점 338 TC 기준 신규 22 TC 추가 → regression 깨짐 0건 재확인

| 상위 테스트 클래스 (TC 수) | 결과 |
|---|---|
| AdminUserServiceTest (38) | PASS |
| AdminApplicationServiceTest (24) | PASS |
| AdminProgramFormServiceTest (17) | PASS |
| AdminApplyQuestionServiceTest (16) | PASS |
| AdminProgramDuplicateServiceTest (16) | PASS |
| AdminTermServiceTest (14) | PASS |
| AdminUserSafeguardTest (14) | PASS |
| AdminProgramEligibilityServiceTest (13) | PASS |
| AdminUserControllerRbacTest (12) | PASS |
| AdminNoticeServiceTest (12) | PASS |
| AdminProgramServiceTest (11) | PASS |
| … (총 43 테스트 클래스) | 모두 PASS |

---

## 동적 검증

범위 외 (지시사항: 코드/테스트 수정 금지, 영향 없음 재확인 생략 가능). 1차 PASS 자산 E2E 2종 (admin-program-duplicate.spec.ts · admin-program-detail.spec.ts) 은 코드 변경 없어 영향 없음.

---

## 시각 확인 (사용자 영역)

범위 외.

---

## 미실행 (환경 한계)

없음. 회사 PC JDK 17 + H2 조합으로 전수 실행 완료.

---

## gap-reports

untracked (로컬 아티팩트).

---

## 작업 큐 메타

- 상태: **qa_done_2nd (UNVERIFIED 전수 해소)**
- 머지 가능 여부: **가능** (ym-verify 1차 조건부 가능 → 2차에서 조건 전수 해소)
- 다음 단계: 사용자 머지 승인 또는 `/wrap-up` 호출
