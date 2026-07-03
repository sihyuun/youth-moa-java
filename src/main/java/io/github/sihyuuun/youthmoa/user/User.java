package io.github.sihyuuun.youthmoa.user;

import io.github.sihyuuun.youthmoa.center.Center;
import io.github.sihyuuun.youthmoa.common.BaseTimeEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 191)
  private String email;

  @Column(length = 255)
  private String password;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(length = 20)
  private String phone;

  @Column(length = 10)
  private String zipcode;

  @Column(length = 255)
  private String address;

  @Column(length = 255)
  private String addressDetail;

  @Column private LocalDate birthDate;

  @Enumerated(EnumType.STRING)
  @Column(length = 10)
  private UserGender gender;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "user_interest", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "interest", length = 50)
  private Set<String> interests = new HashSet<>();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserRole role;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "center_id")
  private Center center;

  @Column(length = 50)
  private String centerScope;

  // ─── D1b: 알림 채널 수신 선호 ───
  // default true / false / true — 시드/신규 회원가입 시 자동 세팅.
  // 값 변경은 도메인 메서드 updateNotificationChannels() 로 수행 (@Setter 금지 원칙).
  @Column(nullable = false)
  private boolean notifyKakao = true;

  @Column(nullable = false)
  private boolean notifySms = false;

  @Column(nullable = false)
  private boolean notifyEmail = true;

  @Builder
  private User(
      String email,
      String password,
      String name,
      String phone,
      String zipcode,
      String address,
      String addressDetail,
      LocalDate birthDate,
      UserGender gender,
      Set<String> interests,
      UserRole role,
      Center center,
      String centerScope,
      Boolean notifyKakao,
      Boolean notifySms,
      Boolean notifyEmail) {
    this.email = email;
    this.password = password;
    this.name = name;
    this.phone = phone;
    this.zipcode = zipcode;
    this.address = address;
    this.addressDetail = addressDetail;
    this.birthDate = birthDate;
    this.gender = gender;
    this.interests = interests != null ? interests : new HashSet<>();
    this.role = role != null ? role : UserRole.USER;
    this.center = center;
    this.centerScope = centerScope;
    this.notifyKakao = notifyKakao != null ? notifyKakao : true;
    this.notifySms = notifySms != null ? notifySms : false;
    this.notifyEmail = notifyEmail != null ? notifyEmail : true;
  }

  /** D1b: 알림 수신 채널 갱신 (마이페이지 설정용). @Setter 금지 → 도메인 메서드. */
  public void updateNotificationChannels(boolean kakao, boolean sms, boolean email) {
    this.notifyKakao = kakao;
    this.notifySms = sms;
    this.notifyEmail = email;
  }

  public void changePassword(String newPassword) {
    this.password = newPassword;
  }

  public void updateProfile(
      String name,
      String phone,
      String zipcode,
      String address,
      String addressDetail,
      LocalDate birthDate,
      Set<String> interests) {
    this.name = name;
    this.phone = phone;
    this.zipcode = zipcode;
    this.address = address;
    this.addressDetail = addressDetail;
    this.birthDate = birthDate;
    this.interests = interests != null ? interests : new HashSet<>();
  }

  public void assignRole(UserRole role, Center center, String centerScope) {
    this.role = role;
    this.center = center;
    this.centerScope = centerScope;
  }
}
