# Remote Routines

claude.ai `/schedule` 을 통해 등록되는 원격 CCR 루틴 프롬프트 모음. 실제 등록은 `RemoteTrigger` API 호출로 이루어짐 (자동 아님, 수동 진행).

## 등록된 루틴 (예정)

| 시각 KST | 파일 | 목적 |
|---|---|---|
| 08:30 | (external / youth-moa repo) | daily todo — 이 repo 외 |
| 08:35 | `pm-review.md` | 오늘의 최우선 화면·정책 PM Review |
| 08:40 | `spec-pending.md` | 어제 PM 결정 → spec 착수 여부 확인 |
| 08:45 | `pr-health.md` | 열린 PR 상태 스캔 |
| 08:50 | `visual-check-queue.md` | 개인 PC 시각 확인 큐 정리 |
| 08:55 (월) | `prototype-gap-weekly.md` | prototype ↔ templates 일치도 주간 스냅샷 |

## 공통 원칙

1. **Read-only from repo**: 원격 세션은 repo 파일을 write/commit 하지 않음. 모든 산출물은 Notion 페이지
2. **KST 변환**: cron 은 UTC. 프롬프트 안에서 `date -u` 실행 후 +9h 로 KST 계산
3. **상위 Notion 페이지**: `38fc33520d0e80b0bdddc3b2a430fb94` (💻 youth-moa-java)
4. **네이밍**: `MM-DD <루틴 이름> — <대상>` (예: `07-01 PM Review — F0e 홈`)
5. **TL;DR**: 페이지 최상단 `> 📌 **한눈에**` callout 3줄 요약 필수
6. **가독성**: 역피라미드 (요약 → 결정 필요 → 상세). 접힘/toggle 활용

## 페르소나 참조

프롬프트 첫 단계에서 `Read` 로 다음 파일 정독:
- `.claude/agents/ym-pm.md` — PM 페르소나 (6관점, 출력 포맷)
- `docs/STATE.md` — 프로젝트 상태 미러

## 등록 절차

1. 저장소 main 에 이 파일들 커밋·push 완료 확인
2. Claude Code 에서 `/schedule` 또는 직접 `RemoteTrigger create` 호출
3. body 의 `events[0].data.message.content` 에 해당 `.md` 파일 내용 그대로 주입
4. 등록 후 URL 은 `https://claude.ai/code/routines/{trigger_id}`

## MCP 커넥터

모든 루틴에 `Notion` 커넥터 attach (create/update/search 필요).
