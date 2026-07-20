package io.github.sihyuuun.youthmoa.notification;

import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 알림 페이지·읽음 처리 컨트롤러.
 *
 * <ul>
 *   <li>{@code GET /notifications} — 전체보기 stub 페이지
 *   <li>{@code POST /notifications/read-all} — HTMX, 드롭다운 fragment + OOB dot 갱신
 *   <li>{@code POST /notifications/{id}/read} — HTMX 개별 읽음 후 링크 리다이렉트
 * </ul>
 */
@Controller
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;
  private final UserRepository userRepository;

  @GetMapping("/notifications")
  public String list(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam(name = "unread", required = false, defaultValue = "false") boolean unreadOnly,
      Model model) {
    var user =
        userRepository
            .findById(principal.getId())
            .orElseThrow(() -> new IllegalStateException("Authenticated user missing"));
    List<Notification> all = notificationService.listAll(user);
    long unreadCount = notificationService.unreadCount(user);
    Map<String, List<Notification>> grouped = notificationService.findGrouped(user, unreadOnly);
    model.addAttribute("notifications", all); // 하위 호환 (기존 렌더 테스트 대비)
    model.addAttribute("groupedNotifications", grouped);
    model.addAttribute("filterUnread", unreadOnly);
    model.addAttribute("totalCount", (long) all.size());
    model.addAttribute("unreadCount", unreadCount);
    return "notification/list";
  }

  /**
   * "모두 읽음" — HTMX 대상: 드롭다운 자체. OOB swap 으로 헤더 dot 도 동시 갱신.
   *
   * <p>fragments/notification-panel.html :: panel fragment 를 반환 → hx-swap="outerHTML" 로 교체.
   */
  @PostMapping("/notifications/read-all")
  public String readAll(@AuthenticationPrincipal UserPrincipal principal, Model model) {
    notificationService.markAllAsRead(principal.getId());
    // Advice 로 headerUnreadCount / headerRecentNotifications 재주입되지만, 같은 요청 스코프에서는
    // ModelAttribute 가 이미 계산됐을 수 있으므로 최신값을 수동 주입.
    var user = userRepository.findById(principal.getId()).orElseThrow();
    model.addAttribute("headerRecentNotifications", notificationService.recentForHeader(user));
    model.addAttribute("headerUnreadCount", notificationService.unreadCount(user));
    return "fragments/notification-panel :: panel";
  }

  @PostMapping("/notifications/{id}/read")
  public String read(
      @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, Model model) {
    Notification n = notificationService.markAsRead(id, principal.getId());
    var user = userRepository.findById(principal.getId()).orElseThrow();
    model.addAttribute("headerRecentNotifications", notificationService.recentForHeader(user));
    model.addAttribute("headerUnreadCount", notificationService.unreadCount(user));
    model.addAttribute("redirectLink", n.getLink() != null ? n.getLink() : "/notifications");
    return "fragments/notification-panel :: panel";
  }
}
