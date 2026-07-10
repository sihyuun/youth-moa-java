package io.github.sihyuuun.youthmoa.center;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CenterContentRepository extends JpaRepository<CenterContent, Long> {

  Optional<CenterContent> findByCenterId(Long centerId);

  List<CenterContent> findByCenterIdIn(Collection<Long> centerIds);
}
