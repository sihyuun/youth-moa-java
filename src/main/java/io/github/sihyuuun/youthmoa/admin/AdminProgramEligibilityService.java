package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramEligibility;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * F4-admin-eligibility (2026-09-09): 관리자 프로그램 자격요건 CRUD 서비스.
 *
 * <p>RBAC (Qn-1 A): SYSTEM_ADMIN 만. 컨트롤러 클래스 레벨 {@code @PreAuthorize} 로 게이트 → 서비스 재검증 없음.
 *
 * <p>정책:
 *
 * <ul>
 *   <li>Qn-2 A — 3필드 모두 공란이면 embedded 값을 null 처리 = 삭제로 취급 (별도 delete endpoint 불필요)
 *   <li>Qn-4 A — 엔티티 컬럼 length 승계 (age/region 100 · etc 200)
 *   <li>Qn-7 A — 검증 실패 메시지는 "…해주세요" 톤
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProgramEligibilityService {

  private static final int AGE_MAX = 100;
  private static final int REGION_MAX = 100;
  private static final int ETC_MAX = 200;

  private final ProgramRepository programRepository;

  @Transactional(readOnly = true)
  public Program findProgram(Long programId) {
    return programRepository
        .findById(programId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로그램이에요: " + programId));
  }

  /**
   * 자격요건 3필드 갱신. 3필드 모두 공란이면 Qn-2 A 에 따라 embedded 값을 null 로 저장 (삭제 취급).
   *
   * <p>Program.update(...) 는 프로그램 전체 필드를 요구하므로, 여기서는 자격요건만 교체하는 도메인 메서드가 없으면 기존 필드를 그대로 유지하며
   * eligibility 만 새로 세팅한다.
   */
  @Transactional
  public void update(Long programId, String rawAge, String rawRegion, String rawEtc) {
    Program program = findProgram(programId);
    String age = normalize(rawAge);
    String region = normalize(rawRegion);
    String etc = normalize(rawEtc);
    validate("연령", age, AGE_MAX);
    validate("거주지", region, REGION_MAX);
    validate("기타 조건", etc, ETC_MAX);

    ProgramEligibility next;
    if (age == null && region == null && etc == null) {
      // Qn-2 A: 3필드 공란 = 삭제
      next = null;
    } else {
      next = ProgramEligibility.builder().age(age).region(region).etc(etc).build();
    }
    program.update(
        program.getTitle(),
        program.getOrganization(),
        program.getCategory(),
        program.getRegion(),
        program.getImageUrl(),
        program.getContent(),
        next,
        program.getStartDate(),
        program.getEndDate(),
        program.getApplyUrl(),
        program.getCapacity());
    log.info(
        "[admin-program-eligibility] updated programId={} age={} region={} etc={}",
        programId,
        age == null ? "-" : "(set)",
        region == null ? "-" : "(set)",
        etc == null ? "-" : "(set)");
  }

  private String normalize(String raw) {
    if (raw == null) return null;
    String trimmed = raw.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private void validate(String label, String value, int max) {
    if (value == null) return;
    if (value.length() > max) {
      throw new IllegalArgumentException(label + "은(는) " + max + "자 이하로 입력해주세요.");
    }
  }
}
