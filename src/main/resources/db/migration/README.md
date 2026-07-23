# Flyway 마이그레이션 폴더

**P0-1 (ADMIN-00 §4)**: admin CRUD 실효성 확보 위한 Flyway 도입 — **2026-07-22 활성화 완료**.

## 현재 상태

- `application.yml`: `spring.flyway.enabled=true` + `baseline-version=1` + `ddl-auto=validate` (환경변수로 override 가능)
- `V1__baseline.sql`: 15개 테이블 (엔티티 인벤토리 100% 일치, B′ 방식 클린 Docker PG + Hibernate create + pg_dump 로 생성)
- 테스트: `src/test/resources/application.properties` 로 H2 경로는 flyway off + create-drop 폴백. Testcontainers 경로 (`YouthMoaApplicationTests`) 만 flyway=true + validate opt-in

## 스키마 변경 절차 (V2 이상)

1. 엔티티 변경 + `V<N>__<snake_case_설명>.sql` 을 같은 PR 에 작성
   - N = main 의 db/migration 최신 버전 + 1. 병렬 브랜치 충돌 시 rebase 후 재부여
2. DDL 확인이 필요하면 로컬 Docker PG 에 `JPA_DDL_AUTO=update FLYWAY_ENABLED=false` 로 1회 띄워 Hibernate DDL 로그 참고
3. `YouthMoaApplicationTests` (Testcontainers) 가 매 PR 에서 빈 PG 에 V1..VN 적용 + validate 자동 검증
4. **한 번 main 에 머지된 V 파일은 절대 수정 금지** — 적용된 환경에서 checksum mismatch 로 부팅 실패. 잘못됐으면 다음 번호의 새 V 파일로 정정
5. 뷰·함수 등 재적용 가능 객체는 `R__<이름>.sql` (repeatable)

## 프로파일별 스키마 소스

| 경로 | DB | 스키마 소스 |
|---|---|---|
| bootRun (local/prod) | Supabase PG | Flyway (`validate`) |
| Testcontainers 테스트 | PG 컨테이너 | Flyway (`validate`) — V 파일 실전 게이트 |
| e2e 프로파일 / @DataJpaTest | H2 | Hibernate `create-drop` (Flyway off) |

## 금지·주의

- ❌ `JPA_DDL_AUTO=update` 로 Supabase 스키마 변경 (Flyway 이력 밖의 drift 발생)
- ❌ Supabase SQL Editor 로 직접 DDL — 필요 시 V 파일로 작성 후 부팅으로 적용
- 시드 데이터는 계속 `DataInitializer` 담당 (idempotent). 마이그레이션은 스키마 전용

## 롤백 (활성화 실패 시)

1. `FLYWAY_ENABLED=false` + `JPA_DDL_AUTO=update` 환경변수 override → 이전 동작 즉시 복귀
2. Supabase 에 이미 생긴 `flyway_schema_history` 는 남아 있어도 무해 (비활성 시 조회 안 함). 초기화 필요하면 `DROP TABLE flyway_schema_history;` 1문
