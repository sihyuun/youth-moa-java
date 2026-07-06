package io.github.sihyuuun.youthmoa.user;

/**
 * F0i: 아이디 찾기 결과 화면에서 이메일을 부분 마스킹해서 노출한다.
 *
 * <p>규칙 (스펙):
 *
 * <ul>
 *   <li>local part 가 3자 이상이면 앞 3자 + "***" + "@domain"
 *   <li>3자 미만이면 첫 1자 + "***" + "@domain"
 *   <li>도메인은 마스킹하지 않음 (사용자가 자기 계정을 확인할 수 있어야 하기 때문)
 * </ul>
 */
public final class EmailMaskingUtil {

  private EmailMaskingUtil() {}

  public static String mask(String email) {
    if (email == null) return "";
    int at = email.indexOf('@');
    if (at <= 0) return email;
    String local = email.substring(0, at);
    String domain = email.substring(at);
    if (local.length() >= 3) return local.substring(0, 3) + "***" + domain;
    return local.charAt(0) + "***" + domain;
  }
}
