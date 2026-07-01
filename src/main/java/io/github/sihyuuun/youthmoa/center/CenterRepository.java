package io.github.sihyuuun.youthmoa.center;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CenterRepository extends JpaRepository<Center, Long> {

    List<Center> findAllByIsActiveTrue();

    List<Center> findByRegion(String region);

    List<Center> findAllByIsFeaturedTrueOrderByNameAsc();

    List<Center> findAllByOrderByNameAsc();
}
