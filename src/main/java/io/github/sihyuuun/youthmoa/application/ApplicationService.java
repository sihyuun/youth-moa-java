package io.github.sihyuuun.youthmoa.application;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.program.ProgramStatus;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;

    /**
     * 프로그램 신청.
     * <ul>
     *   <li>이미 PENDING / APPROVED 상태 신청 있으면 → 차단</li>
     *   <li>REJECTED 상태 신청 있으면 → 차단 (재신청 불가)</li>
     *   <li>CANCELLED 상태 신청 있으면 → 같은 row 재활용 (PENDING 으로 복귀)</li>
     *   <li>없으면 → 신규 row 생성</li>
     * </ul>
     */
    @Transactional
    public Application apply(String userEmail, Long programId, ApplyRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userEmail));

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new IllegalArgumentException("프로그램을 찾을 수 없습니다: " + programId));

        if (!program.isActive()) {
            throw new IllegalStateException("비활성 상태의 프로그램은 신청할 수 없습니다.");
        }
        if (program.getStatus() != ProgramStatus.ACTIVE) {
            throw new IllegalStateException("현재 모집 중인 프로그램이 아닙니다.");
        }

        Optional<Application> existing = applicationRepository.findByUserAndProgram(user, program);
        if (existing.isPresent()) {
            Application app = existing.get();
            switch (app.getStatus()) {
                case PENDING, APPROVED ->
                        throw new IllegalStateException("이미 신청한 프로그램입니다.");
                case REJECTED ->
                        throw new IllegalStateException("이미 반려된 신청이 있어 다시 신청할 수 없습니다.");
                case CANCELLED -> {
                    app.reapply(request.getApplyReason());
                    return app;
                }
            }
        }

        Application application = Application.builder()
                .user(user)
                .program(program)
                .applyReason(request.getApplyReason())
                .build();
        return applicationRepository.save(application);
    }
}
