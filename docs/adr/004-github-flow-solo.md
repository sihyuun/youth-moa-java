# 004. 1인 학습 프로젝트 GitHub Flow 변형

- **날짜**: 2026-06-25
- **상태**: 채택됨

## 배경

1인 프로젝트이지만 Git 컨벤션을 잡아 학습 기록과 롤백 능력을 확보하고 싶었다.

## 결정

GitHub Flow 변형:
- `main` 항상 빌드·테스트 통과 상태 유지
- `feature/*`, `fix/*`, `chore/*`, `refactor/*`, `docs/*` short-lived 브랜치
- self-PR → squash merge → branch 삭제
- 커밋 메시지: `YYMMDD_식별자 - 화면에서 보이는 변화 위주 요약`

## 이유

| 대안 | 탈락 이유 |
|---|---|
| trunk-based (main 직접 push) | 롤백·PR 리뷰 학습 기회 없음 |
| Git Flow (develop/release 분리) | 1인 프로젝트에 오버엔지니어링 |
| 커밋 메시지 영어 강제 | 학습 메모 특성상 한국어가 더 유용 |

squash merge를 선택한 이유: 학습 단계 1개 = PR 1개 = 커밋 1개 → main 히스토리 깔끔.

## 트레이드오프

- WIP 커밋이 main에 노출되지 않아 중간 과정 추적이 어려울 수 있음 → PR 본문에 과정 기록으로 보완
- `git add -A` 금지 규칙 + 스테이징 전 diff 검토 → 처음엔 번거롭지만 시크릿 누출 방지 효과
