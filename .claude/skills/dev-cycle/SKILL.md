---
name: dev-cycle
description: youth-moa-java 표준 5-agent 파이프라인 (ym-spec → 사용자 결정 → ym-impl → ym-qa → ym-verify → PR → CI → merge) 을 슬래시 커맨드 하나로 자동 오케스트레이션. F0c/admin-notice/admin-terms 등 반복된 사이클 (5~7단) 을 압축. 파일럿·정규 티켓 착수 시 호출.
disable-model-invocation: true
---

# /dev-cycle — 표준 파이프라인 오케스트레이션

## 호출 문법

```
/dev-cycle <spec-id>           # 신규 spec 부터 (ym-spec 산출 포함)
/dev-cycle --from-spec <path>  # 이미 spec_confirmed 상태의 파일부터
/dev-cycle --resume            # 실패·중단 지점부터 재개 (진행 상태는 브랜치+커밋으로 자동 감지)
```

**예시**:
- `/dev-cycle F4-admin-eligibility` — 신규 F4 티켓 파일럿, ym-spec 부터 시작
- `/dev-cycle --from-spec docs/specs/F4-admin-eligibility.md` — spec 이미 확정 상태
- `/dev-cycle --resume` — 마지막 실패 지점부터 (CI 반려·qa 반려 등)

---

## 오케스트레이션 원칙

1. **사용자 개입 지점 1곳**: Qn 결정 (spec_done → spec_confirmed 전환) 시만. 그 외 자동
2. **전 단계 실측 원본 인용 유지**: 각 agent 소환 프롬프트에 "요약 수치 금지, raw output 인용" 조항 유지
3. **실패 시 자동 반려·재구현**: qa/verify FAIL → ym-impl 자동 재소환. 무한 루프 방지 위해 최대 3회
4. **CI 실패 자동 recovery**: 표준 패턴 (spotless/test-context/seed-pollution) 은 자동 fix. 미지 패턴은 사용자 알림
5. **명시적 진행 로그**: 각 단계 시작·종료 시 대화창에 상태 표시 (transparent mode)

## 전제 조건

- 브랜치가 `main` 이 아니면 중단·보고 (혼선 방지)
- `git status` 미커밋 변경 있으면 중단·보고 (`/wrap-up` 먼저 안내)
- `.claude/agents/ym-*.md` 5개 파일 존재 확인

---

## Phase 0 — 준비

```bash
git branch --show-current      # main 이어야 함
git status --short             # 빈 결과 여야 함
git log --oneline -3            # 최근 커밋 확인
gh pr list --state open --limit 3  # 열린 PR 확인 (혼선 방지)
```

- main 아니면: **중단** + "먼저 `/wrap-up` 으로 현재 브랜치 정리해주세요" 안내
- 변경 있으면: **중단** + 파일 목록 보여주고 `/wrap-up` 안내

---

## Phase 1 — ym-spec 소환 (신규 spec 시)

`--from-spec` 옵션이면 스킵.

### Agent 소환

`Agent(subagent_type="ym-spec")` — 프롬프트:

```
youth-moa-java 프로젝트의 <spec-id> 명세를 산출해주세요.

## 프로젝트
C:\Users\User\IdeaProjects\youth-moa-java, main 최신 <최신 commit>

## 컨텍스트
- admin 트랙 진입 상태 (ADR PR #204)
- 완료 파일럿: A1 (#205) · admin-notice (#206) · admin-terms (#207) · F0c-dynamic-fields (#208)
- 재활용 인프라: AdminScope · SecurityFilterChain · @EnableMethodSecurity · @PreAuthorize · FileStorage · TestFixtureController reset 패턴

## 요구
1. `docs/specs/<spec-id>.md` 산출 (spec_done)
2. Qn 결정 항목 6~11개, 각 항목 권장안 명시
3. 회귀 방어 최우선 (기존 apply/signup 무회귀)
4. 계약 신설 계획 (docs/design-contracts/ + e2e/contracts/)
5. deferred/deviation 후보 명시

read-only. 코드 변경 금지.
```

### 종료 후

- 산출 파일 경로 사용자에게 보고
- Qn 요약 표 (권장안 강조) 제시
- **"모두 권장 OK" 또는 특정 Qn 조정** 사용자 응답 대기 (**pause 지점 1**)

---

## Phase 2 — Qn 결정 반영 · spec_confirmed 전환

사용자 응답 수신 후:

1. spec 상단 status 갱신 (`spec_done` → `spec_confirmed (YYYY-MM-DD 사용자 결정: Qn-1 X · Qn-2 Y ...)`)
2. Qn 결정 내용을 spec 파일 마지막 섹션에 §후속 로 추가 (Qn-2 B 처럼 권장안과 다른 결정은 상세 이행 요구 포함)
3. 브랜치 생성: `feature/<spec-id>`
4. spec 파일 커밋 + push

```bash
git checkout -b feature/<spec-id>
git add docs/specs/<spec-id>.md
git commit -m "YYMMDD_docs_<spec-id>_spec - 명세 확정 (Qn 결정 반영)"
git push -u origin feature/<spec-id>
```

---

## Phase 3 — ym-impl 소환

`Agent(subagent_type="ym-impl")` — 프롬프트:

```
<spec-id> 구현. 명세 확정 완료.

## 프로젝트
C:\Users\User\IdeaProjects\youth-moa-java, 브랜치: feature/<spec-id> (spec 커밋 위에 구현 추가)

## 명세
docs/specs/<spec-id>.md — Qn 결정 반영 상태. 특히 §후속 섹션 정독 필수.

## 확정된 결정 (재활용)
<Qn 결정 요약 리스트>

## 구현 요구
- 명세 §5~§8 (엔티티·마이그레이션·화면·테스트) 그대로 이행
- V<N> 마이그레이션 (main 최신 + 1)
- 기존 패턴 재활용 (AdminNoticeController/Service · TestFixtureController reset · @PreAuthorize · OWASP sanitizer)
- 계약 신설 (docs/design-contracts/ + e2e/contracts/)

## 검증
1. ./gradlew.bat compileJava spotlessApply — SUCCESS
2. ./gradlew.bat test --tests "*<도메인>*" --tests JpaMappingTest — 회귀 없음
3. **`./gradlew.bat test` 전체 실행** — F0c CI dependency 사고 (ApplicationServiceTest NoSuchBeanDefinition) 재발 방지

## 커밋 & Push
- 최종 메시지: `YYMMDD_<spec-id> - <요약>`
- 실측 raw 원본 인용 (요약 수치 금지, ym-verify 반복 지적 재발 방지)

**중요**: 회귀 위험 도메인 (apply/signup) 은 특히 엄격히 검증.
```

### 실패 시

- ym-impl 이 push 못 하고 error 반환 → 사용자 알림 (수동 조치 필요)

---

## Phase 4 — ym-qa 소환

`Agent(subagent_type="ym-qa")` — 프롬프트:

```
<spec-id> 구현 QA (6영역: 정적·동적·계약·기능·회귀·시각).

## 명세
docs/specs/<spec-id>.md

## 대상 커밋
feature/<spec-id> HEAD

## 범위
1. 동적 (curl 8090 e2e) — RBAC · CRUD · XSS · 회귀
2. 계약 검사 (`--project=contracts <spec-id>*`)
3. 기능 E2E (`--project=chromium <spec-id>*` + 사용자 사이드 회귀)
4. 회귀 최우선: apply/signup/admin 무회귀

## 결과 리포트
docs/qa-checklists/<spec-id>-qa-report.md — 6영역 분리 · 실측 원본 인용

## 처리
- 회귀 발견 즉시 재반려 (프로덕션 코드 수정 금지, test-only 만 허용)
- 커밋 + push
```

### QA 판정 분기

- **PASS**: Phase 5 로 진행
- **FAIL 발견**: **Phase 3 자동 재소환** (반려 사유 + 재현 방법 요약 프롬프트)
- **재구현 카운트**: 최대 3회. 초과 시 사용자 개입 요청

---

## Phase 5 — ym-verify 소환

`Agent(subagent_type="ym-verify")` — 프롬프트:

```
<spec-id> 최종 관문 적대적 검증.

## 대상 커밋
feature/<spec-id> HEAD (전 커밋 이력 포함)

## 명세
docs/specs/<spec-id>.md

## ym-qa 통과 주장 (반박 대상)
<ym-qa 결과 요약>

## 공격 벡터
- spec §5~§8 매핑 vs 실 코드 정합
- 보안 (RBAC 우회 · CSRF · XSS · path traversal · JSON 파싱 우회)
- 회귀 재현 (apply/signup)
- deferred/deviation 위반 여부
- 이전 세션 학습 함정 재발 방지 (defer regression · seed pollution · SQL 예약어 · test context)

## 판정
PASS/FAIL/UNVERIFIED 개수 + 항목별 표. FAIL 시 반려.
```

### Verify 판정 분기

- **PASS**: Phase 6 로 진행
- **FAIL 발견**: **Phase 3 자동 재소환** (재구현 카운트 +1)
- **UNVERIFIED**: 원인별 처리
  - "8090 서버 미기동" → 무시 (CI 로 자동 커버)
  - "Testcontainers Docker" → 무시 (CI ubuntu 위임)
  - "실 SDK/prod 인증" → 사용자 알림
  - 기타 → 사용자 판단 요청

---

## Phase 6 — PR 오픈

```bash
gh pr create --base main --head feature/<spec-id> \
  --title "YYMMDD_<spec-id> - <요약>" \
  --body "<표준 PR 본문 템플릿>"
```

### PR 본문 템플릿

```markdown
## Summary
<티켓 배경 · 주요 기능 · 재활용 인프라>

## Qn 결정 (spec §후속)
<표>

## 변경 (~N 파일 · +M lines)
<카테고리별 요약>

## Test plan
### 정적
- [x] ./gradlew compileJava spotlessApply — SUCCESS
- [x] <도메인> 테스트 N/N PASS

### 동적 (curl 8090 e2e)
- [x] <실측 결과>

### 계약
- [x] <계약 갭 0 or 이월 사유>

### 기능 E2E
- [x] <PASS 카운트>

### 회귀 (최우선)
- [x] apply / signup / admin 무회귀

### ym-verify 최종 판정
- PASS N / FAIL 0 / UNVERIFIED N

## Deferred / Deviation
<spec §deferred 인용>

## 이력 (사이클)
<QA·verify 반려·fix 이력>

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

---

## Phase 7 — CI 대기 · 자동 recovery

```bash
gh pr checks <PR#>  # 반복 폴링 (60초 간격, 최대 20분)
```

### CI 실패 자동 분기

| 실패 유형 | 감지 패턴 | 자동 조치 |
|---|---|---|
| **Spotless** | `Gradle Check (compile + spotless)` fail, 로그에 "Spotless format check" | `./gradlew spotlessApply` 자동 실행 + commit + push |
| **Test context NoSuchBean** | `Build + Test` fail, 로그에 `NoSuchBeanDefinitionException` | ym-impl 자동 반려 (test-only fix, `@TestConfiguration` stub 추가) |
| **Seed pollution** | Playwright fail, 로그에 `expected 2, received N` (N > 2) | ym-impl 자동 반려 (reset endpoint 확장 or spec afterAll 추가) |
| **P0-3 pattern** (F0c 학습) | Playwright fail, 로그에 500 or `seeded 0/3` | ym-impl 자동 반려 (DataInitializer seed 신설) |
| **Backdrop click intercept** | Playwright fail, 로그에 `intercepts pointer events` | spec click `{position:{x:10,y:10}}` 자동 추가 |
| **defer regression** | HTMX form 실패 + CSRF 리스너 순서 | ym-impl 자동 반려 (defer 제거) |
| **Flaky (A/B/backdrop)** | 재실행 시 pass | `gh run rerun --failed` 자동 |
| **미지 패턴** | 위 매칭 없음 | 사용자 알림 + 수동 조치 요청 |

### Recovery 카운트

- 자동 조치 최대 5회
- 초과 시 사용자 개입 요청

---

## Phase 8 — 머지

```bash
gh pr merge <PR#> --squash --delete-branch
git checkout main
git pull --ff-only
git log --oneline -3
```

### 성공 보고

```
✅ /dev-cycle <spec-id> 완료

PR: #<번호>
main 최신: <commit hash>
사이클: N단 (QA 반려 X · CI recovery Y)
주요 학습: <recovery 패턴 있으면 요약>

다음 파일럿: <파생 큐 or 새 spec>
```

---

## Phase 9 — 실패 시 fallback

`--resume` 옵션 없이 재실행 시:
- 브랜치 상태 자동 감지 (spec 파일 status · 최근 commit 메시지)
- 어느 Phase 부터 재개할지 자동 판정
- 재개 지점 사용자에게 확인 후 진행

수동 개입 필요 시:
- 표준 5-agent 개별 소환으로 복귀 가능
- `.claude/skills/dev-cycle/` 은 프롬프트 템플릿만 있고 상태는 브랜치+커밋에 남아있음 → skill 실패해도 파이프라인 복구 가능

---

## 스킬 도입 배경 (2026-09-08)

이번 세션 admin 트랙 4개 티켓 (A1/#205 · notice/#206 · terms/#207 · F0c/#208) 이 각각 4~7단 사이클 반복. 매 사이클 사용자가 개별 agent 소환 명령을 반복해 **표준 자동화 대상** 확정.

### 재활용 프롬프트 패턴 (모두 유지)

- spec 산출: `docs/specs/README.md § 표준 절차`
- impl: 명세 §5~§8 이행 + 실측 raw 인용
- qa: 6영역 분리 리포트 + 재현 절차
- verify: refute-first + PASS/FAIL/UNVERIFIED

이번 스킬은 위 프롬프트 조합만 자동. 각 agent 정의는 `~/.claude/agents/*.md` 그대로 유지.

### 이번 세션 학습된 CI recovery 패턴 (Phase 7 표)

- PR #200 seed pollution (id 기반 reset)
- PR #201 defer regression
- PR #206 admin-notice `@Column(name="answer_value")` 예약어
- PR #208 test context NoSuchBean · P0-3 seed 부재 · backdrop click position

향후 새 패턴 발견 시 Phase 7 표 갱신 (self-improving).

### 확장 예정 (별도 스킬)

- `/hotfix` — spec 산출 없는 flaky·회귀 fix 사이클 (Phase 1~2 스킵)
- `/loop-until-green` — CI 회복 루프만 단독 실행 (Phase 7 만)

이번 스킬 실전 검증 후 필요성 판단.
