package io.github.sihyuuun.youthmoa.program;

import io.github.sihyuuun.youthmoa.center.Center;
import io.github.sihyuuun.youthmoa.center.CenterRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A9-a (2026-09-21) — Program.organization → Program.center 백필.
 *
 * <p>V20 마이그레이션이 {@code program.center_id} 를 nullable 로 추가한 뒤, 부팅 시 이 러너가 {@code center IS NULL AND
 * organization IS NOT NULL} 인 프로그램을 스캔해 {@code Center.name == organization} 매칭으로 FK 를 채운다.
 *
 * <h2>fail-fast 정책 (Q-A9-7 결정)</h2>
 *
 * <p>매칭 실패한 organization 값이 하나라도 있으면 {@link IllegalStateException} 을 던져 부팅을 중단한다. 학습 프로젝트의 조기 감지
 * 원칙에 따라 오타·미등록 센터를 서비스 기동 전에 노출.
 *
 * <h2>실행 순서</h2>
 *
 * <p>{@link io.github.sihyuuun.youthmoa.common.DataInitializer} (@Order 없음 = 기본 우선순위) 보다 뒤에 실행되도록
 * {@code @Order(100)} 지정. DataInitializer 는 A9-a 이후 시드 시점에 이미 center 를 세팅하므로 backfill 대상 자체가 없지만,
 * 기존 운영 DB (Supabase) 에 남은 organization-only row 를 위해 러너는 유지한다.
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class ProgramCenterBackfill implements ApplicationRunner {

  private final ProgramRepository programRepository;
  private final CenterRepository centerRepository;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    List<Program> pending =
        programRepository.findAll().stream()
            .filter(p -> p.getCenter() == null)
            .filter(p -> p.getOrganization() != null && !p.getOrganization().isBlank())
            .toList();

    if (pending.isEmpty()) {
      log.info("[A9-a backfill] center_id 미할당 프로그램 없음 · skip");
      return;
    }

    // Center name lookup 캐시 (반복 조회 방지)
    Map<String, Center> byName = new HashMap<>();
    for (Center c : centerRepository.findAll()) {
      byName.put(c.getName(), c);
    }

    List<String> unresolved = new ArrayList<>();
    int updated = 0;
    for (Program p : pending) {
      Center matched = byName.get(p.getOrganization());
      if (matched == null) {
        unresolved.add(
            "  - program id="
                + p.getId()
                + " title='"
                + p.getTitle()
                + "' organization='"
                + p.getOrganization()
                + "'");
        continue;
      }
      p.assignCenter(matched);
      updated++;
    }

    if (!unresolved.isEmpty()) {
      String detail = String.join("\n", unresolved);
      throw new IllegalStateException(
          "[A9-a backfill] "
              + unresolved.size()
              + " 개 프로그램의 organization 을 Center 와 매칭하지 못했습니다. Center 시드/CSV 를 확인하세요:\n"
              + detail);
    }

    log.info("[A9-a backfill] program.center_id 백필 완료 · updated={}", updated);
  }
}
