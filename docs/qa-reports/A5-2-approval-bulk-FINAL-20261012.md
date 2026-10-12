# Final QA Report: A5-2 승인 처리 페이지

- 작성일: 2026-10-12
- 브랜치: `feature/A5-2-approval-bulk` (미커밋)
- 최종 판정: ✅ **머지 가능** (PASS 10 / FAIL 0 / UNVERIFIED 4 MEDIUM 비차단)

---

## 1. 파이프라인 요약

| 단계 | agent ID | 결과 |
|---|---|---|
| ym-spec | a7f391c502b4b3267 | Q1~Q10 명세 (전부 권장안, 중간 범위 3종 확정) |
| ym-impl 1차 | afc3d5fdafd566053 | 신설 3 + 수정 10+ (A reject bulk + B 대기 전체 승인 + C 성별 열) |
| ym-qa 1차 | ad765998337cbec9a | 🚫 BLOCK — BUG-1 (summary enum/String key 불일치) |
| ym-impl 2차 | a767e58fb49c61f72 | AdminApplicationController 1 파일 Map 재패킹 (pre-existing 뱃지 공란도 동시 해소) |
| ym-qa 2차 | a585badd4f50f0fc1 | ✅ PASS — 뱃지 4개 숫자 복원 + E2E 4/4 PASS |
| ym-verify | ace20e42afd1e3f3b | ✅ PASS 10 / FAIL 0 / UNVERIFIED 4 — 공격 벡터 14건 전수 반박 실패 |

---

## 2. 검증 범위별 최종 결과

| 범위 | 상태 | 근거 |
|---|---|---|
| 정적 compile + 신규 7 TC + RenderTest | ✅ PASS | `./gradlew compileJava test ...` BUILD SUCCESSFUL |
| 전체 회귀 (admin.* + application.*) | ✅ PASS | 2m 40s, 깨짐 0건 |
| 동적 (bootRun 8090) | ✅ PASS | summary 뱃지 숫자 복원 + approve-all-pending 302 round-trip + CENTER_ADMIN 403 + CSV 11열 gender 포함 + 500 cap flashError + blank reason flashError |
| E2E chromium `admin-application-bulk-reject.spec.ts` | ✅ PASS 4/4 (8.7s) | 1차 1 FAIL + 2 did-not-run → 2차 전수 복구 |
| 디자인 계약 | ⚠️ pre-existing | contract runner 공백 (A4 이래 리포지토리 상태, impl 범위 밖) — markup 수동 대조로 PASS 확인 |

---

## 3. 결정 Q1~Q10 이행 매핑

| Q | 결정 | 소스 |
|---|---|---|
| Q1 공통 사유 1건 | ✅ | `AdminApplicationBulkService.bulkReject(programId, ids, admin, reason)` 단일 reason 적용 |
| Q2 200자 cap | ✅ | Controller `@Size(max=200)` + Service length>200 flashError |
| Q3 500건 cap | ✅ | `MAX_BULK_SIZE=500` 상수 + IllegalArgumentException + flashError |
| Q4 CENTER_ADMIN 격리 | ✅ | Controller `@PreAuthorize` + Service `AdminScope.effectiveCenterId()` 2중 레이어 |
| Q5 성별 열 포함 (중간 범위) | ✅ | grid 10열 + CSV gender 컬럼 (4번째) + `.admin-application-row` 반응형 (max-width 1200) |
| Q6 302 PRG | ✅ | 모든 bulk endpoint → `redirect:/admin/programs/{id}/applications` + RedirectAttributes flash |
| Q7 cross-page select 현상 유지 | ✅ | 대기 전체 승인 액션이 cross-page 니즈 대체 |
| Q8 반려 이메일 (기존 리스너) | ✅ | `ApplicationRejectedEvent` 리스너 자동 발행 (bulk 도 동일 호출) |
| Q9 액션바 다크 유지 | ✅ | POLICY §QA 일관, deviation 필드 명시 (admin-application-bulk.ts) |
| Q10 applications "명" | ✅ | fragment data-attribute 분기, users/programs 는 "건" 유지 |

---

## 4. UNVERIFIED (MEDIUM, 비차단)

| # | 항목 | 소스 레벨 | 자동 TC |
|---|---|---|---|
| U1 | CSV gender 자동 검증 | ✅ 코드 리뷰 PASS | ⚠️ admin-bulk-csv.spec.ts 가 applicantEmail 만 체크 |
| U2 | 201자 경계 | ✅ 로직 존재 | ⚠️ 200/201 경계 TC 부재 |
| U3 | 501건 경계 | ✅ MAX_BULK_SIZE 가드 | ⚠️ 500/501 경계 TC 부재 |
| U4 | 다중 센터 교차 AccessDenied E2E | ✅ scope 분기 소스 | ⚠️ E2E spec 없음 |

→ **별도 FOLLOW-A5-2-coverage 티켓으로 분리 권장** (운영 리스크 LOW).

---

## 5. 문서 오타 1건 (LOW)

- `docs/specs/A5-2-approval-bulk.md:31,85` 서술 "최종 9열" ↔ 실제 10열 (44·44·100·1fr·44·120·150·80·90·90)
- `admin.css` 주석은 "10열" 올바름 — UI 자체는 정합
- 커밋 전 doc edit 1회로 해소 가능

---

## 6. 머지 전 체크리스트

- [x] 정적 전수 PASS
- [x] 동적 PASS
- [x] 회귀 PASS (admin.* + application.*)
- [x] 신규 E2E PASS 4/4
- [x] Q1~Q10 이행 매핑 완료
- [x] BUG-1 해소 확인
- [ ] (선택) spec §3,§7-c "9열" → "10열" 오타 수정
- [ ] (선택) FOLLOW-A5-2-coverage 티켓 생성 (U1~U4)
- [ ] (선택) 사용자 시각 확인 (반려 모달 UX·1200px 반응형·성별 열 gap)

---

## 7. 커밋 메시지 초안

```
261012_A5-2_approval_bulk - 신청 승인 처리 페이지 bulk reject·대기 전체 승인·성별 열 추가

- 신청 목록에 "일괄 반려" 버튼 추가 (공통 사유 1건·최대 200자·500건 cap)
- "대기 전체 승인" 버튼 추가 (PENDING>0 조건부 노출)
- 신청 그리드에 성별 열 추가 (남/여/-) · CSV 헤더에도 gender 컬럼 반영
- 1200px 이하 반응형 축소 레이아웃 적용
- 액션바 선택 카운트 라벨 "건" → "명" (applications 도메인만)
- APPROVED→REJECTED 공석 승격 호출 (A4 개별 reject 와 동일 방어)
- 뱃지 공란 pre-existing bug 동시 해소 (Controller Map 재패킹)

정적: compileJava + 신규 7 TC + 회귀 2m40s PASS
동적: bootRun 8090 상세·토글·CSV 11열·500 cap·CENTER_ADMIN 403
E2E: admin-application-bulk-reject.spec.ts 4/4 PASS
UNVERIFIED 4건은 별도 FOLLOW-A5-2-coverage 티켓으로 분리
```
