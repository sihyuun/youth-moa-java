package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.center.Center;
import io.github.sihyuuun.youthmoa.center.CenterRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserGender;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A5 admin-users (2026-09-15 · Qn 결정 확정): 관리자 사용자 관리 서비스.
 *
 * <ul>
 *   <li>Qn-A 이월 — `/admin/staff` 별도 화면 X. role filter 4옵션(all·SYSTEM_ADMIN·CENTER_ADMIN·USER) 로 통합.
 *   <li>Qn-B 페이지 — 사용자 상세는 별도 페이지.
 *   <li>Qn-C 유지 — 차단 시 신청 이력 유지. UserService.withdraw (하드 삭제) 와 분리.
 *   <li>Qn-3 필수 — 차단 사유 필수.
 *   <li>Qn-5 10건 — 페이지당 10건 (admin 일관).
 *   <li>Safeguard 3종: (1) 자기 자신 X (2) 마지막 SYSTEM_ADMIN X (3) SYSTEM_ADMIN 승격은 SYSTEM_ADMIN 만.
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

  /** Qn-5 A: 페이지당 10건. */
  public static final int PAGE_SIZE = 10;

  private final UserRepository userRepository;
  private final ApplicationRepository applicationRepository;
  private final AdminUserSafeguard safeguard;
  private final CenterRepository centerRepository;
  private final PasswordEncoder passwordEncoder;
  private final SecureRandomPasswordGenerator passwordGenerator;

  // ================= 조회 =================

  /**
   * 사용자 목록 조회 (검색·role filter·페이지네이션).
   *
   * @param q 이름 or 이메일 LIKE (nullable/blank 이면 미적용)
   * @param role role 필터 (nullable/blank/ALL 이면 미적용)
   * @param page 0-based 페이지 인덱스
   */
  public Page<User> list(String q, String role, int page) {
    Pageable pageable =
        PageRequest.of(Math.max(0, page), PAGE_SIZE, Sort.by(Sort.Order.desc("createdAt")));
    Specification<User> spec = (root, query, cb) -> cb.conjunction();

    if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role)) {
      try {
        UserRole r = UserRole.valueOf(role.toUpperCase());
        spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), r));
      } catch (IllegalArgumentException ignore) {
        // 잘못된 값이면 필터 미적용
      }
    }
    if (q != null && !q.isBlank()) {
      String pattern = "%" + q.trim().toLowerCase() + "%";
      spec =
          spec.and(
              (root, query, cb) -> {
                Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
                Predicate emailLike = cb.like(cb.lower(root.get("email")), pattern);
                return cb.or(nameLike, emailLike);
              });
    }
    return userRepository.findAll(spec, pageable);
  }

  /** 상세 조회. */
  public User findById(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없어요: " + userId));
  }

  /**
   * 상세 화면 우측 "프로그램 신청 현황" 탭용. status 가 null/blank 이면 전체. 그 외 APPROVED/REJECTED/CANCELLED/PENDING
   * 필터.
   */
  public List<Application> findApplicationsByUser(User user, String status) {
    List<Application> all = applicationRepository.findAllByUserOrderByAppliedAtDesc(user);
    if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) return all;
    try {
      ApplicationStatus s = ApplicationStatus.valueOf(status.toUpperCase());
      return all.stream().filter(a -> a.getStatus() == s).toList();
    } catch (IllegalArgumentException e) {
      return all;
    }
  }

  // ================= 변경 =================

  /**
   * 차단. Qn-3 A: 사유 필수. Safeguard 는 A8 (2026-09-17) 부터 {@link AdminUserSafeguard} 로 추출됨 — 개별·bulk
   * endpoint 재사용.
   */
  @Transactional
  public void deactivate(Long userId, User currentAdmin, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("차단 사유를 입력해주세요.");
    }
    User target = findById(userId);
    safeguard.assertCanDeactivate(currentAdmin, target);
    if (!target.isActive()) return; // idempotent
    target.deactivate(currentAdmin, reason);
  }

  /** 재활성화. Safeguard: 자기 자신 X. */
  @Transactional
  public void reactivate(Long userId, User currentAdmin) {
    User target = findById(userId);
    safeguard.assertCanReactivate(currentAdmin, target);
    if (target.isActive()) return; // idempotent
    target.reactivate();
  }

  /**
   * role 변경 (SYSTEM_ADMIN 전용 — 컨트롤러의 @PreAuthorize 로 이중 방어). Safeguard: (1) 자기 자신 X (2) 마지막
   * SYSTEM_ADMIN 강등 X (3) SYSTEM_ADMIN 승격은 SYSTEM_ADMIN 만 (컨트롤러 @PreAuthorize 로 확보).
   */
  @Transactional
  public void changeRole(Long userId, User currentAdmin, UserRole newRole) {
    User target = findById(userId);
    if (target.getRole() == newRole) return; // idempotent (사전 검증 전 짧게 컷)
    safeguard.assertCanChangeRole(currentAdmin, target, newRole);
    target.promoteTo(newRole);
  }

  /** 관리자 메모 저장 (1000자 제한). null/blank 는 clear 로 처리. */
  @Transactional
  public void updateAdminNote(Long userId, String note) {
    if (note != null && note.length() > 1000) {
      throw new IllegalArgumentException("관리자 메모는 1000자 이하로 입력해주세요.");
    }
    User target = findById(userId);
    target.updateAdminNote(note);
  }

  // ================= A5-1 admin-staff-management (2026-09-18) =================

  /**
   * SYSTEM_ADMIN 이 신규 계정을 발급한다.
   *
   * <ul>
   *   <li>role=USER 도 발급 가능 (prototype L1996~2005 정합) — 관리자 발급 UI 이지만 옵션 B 편입으로 사용자 계정도 함께 발급.
   *   <li>role=CENTER_ADMIN 이면 center 필수. 그 외 role 은 center=null.
   *   <li>초기 password 는 {@link SecureRandomPasswordGenerator} 로 자동 생성. bcrypt 해시 저장.
   *   <li>mustChangePassword=TRUE + invitedBy=admin.
   *   <li>이메일 중복 시 400 (시드 계정 email 포함).
   *   <li>Safeguard: {@link AdminUserSafeguard#assertCanCreateStaff(User, UserRole)}.
   * </ul>
   *
   * @return {@link CreatedStaff} — 생성 유저 + plain-text 초기 password (flash 1회 노출용)
   */
  @Transactional
  public CreatedStaff createStaff(
      User admin, String email, String name, UserGender gender, UserRole role, Long centerId) {
    safeguard.assertCanCreateStaff(admin, role);
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("이메일을 입력해주세요.");
    }
    String normalizedEmail = email.trim().toLowerCase();
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("이름을 입력해주세요.");
    }
    if (userRepository.existsByEmail(normalizedEmail)) {
      throw new IllegalArgumentException("이미 사용 중인 이메일이에요.");
    }
    Center center = null;
    if (role == UserRole.CENTER_ADMIN) {
      if (centerId == null) {
        throw new IllegalArgumentException("소속 센터를 선택해주세요.");
      }
      center =
          centerRepository
              .findById(centerId)
              .orElseThrow(() -> new IllegalArgumentException("소속 센터를 찾을 수 없어요."));
    }
    String plainPassword = passwordGenerator.generate();
    String encoded = passwordEncoder.encode(plainPassword);

    User newUser =
        User.builder()
            .email(normalizedEmail)
            .password(encoded)
            .name(name.trim())
            .gender(gender)
            .role(role)
            .center(center)
            .build();
    newUser.assignInitialPassword(encoded, admin);
    User saved = userRepository.save(newUser);
    return new CreatedStaff(saved, plainPassword);
  }

  /**
   * SYSTEM_ADMIN 이 임시 password 를 재발급한다. mustChangePassword=TRUE 재설정. 자기 자신 리셋 금지 (Qn-5 A).
   *
   * @return plain-text 새 password (flash 1회 노출용)
   */
  @Transactional
  public String resetPassword(Long userId, User admin) {
    User target = findById(userId);
    safeguard.assertCanResetPassword(admin, target);
    String plainPassword = passwordGenerator.generate();
    String encoded = passwordEncoder.encode(plainPassword);
    target.resetPasswordByAdmin(encoded);
    return plainPassword;
  }

  /** 신규 발급 결과 · flash 로 초기 password 1회 노출용. */
  public record CreatedStaff(User user, String plainPassword) {}

  // ================= 헬퍼 =================

  /** role 옵션 리스트 (목록 필터 dropdown 및 상세 role radio). */
  public List<UserRole> roleOptions() {
    List<UserRole> list = new ArrayList<>();
    list.add(UserRole.SYSTEM_ADMIN);
    list.add(UserRole.CENTER_ADMIN);
    list.add(UserRole.USER);
    return list;
  }

  /** role 한글 라벨. */
  public static String roleLabel(UserRole role) {
    if (role == null) return "";
    return switch (role) {
      case USER -> "사용자";
      case ADMIN, CENTER_ADMIN -> "관리자";
      case SYSTEM_ADMIN -> "시스템 관리자";
    };
  }

  public static String applicationStatusLabel(ApplicationStatus s) {
    if (s == null) return "";
    return switch (s) {
      case PENDING -> "대기";
      case APPROVED -> "승인";
      case REJECTED -> "반려";
      case CANCELLED -> "취소";
    };
  }
}
