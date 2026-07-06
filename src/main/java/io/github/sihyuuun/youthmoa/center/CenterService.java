package io.github.sihyuuun.youthmoa.center;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 청년센터 목록·상세 조회.
 *
 * <p>@Transactional(readOnly=true) — 조회 전용. Spring 의 트랜잭션 프록시가 DB 세션을 열어 두므로 JPA 지연 로딩·LOB 접근이 안전.
 * 여기선 lazy 관계·LOB 이 없어도 관례상 유지 (조회 성능 힌트 포함).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CenterService {

  private final CenterRepository centerRepository;

  /**
   * 필터·정렬 적용된 센터 목록.
   *
   * @param q 센터명/지역 부분일치 검색어 (nullable, blank → 무시)
   * @param region 지역명 완전일치 (nullable → 전체)
   * @param onlyActive true = isActive 만
   * @param sort "name"(기본) | "region" — region 지정 시 region asc, name asc 로 이차 정렬
   */
  public List<CenterListItem> list(String q, String region, boolean onlyActive, String sort) {
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

    Comparator<Center> cmp;
    if ("region".equalsIgnoreCase(sort)) {
      cmp = Comparator.comparing(Center::getRegion).thenComparing(Center::getName);
    } else {
      cmp = Comparator.comparing(Center::getName);
    }

    return stream.sorted(cmp).map(CenterListItem::from).toList();
  }

  public Optional<Center> findById(Long id) {
    return centerRepository.findById(id);
  }

  /** 필터 UI 의 지역 드롭다운용 — 활성 센터의 지역 distinct. */
  public List<String> distinctActiveRegions() {
    return centerRepository.findAllByIsActiveTrueOrderByRegionAscNameAsc().stream()
        .map(Center::getRegion)
        .distinct()
        .sorted()
        .toList();
  }
}
