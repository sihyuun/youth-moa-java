# Flyway 마이그레이션 폴더

**P0-1 (ADMIN-00 §4)**: admin CRUD 실효성 확보 위한 Flyway 도입.

## 상태 (2026-07-10)

- Flyway 의존성 추가 완료 (`flyway-core` + `flyway-database-postgresql`)
- `application.yml` 에 `spring.flyway.enabled: false` (준비만, 아직 활성 안 함)
- **V1__baseline.sql 미생성** — `fix/F0h-center-desc-image` 티켓 머지 후 최종 스키마 기준으로 dump 예정

## 활성화 절차 (baseline 생성 후)

1. 로컬에서 최신 main 스키마를 PostgreSQL 에 create-drop 로 한 번 기동
2. `pg_dump --schema-only -U ... -d youth_moa > src/main/resources/db/migration/V1__baseline.sql`
3. dump 내 Flyway 자체 테이블 (`flyway_schema_history`) 제거
4. `application.yml` 갱신:
   - `spring.flyway.enabled: true` (또는 `FLYWAY_ENABLED=true` env var)
   - `spring.jpa.hibernate.ddl-auto: validate` (또는 `JPA_DDL_AUTO=validate`)
5. e2e / test 프로파일은 기존 `create-drop` 유지 (H2 in-memory)
6. `DataInitializer.count() > 0 → skip` 로직이 이미 있으므로 시드는 무변경

## 마이그레이션 네이밍 규칙

- `V<version>__<snake_case_description>.sql` (버전 번호 앞자리)
- `R__<name>.sql` (repeatable, 뷰·함수 등)
- 예: `V2__add_program_apply_period.sql`, `V3__add_daily_visit.sql`

## 원칙

- **한 번 커밋된 V 파일은 절대 수정 금지** — 이미 배포된 환경에 checksum mismatch 로 부트 실패
- 잘못 커밋했으면 새 V 파일로 수정 (rollback + fix)
- 실 데이터 있는 프로덕션에서는 `baseline-on-migrate: true` 확인
