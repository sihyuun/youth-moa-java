package io.github.sihyuuun.youthmoa.user;

import java.util.List;
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

  /**
   * A7 admin 헤더 알림 벨 (2026-09-17): 특정 role 의 활성 사용자 목록. NEW_USER 는 SYSTEM_ADMIN 만 수신하므로 이 쿼리로 대상
   * 결정. isActive=false 관리자는 로그인 자체가 차단되므로 알림 fan-out 대상에서도 제외.
   */
  List<User> findByRoleAndIsActiveTrue(UserRole role);

  /**
   * A9-b (2026-09-22): role + Center FK id 매칭. Program.center FK 로부터 얻은 centerId 로 NEW_APPLICATION
   * 알림 수신자 (CENTER_ADMIN) 조회. 이전의 Center_Name 문자열 매칭은 A9-b 에서 삭제됨 (Q-A9-b 채택안 A).
   *
   * <p>SYSTEM_ADMIN 은 스코프 상 모든 신청을 볼 수 있으나 QC B-1 결정에 따라 NEW_APPLICATION 알림에서는 제외. 대시보드(A6)로 커버.
   */
  List<User> findByRoleAndIsActiveTrueAndCenter_Id(UserRole role, Long centerId);
}
