package io.github.sihyuuun.youthmoa.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  /** F0i: 아이디 찾기 — 이름 + 휴대폰(정규화된 숫자) 일치. */
  Optional<User> findByNameAndPhone(String name, String phone);

  /** F0i: 비밀번호 찾기 본인 확인 — 이메일 + 이름 + 휴대폰(정규화된 숫자) 일치. */
  Optional<User> findByEmailAndNameAndPhone(String email, String name, String phone);

  /** A5 admin-users: safeguard — 마지막 SYSTEM_ADMIN 강등/차단 방지용. role 별 개수 카운트. */
  long countByRole(UserRole role);

  /** A5 admin-users: safeguard — 마지막 활성 SYSTEM_ADMIN 강등 방지 (차단된 SYSTEM_ADMIN 도 제외). */
  long countByRoleAndIsActiveTrue(UserRole role);
}
