package io.github.sihyuuun.youthmoa.notification.watch;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A7-watcher-ui (2026-09-28) — ProgramWatch 도메인 서비스.
 *
 * <p>BookmarkService 패턴 준용:
 *
 * <ul>
 *   <li>{@link #toggle} — 등록/해제. 20개 상한 (Q-A7W-8) — 초과 시 오래된 항목 자동 삭제 후 신규 insert.
 *   <li>{@link #isWatched} — 편집 폼/목록 렌더 시 초기 상태 확인
 *   <li>{@link #getWatchedProgramIds} — 목록 batch 조회 (N+1 방지)
 *   <li>{@link #findWatchedPrograms} — 대시보드 카드 최근 5개
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramWatchService {

  /**
   * Q-A7W-8 A: admin 당 최대 20개. 초과 시 가장 오래된 watch (createdAt 오름차순 첫 항목) 자동 삭제 후 신규 insert. Bookmark
   * 패턴과 동일 상수를 사용하지 않고 별도 상수로 유지 — Watcher 정책이 후속 티켓에서 조정될 여지 확보.
   */
  public static final int MAX_WATCHERS_PER_ADMIN = 20;

  private final ProgramWatchRepository programWatchRepository;
  private final ProgramRepository programRepository;
  private final UserRepository userRepository;

  /**
   * Watch 토글.
   *
   * <ul>
   *   <li>이미 등록 → 삭제 후 {@code false} 반환
   *   <li>없으면 → 생성 후 {@code true} 반환. 20개 상한 초과 시 오래된 항목부터 삭제
   * </ul>
   *
   * <p>ProgramWatchController 는 admin email 을 principal 로 넘기며 여기서 실 User 를 조회. 조회 실패 시
   * IllegalArgumentException (Controller 400 매핑).
   */
  @Transactional
  public boolean toggle(String adminEmail, Long programId) {
    User admin =
        userRepository
            .findByEmail(adminEmail)
            .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없어요: " + adminEmail));

    Program program =
        programRepository
            .findById(programId)
            .orElseThrow(() -> new IllegalArgumentException("프로그램을 찾을 수 없어요: " + programId));

    if (programWatchRepository.existsByAdminAndProgram(admin, program)) {
      programWatchRepository.deleteByAdminAndProgram(admin, program);
      return false;
    }
    // Q-A7W-8 A: 20개 상한. findAllByAdminOrderByCreatedAtDesc 는 신규→과거 → reverse tail slice.
    List<ProgramWatch> existing = programWatchRepository.findAllByAdminOrderByCreatedAtDesc(admin);
    if (existing.size() >= MAX_WATCHERS_PER_ADMIN) {
      int overflow = existing.size() - MAX_WATCHERS_PER_ADMIN + 1;
      List<ProgramWatch> oldest = existing.subList(existing.size() - overflow, existing.size());
      programWatchRepository.deleteAll(oldest);
    }
    programWatchRepository.save(ProgramWatch.builder().admin(admin).program(program).build());
    return true;
  }

  /** 편집 폼 헤더 · 상세 화면 초기 상태. 미인증/미존재는 false. */
  public boolean isWatched(String adminEmail, Long programId) {
    if (adminEmail == null) return false;
    return userRepository
        .findByEmail(adminEmail)
        .flatMap(
            u ->
                programRepository
                    .findById(programId)
                    .map(p -> programWatchRepository.existsByAdminAndProgram(u, p)))
        .orElse(false);
  }

  /** 목록 렌더 시 각 row 눈 아이콘 활성 여부 batch 판단용. 비인증/미존재는 빈 Set. */
  public Set<Long> getWatchedProgramIds(String adminEmail) {
    if (adminEmail == null) return Collections.emptySet();
    return userRepository
        .findByEmail(adminEmail)
        .map(u -> new HashSet<>(programWatchRepository.findProgramIdsByAdmin(u)))
        .orElse(new HashSet<>());
  }

  /**
   * 대시보드 "지켜보는 프로그램" 카드 (Q-A7W-3 b). 최근순. Page 반환은 후속 "전체 보기" 페이지 공유 대비.
   *
   * <p>주의: {@code adminEmail} 이 null 또는 존재하지 않는 사용자면 빈 Page. 이때는 카드 자체를 empty state 로 렌더.
   */
  public Page<ProgramWatch> findWatchedPrograms(String adminEmail, Pageable pageable) {
    if (adminEmail == null) return Page.empty(pageable);
    return userRepository
        .findByEmail(adminEmail)
        .map(u -> programWatchRepository.findAllByAdmin(u, pageable))
        .orElse(Page.empty(pageable));
  }
}
