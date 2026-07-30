package io.github.sihyuuun.youthmoa.bookmark;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

  /**
   * wireframe WF-3-002 즐겨찾기 정책 — 사용자당 최대 20개. 초과 시 가장 오래된 즐겨찾기 (createdAt 오름차순 첫 항목) 를 삭제하고 새 즐겨찾기를
   * 추가한다. admin 편집 대상은 아니고 UX 정책 상수이므로 코드 상수로 유지 (`CLAUDE.md` 「하드코딩 OK」 백엔드 상수 규정 대상).
   */
  public static final int MAX_BOOKMARKS_PER_USER = 20;

  private final BookmarkRepository bookmarkRepository;
  private final ProgramRepository programRepository;
  private final UserRepository userRepository;

  /**
   * 즐겨찾기 토글.
   *
   * <ul>
   *   <li>이미 즐겨찾기 → 삭제 후 {@code false} 반환
   *   <li>없으면 → 생성 후 {@code true} 반환
   * </ul>
   */
  @Transactional
  public boolean toggle(String userEmail, Long programId) {
    User user =
        userRepository
            .findByEmail(userEmail)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userEmail));

    Program program =
        programRepository
            .findById(programId)
            .orElseThrow(() -> new IllegalArgumentException("프로그램을 찾을 수 없습니다: " + programId));

    if (bookmarkRepository.existsByUserAndProgram(user, program)) {
      bookmarkRepository.deleteByUserAndProgram(user, program);
      return false;
    }
    // wireframe WF-3-002 정책: 최대 20개. 초과 시 오래된 즐겨찾기부터 자동 삭제.
    // findAllByUserOrderByCreatedAtDesc 는 신규→과거. reverse 하여 과거→신규 순으로 초과분 잘라낸다.
    List<Bookmark> existing = bookmarkRepository.findAllByUserOrderByCreatedAtDesc(user);
    if (existing.size() >= MAX_BOOKMARKS_PER_USER) {
      int overflow = existing.size() - MAX_BOOKMARKS_PER_USER + 1;
      List<Bookmark> oldest = existing.subList(existing.size() - overflow, existing.size());
      bookmarkRepository.deleteAll(oldest);
    }
    bookmarkRepository.save(Bookmark.builder().user(user).program(program).build());
    return true;
  }

  /** 특정 사용자가 특정 프로그램을 즐겨찾기했는지 여부. (비인증 사용자는 항상 false) */
  public boolean isBookmarked(String userEmail, Long programId) {
    if (userEmail == null) return false;
    return userRepository
        .findByEmail(userEmail)
        .flatMap(
            u ->
                programRepository
                    .findById(programId)
                    .map(p -> bookmarkRepository.existsByUserAndProgram(u, p)))
        .orElse(false);
  }

  /**
   * 사용자가 즐겨찾기한 프로그램 ID Set. 카드 N개 그릴 때 단일 쿼리로 가져와 템플릿에서 {@code bookmarkedIds.contains(p.id)} 로 분기.
   * 비인증 / 존재하지 않는 사용자는 빈 Set.
   */
  public Set<Long> getBookmarkedProgramIds(String userEmail) {
    if (userEmail == null) return Collections.emptySet();
    return userRepository
        .findByEmail(userEmail)
        .map(u -> new HashSet<>(bookmarkRepository.findProgramIdsByUser(u)))
        .orElse(new HashSet<>());
  }
}
