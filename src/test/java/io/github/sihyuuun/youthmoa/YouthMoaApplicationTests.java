package io.github.sihyuuun.youthmoa;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Testcontainers PG 에서 Flyway V1 실전 적용 + validate 검증 게이트.
 *
 * <p>chore/flyway-activation (2026-07-22): test 전용 properties 는 flyway=off + ddl-auto=create-drop 이
 * default. 이 클래스에서만 opt-in 해서 V1__baseline.sql 이 빈 PG 컨테이너에 정상 적용되고 엔티티 매핑과 일치하는지 매 PR 검증.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
    properties = {
      "spring.flyway.enabled=true",
      "spring.jpa.hibernate.ddl-auto=validate"
    })
class YouthMoaApplicationTests {

  @Test
  void contextLoads() {}
}
