package io.github.sihyuuun.youthmoa.user;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * F0i: 아이디 / 비밀번호 찾기 도메인 서비스.
 *
 * <ul>
 *   <li>{@link #findEmailByNameAndPhone(String, String)} — 이름 + 휴대폰 → User (있으면)
 *   <li>{@link #verifyForPasswordReset(String, String, String)} — 이메일+이름+휴대폰 → User (본인 확인)
 *   <li>{@link #resetPassword(Long, String)} — 검증된 사용자 id 에 대해 비밀번호 갱신
 * </ul>
 *
 * <p>휴대폰은 저장 시 하이픈 없이 숫자만 저장되므로(SignUpRequest 정책), 입력값도 하이픈을 제거해서 매칭한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindAccountService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public Optional<User> findEmailByNameAndPhone(String name, String phone) {
    return userRepository.findByNameAndPhone(name, normalizePhone(phone));
  }

  public Optional<User> verifyForPasswordReset(String email, String name, String phone) {
    return userRepository.findByEmailAndNameAndPhone(email, name, normalizePhone(phone));
  }

  @Transactional
  public void resetPassword(Long userId, String newPassword) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));
    user.changePassword(passwordEncoder.encode(newPassword));
  }

  private String normalizePhone(String raw) {
    if (raw == null) return "";
    return raw.replaceAll("[^0-9]", "");
  }
}
