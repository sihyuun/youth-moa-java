package io.github.sihyuuun.youthmoa.application;

import io.github.sihyuuun.youthmoa.application.event.ApplicationApprovedEvent;
import io.github.sihyuuun.youthmoa.application.event.ApplicationCancelledEvent;
import io.github.sihyuuun.youthmoa.application.event.ApplicationRejectedEvent;
import io.github.sihyuuun.youthmoa.common.storage.FileStorage;
import io.github.sihyuuun.youthmoa.common.storage.StoredFile;
import io.github.sihyuuun.youthmoa.notice.NoticeService;
import io.github.sihyuuun.youthmoa.program.ApplyQuestion;
import io.github.sihyuuun.youthmoa.program.ApplyQuestionRepository;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.program.ProgramStatus;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {

  private final ApplicationRepository applicationRepository;
  private final ProgramRepository programRepository;
  private final UserRepository userRepository;
  private final ApplyQuestionRepository applyQuestionRepository;
  private final ApplyAnswerRepository applyAnswerRepository;
  private final FileStorage fileStorage;

  /** F0c-dynamic-fields: ATTACHMENT 응답 저장 bucket. LocalFileStorage 는 파일시스템에 저장. */
  @Value("${youthmoa.storage.supabase.apply-bucket:apply-attachments}")
  private String applyBucket;

  /**
   * chore-observability PR-2: 신청 성공 지표.
   *
   * <p>Micrometer 이름 {@code youthmoa.application.submitted} → Prometheus 노출 이름 {@code
   * youthmoa_application_submitted_total} (Counter 는 자동 {@code _total} 접미사).
   *
   * <p>태그 없음 — programId 나 category 태그는 카디널리티 폭발 위험. 필요 시 후속 티켓에서 상한 있는 태그만 추가.
   */
  private final MeterRegistry meterRegistry;

  private Counter applicationSubmittedCounter;

  @PostConstruct
  void initMetrics() {
    this.applicationSubmittedCounter =
        Counter.builder("youthmoa.application.submitted")
            .description("프로그램 신청 성공 누적 건수 (재신청·PENDING 복귀 포함)")
            .register(meterRegistry);
  }

  /**
   * Spring 표준 도메인 이벤트 퍼블리셔.
   *
   * <p>{@code publishEvent} 호출 시점엔 아직 트랜잭션 커밋 전이며,
   * {@code @TransactionalEventListener(AFTER_COMMIT)} 리스너는 실제 커밋 후에 실행된다. 롤백되면 리스너는 호출되지 않는다.
   */
  private final ApplicationEventPublisher eventPublisher;

  /**
   * 프로그램 신청.
   *
   * <ul>
   *   <li>이미 PENDING / APPROVED 상태 신청 있으면 → 차단
   *   <li>REJECTED 상태 신청 있으면 → 차단 (재신청 불가)
   *   <li>CANCELLED 상태 신청 있으면 → 같은 row 재활용 (PENDING 으로 복귀)
   *   <li>없으면 → 신규 row 생성
   * </ul>
   */
  @Transactional
  public Application apply(String userEmail, Long programId, ApplyRequest request) {
    return apply(userEmail, programId, request, Map.of());
  }

  /**
   * F0c-dynamic-fields (2026-09-08): 동적 응답 파일을 함께 저장하는 확장 오버로드.
   *
   * <p>{@code attachments} 는 questionId → MultipartFile. 검증 순서:
   *
   * <ol>
   *   <li>기존 프로그램/상태 검증 (재신청·중복 로직 그대로)
   *   <li>동적 필드 조회 (활성 필드만) — required 미충족 시 IllegalArgumentException
   *   <li>DROPDOWN whitelist 검증, ATTACHMENT 확장자·크기 검증 (NoticeService 상수 재활용)
   *   <li>Application 저장 → ATTACHMENT storage 업로드 → ApplyAnswer 저장 순으로 트랜잭션 안에서 진행
   * </ol>
   *
   * <p>스토리지 업로드 후 예외 발생 시 이미 저장된 오브젝트는 트랜잭션 롤백으로 정리되지 않는다 (S3 등 외부 스토리지 특성). Best-effort 로 즉시
   * delete 시도.
   */
  @Transactional
  public Application apply(
      String userEmail,
      Long programId,
      ApplyRequest request,
      Map<Long, MultipartFile> attachments) {
    User user =
        userRepository
            .findByEmail(userEmail)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userEmail));

    Program program =
        programRepository
            .findById(programId)
            .orElseThrow(() -> new IllegalArgumentException("프로그램을 찾을 수 없습니다: " + programId));

    if (!program.isActive()) {
      throw new IllegalStateException("비활성 상태의 프로그램은 신청할 수 없습니다.");
    }
    if (program.getStatus() != ProgramStatus.OPEN) {
      throw new IllegalStateException("현재 모집 중인 프로그램이 아닙니다.");
    }

    // F0c-dynamic-fields: 활성 동적 질문 조회 + 응답 사전 검증.
    List<ApplyQuestion> questions =
        applyQuestionRepository.findByProgramIdAndIsActiveTrueOrderBySortOrderAsc(programId);
    Map<Long, String> dynamicAnswers =
        request.getDynamicAnswers() == null ? Map.of() : request.getDynamicAnswers();
    Map<Long, MultipartFile> dynamicAttachments = attachments == null ? Map.of() : attachments;
    validateDynamicAnswers(questions, dynamicAnswers, dynamicAttachments);

    Optional<Application> existing = applicationRepository.findByUserAndProgram(user, program);
    Application application;
    if (existing.isPresent()) {
      Application app = existing.get();
      switch (app.getStatus()) {
        case PENDING, APPROVED -> throw new IllegalStateException("이미 신청한 프로그램입니다.");
        case REJECTED -> throw new IllegalStateException("이미 반려된 신청이 있어 다시 신청할 수 없습니다.");
        case CANCELLED -> {
          app.reapply(request.getApplyReason());
          persistDynamicAnswers(app, questions, dynamicAnswers, dynamicAttachments);
          applicationSubmittedCounter.increment();
          return app;
        }
        default -> throw new IllegalStateException("알 수 없는 신청 상태입니다.");
      }
    } else {
      application =
          Application.builder()
              .user(user)
              .program(program)
              .applyReason(request.getApplyReason())
              .build();
      application = applicationRepository.save(application);
    }
    persistDynamicAnswers(application, questions, dynamicAnswers, dynamicAttachments);
    applicationSubmittedCounter.increment();
    return application;
  }

  /**
   * 동적 질문 응답 사전 검증. 실패 시 IllegalArgumentException 을 던져 트랜잭션이 rollback + controller 가 flash 처리.
   *
   * <ul>
   *   <li>required 미충족 → 400
   *   <li>DROPDOWN whitelist 위반 → 400
   *   <li>TEXT maxLength 초과 → 400
   *   <li>ATTACHMENT 5MB · 확장자 위반 → 400
   * </ul>
   */
  void validateDynamicAnswers(
      List<ApplyQuestion> questions,
      Map<Long, String> answers,
      Map<Long, MultipartFile> attachments) {
    for (ApplyQuestion q : questions) {
      switch (q.getFieldType()) {
        case TEXT -> {
          String v = answers.get(q.getId());
          String trimmed = v == null ? "" : v.trim();
          if (q.isRequired() && trimmed.isEmpty()) {
            throw new IllegalArgumentException("필수 항목이에요: " + q.getLabel());
          }
          if (q.getMaxLength() != null && trimmed.length() > q.getMaxLength()) {
            throw new IllegalArgumentException(
                q.getLabel() + " 은(는) " + q.getMaxLength() + "자 이하로 입력해주세요.");
          }
        }
        case DROPDOWN -> {
          String v = answers.get(q.getId());
          String trimmed = v == null ? "" : v.trim();
          if (q.isRequired() && trimmed.isEmpty()) {
            throw new IllegalArgumentException("필수 항목이에요: " + q.getLabel());
          }
          if (!trimmed.isEmpty() && !q.isValidDropdownValue(trimmed)) {
            throw new IllegalArgumentException(q.getLabel() + " 항목은 허용되지 않은 옵션이에요: " + trimmed);
          }
        }
        case ATTACHMENT -> {
          MultipartFile file = attachments.get(q.getId());
          boolean empty = file == null || file.isEmpty();
          if (q.isRequired() && empty) {
            throw new IllegalArgumentException("필수 첨부 파일이 필요해요: " + q.getLabel());
          }
          if (!empty) {
            validateAttachment(q, file);
          }
        }
        default -> throw new IllegalStateException("알 수 없는 질문 타입: " + q.getFieldType());
      }
    }
  }

  /** Qn-5 A: admin-notice 정책 승계 (5MB · pdf/hwp/docx/xlsx). */
  private void validateAttachment(ApplyQuestion q, MultipartFile file) {
    if (file.getSize() > NoticeService.MAX_ATTACHMENT_SIZE_BYTES) {
      throw new IllegalArgumentException(q.getLabel() + " 파일 크기는 5MB 이하여야 해요.");
    }
    String ext = NoticeService.extensionOf(file.getOriginalFilename());
    if (!NoticeService.ALLOWED_EXTENSIONS.contains(ext)) {
      throw new IllegalArgumentException(q.getLabel() + " 는 pdf, hwp, docx, xlsx 만 업로드할 수 있어요.");
    }
  }

  /**
   * 검증 통과 후 실제 ApplyAnswer row 를 저장. ATTACHMENT 는 storage 업로드 후 경로만 DB 기록. 재신청(CANCELLED) 시 기존 응답
   * row 는 그대로 두고 새 row 를 추가 (이력 보존).
   */
  private void persistDynamicAnswers(
      Application application,
      List<ApplyQuestion> questions,
      Map<Long, String> answers,
      Map<Long, MultipartFile> attachments) {
    for (ApplyQuestion q : questions) {
      switch (q.getFieldType()) {
        case TEXT, DROPDOWN -> {
          String v = answers.get(q.getId());
          if (v == null || v.trim().isEmpty()) continue;
          applyAnswerRepository.save(
              ApplyAnswer.builder().application(application).question(q).value(v.trim()).build());
        }
        case ATTACHMENT -> {
          MultipartFile file = attachments.get(q.getId());
          if (file == null || file.isEmpty()) continue;
          String originalName = file.getOriginalFilename();
          if (originalName == null || originalName.isBlank()) originalName = "unnamed";
          String ext = NoticeService.extensionOf(originalName);
          String storedName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
          String path = application.getId() + "/" + q.getId() + "/" + storedName;
          StoredFile stored;
          try {
            stored = fileStorage.upload(applyBucket, path, file);
          } catch (IOException e) {
            throw new IllegalStateException("첨부 파일 업로드 중 오류가 발생했어요: " + q.getLabel(), e);
          }
          applyAnswerRepository.save(
              ApplyAnswer.builder()
                  .application(application)
                  .question(q)
                  .attachmentPath(path)
                  .attachmentFilename(originalName)
                  .attachmentSize(stored.size())
                  .build());
          log.info(
              "[apply-answer] attachment saved application={} question={} path={}",
              application.getId(),
              q.getId(),
              path);
        }
        default -> throw new IllegalStateException("알 수 없는 질문 타입: " + q.getFieldType());
      }
    }
  }

  /**
   * 신청 승인 (관리자). 상태 전이 후 {@link ApplicationApprovedEvent} 발행.
   *
   * <p>이미 APPROVED 상태이면 no-op (idempotent) — 이벤트도 발행하지 않는다.
   */
  @Transactional
  public void approve(Long applicationId, String adminEmail) {
    Application application = loadWithProgramAndUser(applicationId);
    User admin = loadUser(adminEmail);

    if (application.getStatus() == ApplicationStatus.APPROVED) {
      return; // idempotent
    }
    application.approve(admin);

    eventPublisher.publishEvent(
        new ApplicationApprovedEvent(
            application.getId(),
            application.getUser().getId(),
            application.getProgram().getId(),
            application.getProgram().getTitle()));
  }

  /** 신청 반려 (관리자). 상태 전이 후 {@link ApplicationRejectedEvent} 발행. */
  @Transactional
  public void reject(Long applicationId, String adminEmail, String reason) {
    Application application = loadWithProgramAndUser(applicationId);
    User admin = loadUser(adminEmail);

    if (application.getStatus() == ApplicationStatus.REJECTED) {
      return; // idempotent
    }
    application.reject(admin, reason);

    eventPublisher.publishEvent(
        new ApplicationRejectedEvent(
            application.getId(),
            application.getUser().getId(),
            application.getProgram().getId(),
            application.getProgram().getTitle(),
            reason));
  }

  /**
   * 신청 취소 (신청자 본인).
   *
   * <p>본인이 아니면 {@link IllegalStateException}. 이미 CANCELLED 이면 no-op.
   */
  @Transactional
  public void cancel(Long applicationId, String userEmail) {
    cancel(applicationId, userEmail, null);
  }

  /** D5: 취소 사유와 함께 신청 취소. reason 은 label + optional 텍스트 조합 문자열. */
  @Transactional
  public void cancel(Long applicationId, String userEmail, String reason) {
    Application application = loadWithProgramAndUser(applicationId);
    User user = loadUser(userEmail);

    if (!application.getUser().getId().equals(user.getId())) {
      throw new IllegalStateException("본인의 신청만 취소할 수 있습니다.");
    }
    if (application.getStatus() == ApplicationStatus.CANCELLED) {
      return; // idempotent
    }
    application.cancel(reason);

    eventPublisher.publishEvent(
        new ApplicationCancelledEvent(
            application.getId(),
            application.getUser().getId(),
            application.getProgram().getId(),
            application.getProgram().getTitle(),
            reason));
  }

  private Application loadWithProgramAndUser(Long applicationId) {
    return applicationRepository
        .findWithProgramAndUserById(applicationId)
        .orElseThrow(() -> new IllegalArgumentException("신청을 찾을 수 없습니다: " + applicationId));
  }

  private User loadUser(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
  }
}
