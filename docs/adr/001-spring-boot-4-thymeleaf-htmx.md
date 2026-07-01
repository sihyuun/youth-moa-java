# 001. Spring Boot 4 + Thymeleaf + HTMX 풀스택 선택

- **날짜**: 2026-06-25
- **상태**: 채택됨

## 배경

기존 `youth-moa` 프로젝트는 Next.js + TypeScript로 구현된 CSR/SSR 혼합 방식이었다.
학습 목적으로 Java 풀스택 재작성을 결정하면서 프레임워크와 뷰 레이어를 선택해야 했다.

## 결정

- 백엔드: Spring Boot 4.1.0 (Spring Framework 7)
- 뷰: Thymeleaf (SSR)
- 동적 UI: HTMX 2.0.4 (partial update without full page reload)
- DB: Spring Data JPA + PostgreSQL (Supabase)

## 이유

| 대안 | 탈락 이유 |
|---|---|
| Spring Boot 3.x | Boot 4 가 최신 학습 대상. Java 21 virtual thread 지원 포함 |
| React/Vue 프론트 분리 | 학습 목적: Java 풀스택 경험 (SSR 우선) |
| JSP | Thymeleaf 가 Spring 생태계 표준이고 HTML-valid 템플릿 |
| Vaadin / Wicket | 학습 커뮤니티·레퍼런스 적음 |
| GraphQL | CRUD 수준 API에 과설계 |

HTMX를 선택한 이유: JavaScript 없이 서버 응답 HTML로 부분 갱신 가능 → 학습 단계에서 JS 번들 빌드 도구 없이 인터랙티브 UI 구현.

## 트레이드오프

- Spring Boot 4 는 생태계 라이브러리 호환성이 아직 불안정 (예: springdoc-openapi Boot 4 미지원).
- HTMX는 복잡한 클라이언트 상태 관리에 한계 → 추후 기능 복잡도가 높아지면 Alpine.js 보완 검토.
- Thymeleaf SSR은 Next.js 대비 초기 번들 0이지만, SPA 수준의 UX(즉각적 전환, 오프라인)는 제공 불가.
