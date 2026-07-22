# 작업 명세: chore/flyway-activation — Flyway 실제 활성화 (P0-1 완결)

- **상태**: `impl_done` (2026-07-22 활성화 완료. Q1~Q8 전부 권장안 채택)
- **impl 중 추가 발견**:
  - Boot 4 는 `spring-boot-flyway` auto-config 모듈이 분리됨. 이 의존성 미포함 시 `spring.flyway.enabled=true` 여도 Flyway auto-config 미동작 → build.gradle.kts 에 추가함
  - pg_dump 17.10 이 `\restrict`/`\unrestrict` psql meta-command 를 자동 삽입 → JDBC 실행 불가 → V1__baseline.sql 에서 두 라인 수동 제거
- **브랜치**: `chore/flyway-activation`
- **선행**: PR #82 (260710_flyway_prep) 머지 완료 — 의존성·비활성 설정·README 존재. 추가 선행 없음
- **트랙**: 포트폴리오 강화 / admin 공유 인프라 (ADMIN-00 §4 P0-1)
- **작업 단위**: 1 PR (baseline SQL + 설정 전환 + 테스트 매트릭스 정비 + CLAUDE.md 규칙 교체)
- **코드 영향**: 화면·도메인 로직 무변경. 인프라(부팅 시퀀스·스키마 관리 방식)만 변경

---

## 0. 배경·현황 진단

### 0-1. PR #82 이후 대기 상태

| 항목 | 현재 상태 | 근거 |
|---|---|---|
| 의존성 | `flyway-core` + `flyway-database-postgresql` 존재 | `build.gradle.kts` L38~39 |
| 활성 여부 | `spring.flyway.enabled: ${FLYWAY_ENABLED:false}` — 비활성 대기 | `application.yml` L27 |
| baseline SQL | **미생성** (`db/migration/` 에 README.md 만 존재) | `src/main/resources/db/migration/` |
| ddl-auto | `${JPA_DDL_AUTO:update}` (2026-07-13 create-drop → update 전환) | `application.yml` L21 |
| e2e 프로파일 | H2 in-memory + 자체 `create-drop` | `application-e2e.properties` L5~11 |
| 실 DB | Supabase PostgreSQL — **기존 데이터 존재** (사용자·신청·admin 편집값) | 메모리 / CLAUDE.md |

### 0-2. ⚠️ 조사 중 발견한 잠복 버그 — `spring.flyway.properties` 오중첩 (PR #82 회귀)

PR #82 diff (`git show f0c7707 -- src/main/resources/application.yml`) 확인 결과, `flyway:` 블록이 기존 `spring.jpa` 소속이던 `properties:` 블록 **바로 앞에** 삽입되면서 들여쓰기 상 `properties.hibernate.format_sql` / `properties.hibernate.jdbc.time_zone: Asia/Seoul` 이 `spring.flyway.properties` 아래로 잘못 편입됨.

```yaml
# 현재 application.yml L24~35 (잘못된 상태)
  flyway:
    enabled: ${FLYWAY_ENABLED:false}
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    properties:            # ← 원래 spring.jpa.properties 였음
      hibernate:
        format_sql: true
        jdbc:
          time_zone: Asia/Seoul
```

**결과**: 2026-07-10 (PR #82) 이후 `hibernate.format_sql` 과 `hibernate.jdbc.time_zone=Asia/Seoul` 이 **적용되지 않는 상태**로 운영 중. Flyway 는 미지의 `properties` 키를 조용히 무시하므로 에러 없이 잠복. 이번 티켓에서 `spring.jpa.properties` 로 복원한다 (→ Q3).

### 0-3. ⚠️ `baseline-version: 0` 문제

현재 `baseline-version: 0` + `baseline-on-migrate: true` 조합에서 활성화하면:

1. 기존 Supabase (테이블 있음, history 없음) → version **0** 으로 baseline 마킹
2. Flyway 가 `V1__baseline.sql` 을 "미적용 버전" 으로 판단해 **실행 시도**
3. `CREATE TABLE users ...` 가 기존 테이블과 충돌 → **부팅 실패**

`baseline-version: 1` 로 바꾸면 기존 DB 는 V1 을 건너뛰고(이미 반영된 것으로 간주), 빈 DB (Testcontainers·신규 환경) 만 V1 을 실제 실행한다. 표준 baseline 패턴이며 이 값 수정이 이번 티켓의 핵심 (→ Q1).

---

## 1. 개념 정리 (학습 포인트)

### 1-1. ddl-auto 3모드 vs Flyway

| 방식 | 스키마 생성 주체 | 데이터 | 이력 | 협업·운영 적합성 |
|---|---|---|---|---|
| `create-drop` | Hibernate (엔티티 → DDL 자동) | 재기동마다 소멸 | 없음 | 테스트 전용 |
| `update` | Hibernate (증분 ALTER 자동) | 유지 | 없음. **컬럼 삭제·rename 불가** (추가만 됨 → 죽은 컬럼 누적) | 학습 초기 한정 |
| `validate` | 아무것도 안 만듦 — **매핑과 실제 스키마 일치만 검사** | 유지 | — | Flyway 와 짝으로 운영 표준 |
| Flyway | **버전 붙은 SQL 파일** (`V1__`, `V2__`...) 을 순서대로 적용 | 유지 | `flyway_schema_history` 테이블에 기록 (버전·체크섬·적용일) | 운영 표준 |

핵심 전환: "스키마의 진리 소스" 가 **엔티티 클래스 → 마이그레이션 SQL 파일** 로 이동한다. 엔티티는 이제 스키마를 *만들지* 않고, `validate` 가 둘의 일치를 *검사*만 한다. 불일치 시 부팅 실패 = 매 기동이 스키마 회귀 테스트가 됨.

### 1-2. baseline 이란

Flyway 를 **이미 스키마가 있는 DB** 에 도입할 때, "여기까지는 이미 반영됨" 이라고 출발선을 긋는 작업. `flyway_schema_history` 에 `<< Flyway Baseline >>` 행 1개를 남기고, baseline-version 이하의 마이그레이션은 건너뛴다.

- `baseline-on-migrate: true` — migrate 실행 시 history 테이블이 없고 스키마가 비어있지 않으면 자동 baseline. (빈 스키마면 baseline 없이 V1 부터 정상 적용)
- `baseline-version: 1` — baseline 지점. V1 까지 스킵, V2 부터 적용

즉 같은 설정으로 **기존 Supabase (V1 스킵)** 와 **빈 Testcontainers PG (V1 실행)** 가 둘 다 올바르게 동작한다.

---

## 2. V1__baseline.sql 생성 방법 비교 (Q2)

| 방법 | 개념 | 장점 | 단점 |
|---|---|---|---|
| **A. Hibernate schema export** | `jakarta.persistence.schema-generation.scripts.action=create` 속성으로 부팅 시 DDL 을 파일로 출력 | DB 서버 불필요. 엔티티 매핑과 100% 일치 보장 | 출력이 세미콜론 없는 한 줄 문장 나열이라 후처리 필요. FK·인덱스 순서 제어 어려움. Hibernate 내부 포맷이라 학습 자료로서 가독성 낮음 |
| **B. Supabase 실 DB pg_dump --schema-only** | 운영 DB 의 현재 스키마를 그대로 덤프 | "현실" 을 정확히 캡처 (ddl-auto:update 가 실제로 만든 결과물) | ⚠️ `update` 모드는 컬럼을 **삭제하지 못하므로** 과거 제거된 필드의 죽은 컬럼이 섞여 있을 수 있음 → 신규 환경에도 죽은 컬럼이 복제됨. Supabase 확장·role·grant 노이즈 제거 필요. pg_dump 클라이언트 버전을 서버에 맞춰야 함 |
| **C. 수동 작성** | 테이블 15개를 손으로 CREATE TABLE 작성 | DDL 학습 효과 최대 | 15 테이블 + FK + 유니크 제약 전수 오타 리스크. validate 불일치 디버깅에 시간 소모 |
| **B′. 권장 — 로컬 클린 PG + Hibernate create + pg_dump (하이브리드)** | Docker 로 빈 PG 기동 → `JPA_DDL_AUTO=create` 1회 부팅 (Hibernate 가 클린 스키마 생성) → 그 DB 를 pg_dump | 엔티티와 100% 일치 (validate 확실 통과) + 죽은 컬럼 없는 클린 스키마 + pg_dump 의 읽기 좋은 표준 DDL 포맷. db/migration/README.md 의 기존 계획과 동일 | Docker 필요 (회사 PC 2026-07-10 실증 완료 — 문제 없음) |

### B′ 실행 절차 (impl 단계에서 그대로 수행)

```powershell
# 1. 빈 PG 컨테이너 (호스트 15432 — IntelliJ 로컬 PG 5432 와 무충돌)
docker run -d --name ym-baseline -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=youthmoa -p 15432:5432 postgres:17

# 2. Hibernate 가 클린 스키마 생성하도록 1회 기동 (기동 확인 후 Ctrl+C)
$env:DATABASE_URL = "jdbc:postgresql://localhost:15432/youthmoa"
$env:JPA_DDL_AUTO = "create"
$env:FLYWAY_ENABLED = "false"
.\gradlew.bat bootRun

# 3. schema-only dump (owner/privilege 제외 — 환경 이식성)
docker exec ym-baseline pg_dump -U postgres --schema-only --no-owner --no-privileges youthmoa > src/main/resources/db/migration/V1__baseline.sql

# 4. 후처리: SET 문·주석 헤더 정리 (선택). Flyway 비활성 상태로 dump 했으므로 flyway_schema_history 는 애초에 없음
# 5. 컨테이너 정리
docker rm -f ym-baseline
```

**보조 검증 (Q8)**: Supabase 실 스키마도 pg_dump 로 받아 B′ 결과와 diff → `update` 모드가 남긴 drift (죽은 컬럼 등) 를 문서화. `validate` 는 "매핑된 컬럼이 존재하는가" 만 검사하고 **여분 컬럼은 무시**하므로 drift 가 있어도 부팅은 성공하지만, 실태 파악용으로 1회 수행 가치 있음.

### baseline 대상 테이블 인벤토리 (엔티티 전수 — 15개)

| 테이블 | 소스 |
|---|---|
| `users` | `user/User.java` (`@Table(name="users")`) |
| `user_interest_region` | `User` `@ElementCollection` (L66) |
| `user_interest_category` | `User` `@ElementCollection` (L71) |
| `persistent_logins` | `user/PersistentLogin.java` (remember-me) |
| `phone_verifications` | `user/PhoneVerification.java` |
| `center` | `center/Center.java` |
| `center_content` | `center/CenterContent.java` |
| `region` | `region/Region.java` |
| `program` | `program/Program.java` (+ `ProgramEligibility` @Embedded → 동일 테이블 컬럼) |
| `application` | `application/Application.java` (@Table 없음 → 기본명) |
| `bookmark` | `bookmark/Bookmark.java` (@Table 없음 → 기본명) |
| `notice` | `notice/Notice.java` |
| `notice_attachment` | `notice/NoticeAttachment.java` |
| `notification` | `notification/Notification.java` (@Table 없음 → 기본명) |
| `site_image` | `common/SiteImage.java` |

impl 시 dump 결과의 CREATE TABLE 수가 15개인지 이 표와 대조한다 (누락/여분 즉시 감지).

---

## 3. 기존 Supabase 데이터 위 baseline 적용 절차 (Q1)

### 채택 설계: `baseline-on-migrate: true` + `baseline-version: 1` (0 → 1 수정)

| 시나리오 | history 테이블 | 스키마 | Flyway 동작 |
|---|---|---|---|
| 기존 Supabase 첫 기동 | 없음 | 테이블 존재 | 자동 baseline (version 1 마킹) → V1 스킵 → 이후 validate. **기존 데이터 무손실** |
| 기존 Supabase 재기동 | 있음 | 그대로 | "Schema is up to date" — no-op (멱등) |
| 빈 DB (Testcontainers·신규 환경) | 없음 | 빈 스키마 | baseline 발동 안 함 → **V1 실제 실행** → 스키마 생성 |
| 미래 V2 추가 후 기동 | 있음 (v1) | v1 상태 | V2 만 적용 |

### 왜 `baseline-version: 0` 이면 안 되는가 (재확인)

version 0 baseline 은 "V1 도 아직 미적용" 을 의미 → 기존 Supabase 에 V1 의 CREATE TABLE 을 실행 시도 → `relation "users" already exists` 로 즉사. `IF NOT EXISTS` 로 우회하는 안도 있으나 baseline SQL 을 오염시키므로 비권장.

### 롤백 플랜 (활성화 실패 시)

1. `FLYWAY_ENABLED=false` + `JPA_DDL_AUTO=update` 환경변수 override → 즉시 이전 동작 복귀 (yml default 를 뒤집어도 env 로 되돌릴 수 있는 구조 유지)
2. Supabase 에 이미 생긴 `flyway_schema_history` 는 남아 있어도 무해 (비활성 시 조회 안 함). 재시도 전 초기화 필요하면 `DROP TABLE flyway_schema_history;` 1문

---

## 4. 프로파일 · 테스트 매트릭스 (Q4 · Q5)

### 4-1. 설계 원칙

- **PostgreSQL 경로 (Supabase·Testcontainers) 만 Flyway 적용.** H2 경로는 전부 Flyway off + 기존 Hibernate DDL 유지
- 이유: V1 은 PG dialect SQL (pg_dump 산출물). H2 `MODE=PostgreSQL` 은 근사 호환일 뿐이라 baseline 이 그대로 돌지 않음. H2 호환 마이그레이션 이중 관리는 학습 프로젝트 비용 대비 무가치 — H2 는 "엔티티 기준 빠른 검증", PG 는 "마이그레이션 기준 실전 검증" 으로 역할 분리가 오히려 정석

### 4-2. 부트 경로별 처리 (전수)

| 부트 경로 | DB | 현재 | 전환 후 | 수정 파일 |
|---|---|---|---|---|
| `bootRun` (default=local) | Supabase PG | ddl-auto update, flyway off | **flyway on + validate** | `application.yml` default 반전 |
| `bootrun-e2e.cmd` / CI Playwright | H2 | create-drop | 유지 + **flyway off 명시** | `application-e2e.properties` 1줄 추가 |
| `@SpringBootTest` + `@ActiveProfiles("e2e")` (Render·Security·Seed 테스트 ~15개) | H2 | create-drop | e2e properties 상속 → 자동 해결 | — |
| `@DataJpaTest` + `@AutoConfigureTestDatabase` (JpaMappingTest 등 ~10개) | H2 embedded | main yml 상속 (flyway off 덕에 무사) | **default 반전 시 깨짐** → test 전용 properties 로 차단 | `src/test/resources/application.properties` 신규 |
| `@WebMvcTest` (~6개) | 없음 | — | DataSource 미기동 → 무영향 | — |
| `YouthMoaApplicationTests` (@SpringBootTest + Testcontainers) | PG 컨테이너 | ddl-auto update | **flyway on + validate 로 opt-in** → 매 PR 에서 V1 실전 검증 | 테스트 클래스에 properties 부여 |

주의: `@DataJpaTest` 는 slice 에 Flyway auto-configuration 을 포함하므로 "슬라이스라서 안전" 하지 않다. 또한 Boot 의 embedded-DB create-drop 자동 추론은 `ddl-auto` 가 **명시되지 않은 경우만** 발동 → main yml default 를 validate 로 바꾸면 명시값이 이겨 H2 테스트가 validate 로 부팅 시도한다. 따라서 test 전용 properties 에 `ddl-auto=create-drop` 도 함께 명시해야 한다.

### 4-3. 구체 변경안

**`src/test/resources/application.properties` (신규)** — 모든 JUnit 테스트의 공통 기본값 (main yml 동일 키 override):

```properties
# 테스트 기본: H2 경로는 Flyway off + Hibernate DDL (V1 은 PG 전용 SQL)
# PG(Testcontainers) 로 실전 검증하는 테스트는 클래스에서 opt-in (YouthMoaApplicationTests 참조)
spring.flyway.enabled=false
spring.jpa.hibernate.ddl-auto=create-drop
```

**`YouthMoaApplicationTests.java`** — Testcontainers PG 에서 Flyway 실전 opt-in:

```java
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
    "spring.flyway.enabled=true",          // V1 을 빈 PG 에 실제 적용
    "spring.jpa.hibernate.ddl-auto=validate" // 엔티티 매핑 ↔ V1 스키마 일치 검사
})
class YouthMoaApplicationTests { ... }
```

→ 이 테스트 하나가 "V1 이 빈 PG 에서 돌아가는가 + 엔티티와 일치하는가 + DataInitializer 시드가 그 위에서 도는가" 3중 검증 게이트가 된다.

**`application-e2e.properties`** — 1줄 추가:

```properties
spring.flyway.enabled=false
```

(현재도 main yml default 가 false 라 동작엔 문제 없지만, main default 를 true 로 반전하므로 명시 필수)

**`application.yml`** — default 반전 + 오중첩 복원:

```yaml
  jpa:
    hibernate:
      ddl-auto: ${JPA_DDL_AUTO:validate}   # update → validate
    open-in-view: false
    properties:                             # ← flyway 밑에서 복원 (0-2 잠복 버그 수정)
      hibernate:
        format_sql: true
        jdbc:
          time_zone: Asia/Seoul

  flyway:
    enabled: ${FLYWAY_ENABLED:true}         # false → true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 1                     # 0 → 1 (§3)
```

### 4-4. CI 영향 분석

| workflow | job | 영향 |
|---|---|---|
| `ci.yml` | build-and-test (H2) — JpaMappingTest 외 3개 | 전부 `@DataJpaTest` → test properties 로 flyway off. **workflow 수정 불필요** |
| `ci.yml` | integration-test (Testcontainers 전체 + 커버리지 게이트) | `YouthMoaApplicationTests` 가 flyway 경로로 전환 → **매 PR 마다 V1 자동 검증 (이번 티켓의 최대 이득)**. workflow 수정 불필요 |
| `e2e-playwright.yml` | boot jar + e2e 프로파일 (H2) | e2e properties 에 flyway off → 무영향. workflow 수정 불필요 |
| `lint.yml` / `deploy.yml` / `prototype-gap.yml` | — | 무영향 |

**결론: `.github/workflows/` 파일은 하나도 수정하지 않는다.**

---

## 5. 이후 스키마 변경 워크플로우 (V2, V3...) — CLAUDE.md 규칙 초안

활성화 후 CLAUDE.md §"DB / 마이그레이션" 절을 아래로 **전문 교체**한다 (impl 단계 포함):

```markdown
## DB / 마이그레이션 (Flyway — 2026-07-XX 활성화)

스키마의 진리 소스는 `src/main/resources/db/migration/V*.sql`. 엔티티는 스키마를 만들지 않으며
`ddl-auto: validate` 가 매핑·스키마 일치만 검사한다 (불일치 시 부팅 실패 = 조기 감지).

### 스키마 변경 절차 (엔티티 수정 시 반드시 세트로)

1. 엔티티 변경 + `V<N>__<snake_case_설명>.sql` 을 **같은 PR** 에 작성
   - N = main 의 db/migration 최신 버전 + 1. 병렬 브랜치가 같은 N 을 선점했으면 rebase 후 번호 재부여
   - 예: `V2__add_program_apply_period.sql`, `V3__create_daily_visit.sql`
2. DDL 작성이 막히면: 로컬 Docker PG 에 `JPA_DDL_AUTO=update FLYWAY_ENABLED=false` 로 1회 띄워
   Hibernate 가 찍는 DDL 로그를 참고해 옮겨 적는다 (검증은 어차피 3번이 함)
3. 검증: `YouthMoaApplicationTests` (Testcontainers) 가 빈 PG 에 V1..VN 적용 + validate 를 매 PR 자동 수행
4. **한 번 main 에 머지된 V 파일은 절대 수정 금지** — 적용된 환경에서 checksum mismatch 로 부팅 실패.
   잘못됐으면 다음 번호의 새 V 파일로 정정
5. 뷰·함수 등 재적용 가능 객체는 `R__<이름>.sql` (repeatable)

### 프로파일별 스키마 소스

| 경로 | DB | 스키마 소스 |
|---|---|---|
| bootRun (local/prod) | Supabase PG | Flyway (`validate`) |
| Testcontainers 테스트 | PG 컨테이너 | Flyway (`validate`) — V 파일 실전 게이트 |
| e2e 프로파일 / @DataJpaTest | H2 | Hibernate `create-drop` (Flyway off — V 파일은 PG 전용) |

### 금지·주의

- ❌ `JPA_DDL_AUTO=update` 로 Supabase 스키마 변경 (Flyway 이력 밖의 drift 발생)
- ❌ Supabase SQL Editor 로 직접 DDL (동일 — 필요 시 V 파일로 작성 후 부팅으로 적용)
- 시드 데이터는 계속 `DataInitializer` (멱등 체크) 담당. 마이그레이션은 스키마 전용
- 활성화 이전 이력: `V1__baseline.sql` = 2026-07-XX 시점 스냅샷, 기존 Supabase 는 baseline-version 1 로 스킵
```

(N 충돌 대비가 필요해지면 `flyway.out-of-order` 옵션이 있으나 1인 프로젝트에선 비활성 유지가 단순 — 초안에 미포함)

---

## 6. 변경 파일 전체 목록 (병렬 브랜치 충돌 판단용)

| 파일 | 변경 | 충돌 리스크 |
|---|---|---|
| `src/main/resources/application.yml` | flyway enabled true·baseline-version 1·ddl-auto validate·properties 오중첩 복원 | **높음** — 설정 건드리는 모든 브랜치와 겹칠 수 있는 핫파일 |
| `src/main/resources/application-e2e.properties` | `spring.flyway.enabled=false` 1줄 | 낮음 |
| `src/main/resources/db/migration/V1__baseline.sql` | **신규** (pg_dump 산출) | 없음 (신규) |
| `src/main/resources/db/migration/README.md` | 상태 갱신 ("활성화 완료" + 절차 이관 기록) | 낮음 |
| `src/test/resources/application.properties` | **신규** (테스트 기본 flyway off + create-drop) | 없음 (신규) — 단, 이후 모든 테스트의 공통 기본값이 되므로 존재 자체를 팀 규칙으로 인지 필요 |
| `src/test/java/.../YouthMoaApplicationTests.java` | `@SpringBootTest(properties=...)` opt-in 2줄 | 낮음 |
| `CLAUDE.md` | §"DB / 마이그레이션" 절 전문 교체 (§5 초안) + L15 표의 DB 행 문구 갱신 | **중간** — 문서 동시 수정 브랜치 주의 |
| `docs/specs/chore-flyway-activation.md` | 본 명세 (상태 갱신) | 없음 |

- `build.gradle.kts` **무변경** (의존성 PR #82 완료)
- `.github/workflows/*` **무변경** (§4-4)
- `DataInitializer.java` **무변경** — 멱등 체크 (`existsByEmail`, count>0 skip) 가 이미 있어 Flyway 스키마 위에서 그대로 동작. 시드의 마이그레이션 이관은 하지 않음 (→ Q6)

---

## 7. 검증 시나리오

### 정적 검증

1. `.\gradlew.bat compileJava` 통과
2. `.\gradlew.bat test --tests JpaMappingTest --tests ProgramSearchTest --tests ProgramServiceTest --tests ApplicationServiceTest` — @DataJpaTest 그룹이 test properties (flyway off + create-drop) 로 기존과 동일하게 통과
3. `.\gradlew.bat test` 전체 (회사 PC Docker 기동 상태) — 특히 `YouthMoaApplicationTests`:
   - 로그에 `Migrating schema "public" to version "1 - baseline"` 확인 (빈 PG → V1 실행)
   - `ddl-auto=validate` 통과 = V1 ↔ 엔티티 매핑 일치 증명
   - DataInitializer 시드가 Flyway 스키마 위에서 정상 완료
4. V1 내 CREATE TABLE 수 = §2 인벤토리 15개 대조

### 동적 검증 (bootRun + 기동 로그)

**(a) e2e 경로 무회귀** — 회사 PC 필수:
```bash
# .claude/scripts/bootrun-e2e.cmd (H2, 8090)
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8090/          # 200
# 기동 로그에 Flyway 동작 흔적이 없어야 함 (enabled=false)
grep -i flyway <기동로그>   # "FlywayAutoConfiguration ... did not match" 계열 외 migrate 로그 없음
```

**(b) 빈 PG 신규 적용** — 회사 PC Docker 로 수행 가능:
```bash
docker run -d --name ym-verify -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=youthmoa -p 15433:5432 postgres:17
DATABASE_URL=jdbc:postgresql://localhost:15433/youthmoa ./gradlew bootRun
# 기동 로그: "Successfully applied 1 migration ... (v1)" + Hibernate validate 무예외 + 시드 로그
docker exec ym-verify psql -U postgres -d youthmoa -c "select version, description, success from flyway_schema_history;"
# → 1 | baseline | t
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/          # 200
```

**(c) 기존 Supabase baseline** — Supabase 자격증명 있는 환경 (→ Q7):
1. 1차 기동 로그: `Successfully baselined schema with version: 1` → V1 스킵 → validate 통과
2. Supabase SQL Editor: `select * from flyway_schema_history;` → `<< Flyway Baseline >>` 1행
3. **기존 데이터 무손실 확인**: 기동 전후 `select count(*) from users;` 동일
4. 2차 재기동: `Schema "public" is up to date. No migration necessary.` (멱등)
5. 화면 스모크: `GET /`, `/programs`, `/centers` 200

### 실패 시 관찰 포인트

- validate 실패 (`Schema-validation: missing column ...`) → V1 이 엔티티와 어긋남. B′ 절차 재수행 (엔티티 최신 main 기준인지 확인)
- `relation ... already exists` → baseline-version 이 1 이 아니거나 history 초기화 누락 (§3 롤백 플랜)

---

## 8. 사용자 결정 필요 항목 (Q 리스트)

| # | 질문 | 권장안 |
|---|---|---|
| Q1 | `baseline-version` 0 → **1** 변경 (기존 Supabase 는 V1 스킵, 빈 DB 만 V1 실행) | **변경** — 0 유지 시 기존 DB 부팅 실패 (§3) |
| Q2 | V1 생성 방법: A(Hibernate export) / B(Supabase pg_dump) / C(수동) / **B′(클린 Docker PG + Hibernate create + pg_dump)** | **B′** — 엔티티 정합 + 클린 스키마 + 표준 DDL 포맷 (§2) |
| Q3 | PR #82 오중첩 (`spring.flyway.properties.hibernate.*`) 복원 — 복원 시 `hibernate.jdbc.time_zone=Asia/Seoul` 이 약 열흘 만에 재적용되어 timestamp 처리 동작이 PR #82 이전으로 돌아감 | **복원** — 원래 의도된 설정. 단 동작 변화임을 인지하고 (c) 검증에서 기존 데이터 시각 표시 확인 |
| Q4 | H2 경로 (e2e·@DataJpaTest) 처리: **Flyway off 유지** vs H2 호환 마이그레이션 이중 관리 | **off 유지** — V1 은 PG 전용 SQL. H2=엔티티 검증 / PG=마이그레이션 검증 역할 분리 (§4-1) |
| Q5 | default 반전 (`enabled:true` + `validate` 를 yml 기본값으로) vs 환경변수 opt-in 유지 | **반전** — env 미설정 실수로 update 가 조용히 도는 사고 차단. 롤백은 env override 로 가능 (§3) |
| Q6 | 시드 데이터의 Flyway 이관 여부 | **이관 안 함** — DataInitializer 멱등 유지. 마이그레이션은 스키마 전용 (BCrypt encode 등 Java 로직 시드는 SQL 이관 부적합) |
| Q7 | Supabase 실 DB 첫 baseline 기동 (§7-c) 을 어느 환경에서 수행? 회사 PC 에 Supabase 자격증명(DATABASE_URL 등) 주입 가능 여부 확인 필요. 불가면 개인 PC(Mac) 검증 항목으로 이월 | 회사 PC 가능하면 즉시, 아니면 **개인 PC 이월 목록 등재** + PR 본문에 "(c) 검증 보류" 명시 |
| Q8 | Supabase 실 스키마 vs V1 drift 검사 (pg_dump 상호 diff — 죽은 컬럼 실태 파악) 수행 여부 | **수행** (Q7 환경에서 함께) — validate 는 여분 컬럼을 무시하므로 필수는 아니나 update 시대의 부채 문서화 가치 |

---

## 9. 작업 큐 메타

- 작업 ID: `chore/flyway-activation`
- 우선순위: P0-1 (ADMIN-00 §4 — admin 트랙 전체의 선행)
- 추정 단위: 1 PR
- 상태: `spec_done`
- 후속 파생: CLAUDE.md 계획의 `/db-migrate` SKILL 신설 (ADMIN-00 §4 P0-1 언급) — 본 티켓 범위 외, 활성화 안착 후 별도
