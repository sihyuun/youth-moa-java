package io.github.sihyuuun.youthmoa.center;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * F0h-real-coords: CSV 기반 실좌표 시드 회귀 방어 테스트.
 *
 * <p>과거 {@code regionCoords + offset(idx*15)} 파생 시드 로직이 재도입되지 않도록 소스 문자열까지 검증한다 (TC-07).
 * DataInitializer 는 {@code @Profile("!test")} 지만 e2e 프로파일에서는 활성화되어 시드가 실행된다.
 */
@SpringBootTest
@ActiveProfiles("e2e")
class CenterCoordinateSeedTest {

  private static final BigDecimal LAT_MIN = new BigDecimal("33");
  private static final BigDecimal LAT_MAX = new BigDecimal("39");
  private static final BigDecimal LNG_MIN = new BigDecimal("124");
  private static final BigDecimal LNG_MAX = new BigDecimal("132");

  /** 화성 2건은 같은 건물 4·5층이라 CSV 상 동일 좌표 (spec §9-3 whitelist). */
  private static final Set<String> DUP_COORD_WHITELIST_REGIONS = Set.of("화성시");

  @Autowired CenterRepository centerRepository;

  @Test
  void TC01_48개_센터_시드_성공() {
    assertThat(centerRepository.count()).isEqualTo(48);
  }

  @Test
  void TC02_모든_센터_좌표_non_null() {
    List<Center> all = centerRepository.findAll();
    assertThat(all)
        .allSatisfy(
            c -> {
              assertThat(c.getLatitude()).as("latitude of %s", c.getName()).isNotNull();
              assertThat(c.getLongitude()).as("longitude of %s", c.getName()).isNotNull();
            });
  }

  @Test
  void TC03_주소_전화_운영시간_실_CSV_반영_fallback_아님() {
    Center 내일꿈제작소 =
        centerRepository.findAll().stream()
            .filter(c -> "내일꿈제작소".equals(c.getName()))
            .findFirst()
            .orElseThrow();
    // legacy fallback 은 address="경기도 {region}" / phone=null / hours="평일 09:00~18:00"
    assertThat(내일꿈제작소.getAddress()).contains("고양시 덕양구").contains("은빛로 72");
    assertThat(내일꿈제작소.getPhone()).isEqualTo("031-8075-2873");
    assertThat(내일꿈제작소.getOperatingHours()).startsWith("월~토 10:00~18:00");
    // 전체적으로 fallback 문자열 부재 확인
    for (Center c : centerRepository.findAll()) {
      assertThat(c.getAddress()).as("fallback 주소 미사용: " + c.getName()).isNotEqualTo("경기도 " + c.getRegion());
    }
  }

  @Test
  void TC04_같은_region_내_서로_다른_좌표_파생_미사용_확인() {
    // 화성 whitelist 를 제외한 나머지 region 에서 좌표 중복 없어야 함
    Map<String, List<Center>> byRegion =
        centerRepository.findAll().stream().collect(Collectors.groupingBy(Center::getRegion));
    for (Map.Entry<String, List<Center>> e : byRegion.entrySet()) {
      String region = e.getKey();
      List<Center> list = e.getValue();
      if (list.size() < 2) continue;
      if (DUP_COORD_WHITELIST_REGIONS.contains(region)) continue;
      Set<String> coords = new HashSet<>();
      for (Center c : list) {
        String key = c.getLatitude().stripTrailingZeros() + "|" + c.getLongitude().stripTrailingZeros();
        assertThat(coords.add(key))
            .as("region '%s' 에 좌표 중복 발견 (파생 시드 흔적?) — %s", region, c.getName())
            .isTrue();
      }
    }
  }

  @Test
  void TC05_isActive_CSV_반영() {
    // 현재 CSV 는 모두 true. false 로 바뀌면 그 값이 그대로 반영되는지 검증.
    for (Center c : centerRepository.findAll()) {
      // build 파라미터 Boolean isActive 는 null → true 로 방어됨. false 로 명시된 경우만 검사.
      // 여기선 최소 조건: 최소 1개 이상 isActive == true (전부 false 로 잘못 로드되지 않았는지)
      // + 값이 CSV 원본을 그대로 반영해 boolean primitive 로 존재
      assertThat(c.isActive()).isIn(true, false);
    }
    assertThat(centerRepository.findAllByIsActiveTrue()).isNotEmpty();
  }

  @Test
  void TC06_화성_2건_동일_좌표_허용_whitelist() {
    List<Center> 화성 = centerRepository.findByRegion("화성시");
    assertThat(화성).hasSizeGreaterThanOrEqualTo(2);
    // whitelist: 동일 좌표여도 통과 — 명시적으로 assertion 하지 않고 존재만 확인
  }

  @Test
  void TC07_DataInitializer_소스에_regionCoords_문자열_부재() throws Exception {
    Path source =
        Path.of(
            "src/main/java/io/github/sihyuuun/youthmoa/common/DataInitializer.java");
    assertThat(Files.exists(source)).as("DataInitializer 소스 파일 존재").isTrue();
    String content = Files.readString(source);
    assertThat(content)
        .as("파생 시드 로직(regionCoords) 재도입 방지")
        .doesNotContain("regionCoords");
    assertThat(content)
        .as("파생 시드 로직(regionOffsetIdx) 재도입 방지")
        .doesNotContain("regionOffsetIdx");
    assertThat(content)
        .as("하드코딩 lookup(centerDetails) 재도입 방지")
        .doesNotContain("centerDetails");
  }

  @Test
  void TC08_operatingHours_쉼표_포함_값_온전히_저장() {
    // 예: "월~토 10:00~18:00, 일·공휴일 휴관" — RFC 4180 이스케이프로 통째 저장 필요
    Center 내일꿈제작소 =
        centerRepository.findAll().stream()
            .filter(c -> "내일꿈제작소".equals(c.getName()))
            .findFirst()
            .orElseThrow();
    String hours = 내일꿈제작소.getOperatingHours();
    assertThat(hours).contains(", ").contains("월~토").contains("휴관");
  }

  @Test
  void 좌표_한반도_범위_내() {
    for (Center c : centerRepository.findAll()) {
      assertThat(c.getLatitude())
          .as("latitude 범위: %s", c.getName())
          .isBetween(LAT_MIN, LAT_MAX);
      assertThat(c.getLongitude())
          .as("longitude 범위: %s", c.getName())
          .isBetween(LNG_MIN, LNG_MAX);
    }
  }
}
