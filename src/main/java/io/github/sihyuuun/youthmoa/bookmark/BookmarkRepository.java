package io.github.sihyuuun.youthmoa.bookmark;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByUserAndProgram(User user, Program program);

    boolean existsByUserAndProgram(User user, Program program);

    Page<Bookmark> findAllByUser(User user, Pageable pageable);

    void deleteByUserAndProgram(User user, Program program);
}
