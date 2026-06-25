package io.github.sihyuuun.youthmoa.program;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProgramRepository extends JpaRepository<Program, Long>, JpaSpecificationExecutor<Program> {

    Page<Program> findAllByIsActiveTrue(Pageable pageable);

    Page<Program> findAllByCategoryAndIsActiveTrue(String category, Pageable pageable);

    @Query("SELECT DISTINCT p.region FROM Program p WHERE p.isActive = true AND p.region IS NOT NULL ORDER BY p.region")
    List<String> findDistinctRegions();
}
