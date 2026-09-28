package io.github.sihyuuun.youthmoa.notification.watch;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A7-watcher-ui (2026-09-28 · B-3-B): 관리자가 특정 프로그램을 "지켜보기" 등록한 관계 엔티티.
 *
 * <p>Bookmark 엔티티 패턴을 준용한다. 차이점:
 *
 * <ul>
 *   <li>주체가 일반 User 가 아닌 admin (CENTER_ADMIN + SYSTEM_ADMIN). 컬럼명은 {@code admin_id} 로 의미 명시
 *   <li>목적이 UX 즐겨찾기가 아니라 알림 fan-out 트리거 (B-3-B 축)
 *   <li>center scope 격리 없음 — 감독 관점 (SYSTEM 이 타 센터 프로그램 관찰 가능)
 * </ul>
 *
 * <p>Q-A7W-1 (UNIQUE admin_id + program_id) · Q-A7W-8 (20개 상한, 초과 시 오래된 자동 삭제, 애플리케이션 레벨).
 *
 * <p>{@code program} EAGER — {@link io.github.sihyuuun.youthmoa.admin.AdminDashboardService} "지켜보는
 * 프로그램" 카드가 program.title/status 를 접근하며 open-in-view=false 환경에서 LazyInitializationException 방어.
 * Bookmark A9-b 승계.
 */
@Getter
@Entity
@Table(
    name = "program_watch",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_program_watch_admin_program",
            columnNames = {"admin_id", "program_id"}))
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProgramWatch {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "admin_id", nullable = false)
  private User admin;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "program_id", nullable = false)
  private Program program;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Builder
  private ProgramWatch(User admin, Program program) {
    this.admin = admin;
    this.program = program;
  }
}
