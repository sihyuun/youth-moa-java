package io.github.sihyuuun.youthmoa.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  /** F0i: 아이디 찾기 — 이름 + 휴대폰(정규화된 숫자) 일치. */
  Optional<User> findByNameAndPhone(String name, String phone);

  /** F0i: 비밀번호 찾기 본인 확인 — 이메일 + 이름 + 휴대폰(정규화된 숫자) 일치. */
  Optional<User> findByEmailAndNameAndPhone(String email, String name, String phone);
}
