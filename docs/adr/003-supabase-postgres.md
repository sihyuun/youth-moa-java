# 003. Supabase PostgreSQL 사용

- **날짜**: 2026-06-26
- **상태**: 채택됨

## 배경

학습용 프로젝트이므로 로컬 DB 설치 없이 외부 PostgreSQL이 필요했다.

## 결정

Supabase 무료 플랜 + Session Pooler(포트 5432) 사용.
- 환경변수 `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` 로 분리
- IntelliJ Run Config에 주입 (Git 비추적)

## 이유

| 대안 | 탈락 이유 |
|---|---|
| Docker PostgreSQL 로컬 | 회사 PC 설정 부담, 듀얼 PC 동기화 복잡 |
| H2 인메모리 | 운영 DB와 동작 차이 (데이터 타입, 함수) → 학습 목적에 부적합 |
| AWS RDS | 비용 발생, 학습 단계 과설계 |
| Neon | Supabase 대비 Spring 레퍼런스 적음 |

Supabase Session Pooler는 AWS ap-northeast-2 (서울) 리전 → 국내에서 레이턴시 낮음.

## 트레이드오프

- `ddl-auto: create-drop` 사용 중 → 기동마다 스키마 재생성 (학습 단계 OK, 운영 시 Flyway로 교체 예정)
- 비밀번호 노출 사고(2026-06-26) → 재설정 완료. Git 비추적 환경변수 원칙 강화.
- Supabase 무료 플랜은 7일 미사용 시 프로젝트 일시정지.
