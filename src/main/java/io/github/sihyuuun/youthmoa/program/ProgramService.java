package io.github.sihyuuun.youthmoa.program;

import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramService {

    private static final int PAGE_SIZE = 9;

    private static final List<ApplicationStatus> ACTIVE_STATUSES =
            List.of(ApplicationStatus.PENDING, ApplicationStatus.APPROVED);

    private final ProgramRepository programRepository;
    private final RegionRepository regionRepository;
    private final CenterRepository centerRepository;
    private final ApplicationRepository applicationRepository;

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

    /**
     * Program 목록을 ProgramCardDto 로 변환 (IN 쿼리 1회로 N+1 방지).
     * 카드 목록 표시 시 사용.
     */
    public List<ProgramCardDto> toCardDtos(List<Program> programs) {
        if (programs.isEmpty()) return List.of();

        List<Long> ids = programs.stream().map(Program::getId).collect(Collectors.toList());
        Map<Long, Long> countMap = applicationRepository
                .countByProgramIdsAndStatuses(ids, ACTIVE_STATUSES)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        return programs.stream()
                .map(p -> new ProgramCardDto(p, countMap.getOrDefault(p.getId(), 0L)))
                .collect(Collectors.toList());
    }
}
