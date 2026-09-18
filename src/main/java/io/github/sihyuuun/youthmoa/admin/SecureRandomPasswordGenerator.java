package io.github.sihyuuun.youthmoa.admin;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * A5-1 admin-staff-management (2026-09-18): 관리자 신규 발급 시 초기 비밀번호 자동 생성기.
 *
 * <p>정책 (QA + Qn-2 A):
 *
 * <ul>
 *   <li>12자 고정 길이
 *   <li>4문자 클래스 각 최소 1자 보장 — 대문자 · 소문자 · 숫자 · 특수문자 (!@#$%^&*)
 *   <li>{@link SecureRandom} 사용 (예측 불가)
 *   <li>사용자 password 정책 (영문+숫자 8자 이상) 상회
 * </ul>
 *
 * <p>알고리즘: 각 클래스에서 1자씩 필수 픽 → 나머지 8자는 전체 pool 에서 무작위 픽 → 셔플 → String.
 */
@Component
public class SecureRandomPasswordGenerator {

  static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
  static final String DIGIT = "0123456789";
  static final String SPECIAL = "!@#$%^&*";
  static final String ALL = UPPER + LOWER + DIGIT + SPECIAL;
  static final int LENGTH = 12;

  private final SecureRandom random = new SecureRandom();

  public String generate() {
    List<Character> picks = new ArrayList<>(LENGTH);
    picks.add(UPPER.charAt(random.nextInt(UPPER.length())));
    picks.add(LOWER.charAt(random.nextInt(LOWER.length())));
    picks.add(DIGIT.charAt(random.nextInt(DIGIT.length())));
    picks.add(SPECIAL.charAt(random.nextInt(SPECIAL.length())));
    for (int i = 4; i < LENGTH; i++) {
      picks.add(ALL.charAt(random.nextInt(ALL.length())));
    }
    Collections.shuffle(picks, random);
    StringBuilder sb = new StringBuilder(LENGTH);
    for (Character c : picks) sb.append(c);
    return sb.toString();
  }
}
