# Architecture Decision Records (ADR)

이 디렉토리는 youth-moa-java 프로젝트의 주요 아키텍처 결정을 기록합니다.

## ADR이란?

ADR(Architecture Decision Record)은 중요한 설계·기술 결정을 "왜 이 선택을 했는가"와 함께 문서화하는 짧은 기록입니다.
나중에 돌아봤을 때 "왜 이렇게 만들었지?"라는 질문에 답할 수 있게 해줍니다.

## 목록

| 번호 | 제목 | 상태 |
|---|---|---|
| [001](001-spring-boot-4-thymeleaf-htmx.md) | Spring Boot 4 + Thymeleaf + HTMX 풀스택 선택 | 채택됨 |
| [002](002-domain-centric-package.md) | 도메인 중심 패키지 구조 선택 | 채택됨 |
| [003](003-supabase-postgres.md) | Supabase PostgreSQL 사용 | 채택됨 |
| [004](004-github-flow-solo.md) | 1인 학습 프로젝트 GitHub Flow 변형 | 채택됨 |

## 새 ADR 추가 방법

1. `docs/adr/` 에 `NNN-짧은-영문-제목.md` 파일 생성
2. 아래 템플릿 사용
3. `README.md` 목록에 추가

## 템플릿

```markdown
# NNN. 제목

- **날짜**: YYYY-MM-DD
- **상태**: 제안됨 / 채택됨 / 폐기됨 / 대체됨 (by ADR-NNN)

## 배경

어떤 상황에서 이 결정이 필요했는지.

## 결정

무엇을 결정했는지.

## 이유

왜 이 방안을 선택했는지. 대안과 비교.

## 트레이드오프

이 결정의 단점 또는 향후 고려사항.
```
