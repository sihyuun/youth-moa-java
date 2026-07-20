# 확정 명세 (specs) — 구현 세션 인계 가이드

ym-spec 이 산출하고 사용자가 결정까지 마친 명세 모음. **`spec_confirmed` 상태의 명세는 이 파일 하나만 읽고 ym-impl 이 바로 구현을 시작할 수 있어야 한다** (추가 질문 없이).

## 상태 규약

| 상태 | 의미 |
|---|---|
| `spec_done` | 명세 산출 완료, 사용자 결정 (Q-번호) 대기 |
| `spec_confirmed` | 결정 반영 완료 — ym-impl 인계 가능 |
| `impl_done` | 구현 PR 머지 완료 — 파일 상단에 PR 번호 기록 후 보관 |

## 현재 큐 (2026-07-07)

| 명세 | 브랜치 후보 | 상태 | 비고 |
|---|---|---|---|
| [F0h-c1-center-data-model.md](F0h-c1-center-data-model.md) | `feature/F0h-c1-center-data-model` | spec_confirmed | Center 3필드(desc/hours/imageUrl) + 시드. c2/c3/c4 선행 |
| [F0h-c2-list-3col.md](F0h-c2-list-3col.md) | `feature/F0h-c2-list-3col` | spec_confirmed | 청년센터 목록 3-column 재구성. 8개 상태 매핑·커스텀 지역 드롭다운·정렬 pill·토글 스위치. c1 선행 권장 |
| [F4-detail-requirements-grid.md](F4-detail-requirements-grid.md) | `feature/F4-requirements-data` | spec_confirmed | 가장 작은 단위 (entity+시드+바인딩 1 PR). 워밍업 추천 |
| [F2c-header-transparent.md](F2c-header-transparent.md) | `feature/F2c-header-transparent` | spec_confirmed | `main.css` 수정 |
| [F0c-remainder.md](F0c-remainder.md) | `feature/F0c-apply-wizard` | spec_confirmed | `main.css` 수정 |

## 포트폴리오 강화 트랙 큐 (2026-07-20)

이직 포트폴리오 강화 목적으로 ym-spec 이 일괄 산출한 6건. 전부 `spec_done` — 각 spec 의 Q 결정 반영 후 `spec_confirmed` 전환.

| 명세 | 브랜치 후보 | 상태 | 비고 |
|---|---|---|---|
| [chore-flyway-activation.md](chore-flyway-activation.md) | `chore/flyway-activation` | spec_done | **최우선 권장** — PR #82 yml 오중첩 잠복 버그 복원 포함. ADMIN-01 과 DataInitializer 충돌 → 선행 머지 |
| [chore-observability.md](chore-observability.md) | `chore/observability` | spec_done | 별도 management 포트 9091 권장. 3 PR 분할. 캐싱 티켓 Q5(actuator) 와 조율 필요 |
| [chore-caching-loadtest.md](chore-caching-loadtest.md) | `chore/caching-loadtest` | spec_done | Caffeine → VT+k6 → Redis 3 PR. HomeService self-invocation 리팩터 포함 |
| [feature-oauth2-kakao.md](feature-oauth2-kakao.md) | `feature/oauth2-kakao` | spec_done | 항상 신규 생성 정책 (email scope 제약). mypage 비번 재확인 게이트 파급 포함 |
| [feature-F2c-sse-notifications.md](feature-F2c-sse-notifications.md) | `feature/F2c-sse-notifications` | spec_done | 폴링 대신 SSE. listener REQUIRES_NEW 재구성. 2 PR (백엔드 → 프론트) |
| [ADMIN-01-approval-cycle-and-upload.md](ADMIN-01-approval-cycle-and-upload.md) | Phase 별 (`feature/A1-admin-shell` 등) | spec_done | ADMIN-00 실행 오버레이. Phase 1 = 승인 사이클 시연 완성 |

### 트랙 내 머지 순서 권장

1. **Flyway** (DataInitializer·yml 선점, 이후 모든 스키마 변경의 기반)
2. **Observability PR-1** (actuator 도입 — 캐싱 티켓의 cache metrics 선행)
3. 이후 **캐싱 / OAuth2 / SSE** 는 파일 겹침이 적어 병렬 가능하되, `SecurityConfig`·헤더 fragment 를 건드리는 **OAuth2 ↔ SSE PR-2 는 순차 머지** (뒤 브랜치 rebase 1회)
4. **ADMIN-01 Phase 1~3** — Flyway 머지 후 착수

### 병렬 실행 시 주의

- 세 작업은 Java·템플릿 파일이 겹치지 않아 **worktree 격리 병렬 가능**
- 단 **F2c 와 F0c 는 둘 다 `main.css` 를 수정** → 머지는 순차로 하고, 뒤에 머지하는 브랜치에서 main rebase 후 충돌 해소 1회 필요
- 세션 시작 시 `git branch --show-current` 확인 (다중 세션 규칙) — SessionStart hook 이 worktree 를 자동 안내함

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
