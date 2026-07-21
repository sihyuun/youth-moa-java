package io.github.sihyuuun.youthmoa.user;

import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.bookmark.BookmarkRepository;
import io.github.sihyuuun.youthmoa.notification.NotificationRepository;
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

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

    return new UserPrincipal(user);
  }

  /** D5: 마이페이지 프로필 수정. 세션 재확인 통과 후 호출. */
  @Transactional
  public void updateProfile(String email, ProfileUpdateRequest request) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
    user.updateProfile(
        request.getName(),
        request.getPhone(),
        request.getZipcode(),
        request.getAddress(),
        request.getAddressDetail(),
        request.getBirthDate(),
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
    boolean remindD1 = request.getRemindD1() != null ? request.getRemindD1() : user.isNotifyRemindD1();
    boolean waitlistEmpty =
        request.getWaitlistEmpty() != null ? request.getWaitlistEmpty() : user.isNotifyWaitlistEmpty();
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
   * F-signup-01: phoneVerified 는 UserController 가 세션을 재확인한 뒤에만 true 로 넘긴다. hidden field 값은 절대
   * 신뢰하지 않는다.
   */
  @Transactional
  public void signUp(SignUpRequest request, boolean phoneVerified) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
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
  }
}
