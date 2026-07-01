package io.github.sihyuuun.youthmoa.program;

import lombok.Getter;

/**
 * 프로그램 카드 표시용 DTO.
 * Program 엔티티에 실시간 신청자 수(applicantCount)를 조합한 뷰 모델.
 */
@Getter
public class ProgramCardDto {

    private final Program program;
    private final long applicantCount;

    // CapacityBar 계산값 (서비스에서 미리 계산)
    private final int pct;
    private final String colorClass;
    private final String barLabel;
    private final String capacityText;

    public ProgramCardDto(Program program, long applicantCount) {
        this.program = program;
        this.applicantCount = applicantCount;

        ProgramStatus status = program.getStatus();
        Integer capacity = program.getCapacity();

        if (status == ProgramStatus.UPCOMING) {
            this.pct = 0;
            this.colorClass = "secondary";
            this.barLabel = "신청 오픈 예정";
        } else if (status == ProgramStatus.CLOSED) {
            this.pct = 100;
            this.colorClass = "muted";
            this.barLabel = "모집 마감";
        } else if (capacity == null || capacity == 0) {
            this.pct = 0;
            this.colorClass = "primary";
            this.barLabel = "모집중";
        } else {
            double ratio = (double) applicantCount / capacity;
            this.pct = Math.min(100, (int) Math.round(ratio * 100));
            if (ratio >= 0.9) {
                this.colorClass = "error";
                this.barLabel = "마감임박";
            } else if (ratio >= 0.7) {
                this.colorClass = "warning";
                this.barLabel = "서두르세요";
            } else {
                this.colorClass = "primary";
                this.barLabel = "모집중";
            }
        }

        if (capacity == null) {
            this.capacityText = "정원 제한 없음";
        } else {
            this.capacityText = "정원 " + applicantCount + "/" + capacity + "명";
        }
    }

    // Program 위임 접근자 — 템플릿에서 dto.id, dto.title 등으로 접근
    public Long getId() { return program.getId(); }
    public String getTitle() { return program.getTitle(); }
    public String getOrganization() { return program.getOrganization(); }
    public String getRegion() { return program.getRegion(); }
    public String getImageUrl() { return program.getImageUrl(); }
    public Integer getCapacity() { return program.getCapacity(); }
    public ProgramStatus getStatus() { return program.getStatus(); }
    public String getDdayLabel() { return program.getDdayLabel(); }
    public long getDaysUntilDeadline() { return program.getDaysUntilDeadline(); }
    public java.time.LocalDate getStartDate() { return program.getStartDate(); }
    public java.time.LocalDate getEndDate() { return program.getEndDate(); }
}
