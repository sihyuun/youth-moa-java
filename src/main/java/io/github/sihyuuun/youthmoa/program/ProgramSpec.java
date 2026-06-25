package io.github.sihyuuun.youthmoa.program;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class ProgramSpec {

    public static Specification<Program> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    /** status: "active" | "upcoming" | "closed" | "" | null → null 반환 시 조건 없음 */
    public static Specification<Program> withDateStatus(String status) {
        if (status == null || status.isBlank()) return null;
        LocalDate today = LocalDate.now();
        return switch (status) {
            case "active" -> (root, query, cb) -> cb.and(
                    cb.or(cb.isNull(root.get("startDate")),
                          cb.lessThanOrEqualTo(root.get("startDate"), today)),
                    cb.or(cb.isNull(root.get("endDate")),
                          cb.greaterThanOrEqualTo(root.get("endDate"), today))
            );
            case "upcoming" -> (root, query, cb) ->
                    cb.greaterThan(root.get("startDate"), today);
            case "closed" -> (root, query, cb) ->
                    cb.lessThan(root.get("endDate"), today);
            default -> null;
        };
    }

    public static Specification<Program> withRegion(String region) {
        if (region == null || region.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("region"), region);
    }

    public static Specification<Program> withCategory(String category) {
        if (category == null || category.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }
}
