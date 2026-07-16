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

  // F-signup-03: 관심 정보를 지역/분야 2개 컬럼으로 분리 (spec §A-Q7).
  // 기존 interests 단일 컬럼은 제거. Hibernate update 모드에서 옛 user_interest 테이블은 DROP 되지 않지만 무해.
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "user_interest_region", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "region_name", length = 50)
  private Set<String> interestRegions = new HashSet<>();

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "user_interest_category", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "category", length = 50)
  private Set<String> interestCategories = new HashSet<>();

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

  // ─── F-signup-01: 휴대폰 인증 여부 ───
  // NOT NULL default false. 회원가입 시 세션 검증 통과하면 true.
  // ddl-auto=update 환경에서 기존 row 는 default 로 채워지도록 columnDefinition 명시.
  @Column(nullable = false, columnDefinition = "boolean not null default false")
  private boolean phoneVerified = false;

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
      Set<String> interestRegions,
      Set<String> interestCategories,
      UserRole role,
      Center center,
      String centerScope,
      Boolean notifyKakao,
      Boolean notifySms,
      Boolean notifyEmail,
      Boolean phoneVerified) {
    this.email = email;
    this.password = password;
    this.name = name;
    this.phone = phone;
    this.zipcode = zipcode;
    this.address = address;
    this.addressDetail = addressDetail;
    this.birthDate = birthDate;
    this.gender = gender;
    this.interestRegions = interestRegions != null ? interestRegions : new HashSet<>();
    this.interestCategories = interestCategories != null ? interestCategories : new HashSet<>();
    this.role = role != null ? role : UserRole.USER;
    this.center = center;
    this.centerScope = centerScope;
    this.notifyKakao = notifyKakao != null ? notifyKakao : true;
    this.notifySms = notifySms != null ? notifySms : false;
    this.notifyEmail = notifyEmail != null ? notifyEmail : true;
    this.phoneVerified = phoneVerified != null ? phoneVerified : false;
  }

  /** F-signup-01: 회원가입 시 세션 인증 확인 통과 후 호출. */
  public void verifyPhone() {
    this.phoneVerified = true;
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
      Set<String> interestRegions,
      Set<String> interestCategories) {
    this.name = name;
    this.phone = phone;
    this.zipcode = zipcode;
    this.address = address;
    this.addressDetail = addressDetail;
    this.birthDate = birthDate;
    this.interestRegions = interestRegions != null ? interestRegions : new HashSet<>();
    this.interestCategories = interestCategories != null ? interestCategories : new HashSet<>();
  }

  /**
   * F-signup-03: WelcomeScreen 에서 관심 정보만 저장 (프로필 다른 필드는 미변경).
   *
   * <p>Hibernate {@code @ElementCollection} 은 필드 참조 재할당 시 트래킹이 끊겨 UPDATE 가 flush 되지 않을 수 있음. 반드시
   * 동일 PersistentSet 인스턴스를 mutate (clear + addAll) 해야 DELETE + INSERT 이 정상 실행됨.
   */
  public void updateInterests(Set<String> regions, Set<String> categories) {
    this.interestRegions.clear();
    if (regions != null) this.interestRegions.addAll(regions);
    this.interestCategories.clear();
    if (categories != null) this.interestCategories.addAll(categories);
  }

  public void assignRole(UserRole role, Center center, String centerScope) {
    this.role = role;
    this.center = center;
    this.centerScope = centerScope;
  }
}
