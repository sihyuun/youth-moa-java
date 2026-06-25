package io.github.sihyuuun.youthmoa.program;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramService {

    private static final int PAGE_SIZE = 9;

    private final ProgramRepository programRepository;

    public Page<Program> search(String status, String region, String category,
                                String sort, int page) {
        Specification<Program> spec = Specification.where(ProgramSpec.isActive());

        Specification<Program> dateSpec = ProgramSpec.withDateStatus(status);
        if (dateSpec != null) spec = spec.and(dateSpec);

        Specification<Program> regionSpec = ProgramSpec.withRegion(region);
        if (regionSpec != null) spec = spec.and(regionSpec);

        Specification<Program> categorySpec = ProgramSpec.withCategory(category);
        if (categorySpec != null) spec = spec.and(categorySpec);

        Sort sortOrder = switch (sort == null ? "" : sort) {
            case "deadline" -> Sort.by(Sort.Direction.ASC, "endDate");
            case "newest"   -> Sort.by(Sort.Direction.DESC, "createdAt");
            default         -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        return programRepository.findAll(spec, PageRequest.of(page, PAGE_SIZE, sortOrder));
    }

    public List<String> getRegions() {
        return programRepository.findDistinctRegions();
    }

    public Program findById(Long id) {
        return programRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("프로그램을 찾을 수 없습니다: " + id));
    }
}
