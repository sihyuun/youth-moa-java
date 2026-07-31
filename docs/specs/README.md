# 확정 명세 (specs) — 구현 세션 인계 가이드

ym-spec 이 산출하고 사용자가 결정까지 마친 명세 모음. **`spec_confirmed` 상태의 명세는 이 파일 하나만 읽고 ym-impl 이 바로 구현을 시작할 수 있어야 한다** (추가 질문 없이).

## 상태 규약

| 상태 | 의미 |
|---|---|
| `spec_done` | 명세 산출 완료, 사용자 결정 (Q-번호) 대기 |
| `spec_confirmed` | 결정 반영 완료 — ym-impl 인계 가능 |
| `impl_done` | 구현 PR 머지 완료 — 파일 상단에 PR 번호 기록 후 보관 |

## 현재 큐 (2026-07-31 갱신 — 모두 완료)

**큐 비어있음.** 아래는 2026-07-07 등록 후 이미 구현된 이력을 감사·정리 (2026-07-31).

| 명세 | 구현 PR | 상태 |
|---|---|---|
| [F0h-c1-center-data-model.md](F0h-c1-center-data-model.md) | #83 (`8110498`) | ✅ impl_done |
| [F0h-c2-list-3col.md](F0h-c2-list-3col.md) | #78 (`b7cddf8`) + #116 후속 | ✅ impl_done |
| [F4-detail-requirements-grid.md](F4-detail-requirements-grid.md) | #73 (`194605f`) | ✅ impl_done |
| [F2c-header-transparent.md](F2c-header-transparent.md) | #72 (`68df6b6`) | ✅ impl_done |
| [F0c-remainder.md](F0c-remainder.md) | #75 (`80f1dd3`) + #85 (E2E) | ✅ impl_done |

### 2026-07-31 갱신 이력

이 상태 정리는 spec 파일이 `spec_confirmed` 로 남아있으나 실제 코드는 이미 구현된 상태였음을 감사해서 정리한 결과. 이후 신설된 spec:

| 명세 | PR | 상태 |
|---|---|---|
| [F-signup-terms-agreement](F-signup-terms-agreement.md) | #125 | ✅ impl_done |
| [F-notice-attachment](F-notice-attachment.md) | #130 | ✅ impl_done |
| [F-home-30-pagination](F-home-30-pagination.md) | #129 | ⛔ spec_declined (wireframe 이탈 결정) |

## 구현 세션 표준 절차

1. 명세 파일 정독 (특히 "✅ 결정 확정" 섹션 — 여기 없는 임의 확장 금지)
2. `main` 최신화 후 브랜치 생성 (위 브랜치 후보 명 사용)
3. ym-impl 구현 → compileJava → commit
4. ym-qa 검증: 정적 → **동적 (Claude Preview 우선: `preview_start(name: "youth-moa-e2e")` → `preview_resize(1280x800)` → snapshot/inspect/console/network)** → 회귀 → E2E spec 추가
5. PR 본문에 정적/동적/시각 검증 분리 표기 (CLAUDE.md 규칙)
6. 머지 후 명세 파일 상태를 `impl_done` + PR 번호로 갱신

## 파생 큐 (명세에서 분리된 후속 작업)

| 작업 | 출처 | 내용 |
|---|---|---|
| `F0c-dynamic-fields` | F0c Q5 | 관리자 설정 동적 추가정보 (강좌 dropdown·질문·첨부) — admin 트랙 선행 필요 |
| `fix/apply-complete-header` | F2c Q4 | 신청 완료 페이지 헤더 추가 + complete.html 주석 오기 정정 |
| admin 프로그램 등록 폼의 자격요건 입력 | F4 | `ProgramEligibility` 스키마 사용 — admin 트랙 |
| admin 공지사항 첨부파일 업로드 | F-notice-attachment | 다운로드는 PR #130 완료. `POST /admin/notices/{id}/attachments` 추가 필요 |
| admin 약관 CRUD | F-signup-terms-agreement | Term 엔티티 준비 완료 (PR #125). `POST/PUT /admin/terms` 추가 필요 |
