package io.github.sihyuuun.youthmoa.user;

public enum UserRole {
  USER,

  /**
   * Q9 결정 (ADMIN-00 P0-2): 기존 ADMIN 값은 A5 사용자 관리 착수 시 DB row 마이그레이션 후 제거 예정. 신규 관리자 계정은 {@link
   * #CENTER_ADMIN} / {@link #SYSTEM_ADMIN} 을 사용한다.
   */
  @Deprecated
  ADMIN,

  CENTER_ADMIN,
  SYSTEM_ADMIN
}
