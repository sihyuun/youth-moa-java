package io.github.sihyuuun.youthmoa.program;

import io.github.sihyuuun.youthmoa.center.Center;
import io.github.sihyuuun.youthmoa.center.CenterRepository;
import io.github.sihyuuun.youthmoa.region.Region;
import io.github.sihyuuun.youthmoa.region.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final RegionRepository regionRepository;
    private final CenterRepository centerRepository;

    public Page<Program> search(String status, List<String> regions, List<String> centers,
                                String sort, int page) {
        Specification<Program> spec = Specification.where(ProgramSpec.isActive());

        Specification<Program> dateSpec = ProgramSpec.withDateStatus(status);
        if (dateSpec != null) spec = spec.and(dateSpec);

        Specification<Program> regionSpec = ProgramSpec.withRegions(regions);
        if (regionSpec != null) spec = spec.and(regionSpec);

        Specification<Program> centerSpec = ProgramSpec.withCenters(centers);
        if (centerSpec != null) spec = spec.and(centerSpec);

        String safeSort = sort == null ? "newest" : sort;

        if ("popular".equals(safeSort)) {
            // 인기순: Specification 안에서 orderBy 주입 → Pageable 의 Sort 는 unsorted
            spec = spec.and(ProgramSpec.orderByPopularity());
            Pageable pageable = PageRequest.of(page, PAGE_SIZE);
            return programRepository.findAll(spec, pageable);
        }

        Sort sortOrder = switch (safeSort) {
            case "deadline" -> Sort.by(Sort.Direction.ASC, "endDate");
            case "newest"   -> Sort.by(Sort.Direction.DESC, "createdAt");
            default         -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        return programRepository.findAll(spec, PageRequest.of(page, PAGE_SIZE, sortOrder));
    }

    /** 사이드바 노출용 지역 5개 (isFeatured=true, 가나다순) */
    public List<Region> getSidebarRegions() {
        return regionRepository.findAllByIsFeaturedTrueOrderByNameAsc();
    }

    /** 팝오버 전체 지역 (가나다순) */
    public List<Region> getAllRegions() {
        return regionRepository.findAllByOrderByNameAsc();
    }

    /** 사이드바 노출용 청년센터 5개 (isFeatured=true, 가나다순) */
    public List<Center> getSidebarCenters() {
        return centerRepository.findAllByIsFeaturedTrueOrderByNameAsc();
    }

    /** 팝오버 전체 청년센터 (가나다순) */
    public List<Center> getAllCenters() {
        return centerRepository.findAllByOrderByNameAsc();
    }

    /** 하위 호환 — distinct region 리스트 */
    public List<String> getRegions() {
        return programRepository.findDistinctRegions();
    }

    public Program findById(Long id) {
        return programRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("프로그램을 찾을 수 없습니다: " + id));
    }
}
