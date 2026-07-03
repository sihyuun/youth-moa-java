package io.github.sihyuuun.youthmoa.center;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CenterRepository extends JpaRepository<Center, Long> {

  List<Center> findAllByIsActiveTrue();

  List<Center> findByRegion(String region);

  List<Center> findAllByIsFeaturedTrueOrderByNameAsc();

  List<Center> findAllByOrderByNameAsc();
}
