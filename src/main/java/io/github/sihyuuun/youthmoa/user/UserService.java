package io.github.sihyuuun.youthmoa.user;

import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.bookmark.BookmarkRepository;
import io.github.sihyuuun.youthmoa.notification.NotificationRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final ApplicationRepository applicationRepository;
  private final BookmarkRepository bookmarkRepository;
  private final NotificationRepository notificationRepository;
  private final TermRepository termRepository;
  private final UserAgreementRepository userAgreementRepository;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

    return new UserPrincipal(user);
  }

  /**
   * D5: 마이페이지 프로필 수정. 세션 재확인 통과 후 호출.
   *
   * <p>2026-07-31 (fix/password-change-inline): 새 비밀번호가 제공된 경우 회원가입 정책과 동일한 검증 (8자 이상, 영문+숫자 포함,
   * confirm 일치) 을 수행한 뒤 encode + save. 빈 값이면 변경 스킵. wireframe WF-3-003-02 정합.
   */
  @Transactional
  public void updateProfile(String email, ProfileUpdateRequest request) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

    // 비밀번호 변경 요청이 있으면 정책 검증 후 encode. 빈 값이면 스킵.
    if (request.getPassword() != null && !request.getPassword().isBlank()) {
      String pw = request.getPassword();
      if (pw.length() < 8) {
        throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
      }
      if (!pw.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
        throw new IllegalArgumentException("비밀번호는 영문과 숫자를 모두 포함해야 합니다.");
      }
      if (!pw.equals(request.getPasswordConfirm())) {
        throw new IllegalArgumentException("입력한 비밀번호와 일치하지 않습니다.");
      }
      user.changePassword(passwordEncoder.encode(pw));
    }

    user.updateProfile(
        request.getName(),
        request.getPhone(),
        request.getZipcode(),
        request.getAddress(),
        request.getAddressDetail(),
        request.getBirthDate(),
        request.getGender(),
        request.getInterestRegions(),
        request.getInterestCategories());
  }

  /** F-signup-03: WelcomeScreen 에서 관심 지역/분야 저장. */
  @Transactional
  public void updateInterests(String email, Set<String> regions, Set<String> categories) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
    user.updateInterests(regions, categories);
  }

  /** D5: 알림 수신 채널(카카오/SMS/이메일) 갱신. */
  @Transactional
  public void updateNotificationChannels(String email, NotificationChannelRequest request) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
    user.updateNotificationChannels(request.isKakao(), request.isSms(), request.isEmail());
    // 항목 3필드는 Boolean(nullable). null 이면 기존값 유지 (부분 저장 클라이언트 방어).
    boolean remindD1 =
        request.getRemindD1() != null ? request.getRemindD1() : user.isNotifyRemindD1();
    boolean waitlistEmpty =
        request.getWaitlistEmpty() != null
            ? request.getWaitlistEmpty()
            : user.isNotifyWaitlistEmpty();
    boolean newProgramNews =
        request.getNewProgramNews() != null
            ? request.getNewProgramNews()
            : user.isNotifyNewProgramNews();
    user.updateNotificationItems(remindD1, waitlistEmpty, newProgramNews);
  }

  /**
   * F0f-fix-4: 회원 탈퇴. 관련 하위 데이터(Application/Bookmark/Notification) 를 먼저 삭제한 뒤 User 를 삭제한다.
   *
   * <p>모든 하위 엔티티는 User 를 {@code @ManyToOne} 으로 참조하므로 FK 제약 회피 목적으로 순서 유지 필수.
   */
  @Transactional
  public void withdraw(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
    // 하위 엔티티 삭제 (FK cascade 없음 — 명시 삭제)
    applicationRepository.deleteAll(applicationRepository.findAllByUserOrderByAppliedAtDesc(user));
    bookmarkRepository.deleteAll(bookmarkRepository.findAllByUserOrderByCreatedAtDesc(user));
    // Notification 은 페이징만 있어 전량 조회 후 삭제
    notificationRepository.deleteAll(
        notificationRepository.findAllByUserOrderByCreatedAtDesc(
            user, org.springframework.data.domain.Pageable.unpaged()));
    // F-signup-terms-agreement: user_agreements FK 회피
    userAgreementRepository.deleteAllByUser(user);
    userRepository.delete(user);
  }

  /** D5: Step1 비밀번호 재확인. 일치하면 true. */
  public boolean verifyPassword(String email, String rawPassword) {
    return userRepository
        .findByEmail(email)
        .map(u -> passwordEncoder.matches(rawPassword, u.getPassword()))
        .orElse(false);
  }

  @Transactional
  public void signUp(SignUpRequest request) {
    signUp(request, false);
  }

  /**
   * F-signup-01: phoneVerified 는 UserController 가 세션을 재확인한 뒤에만 true 로 넘긴다. hidden field 값은 절대 신뢰하지
   * 않는다.
   *
   * <p>F-signup-terms-agreement: 활성 약관 목록을 조회해 (1) 필수 약관 전건 동의 검증 (누락 시 TermsAgreementException)
   * (2) 활성 약관 전건에 대해 UserAgreement 이력 INSERT. 컨트롤러에서 사전 검증했더라도 방어적으로 재검증한다.
   */
  @Transactional
  public void signUp(SignUpRequest request, boolean phoneVerified) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
    }

    List<Term> activeTerms = termRepository.findByIsActiveTrueOrderBySortOrderAsc();
    Map<String, Boolean> agreements =
        request.getAgreements() != null ? request.getAgreements() : Collections.emptyMap();
    List<String> missing = findMissingRequiredTermCodes(activeTerms, agreements);
    if (!missing.isEmpty()) {
      throw new TermsAgreementException(missing);
    }

    User user =
        User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .name(request.getName())
            .phone(request.getPhone())
            .gender(request.getGender())
            .birthDate(request.parseBirthDate())
            .zipcode(request.getZipcode())
            .address(request.getAddress())
            .addressDetail(request.getAddressDetail())
            .role(UserRole.USER)
            .phoneVerified(phoneVerified)
            .build();
    userRepository.save(user);

    LocalDateTime now = LocalDateTime.now();
    for (Term term : activeTerms) {
      boolean agreed = Boolean.TRUE.equals(agreements.get(term.getCode()));
      userAgreementRepository.save(
          UserAgreement.builder()
              .user(user)
              .term(term)
              .agreedVersion(term.getVersion())
              .agreed(agreed)
              .agreedAt(now)
              .build());
    }
  }

  /**
   * F-signup-terms-agreement: 컨트롤러 사전 검증용 — 활성 약관 중 필수인데 동의 누락된 code 목록을 반환.
   *
   * <p>bindingResult 다른 위반과 함께 한 번에 노출하기 위해 서비스가 아닌 컨트롤러가 먼저 호출한다.
   */
  public List<String> findMissingRequiredTermCodes(Map<String, Boolean> agreements) {
    return findMissingRequiredTermCodes(
        termRepository.findByIsActiveTrueOrderBySortOrderAsc(),
        agreements != null ? agreements : Collections.emptyMap());
  }

  private List<String> findMissingRequiredTermCodes(
      List<Term> activeTerms, Map<String, Boolean> agreements) {
    return activeTerms.stream()
        .filter(Term::isRequired)
        .filter(t -> !Boolean.TRUE.equals(agreements.get(t.getCode())))
        .map(Term::getCode)
        .toList();
  }
}
