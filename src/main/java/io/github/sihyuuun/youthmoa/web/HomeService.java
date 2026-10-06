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

  /** Hero 배너 fallback URL (Unsplash A — 톤 일관성). site_image 시드 실패 시 최소 1건 확보용. */
  private static final String HERO_FALLBACK_URL =
      "https://images.unsplash.com/photo-1531482615713-2afd69097998?w=1440&h=560&fit=crop";

  /** F0e-2: Hero 배너 이미지 URL 리스트 (sortOrder ASC). 로테이션 대상 6장. 빈 경우 fallback 1건 리스트. */
  public List<String> getHeroImageUrls() {
    List<String> urls =
        siteImageRepository.findAllBySlotAndIsActiveTrueOrderBySortOrderAsc(SLOT_HERO).stream()
            .map(SiteImage::getImageUrl)
            .collect(Collectors.toList());
    if (urls.isEmpty()) {
      return List.of(HERO_FALLBACK_URL);
    }
    return urls;
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

  /**
   * Top 4 프로그램 — 모집중 + applyEndDate ASC (신청 마감임박).
   *
   * <p>D5-Q1b (2026-10-02): endDate → applyEndDate 축으로 전환. 홈 뱃지(D-N)와 Top 4 정렬이 모두 신청기간 기준으로
   * 일치하도록. applyEndDate 가 null 이면 endDate 폴백 (Repository @Query 의 COALESCE).
   */
  public List<Program> findTopPrograms() {
    return programRepository.findTop4ByIsActiveTrueOrderByApplyEndDateAsc(
        org.springframework.data.domain.PageRequest.of(0, 4));
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

  /**
   * 맞춤 추천 섹션 제목 옆 관심 태그 chip 문자열.
   *
   * <p>prototype.tsx L581 형식: {첫 관심 지역} · {관심 카테고리들 · 로 join} 관심. 예: "부천시 · 취업·창업 관심".
   *
   * <p>지역·카테고리 둘 다 없으면 null → 뷰에서 chip 미노출.
   */
  public String getRecommendInterestChip(Long userId) {
    return userRepository
        .findById(userId)
        .map(
            u -> {
              String firstRegion =
                  u.getInterestRegions() != null && !u.getInterestRegions().isEmpty()
                      ? u.getInterestRegions().iterator().next()
                      : null;
              String catsJoined =
                  u.getInterestCategories() != null && !u.getInterestCategories().isEmpty()
                      ? String.join("·", u.getInterestCategories())
                      : null;
              if (firstRegion == null && catsJoined == null) return null;
              if (firstRegion != null && catsJoined != null) {
                return firstRegion + " · " + catsJoined + " 관심";
              }
              return firstRegion != null ? firstRegion + " 관심" : catsJoined + " 관심";
            })
        .orElse(null);
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
    // 활성 프로그램 pool (스코어링용) — 50건 넉넉히 로드. 정렬은 아래 sort() 에서 재적용되므로
    // DB 레벨 정렬 축은 보조. D5-Q1b 로 Top 4 는 applyEndDate 축으로 전환됐지만, 여기 pool 재조회는
    // 뒤의 scoreOf → endDate ASC fallback 과 일관을 위해 endDate 축 유지 (ArrayList 복사 후 재정렬).
    List<Program> pool =
        new ArrayList<>(
            programRepository
                .findAllByIsActiveTrue(
                    org.springframework.data.domain.PageRequest.of(
                        0, 50, org.springframework.data.domain.Sort.by("endDate").ascending()))
                .getContent());

    // F-signup-03: interests → interestCategories (category 매칭용).
    Set<String> interests =
        user.getInterestCategories() != null ? user.getInterestCategories() : Set.of();
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
