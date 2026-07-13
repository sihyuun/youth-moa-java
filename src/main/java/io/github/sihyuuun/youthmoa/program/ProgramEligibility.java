package io.github.sihyuuun.youthmoa.program;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로그램 자격요건 값 객체 (HANDOFF §5-E.8).
 *
 * <p>연령 / 거주지 / 기타 3필드로 구성. Program 에 @Embedded 로 편입되어 별도 테이블 없이 program 테이블에 `eligibility_age`,
 * `eligibility_region`, `eligibility_etc` 컬럼으로 저장된다.
 *
 * <p>@Lob 사용 금지 — open-in-view:false 환경에서의 PG LOB streaming 사고 회피 (F4 spec 명시).
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProgramEligibility {

  @Column(name = "eligibility_age", length = 100)
  private String age;

  @Column(name = "eligibility_region", length = 100)
  private String region;

  @Column(name = "eligibility_etc", length = 200)
  private String etc;
}
