package io.github.sihyuuun.youthmoa.program;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** A3-2 admin-program-form-integration (2026-09-11): ProgramAttachment 조회 Repository. */
public interface ProgramAttachmentRepository extends JpaRepository<ProgramAttachment, Long> {

  List<ProgramAttachment> findByProgramIdOrderBySortOrderAscIdAsc(Long programId);

  Optional<ProgramAttachment> findByIdAndProgramId(Long id, Long programId);
}
