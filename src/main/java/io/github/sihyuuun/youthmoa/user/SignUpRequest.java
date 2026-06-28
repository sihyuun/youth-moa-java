package io.github.sihyuuun.youthmoa.user;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 회원가입 폼 DTO. HANDOFF 5.7 명세 기준.
 * <ul>
 *   <li>계정 정보: email(아이디) / password / passwordConfirm</li>
 *   <li>개인 정보: name / phone / birthDate / zipcode / address / addressDetail</li>
 *   <li>약관 동의: termsAgreed</li>
 * </ul>
 * 성별(gender) 은 User entity 에 필드 없어 미포함 — 추후 entity 확장 시 추가.
 */
@Getter
@Setter
public class SignUpRequest {

    // ── 계정 정보 ───────────────────────────────────────
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    private String passwordConfirm;

    // ── 개인 정보 ───────────────────────────────────────
    @NotBlank(message = "이름을 입력해주세요.")
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다.")
    private String name;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "올바른 휴대폰 번호를 입력해주세요. (숫자만, 10~11자리)")
    private String phone;

    @Past(message = "올바른 생년월일을 입력해주세요.")
    private LocalDate birthDate;

    @Pattern(regexp = "^[0-9]{5}$|^$", message = "우편번호 5자리를 입력해주세요.")
    private String zipcode;

    private String address;

    private String addressDetail;

    // ── 약관 동의 ───────────────────────────────────────
    private boolean termsAgreed;

    // ── 객체 레벨 검증 (cross-field) ────────────────────

    @AssertTrue(message = "비밀번호와 비밀번호 확인이 일치하지 않습니다.")
    public boolean isPasswordMatched() {
        return password != null && password.equals(passwordConfirm);
    }

    @AssertTrue(message = "이용약관에 동의해주세요.")
    public boolean isTermsAccepted() {
        return termsAgreed;
    }
}
