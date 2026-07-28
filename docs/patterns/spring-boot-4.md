# Spring Boot 4.x 함정

> CLAUDE.md 에서 분리 (2026-07-28). 상시 준수 규칙이 아니라 **해당 영역을 건드릴 때 참조하는 사고 패턴 모음**이다.
> 여기 있는 항목은 전부 이 프로젝트에서 실제로 사고 난 것이다.

Boot 4 는 패키지를 모듈화하면서 다수 starter 의 클래스 경로가 이동했습니다. IDE auto-import 가 구 경로(Boot 3.x) 를 잡으면 수동 수정 필요.

| 어노테이션 | Boot 3.x | Boot 4.x |
|---|---|---|
| `@DataJpaTest` | `org.springframework.boot.test.autoconfigure.orm.jpa` | `org.springframework.boot.data.jpa.test.autoconfigure` |
| `@AutoConfigureTestDatabase` | `...test.autoconfigure.jdbc` | `org.springframework.boot.jdbc.test.autoconfigure` |

Spring Security 7 변경:
- `AntPathRequestMatcher` 제거 → POST 폼 기본 사용 또는 신 matcher 적용

