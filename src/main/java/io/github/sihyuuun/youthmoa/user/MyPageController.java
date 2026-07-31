package io.github.sihyuuun.youthmoa.user;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.bookmark.Bookmark;
import io.github.sihyuuun.youthmoa.bookmark.BookmarkRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * D5 마이페이지 컨트롤러.
 *
 * <p>탭 4종을 URL 쿼리 파라미터 ({@code ?tab=history|favorites|noti|profile}) 로 분기한다. HTMX 부분 전환은 D5b 후속에서
 * 도입 예정.
 *
 * <p><b>@Transactional(readOnly=true) 부착 이유</b>: OSIV=false 환경에서 Program.content 등 {@code @Lob} 필드나
 * 지연 로딩 관계를 템플릿이 접근할 때 트랜잭션이 없으면 예외 발생. Repository EntityGraph 로 program 은 즉시 로딩되지만, 안전망으로 readOnly
 * 트랜잭션을 열어 둔다.
 *
 * <p><b>{@code @SessionAttribute} 개념</b>: Spring MVC 가 HTTP 세션의 특정 키를 컨트롤러 파라미터로 주입해 주는 어노테이션. 여기서는
 * 세션 flag 조회에 {@code HttpSession} 을 직접 주입해 명시적으로 다룬다 (TTL 계산·초기화 로직이 있어 더 유연).
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {

  /** 프로필 수정 Step2 진입 허용 시간 (Step1 통과 후 10분). */
  static final long PROFILE_VERIFY_TTL_MINUTES = 10;

  /** 세션에 저장되는 Step1 통과 시각 키. */
  static final String SESSION_KEY_PROFILE_VERIFIED_AT = "mypageProfileVerifiedAt";

  private final UserRepository userRepository;
  private final UserService userService;
  private final ApplicationRepository applicationRepository;
  private final BookmarkRepository bookmarkRepository;

  @GetMapping
  @Transactional(readOnly = true)
  public String mypage(
      @RequestParam(name = "tab", required = false, defaultValue = "history") String tab,
      @RequestParam(name = "period", required = false, defaultValue = "3M") String period,
      @RequestParam(name = "status", required = false, defaultValue = "ALL") String statusFilter,
      @AuthenticationPrincipal UserDetails principal,
      Model model) {

    User currentUser = loadUser(principal);
    addSummary(currentUser, model);
    model.addAttribute("currentTab", tab);
    model.addAttribute("currentPage", "mypage");

    switch (tab) {
      case "favorites" -> {
        List<Bookmark> bookmarks =
            bookmarkRepository.findAllByUserOrderByCreatedAtDesc(currentUser);
        model.addAttribute("bookmarks", bookmarks);
        return "mypage/favorites";
      }
      case "noti" -> {
        return "mypage/notifications";
      }
      case "profile" -> {
        // 이메일 재확인 폼 표시. 성공 시 redirect /mypage/profile/edit.
        return "mypage/profile-verify";
      }
      default -> {
        List<Application> allApps =
            applicationRepository.findAllByUserOrderByAppliedAtDesc(currentUser);
        // 기간 필터 (prototype L1407~1411: 3개월/6개월/1년/3년)
        java.time.LocalDateTime cutoff = periodCutoff(period);
        List<Application> byPeriod =
            cutoff == null
                ? allApps
                : allApps.stream().filter(a -> a.getAppliedAt().isAfter(cutoff)).toList();
        // 상태 필터 (prototype L1413~1424: 전체/승인/대기/반려/취소)
        ApplicationStatus filter = mapStatusFilter(statusFilter);
        List<Application> shown =
            filter == null
                ? byPeriod
                : byPeriod.stream().filter(a -> a.getStatus() == filter).toList();
        model.addAttribute("applications", shown);
        model.addAttribute("currentPeriod", period);
        model.addAttribute("currentStatusFilter", statusFilter);
        // 카운트 (필터 UI 표시용) — 기간 반영 후 상태별
        model.addAttribute("countAll", byPeriod.size());
        model.addAttribute(
            "countApproved",
            byPeriod.stream().filter(a -> a.getStatus() == ApplicationStatus.APPROVED).count());
        model.addAttribute(
            "countPending",
            byPeriod.stream().filter(a -> a.getStatus() == ApplicationStatus.PENDING).count());
        model.addAttribute(
            "countRejected",
            byPeriod.stream().filter(a -> a.getStatus() == ApplicationStatus.REJECTED).count());
        model.addAttribute(
            "countCancelled",
            byPeriod.stream().filter(a -> a.getStatus() == ApplicationStatus.CANCELLED).count());
        return "mypage/history";
      }
    }
  }

  /**
   * 기간 코드 (3M/6M/1Y/3Y) → cutoff LocalDateTime. 지원 안 되는 값은 3M 기본. package-private: 단위 테스트 접근 허용.
   */
  static java.time.LocalDateTime periodCutoff(String code) {
    java.time.LocalDateTime now = java.time.LocalDateTime.now();
    return switch (code == null ? "3M" : code) {
      case "6M" -> now.minusDays(180);
      case "1Y" -> now.minusDays(365);
      case "3Y" -> now.minusDays(365L * 3);
      default -> now.minusDays(90);
    };
  }

  /**
   * 상태 필터 코드 (ALL/APPROVED/PENDING/REJECTED/CANCELLED) → enum. ALL 은 null. package-private: 단위 테스트
   * 접근 허용.
   */
  static ApplicationStatus mapStatusFilter(String code) {
    if (code == null || "ALL".equalsIgnoreCase(code)) return null;
    try {
      return ApplicationStatus.valueOf(code);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /** Step1: 비밀번호 재확인. 통과 시 세션에 시각 기록 후 Step2 리다이렉트. */
  @PostMapping("/profile/verify")
  public String verifyPassword(
      @RequestParam("password") String password,
      @AuthenticationPrincipal UserDetails principal,
      HttpSession session,
      RedirectAttributes redirectAttributes) {
    boolean ok = userService.verifyPassword(principal.getUsername(), password);
    if (!ok) {
      redirectAttributes.addFlashAttribute("profileVerifyError", "비밀번호가 일치하지 않습니다.");
      return "redirect:/mypage?tab=profile";
    }
    session.setAttribute(SESSION_KEY_PROFILE_VERIFIED_AT, Instant.now());
    return "redirect:/mypage/profile/edit";
  }

  /** Step2: 프로필 수정 폼. 세션 flag 없거나 10분 초과 시 Step1 로 리다이렉트. */
  @GetMapping("/profile/edit")
  @Transactional(readOnly = true)
  public String profileEditForm(
      @AuthenticationPrincipal UserDetails principal, HttpSession session, Model model) {
    if (!isProfileVerified(session)) {
      return "redirect:/mypage?tab=profile";
    }
    User currentUser = loadUser(principal);
    addSummary(currentUser, model);
    model.addAttribute("currentTab", "profile");
    model.addAttribute("currentPage", "mypage");
    model.addAttribute("editUser", currentUser);
    if (!model.containsAttribute("profileUpdateRequest")) {
      ProfileUpdateRequest req = new ProfileUpdateRequest();
      req.setName(currentUser.getName());
      req.setPhone(currentUser.getPhone());
      req.setZipcode(currentUser.getZipcode());
      req.setAddress(currentUser.getAddress());
      req.setAddressDetail(currentUser.getAddressDetail());
      req.setBirthDate(currentUser.getBirthDate());
      req.setInterestRegions(currentUser.getInterestRegions());
      req.setInterestCategories(currentUser.getInterestCategories());
      model.addAttribute("profileUpdateRequest", req);
    }
    return "mypage/profile-edit";
  }

  @PostMapping("/profile")
  public String updateProfile(
      @Valid @ModelAttribute("profileUpdateRequest") ProfileUpdateRequest request,
      BindingResult bindingResult,
      @AuthenticationPrincipal UserDetails principal,
      HttpSession session,
      Model model,
      RedirectAttributes redirectAttributes) {
    if (!isProfileVerified(session)) {
      return "redirect:/mypage?tab=profile";
    }
    if (bindingResult.hasErrors()) {
      User currentUser = loadUser(principal);
      addSummary(currentUser, model);
      model.addAttribute("currentTab", "profile");
      model.addAttribute("currentPage", "mypage");
      model.addAttribute("editUser", currentUser);
      return "mypage/profile-edit";
    }
    // 2026-07-31: 비밀번호 인라인 변경 정책 검증 실패 시 IllegalArgumentException 이 발생.
    // password 필드에 rejectValue 로 매핑하여 폼 재렌더.
    try {
      userService.updateProfile(principal.getUsername(), request);
    } catch (IllegalArgumentException e) {
      bindingResult.rejectValue("password", "password.invalid", e.getMessage());
      User currentUser = loadUser(principal);
      addSummary(currentUser, model);
      model.addAttribute("currentTab", "profile");
      model.addAttribute("currentPage", "mypage");
      model.addAttribute("editUser", currentUser);
      return "mypage/profile-edit";
    }
    // 갱신 후 세션 flag 는 유지 (사용자 편의)
    redirectAttributes.addFlashAttribute("mypageToast", "저장했어요");
    return "redirect:/mypage?tab=profile";
  }

  /**
   * F0f-fix-4: 회원 탈퇴 실행. 확인 다이얼로그에서 [탈퇴하기] 클릭 시 도달.
   *
   * <p>탈퇴 후 SecurityContext / 세션 무효화 → 로그인 페이지로 리다이렉트.
   */
  @PostMapping("/withdraw")
  public String withdraw(
      @AuthenticationPrincipal UserDetails principal,
      HttpSession session,
      jakarta.servlet.http.HttpServletRequest request,
      jakarta.servlet.http.HttpServletResponse response) {
    userService.withdraw(principal.getUsername());
    // 세션 + SecurityContext 정리
    new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler()
        .logout(
            request,
            response,
            org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication());
    return "redirect:/login?withdraw";
  }

  @PostMapping("/notifications")
  public String updateNotifications(
      @ModelAttribute NotificationChannelRequest request,
      @AuthenticationPrincipal UserDetails principal,
      RedirectAttributes redirectAttributes) {
    userService.updateNotificationChannels(principal.getUsername(), request);
    redirectAttributes.addFlashAttribute("mypageToast", "저장했어요");
    return "redirect:/mypage?tab=noti";
  }

  private User loadUser(UserDetails principal) {
    return userRepository
        .findByEmail(principal.getUsername())
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
  }

  private void addSummary(User user, Model model) {
    // interests 는 LAZY @ElementCollection — 뷰 렌더 시점엔 tx 종료 상태라 접근 불가.
    // 트랜잭션 안에서 강제 초기화 후 plain 스냅샷으로 노출.
    // 프로필 요약 관심 정보: prototype.tsx L1236~1252 매칭 그룹형.
    //   - 지역·분야 각 그룹: 아이콘+라벨 + 값칩 (최대 3) + `+N` 축약칩
    //   - 값 0개 그룹은 "미설정" 표시 (그룹 자체는 항상 노출)
    //   - 끝에 "관심 정보 수정" 편집 링크 → ?tab=profile
    java.util.List<String> regions =
        user.getInterestRegions() == null
            ? java.util.List.of()
            : new java.util.ArrayList<>(user.getInterestRegions());
    java.util.List<String> categories =
        user.getInterestCategories() == null
            ? java.util.List.of()
            : new java.util.ArrayList<>(user.getInterestCategories());
    java.util.List<InterestGroup> interestGroups =
        java.util.List.of(
            InterestGroup.of("pin", "관심 지역", regions),
            InterestGroup.of("star", "관심 분야", categories));
    model.addAttribute("interestGroups", interestGroups);
    List<Application> apps = applicationRepository.findAllByUserOrderByAppliedAtDesc(user);
    long ongoing =
        apps.stream()
            .filter(
                a ->
                    a.getStatus() == ApplicationStatus.PENDING
                        || a.getStatus() == ApplicationStatus.APPROVED)
            .count();
    long finished =
        apps.stream()
            .filter(
                a ->
                    a.getStatus() == ApplicationStatus.REJECTED
                        || a.getStatus() == ApplicationStatus.CANCELLED)
            .count();
    long favorites = bookmarkRepository.findAllByUserOrderByCreatedAtDesc(user).size();

    // Thymeleaf 예약어(application/session/request) 회피 → myUser
    model.addAttribute("myUser", user);
    model.addAttribute("kpiOngoing", ongoing);
    model.addAttribute("kpiFinished", finished);
    model.addAttribute("kpiFavorites", favorites);
  }

  private boolean isProfileVerified(HttpSession session) {
    Object v = session.getAttribute(SESSION_KEY_PROFILE_VERIFIED_AT);
    if (!(v instanceof Instant verifiedAt)) return false;
    long minutes = ChronoUnit.MINUTES.between(verifiedAt, Instant.now());
    return minutes < PROFILE_VERIFY_TTL_MINUTES;
  }
}
