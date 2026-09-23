package io.github.sihyuuun.youthmoa.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * A9-b verify UNVERIFIED #15.3 해소: V21 방어 블록 실 발화 시나리오 검증.
 *
 * <p>V21 (program.center_id NOT NULL 승격) 은 사전 방어 블록 {@code DO $$ ... IF null_count > 0 THEN RAISE
 * EXCEPTION $$} 으로 center_id IS NULL row 존재 시 명시적 오류를 던진다. H2 는 PL/pgSQL 미지원이라 e2e 프로파일에서 이 발화 경로를
 * 검증할 수 없었다. 본 테스트는 Testcontainers 실 PostgreSQL 로:
 *
 * <ol>
 *   <li>V1..V20 만 적용 (baseline + center_id 컬럼 추가)
 *   <li>center_id IS NULL row 를 직접 INSERT (JPA 우회)
 *   <li>V21 을 수동 migrate → 실패 · 오류 메시지에 "A9-b V21 blocked" 포함 확인
 *   <li>정상 경로: NULL row 없을 때 V21 성공 확인
 * </ol>
 *
 * <p>Spring 컨텍스트 없이 순수 JDBC + Flyway API 로 실행. 회사 PC Docker 미기동 시 Testcontainers 가 skip. CI ubuntu
 * 러너에서 자동 통과.
 */
@Testcontainers
class V21DefensiveBlockIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:latest");

  private static Flyway flywayTo(int targetVersion) {
    return Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .target(String.valueOf(targetVersion))
        .cleanDisabled(false)
        .load();
  }

  @BeforeAll
  static void ensureRunning() {
    // Container 는 @Testcontainers 로 자동 start · verify only
    assertThat(POSTGRES.isRunning()).isTrue();
  }

  @AfterAll
  static void resetForNextTest() {
    // 다른 테스트에 영향 없도록 clean (컨테이너는 static 이므로 클래스 내 test 간 공유)
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .cleanDisabled(false)
        .load()
        .clean();
  }

  @Test
  void v21_center_id_null_존재_시_RAISE_EXCEPTION_발화() throws Exception {
    // 1. V1..V20 만 적용 (center_id 컬럼 추가 완료, NOT NULL 승격 전)
    Flyway pre = flywayTo(20);
    pre.clean();
    pre.migrate();

    // 2. center_id IS NULL row 준비 (JPA 우회 · organization 은 NOT NULL 유지)
    try (Connection conn =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Statement stmt = conn.createStatement()) {

      // program 테이블 필수 컬럼 최소 세트로 INSERT (center_id 만 NULL)
      stmt.executeUpdate(
          "INSERT INTO program "
              + "(title, organization, category, region, description, content, "
              + " start_date, end_date, apply_start_date, apply_end_date, venue, contact, "
              + " capacity, approval_mode, is_active, has_courses, view_count, center_id, "
              + " created_at, updated_at) "
              + "VALUES "
              + "('orphan-A9-b-test', 'A9-b test 센터', 'CULTURE', '양평군', '설명', '내용', "
              + " CURRENT_DATE, CURRENT_DATE, CURRENT_DATE, CURRENT_DATE, '장소', '02-000-0000', "
              + " 10, 'MANUAL', TRUE, FALSE, 0, NULL, "
              + " CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

      try (ResultSet rs =
          stmt.executeQuery("SELECT COUNT(*) FROM program WHERE center_id IS NULL")) {
        rs.next();
        assertThat(rs.getLong(1)).isGreaterThan(0);
      }
    }

    // 3. V21 을 수동 실행 → RAISE EXCEPTION 발화 확인
    Flyway v21 = flywayTo(21);

    assertThatThrownBy(v21::migrate)
        .as("V21 은 center_id IS NULL row 가 있으면 RAISE EXCEPTION 으로 실패해야 한다")
        .hasMessageContaining("A9-b V21 blocked")
        .hasMessageContaining("center_id IS NULL");
  }

  @Test
  void v21_center_id_null_없음_정상_성공() throws Exception {
    // clean → V1..V21 전체 적용 (시드는 없음 · program 테이블 빈 상태)
    Flyway all = flywayTo(21);
    all.clean();
    all.migrate();

    // program.center_id 컬럼이 NOT NULL 이 됐는지 스키마로 확인
    try (Connection conn =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Statement stmt = conn.createStatement();
        ResultSet rs =
            stmt.executeQuery(
                "SELECT is_nullable FROM information_schema.columns "
                    + "WHERE table_name = 'program' AND column_name = 'center_id'")) {
      rs.next();
      assertThat(rs.getString(1)).isEqualTo("NO");
    }
  }
}
