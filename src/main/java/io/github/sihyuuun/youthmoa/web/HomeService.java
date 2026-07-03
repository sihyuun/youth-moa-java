package io.github.sihyuuun.youthmoa.web;

import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.center.CenterRepository;
import io.github.sihyuuun.youthmoa.common.SiteImage;
import io.github.sihyuuun.youthmoa.common.SiteImageRepository;
import io.github.sihyuuun.youthmoa.notice.Notice;
import io.github.sihyuuun.youthmoa.notice.NoticeRepository;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramCardDto;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 홈 화면 데이터 조합 로직.
 *
 * <p>각 model attribute 별 메서드로 분리하여 테스트 & 향후 admin 커스터마이즈에 대비.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

  private static final String SLOT_HERO = "HERO_BANNER";
  private static final int RECOMMEND_SIZE = 4;
  private static final List<ApplicationStatus> ACTIVE_STATUSES =
      List.of(ApplicationStatus.PENDING, ApplicationStatus.APPROVED);

  private final ProgramRepository programRepository;
  private final CenterRepository centerRepository;
  private final ApplicationRepository applicationRepository;
  private final NoticeRepository noticeRepository;
  private final SiteImageRepository siteImageRepository;
  private final UserRepository userRepository;

  /** Hero 배너 이미지 URL. site_image 에 없으면 fallback. */
  public String getHeroImageUrl() {
    return siteImageRepository
        .findBySlotAndIsActiveTrue(SLOT_HERO)
        .map(SiteImage::getImageUrl)
        .orElse("/images/banner_01.png");
  }

  /** Quick Stats — 모집중 프로그램 개수. */
  public long countActivePrograms() {
    return programRepository.countByIsActiveTrue();
  }

  /** Quick Stats — 참여 청년센터 개수. */
  public long countCenters() {
    return centerRepository.count();
  }

  /** Quick Stats — 누적 참여자 (distinct user). */
  public long countTotalApplicants() {
    return applicationRepository.countDistinctUsers();
  }

  /** Top 4 프로그램 — 모집중 + endDate ASC (마감임박). */
  public List<Program> findTopPrograms() {
    return programRepository.findTop4ByIsActiveTrueOrderByEndDateAsc();
  }

  /** Top 4 프로그램 → ProgramCardDto 변환 (CapacityBar용). */
  public List<ProgramCardDto> findTopProgramCards() {
    List<Program> programs = findTopPrograms();
    return toCardDtos(programs);
  }

  /** 맞춤추천 → ProgramCardDto 변환 (CapacityBar용). */
  public List<ProgramCardDto> findRecommendedProgramCards(Long userId) {
    List<Program> programs = findRecommendedPrograms(userId);
    return toCardDtos(programs);
  }

  private List<ProgramCardDto> toCardDtos(List<Program> programs) {
    if (programs.isEmpty()) return List.of();
    List<Long> ids = programs.stream().map(Program::getId).collect(Collectors.toList());
    Map<Long, Long> countMap =
        applicationRepository.countByProgramIdsAndStatuses(ids, ACTIVE_STATUSES).stream()
            .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    return programs.stream()
        .map(p -> new ProgramCardDto(p, countMap.getOrDefault(p.getId(), 0L)))
        .collect(Collectors.toList());
  }

  /**
   * 로그인 사용자용 맞춤추천 4건. 알고리즘: interests ∩ category 우선 → region 일치 우선 → endDate ASC → 부족하면 마감임박
   * fallback
   */
  public List<Program> findRecommendedPrograms(Long userId) {
    User user = userRepository.findById(userId).orElse(null);
    if (user == null) return findTopPrograms();
    // 활성 프로그램 마감임박순 pool (한 번만 조회 후 스코어링)
    List<Program> pool = programRepository.findTop4ByIsActiveTrueOrderByEndDateAsc();
    // pool 이 4개보다 부족할 여지가 있어 넉넉히 재조회 — findAllByIsActiveTrue 로 대체
    pool =
        programRepository
            .findAllByIsActiveTrue(
                org.springframework.data.domain.PageRequest.of(
                    0, 50, org.springframework.data.domain.Sort.by("endDate").ascending()))
            .getContent();
    // Page.getContent() 가 unmodifiable List 반환 → sort 위해 새 ArrayList 로 복사
    pool = new ArrayList<>(pool);

    Set<String> interests = user.getInterests() != null ? user.getInterests() : Set.of();
    String userRegion = null; // User 엔티티에 region 필드 없음. address 기반 확장 여지.

    // 스코어링: interests 매치 +10, region 매치 +5. 같은 점수 내에서 endDate ASC 유지.
    pool.sort(
        (a, b) -> {
          int sa = scoreOf(a, interests, userRegion);
          int sb = scoreOf(b, interests, userRegion);
          if (sa != sb) return Integer.compare(sb, sa); // desc
          return 0; // 원본 endDate ASC 순 유지
        });

    // fallback: 상위 4개 반환. 4 미만이어도 available 만.
    LinkedHashSet<Program> result = new LinkedHashSet<>();
    for (Program p : pool) {
      if (result.size() >= RECOMMEND_SIZE) break;
      result.add(p);
    }
    return new ArrayList<>(result);
  }

  private int scoreOf(Program p, Set<String> interests, String userRegion) {
    int score = 0;
    if (p.getCategory() != null && interests.contains(p.getCategory())) score += 10;
    if (userRegion != null && userRegion.equals(p.getRegion())) score += 5;
    return score;
  }

  /** 홈 대표 공지 (pinned + 최신 1건). 없으면 null. */
  public Notice findMainNotice() {
    return noticeRepository.findTop1ByIsPinnedTrueOrderByCreatedAtDesc().orElse(null);
  }

  /** 홈 서브 공지 3건. */
  public List<Notice> findSubNotices() {
    return noticeRepository.findTop3ByIsPinnedFalseOrderByCreatedAtDesc();
  }

  /** 홈 공간 이미지 3건 (SiteImage 중 HOME_SPACE_* slot 만). */
  public List<SiteImage> findSpaceImages() {
    return siteImageRepository.findAllByIsActiveTrueOrderBySortOrderAsc().stream()
        .filter(si -> si.getSlot() != null && si.getSlot().startsWith("HOME_SPACE_"))
        .toList();
  }
}
