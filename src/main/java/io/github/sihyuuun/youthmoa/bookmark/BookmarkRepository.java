package io.github.sihyuuun.youthmoa.bookmark;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

  Optional<Bookmark> findByUserAndProgram(User user, Program program);

  boolean existsByUserAndProgram(User user, Program program);

  Page<Bookmark> findAllByUser(User user, Pageable pageable);

  void deleteByUserAndProgram(User user, Program program);

  /** 특정 사용자가 즐겨찾기한 모든 프로그램 ID. 카드 N개 그릴 때 N+1 회피 */
  @Query("SELECT b.program.id FROM Bookmark b WHERE b.user = :user")
  List<Long> findProgramIdsByUser(User user);
}
