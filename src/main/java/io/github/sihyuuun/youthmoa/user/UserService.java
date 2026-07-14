package io.github.sihyuuun.youthmoa.user;

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
            .build();
    userRepository.save(user);
  }
}
