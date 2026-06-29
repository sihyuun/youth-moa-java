package io.github.sihyuuun.youthmoa.user;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 회원가입 폼 DTO — prototype.tsx SignupScreen (line 1414~1539) 기준.
 * <ul>
 *   <li>계정 정보: email / password / passwordConfirm</li>
 *   <li>개인 정보: name / phone / gender / birthDate (YYYY/MM/DD text) /
 *                  zipcode / address / addressDetail</li>
 *   <li>약관: termsAgreed + privacyAgreed (2개 분리)</li>
 * </ul>
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
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "비밀번호는 영문과 숫자를 모두 포함해야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    private String passwordConfirm;

    // ── 개인 정보 ───────────────────────────────────────
    @NotBlank(message = "이름을 입력해주세요.")
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "핸드폰 번호를 입력해주세요.")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "숫자만 입력해주세요. (10~11자리)")
    private String phone;

    @NotNull(message = "성별을 선택해주세요.")
    private UserGender gender;

    /** prototype.tsx 는 type=text 로 "YYYY / MM / DD" 또는 "YYYY-MM-DD" 입력 받음 */
    @NotBlank(message = "생년월일을 입력해주세요.")
    @Pattern(regexp = "^\\d{4}[-/.\\s]?\\d{2}[-/.\\s]?\\d{2}$",
            message = "YYYY-MM-DD 또는 YYYY/MM/DD 형식으로 입력해주세요.")
    private String birthDateText;

    @NotBlank(message = "우편번호를 입력해주세요.")
    @Pattern(regexp = "^[0-9]{5}$", message = "우편번호 5자리를 입력해주세요.")
    private String zipcode;

    @NotBlank(message = "주소를 입력해주세요.")
    private String address;

    private String addressDetail;

    // ── 약관 동의 (2개 분리, prototype.tsx) ────────────
    private boolean termsAgreed;
    private boolean privacyAgreed;

    // 중복확인 통과 여부 — hidden input 으로 전송, 이메일 변경 시 false 로 reset
    private boolean emailChecked;

    // ── 객체 레벨 검증 ──────────────────────────────────

    @AssertTrue(message = "비밀번호와 비밀번호 확인이 일치하지 않습니다.")
    public boolean isPasswordMatched() {
        return password != null && password.equals(passwordConfirm);
    }

    @AssertTrue(message = "이용약관과 개인정보처리방침에 모두 동의해주세요.")
    public boolean isAllTermsAccepted() {
        return termsAgreed && privacyAgreed;
    }

    @AssertTrue(message = "아이디 중복확인을 진행해주세요.")
    public boolean isEmailChecked() {
        return emailChecked;
    }

    /** birthDateText 를 LocalDate 로 파싱 (UserService 에서 호출). */
    public LocalDate parseBirthDate() {
        if (birthDateText == null || birthDateText.isBlank()) return null;
        String normalized = birthDateText.replaceAll("[\\s/.]", "-");
        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
