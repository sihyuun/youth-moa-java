# 002. 도메인 중심 패키지 구조 선택

- **날짜**: 2026-06-25
- **상태**: 채택됨

## 배경

Spring 프로젝트의 패키지 구조는 크게 두 방식이 있다.

## 결정

도메인 중심 구조 채택:
```
io.github.sihyuuun.youthmoa/
├── user/        (User, UserService, UserController, UserRepository)
├── program/     (Program, ProgramService, ProgramController, ...)
└── application/ (Application, ApplicationService, ...)
```

## 이유

| 방식 | 설명 | 이 프로젝트 선택 여부 |
|---|---|---|
| 계층 중심 | `controller/`, `service/`, `repository/` 로 분리 | ❌ |
| 도메인 중심 | `user/`, `program/` 처럼 기능 단위로 분리 | ✅ |

도메인 중심은:
- 한 기능(예: 프로그램)의 모든 파일이 한 폴더에 → 파일 탐색 쉬움
- 도메인 응집도 높음 → 추후 모듈 분리·마이크로서비스 전환 용이
- 팀 규모가 작을 때 특히 효과적

## 트레이드오프

- 공통 유틸·설정은 `common/`, `config/` 로 별도 분리 필요
- 도메인 간 의존관계가 복잡해지면 순환 참조 위험 → 서비스 레이어에서만 타 도메인 호출 원칙
