package io.github.sihyuuun.youthmoa.program;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class ProgramSpec {

  public static Specification<Program> isActive() {
    return (root, query, cb) -> cb.isTrue(root.get("isActive"));
  }

  /** status: "active" | "upcoming" | "closed" | "" | null → null 반환 시 조건 없음 */
  public static Specification<Program> withDateStatus(String status) {
    if (status == null || status.isBlank()) return null;
    LocalDate today = LocalDate.now();
    return switch (status) {
      case "active" ->
          (root, query, cb) ->
              cb.and(
                  cb.or(
                      cb.isNull(root.get("startDate")),
                      cb.lessThanOrEqualTo(root.get("startDate"), today)),
                  cb.or(
                      cb.isNull(root.get("endDate")),
                      cb.greaterThanOrEqualTo(root.get("endDate"), today)));
      case "upcoming" -> (root, query, cb) -> cb.greaterThan(root.get("startDate"), today);
      case "closed" -> (root, query, cb) -> cb.lessThan(root.get("endDate"), today);
      default -> null;
    };
  }

  /** 단일 지역 (하위 호환용 — 사용처 없으면 추후 제거) */
  public static Specification<Program> withRegion(String region) {
    if (region == null || region.isBlank()) return null;
    return (root, query, cb) -> cb.equal(root.get("region"), region);
  }

  /** 다중 지역 IN 절. null/빈 리스트 → 조건 없음 */
  public static Specification<Program> withRegions(List<String> regions) {
    if (regions == null || regions.isEmpty()) return null;
    return (root, query, cb) -> root.get("region").in(regions);
  }

  /** 다중 청년센터(=organization) IN 절. null/빈 리스트 → 조건 없음 */
  public static Specification<Program> withCenters(List<String> centers) {
    if (centers == null || centers.isEmpty()) return null;
    return (root, query, cb) -> root.get("organization").in(centers);
  }

  /**
   * 인기순 정렬 — applied_count / capacity DESC. Specification.toPredicate 안에서 query.orderBy() 로 정렬을
   * 주입하고 predicate 는 conjunction(=true) 반환. 다른 Specification 과 .and() 로 결합 가능.
   */
  public static Specification<Program> orderByPopularity() {
    return (root, query, cb) -> {
      // count() 등 집계 쿼리는 정렬을 무시한다 (Page 가 count 쿼리를 별도로 실행)
      if (query != null
          && Long.class != query.getResultType()
          && long.class != query.getResultType()) {
        jakarta.persistence.criteria.Subquery<Long> appSubquery = query.subquery(Long.class);
        jakarta.persistence.criteria.Root<io.github.sihyuuun.youthmoa.application.Application>
            appRoot = appSubquery.from(io.github.sihyuuun.youthmoa.application.Application.class);
        appSubquery
            .select(cb.count(appRoot))
            .where(
                cb.equal(appRoot.get("program"), root),
                appRoot
                    .get("status")
                    .in(
                        io.github.sihyuuun.youthmoa.application.ApplicationStatus.PENDING,
                        io.github.sihyuuun.youthmoa.application.ApplicationStatus.APPROVED));
        // capacity null/0 → 1 로 보정해 0 division 회피
        jakarta.persistence.criteria.Expression<Integer> capExpr =
            cb.<Integer>selectCase()
                .when(cb.or(cb.isNull(root.get("capacity")), cb.equal(root.get("capacity"), 0)), 1)
                .otherwise(root.get("capacity"));
        jakarta.persistence.criteria.Expression<Double> ratio =
            cb.quot(appSubquery.as(Double.class), capExpr.as(Double.class)).as(Double.class);
        query.orderBy(cb.desc(ratio), cb.desc(root.get("createdAt")));
      }
      return cb.conjunction();
    };
  }
}
