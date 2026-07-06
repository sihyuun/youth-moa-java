package io.github.sihyuuun.youthmoa.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailMaskingUtilTest {

  @Test
  void 앞_3자_이상이면_3자_노출한다() {
    assertThat(EmailMaskingUtil.mask("abcdef@youth-moa.test")).isEqualTo("abc***@youth-moa.test");
  }

  @Test
  void local_3자_미만이면_1자_노출한다() {
    assertThat(EmailMaskingUtil.mask("ab@youth-moa.test")).isEqualTo("a***@youth-moa.test");
  }

  @Test
  void local_1자면_1자_그대로_노출한다() {
    assertThat(EmailMaskingUtil.mask("a@youth-moa.test")).isEqualTo("a***@youth-moa.test");
  }

  @Test
  void null_이면_빈문자열을_반환한다() {
    assertThat(EmailMaskingUtil.mask(null)).isEqualTo("");
  }

  @Test
  void 골뱅이_없으면_원본을_반환한다() {
    assertThat(EmailMaskingUtil.mask("invalid")).isEqualTo("invalid");
  }
}
