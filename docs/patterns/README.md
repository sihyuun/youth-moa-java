# 프레임워크 함정 모음

> 2026-07-28 에 `CLAUDE.md` 에서 분리. **상시 준수 규칙이 아니라 해당 영역을 건드릴 때 펼쳐 보는 자료**다.

## 왜 분리했나

`CLAUDE.md` 가 917줄까지 늘어난 원인의 3분의 1이 이 사고 패턴 모음(약 290줄)이었다. 상시 컨텍스트에 있으면 **정작 항상 지켜야 하는 규칙의 준수율이 떨어진다** — 규칙을 늘려도 준수율이 오르지 않는다는 것은 2026-07-27 갭 스캔(시각 대조 규칙 신설 후에도 갭 60건)에서 확인됐다.

## 목록

| 문서 | 언제 읽나 | 줄 |
|---|---|---|
| [thymeleaf-spring.md](thymeleaf-spring.md) | 템플릿·폼·HTMX·`th:fragment` 작업 전 (**새 화면 작업 시 일독 권장**) | 198 |
| [jpa-postgres.md](jpa-postgres.md) | 엔티티·연관관계·`@Lob`·`@ElementCollection` 작업 전 | 74 |
| [spring-boot-4.md](spring-boot-4.md) | Boot 3.x 예제를 참고할 때 (패키지 경로가 이동했음) | 15 |

## 여기 있는 것들의 공통점

전부 **`@WebMvcTest` 로는 잡히지 않는다.** view name 과 model attribute 만 검증하므로 Thymeleaf 실제 파싱·SpEL 평가·lazy 로딩이 실행되지 않는다. 그래서 화면 변경 PR 은 동적 검증(curl 또는 Claude Preview)이 필수다.

## 새 항목 추가 기준

사고가 났고, 재발 가능성이 있고, **코드나 테스트로 자동 감지할 수 없을 때** 여기 적는다. 자동 감지가 가능하면 문서가 아니라 테스트로 만든다.

- 렌더 관련 → `*RenderTest` assertion
- prototype 대비 시각·수치 → `e2e/contracts/<screen>.ts` 계약 항목
- 사고 경위 자체가 기록 가치가 있으면 → `docs/postmortems/`
