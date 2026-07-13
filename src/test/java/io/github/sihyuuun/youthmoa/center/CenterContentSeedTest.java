package io.github.sihyuuun.youthmoa.center;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * F0h-center-desc-image (spec §11): CenterContent 시드 회귀 방어 테스트.
 *
 * <p>centers-content.csv 48행이 CenterContent 엔티티로 온전히 시드되고, Center 와 1:1 매핑되며, description·imageUrl
 * 이 non-null 로 로드되는지 검증한다. 파생 시드(imagePool·descByKeyword·featuredDesc) 완전 제거 후에도 featured 8건 문안이
 * 정확히 CSV 원본을 반영하는지 함께 확인한다.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@Transactional // @OneToOne(LAZY) center 참조를 assertion 메시지에서 사용 → 트랜잭션 스코프 필수
class CenterContentSeedTest {

  /** spec §11: featured 8건 description 정확 문안 매핑 (centers-content.csv 원본 기준). */
  private static final Map<String, String> FEATURED_DESC =
      Map.of(
          "청년바람지대", "청년 창업과 네트워킹을 위한 복합문화공간",
          "청년이봄", "취업·역량강화 특화 청년지원센터",
          "안양청년1번가", "정신건강·힐링 프로그램 전문 센터",
          "소사청년공간 소사로움", "취업·역량강화 특화 청년지원센터",
          "화성시청년지원센터 H.E.Y", "취업·진로 전문 지원 청년센터",
          "광명시 청년동", "지역사회 연계 청년 커뮤니티 허브",
          "양평청년공간 오름", "소셜벤처·사회적 경제 청년 지원",
          "의왕청년발전소", "지역사회 연계 청년 커뮤니티 허브");

  @Autowired CenterRepository centerRepository;
  @Autowired CenterContentRepository centerContentRepository;

  @Test
  void TC01_CenterContent_48건_시드_성공() {
    assertThat(centerContentRepository.count()).isEqualTo(48);
  }

  @Test
  void TC02_모든_Center_에_CenterContent_존재() {
    List<Center> centers = centerRepository.findAll();
    for (Center c : centers) {
      assertThat(centerContentRepository.findByCenterId(c.getId()))
          .as("Center '%s' 에 매칭되는 CenterContent 가 있어야 함", c.getName())
          .isPresent();
    }
  }

  @Test
  void TC03_description_imageUrl_모두_non_null() {
    List<CenterContent> all = centerContentRepository.findAll();
    for (CenterContent cc : all) {
      assertThat(cc.getDescription())
          .as("description non-null (center=%s)", cc.getCenter().getName())
          .isNotNull()
          .isNotBlank();
      assertThat(cc.getImageUrl())
          .as("imageUrl non-null (center=%s)", cc.getCenter().getName())
          .isNotNull()
          .isNotBlank();
    }
  }

  @Test
  void TC04_featured_8건_문안_정확_반영() {
    for (Map.Entry<String, String> e : FEATURED_DESC.entrySet()) {
      String name = e.getKey();
      String expected = e.getValue();
      Center c =
          centerRepository.findAll().stream()
              .filter(x -> name.equals(x.getName()))
              .findFirst()
              .orElseThrow(() -> new AssertionError("Center 부재: " + name));
      CenterContent cc =
          centerContentRepository
              .findByCenterId(c.getId())
              .orElseThrow(() -> new AssertionError("CenterContent 부재: " + name));
      assertThat(cc.getDescription()).as("featured description '%s'", name).isEqualTo(expected);
    }
  }

  @Test
  void TC05_findByCenterIdIn_배치_조회_정상() {
    List<Long> ids = centerRepository.findAll().stream().map(Center::getId).limit(10).toList();
    List<CenterContent> batch = centerContentRepository.findByCenterIdIn(ids);
    assertThat(batch).hasSize(10);
  }
}
