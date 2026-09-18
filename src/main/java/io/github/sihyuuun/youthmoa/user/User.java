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
import java.time.LocalDateTime;
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

  // ─── D5 알림 항목 (prototype L1572~1577) — 신청 승인/반려는 필수(항상 true, UI lock) ───
  // ddl-auto=update 환경에서 기존 row 는 default 로 채워지도록 columnDefinition 명시.
  @Column(nullable = false, columnDefinition = "boolean not null default true")
  private boolean notifyRemindD1 = true;

  @Column(nullable = false, columnDefinition = "boolean not null default true")
  private boolean notifyWaitlistEmpty = true;

  @Column(nullable = false, columnDefinition = "boolean not null default false")
  private boolean notifyNewProgramNews = false;

  // ─── F-signup-01: 휴대폰 인증 여부 ───
  // NOT NULL default false. 회원가입 시 세션 검증 통과하면 true.
  // ddl-auto=update 환경에서 기존 row 는 default 로 채워지도록 columnDefinition 명시.
  @Column(nullable = false, columnDefinition = "boolean not null default false")
  private boolean phoneVerified = false;

  // ─── A5 admin-users (2026-09-15) — 관리자 차단·감사·메모 ───
  // V16 마이그레이션과 세트. 기존 row 는 DEFAULT TRUE 로 초기화됨.
  // UserPrincipal.isEnabled() 가 이 값을 읽어 로그인 차단 여부 결정.
  @Column(nullable = false, columnDefinition = "boolean not null default true")
  private boolean isActive = true;

  @Column private LocalDateTime lastAccessAt;

  @Column(length = 1000)
  private String adminNote;

  @Column private LocalDateTime deactivatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "deactivated_by")
  private User deactivatedBy;

  @Column(length = 500)
  private String deactivationReason;

  // ─── A5-1 admin-staff-management (2026-09-18) — 관리자 발급 · 초기 password 강제 변경 · 감사 ───
  // V19 마이그레이션과 세트. 기존 row 는 DEFAULT FALSE / NULL 로 초기화됨.
  //   - mustChangePassword=TRUE 이면 로그인 후 PasswordChangeRequiredInterceptor 가 /password/change 로 강제
  // redirect.
  //   - passwordChangedAt 은 감사용 (unknown=NULL). 신규 발급 시 NULL 유지, 자체 변경 시 now().
  //   - invitedBy 는 관리자가 발급한 계정 감사 추적. 자체 가입 유저는 NULL.
  @Column(nullable = false, columnDefinition = "boolean not null default false")
  private boolean mustChangePassword = false;

  @Column private LocalDateTime passwordChangedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invited_by")
  private User invitedBy;

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

  /**
   * 260826 A#2: 마이페이지 프로필 편집에서 phone 이 변경되면 phoneVerified 를 리셋하여 데이터 무결성 유지. 재인증 흐름은 admin 트랙 이후
   * 사용자 프로필 편집 UX 로 확장 예정. 현재는 리셋만 수행 (사용자 안내 UX 는 후속).
   */
  public void resetPhoneVerified() {
    this.phoneVerified = false;
  }

  /** D1b: 알림 수신 채널 갱신 (마이페이지 설정용). @Setter 금지 → 도메인 메서드. */
  public void updateNotificationChannels(boolean kakao, boolean sms, boolean email) {
    this.notifyKakao = kakao;
    this.notifySms = sms;
    this.notifyEmail = email;
  }

  /** D5 알림 항목 갱신 (신청 승인/반려는 필수라 항상 true 유지). */
  public void updateNotificationItems(
      boolean remindD1, boolean waitlistEmpty, boolean newProgramNews) {
    this.notifyRemindD1 = remindD1;
    this.notifyWaitlistEmpty = waitlistEmpty;
    this.notifyNewProgramNews = newProgramNews;
  }

  /**
   * 사용자 자체 비밀번호 변경 (마이페이지 · 비밀번호 찾기 · 강제 변경 화면 공용). A5-1: mustChangePassword flag 해제 및
   * passwordChangedAt 갱신.
   */
  public void changePassword(String newPassword) {
    this.password = newPassword;
    this.mustChangePassword = false;
    this.passwordChangedAt = LocalDateTime.now();
  }

  /**
   * A5-1: 관리자가 신규 관리자 계정을 발급할 때 · 자동생성 password 를 세팅한다.
   *
   * <p>mustChangePassword=TRUE 세팅 후 인터셉터가 최초 로그인 시 /password/change 로 강제 이동시킨다. invitedBy 는 감사 로그.
   */
  public void assignInitialPassword(String encodedPassword, User invitedBy) {
    this.password = encodedPassword;
    this.mustChangePassword = true;
    this.passwordChangedAt = null;
    this.invitedBy = invitedBy;
  }

  /** A5-1: SYSTEM_ADMIN 이 임시 password 를 재발급한다. invitedBy 는 유지 (원 발급자 감사 보존). */
  public void resetPasswordByAdmin(String encodedPassword) {
    this.password = encodedPassword;
    this.mustChangePassword = true;
    this.passwordChangedAt = null;
  }

  public void updateProfile(
      String name,
      String phone,
      String zipcode,
      String address,
      String addressDetail,
      LocalDate birthDate,
      UserGender gender,
      Set<String> interestRegions,
      Set<String> interestCategories) {
    this.name = name;
    this.phone = phone;
    this.zipcode = zipcode;
    this.address = address;
    this.addressDetail = addressDetail;
    this.birthDate = birthDate;
    // Q-5: 성별 편집 허용 (null 이면 미변경)
    if (gender != null) {
      this.gender = gender;
    }
    // 260826 fix: @ElementCollection 재할당 → mutate 패턴 (F-signup-01 ym-verify PR #95 refute 후속).
    // Hibernate PersistentSet 은 인스턴스 재할당 시 트래킹이 끊겨 UPDATE 가 flush 되지 않음.
    // updateInterests() 와 동일하게 clear + addAll 로 통일.
    this.interestRegions.clear();
    if (interestRegions != null) this.interestRegions.addAll(interestRegions);
    this.interestCategories.clear();
    if (interestCategories != null) this.interestCategories.addAll(interestCategories);
  }

  /**
   * F-signup-03: WelcomeScreen 에서 관심 정보만 저장 (프로필 다른 필드는 미변경).
   *
   * <p>Hibernate {@code @ElementCollection} 은 필드 참조 재할당 시 트래킹이 끊겨 UPDATE 가 flush 되지 않을 수 있음. 반드시 동일
   * PersistentSet 인스턴스를 mutate (clear + addAll) 해야 DELETE + INSERT 이 정상 실행됨.
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

  // ─── A5 admin-users 도메인 메서드 ───

  /**
   * A5: 관리자 차단. isActive=false 로 전환 + 감사 컬럼 (deactivated_at/by/reason) 세트 기록. 재활성화 시에도 마지막 차단 이력은
   * 유지 (감사 목적).
   */
  public void deactivate(User admin, String reason) {
    this.isActive = false;
    this.deactivatedAt = LocalDateTime.now();
    this.deactivatedBy = admin;
    this.deactivationReason = reason;
  }

  /** A5: 재활성화. 감사 컬럼은 이력 보존을 위해 clear 하지 않음. */
  public void reactivate() {
    this.isActive = true;
  }

  /** A5: 로그인 성공 시 AuthenticationSuccessHandler 에서 호출. REQUIRES_NEW 트랜잭션에서 실행. */
  public void updateLastAccess(LocalDateTime now) {
    this.lastAccessAt = now;
  }

  /** A5: 관리자 메모 (사용자에게 노출되지 않음). */
  public void updateAdminNote(String note) {
    this.adminNote = note;
  }

  /**
   * A5: role 승격/강등 (SYSTEM_ADMIN 전용). center/centerScope 는 별도로 assignRole 로 관리. USER 로 강등 시 기존 센터
   * 소속도 유지 (신청 이력·history 보존).
   */
  public void promoteTo(UserRole newRole) {
    this.role = newRole;
  }
}
