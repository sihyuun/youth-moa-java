package io.github.sihyuuun.youthmoa.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * F0h-center-desc-image (spec §11): DataInitializer 파생 시드 재발 방지 소스 검증.
 *
 * <p>2026-07-09 F0h 좌표 사고 회고에 이어, description·imageUrl 파생 시드(imagePool 로테이션·descByKeyword
 * 규칙·featuredDesc 하드코딩) 도 완전 삭제되고 centers-content.csv 를 진리 소스로 사용해야 한다. DataInitializer.java 소스에서
 * 해당 식별자가 재도입되지 않았는지 문자열 부재로 방어한다.
 *
 * <p>테스트 실행 환경(회사 PC · JDK17 부트스트랩) 특성상 소스 파일 정적 검증만 수행한다.
 */
class DataInitializerNoDerivedSeedTest {

  @Test
  void 파생_시드_식별자_부재() throws Exception {
    Path source = Path.of("src/main/java/io/github/sihyuuun/youthmoa/common/DataInitializer.java");
    assertThat(Files.exists(source)).as("DataInitializer 소스 파일 존재").isTrue();
    String content = Files.readString(source);

    assertThat(content)
        .as("imagePool 로테이션 로직 재도입 방지 (centers-content.csv 로 이관됨)")
        .doesNotContain("imagePool");
    assertThat(content).as("descByKeyword 파생 매핑 로직 재도입 방지").doesNotContain("descByKeyword");
    assertThat(content).as("featuredDesc 하드코딩 로직 재도입 방지").doesNotContain("featuredDesc");
    assertThat(content).as("imgIdx 로테이션 인덱스 재도입 방지").doesNotContain("imgIdx");
  }
}
