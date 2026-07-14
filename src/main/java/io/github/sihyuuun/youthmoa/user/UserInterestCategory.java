package io.github.sihyuuun.youthmoa.user;

import java.util.List;

/**
 * F-signup-03: WelcomeScreen 관심 분야 7종 하드코딩 상수.
 *
 * <p>spec §A-Q5 결정: Category 엔티티 승격은 admin 트랙에서. 지금은 상수 유지.
 */
public final class UserInterestCategory {

  public static final List<String> ALL =
      List.of("취업·역량", "창업", "심리·건강", "문화·예술", "주거", "금융", "네트워킹");

  private UserInterestCategory() {}
}
