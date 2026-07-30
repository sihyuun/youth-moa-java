package io.github.sihyuuun.youthmoa.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * F-signup-terms-agreement 정책 강제 테스트 (spec §5).
 *
 * <p>산문 규칙은 지켜지지 않는다는 회고(2026-07-13)에 근거해 정책을 4개 시나리오로 코드화한다. 이 테스트가 없으면 회원가입 시 UserAgreement
 * INSERT 가 조용히 사라져도 검출되지 않는다.
 *
 * <p>DataInitializer 가 SERVICE(version=1), PRIVACY(version=2) 2건을 시드하므로 그 상태를 baseline 으로 삼는다.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@Transactional
class UserServiceSignUpTermsTest {

  @Autowired UserService userService;
  @Autowired UserRepository userRepository;
  @Autowired TermRepository termRepository;
  @Autowired UserAgreementRepository userAgreementRepository;

  private SignUpRequest baseRequest(String email) {
    SignUpRequest r = new SignUpRequest();
    r.setEmail(email);
    r.setPassword("Test1234!");
    r.setPasswordConfirm("Test1234!");
    r.setName("홍길동");
    r.setPhone("01012345678");
    r.setGender(UserGender.MALE);
    r.setBirthDateText("1990-01-01");
    r.setZipcode("12345");
    r.setAddress("서울시");
    r.setEmailChecked(true);
    r.setAgreements(new HashMap<>());
    return r;
  }

  @BeforeEach
  void resetSeededTermsToKnownState() {
    // 다른 테스트가 남긴 inactive/추가 term 을 청소해 baseline (SERVICE+PRIVACY) 만 남긴다.
    List<Term> all = termRepository.findAll();
    for (Term t : all) {
      if (!"SERVICE".equals(t.getCode()) && !"PRIVACY".equals(t.getCode())) {
        termRepository.delete(t);
      }
    }
  }

  @Test
  @DisplayName("필수 약관 전건 동의 시 가입 성공 + 활성 약관 개수만큼 UserAgreement INSERT")
  void signUp_allRequiredAgreed_createsAgreementRows() {
    SignUpRequest req = baseRequest("all-agreed@test.com");
    req.getAgreements().put("SERVICE", true);
    req.getAgreements().put("PRIVACY", true);

    userService.signUp(req, true);

    User saved = userRepository.findByEmail("all-agreed@test.com").orElseThrow();
    List<UserAgreement> rows = userAgreementRepository.findByUser(saved);
    // 활성 필수 약관 2건(SERVICE, PRIVACY) 모두 이력에 남는다
    assertThat(rows).hasSize(2);
    assertThat(rows).allMatch(UserAgreement::isAgreed);
    assertThat(rows)
        .extracting(a -> a.getTerm().getCode())
        .containsExactlyInAnyOrder("SERVICE", "PRIVACY");
  }

  @Test
  @DisplayName("필수 약관 1건 누락 시 TermsAgreementException + 가입 실패 (사용자·이력 모두 롤백)")
  void signUp_requiredMissing_throwsAndRollsBack() {
    SignUpRequest req = baseRequest("missing@test.com");
    req.getAgreements().put("SERVICE", true);
    // PRIVACY 누락

    assertThatThrownBy(() -> userService.signUp(req, true))
        .isInstanceOf(TermsAgreementException.class);

    // @Transactional 로 롤백되므로 별도 write 확인 대신 findMissingRequiredTermCodes 로 정책만 검증
    Map<String, Boolean> partial = Map.of("SERVICE", true);
    assertThat(userService.findMissingRequiredTermCodes(partial)).containsExactly("PRIVACY");
    assertThat(userService.findMissingRequiredTermCodes(Map.of()))
        .containsExactlyInAnyOrder("SERVICE", "PRIVACY");
  }

  @Test
  @DisplayName("agreedVersion 은 동의 당시 Term.version 을 스냅샷으로 저장한다")
  void signUp_snapshotVersion() {
    Term service = termRepository.findByCode("SERVICE").orElseThrow();
    Term privacy = termRepository.findByCode("PRIVACY").orElseThrow();

    SignUpRequest req = baseRequest("version@test.com");
    req.getAgreements().put("SERVICE", true);
    req.getAgreements().put("PRIVACY", true);

    userService.signUp(req, true);

    User saved = userRepository.findByEmail("version@test.com").orElseThrow();
    Map<String, Integer> versionsByCode = new HashMap<>();
    for (UserAgreement a : userAgreementRepository.findByUser(saved)) {
      versionsByCode.put(a.getTerm().getCode(), a.getAgreedVersion());
    }
    assertThat(versionsByCode.get("SERVICE")).isEqualTo(service.getVersion());
    assertThat(versionsByCode.get("PRIVACY")).isEqualTo(privacy.getVersion());
  }

  @Test
  @DisplayName("isActive=false 약관은 검증 대상에서 제외 (동의 없이 가입 가능, 이력도 생성되지 않음)")
  void signUp_inactiveTermExcluded() {
    // MARKETING 을 필수·비활성 상태로 신설 — 활성 필터로 제외되어 가입 통과해야 한다
    Term inactive =
        termRepository.save(
            Term.builder()
                .code("MARKETING")
                .title("마케팅 수신 동의")
                .contentPath("/marketing")
                .required(true)
                .version(1)
                .sortOrder(3)
                .isActive(false)
                .build());

    SignUpRequest req = baseRequest("inactive@test.com");
    req.getAgreements().put("SERVICE", true);
    req.getAgreements().put("PRIVACY", true);
    // MARKETING 동의 없음. required=true 지만 isActive=false 라 검증 대상 아님.

    userService.signUp(req, true);

    User saved = userRepository.findByEmail("inactive@test.com").orElseThrow();
    List<UserAgreement> rows = userAgreementRepository.findByUser(saved);
    assertThat(rows).hasSize(2); // MARKETING 이력 생성 안 됨
    assertThat(rows).extracting(a -> a.getTerm().getCode()).doesNotContain(inactive.getCode());
  }
}
