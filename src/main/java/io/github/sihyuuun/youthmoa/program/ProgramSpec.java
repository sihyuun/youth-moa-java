package io.github.sihyuuun.youthmoa.program;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class ProgramSpec {

  public static Specification<Program> isActive() {
    return (root, query, cb) -> cb.isTrue(root.get("isActive"));
  }

  /**
   * 통합 검색 키워드 매칭 — title / organization / region / content 4개 컬럼 OR LIKE. 대소문자 무시. q 가 null/빈 문자열이면
   * 조건 없음(cb.conjunction) 반환하여 다른 Specification 과 안전하게 결합.
   */
  public static Specification<Program> withKeyword(String q) {
    return (root, query, cb) -> {
      if (q == null || q.isBlank()) return cb.conjunction();
      String pattern = "%" + q.toLowerCase() + "%";
      return cb.or(
          cb.like(cb.lower(root.get("title")), pattern),
          cb.like(cb.lower(root.get("organization")), pattern),
          cb.like(cb.lower(root.get("region")), pattern),
          cb.like(cb.lower(root.get("content")), pattern));
    };
  }

  /**
   * status: "open" | "upcoming" | "ended" | "" | null → null 반환 시 조건 없음.
   *
   * <p>하위 호환: 기존 "active" → "open", "closed" → "ended" 로 매핑.
   *
   * <p>"ended" 는 endDate < today AND isActive=true (SUSPENDED 는 별도 상태이므로 제외).
   */
  public static Specification<Program> withDateStatus(String status) {
    if (status == null || status.isBlank()) return null;
    LocalDate today = LocalDate.now();
    // 하위 호환
    String key = status;
    if ("active".equals(key)) key = "open";
    if ("closed".equals(key)) key = "ended";
    return switch (key) {
      case "open" ->
          (root, query, cb) ->
              cb.and(
                  cb.or(
                      cb.isNull(root.get("startDate")),
                      cb.lessThanOrEqualTo(root.get("startDate"), today)),
                  cb.or(
                      cb.isNull(root.get("endDate")),
                      cb.greaterThanOrEqualTo(root.get("endDate"), today)));
      case "upcoming" -> (root, query, cb) -> cb.greaterThan(root.get("startDate"), today);
      case "ended" ->
          (root, query, cb) ->
              cb.and(
                  cb.lessThan(root.get("endDate"), today), cb.isTrue(root.get("isActive")));
      default -> null;
    };
  }

  /**
   * 전체 탭 (status 필터 없음) 조회 시 ENDED 를 뒤로 밀어내는 정렬 보정.
   *
   * <p>Predicate 는 conjunction 반환하고 query.orderBy() 로 정렬만 주입한다. 카운트 쿼리는 무시. 다른 Sort/Spec 과 결합
   * 시 이 정렬이 최상단 우선으로 적용된다 (예: newest/deadline 이 뒤로 밀림). 필요 시 호출부에서 순서 조정.
   *
   * <p>기준: endDate < today → 1, 그 외 → 0 (ASC → 진행중/예정이 먼저).
   */
  public static Specification<Program> endedLast() {
    return (root, query, cb) -> {
      if (query != null
          && Long.class != query.getResultType()
          && long.class != query.getResultType()) {
        LocalDate today = LocalDate.now();
        jakarta.persistence.criteria.Expression<Integer> endedFlag =
            cb.<Integer>selectCase()
                .when(cb.lessThan(root.get("endDate"), today), 1)
                .otherwise(0)
                .as(Integer.class);
        // 기존 orderBy 를 유지하면서 앞쪽에 endedFlag ASC 를 삽입
        java.util.List<jakarta.persistence.criteria.Order> existing =
            new java.util.ArrayList<>(query.getOrderList());
        java.util.List<jakarta.persistence.criteria.Order> merged = new java.util.ArrayList<>();
        merged.add(cb.asc(endedFlag));
        merged.addAll(existing);
        query.orderBy(merged);
      }
      return cb.conjunction();
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
