package io.github.sihyuuun.youthmoa.user;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** F-signup-03: WelcomeScreen 관심 정보 저장 폼. */
@Getter
@Setter
@NoArgsConstructor
public class WelcomeForm {

  private Set<String> regions = new HashSet<>();

  private Set<String> categories = new HashSet<>();
}
