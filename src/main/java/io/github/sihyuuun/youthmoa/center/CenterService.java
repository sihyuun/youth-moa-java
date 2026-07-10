package io.github.sihyuuun.youthmoa.center;

import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.region.Region;
import io.github.sihyuuun.youthmoa.region.RegionRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 청년센터 목록·상세 조회.
 *
 * <p>F0h-c2: RegionRepository 기반 지역 드롭다운, ProgramRepository 배치 카운트로 programCount 채움, sort=programs
 * 지원.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CenterService {

  private final CenterRepository centerRepository;
  private final RegionRepository regionRepository;
  private final ProgramRepository programRepository;
  private final CenterContentRepository centerContentRepository;

  /**
   * 필터·정렬 적용된 센터 목록.
   *
   * @param q 센터명/지역 부분일치 검색어 (nullable, blank → 무시)
   * @param region 지역명 완전일치 (nullable → 전체)
   * @param onlyActive true = isActive 만
   * @param sort "name"(기본) | "programs"(진행중 프로그램 수 내림차순, 동률 시 이름 asc) | "region"(하위호환)
   * @param now 배지 판정 기준 시각 (F0h-operating-hours-badge spec §9-6)
   * @param isHoliday 공휴일 여부 (Controller 가 KoreanHolidayRegistry 로 사전 계산해 전달)
   */
  public List<CenterListItem> list(
      String q, String region, boolean onlyActive, String sort, LocalDateTime now, boolean isHoliday) {
    List<Center> base;
    if (region != null && !region.isBlank()) {
      base =
          onlyActive
              ? centerRepository.findByRegionAndIsActiveTrue(region)
              : centerRepository.findByRegion(region);
    } else if (onlyActive) {
      base = centerRepository.findAllByIsActiveTrueOrderByRegionAscNameAsc();
    } else {
      base = centerRepository.findAllByOrderByNameAsc();
    }

    String needle = (q == null) ? null : q.trim();
    java.util.stream.Stream<Center> stream = base.stream();
    if (needle != null && !needle.isEmpty()) {
      final String kw = needle;
      stream =
          stream.filter(
              c ->
                  (c.getName() != null && c.getName().contains(kw))
                      || (c.getRegion() != null && c.getRegion().contains(kw)));
    }

    List<Center> filtered = stream.toList();

    // programCount 배치 조회 (organization 문자열 매칭 근사)
    Map<String, Integer> countByOrg = new HashMap<>();
    for (Object[] row : programRepository.countActiveGroupByOrganization()) {
      String org = (String) row[0];
      Long cnt = (Long) row[1];
      countByOrg.put(org, cnt.intValue());
    }

    // F0h-center-desc-image: CenterContent 일괄 조회 (N+1 방어)
    List<Long> centerIds = filtered.stream().map(Center::getId).toList();
    Map<Long, CenterContent> contentByCenterId = new HashMap<>();
    if (!centerIds.isEmpty()) {
      for (CenterContent cc : centerContentRepository.findByCenterIdIn(centerIds)) {
        contentByCenterId.put(cc.getCenter().getId(), cc);
      }
    }

    List<CenterListItem> items =
        filtered.stream()
            .map(
                c ->
                    CenterListItem.of(
                        c,
                        countByOrg.getOrDefault(c.getName(), 0),
                        now,
                        isHoliday,
                        contentByCenterId.get(c.getId())))
            .collect(java.util.stream.Collectors.toList());

    Comparator<CenterListItem> cmp;
    if ("programs".equalsIgnoreCase(sort)) {
      cmp =
          Comparator.comparingInt(CenterListItem::programCount)
              .reversed()
              .thenComparing(CenterListItem::name);
    } else if ("region".equalsIgnoreCase(sort)) {
      cmp = Comparator.comparing(CenterListItem::region).thenComparing(CenterListItem::name);
    } else {
      cmp = Comparator.comparing(CenterListItem::name);
    }
    items.sort(cmp);
    return items;
  }

  public Optional<Center> findById(Long id) {
    return centerRepository.findById(id);
  }

  /**
   * F0h-center-desc-image (spec §9-1): 상세 패널용 CenterContent 조회. 없으면 empty (View 는 fallback).
   */
  public Optional<CenterContent> findContentByCenterId(Long centerId) {
    return centerContentRepository.findByCenterId(centerId);
  }

  /** F0h gap fix: 상세 패널의 "진행중인 프로그램 N건" 카드용. organization 문자열 매칭. */
  public int programCountFor(String centerName) {
    if (centerName == null) return 0;
    for (Object[] row : programRepository.countActiveGroupByOrganization()) {
      if (centerName.equals(row[0])) {
        return ((Long) row[1]).intValue();
      }
    }
    return 0;
  }

  /**
   * 필터 UI 의 지역 드롭다운용 — F0h-c2: Region 엔티티 기반으로 교체. 이름 오름차순.
   */
  public List<String> distinctActiveRegions() {
    return regionRepository.findAllByOrderByNameAsc().stream().map(Region::getName).toList();
  }
}
