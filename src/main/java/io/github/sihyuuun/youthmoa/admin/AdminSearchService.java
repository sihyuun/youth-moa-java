package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.program.ProgramSpec;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import jakarta.persistence.criteria.Predicate;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A8-search (2026-10-01) — 관리자 헤더 글로벌 검색 서비스.
 *
 * <p>프로그램(ProgramSpec.withKeyword 재사용) + 사용자(name·email contains) 두 축 검색.
 *
 * <p>RBAC (Q3 결정 A안):
 *
 * <ul>
 *   <li>SYSTEM_ADMIN → 전역
 *   <li>CENTER_ADMIN → 자기 센터 프로그램 + 자기 센터 소속 사용자
 * </ul>
 *
 * <p>Q2 결정: 공지 제외. Q5 결정: 센터 도메인 미포함.
 *
 * <p>q null/blank → 빈 결과. q 길이 100자 제한 (앞에서 자름 — SearchService 와 동일 UX).
 */
@Service
@RequiredArgsConstructor
public class AdminSearchService {

  private static final int PROGRAM_LIMIT = 4;
  private static final int USER_LIMIT = 3;
  private static final int MAX_QUERY_LENGTH = 100;

  private final ProgramRepository programRepository;
  private final UserRepository userRepository;
  private final AdminScope adminScope;

  @Transactional(readOnly = true)
  public AdminSearchResult search(String rawQuery) {
    if (rawQuery == null || rawQuery.isBlank()) {
      return new AdminSearchResult(Collections.emptyList(), Collections.emptyList());
    }
    String q = rawQuery.trim();
    if (q.length() > MAX_QUERY_LENGTH) {
      q = q.substring(0, MAX_QUERY_LENGTH);
    }

    Long scopeCenterId = adminScope.effectiveCenterId();

    List<AdminSearchResult.ProgramHit> programs = searchPrograms(q, scopeCenterId);
    List<AdminSearchResult.UserHit> users = searchUsers(q, scopeCenterId);

    return new AdminSearchResult(programs, users);
  }

  private List<AdminSearchResult.ProgramHit> searchPrograms(String q, Long scopeCenterId) {
    Specification<Program> spec = ProgramSpec.withKeyword(q);
    if (scopeCenterId != null) {
      // CENTER_ADMIN: 자기 센터 FK 격리
      spec = spec.and((root, query, cb) -> cb.equal(root.get("center").get("id"), scopeCenterId));
    }
    return programRepository
        .findAll(spec, PageRequest.of(0, PROGRAM_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt")))
        .getContent()
        .stream()
        .map(
            p ->
                new AdminSearchResult.ProgramHit(
                    p.getId(), p.getTitle(), p.getCenter() != null ? p.getCenter().getName() : ""))
        .toList();
  }

  private List<AdminSearchResult.UserHit> searchUsers(String q, Long scopeCenterId) {
    String pattern = "%" + q.toLowerCase() + "%";
    Specification<User> spec =
        (root, query, cb) -> {
          Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
          Predicate emailLike = cb.like(cb.lower(root.get("email")), pattern);
          Predicate keyword = cb.or(nameLike, emailLike);
          if (scopeCenterId != null) {
            // CENTER_ADMIN: 자기 센터 소속 사용자만 (center FK). center 가 null 인 USER 는 제외.
            return cb.and(
                keyword,
                cb.equal(root.get("center").get("id"), scopeCenterId),
                // 자기 센터 사용자 안에서도 role 은 USER / CENTER_ADMIN 포함, SYSTEM_ADMIN 은 전역 역할이라 제외.
                cb.notEqual(root.get("role"), UserRole.SYSTEM_ADMIN));
          }
          return keyword;
        };
    return userRepository
        .findAll(spec, PageRequest.of(0, USER_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt")))
        .getContent()
        .stream()
        .map(u -> new AdminSearchResult.UserHit(u.getId(), u.getName(), u.getEmail()))
        .toList();
  }
}
