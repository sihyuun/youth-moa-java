package io.github.sihyuuun.youthmoa.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * fix-e2e-seed-pollution 회귀 방지.
 *
 * <p>test-only {@link TestFixtureController} 는 {@code @Profile("e2e")} 로 격리된다. e2e 가 아닌 프로파일(=프로덕션
 * local·prod 계열) 로 컨텍스트를 로드했을 때 이 Bean 이 등록되지 않음을 보장한다. 프로덕션 빌드로 유출되면 인증 없이 신청 데이터를 삭제할 수 있는
 * endpoint 가 노출되기 때문.
 *
 * <p>SecurityConfig 는 {@code environment.matchesProfiles("e2e")} 로 매처 등록 여부를 통제하므로, Bean 자체 미등록
 * 이중 안전장치로 동작한다.
 *
 * <p>구현 노트: 실 프로덕션 프로파일(local)은 Supabase 접속을 요구하므로 CI 에서 로드 불가. e2e 프로파일과 동일한 H2 설정을 property 로 직접
 * 주입하되 {@code @ActiveProfiles} 는 "test-guard" (존재하지 않는 임의 프로파일) 로 지정해 e2e 프로파일 activation 을 회피한다.
 */
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:test-guard;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false",
      "app.seed.enabled=false"
    })
@ActiveProfiles("test-guard")
class TestFixtureProfileGuardTest {

  @Autowired private ApplicationContext applicationContext;

  @Test
  void testFixtureController_e2e_외_프로파일에서_Bean_미등록() {
    assertThat(applicationContext.getEnvironment().matchesProfiles("e2e")).isFalse();
    assertThat(applicationContext.containsBean("testFixtureController")).isFalse();
    assertThatThrownBy(() -> applicationContext.getBean(TestFixtureController.class))
        .isInstanceOf(NoSuchBeanDefinitionException.class);
  }
}
