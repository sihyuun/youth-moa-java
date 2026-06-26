package io.github.sihyuuun.youthmoa.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplyRequest {

    @NotBlank(message = "신청 사유를 입력해주세요.")
    @Size(min = 10, max = 1000, message = "신청 사유는 10자 이상 1000자 이하로 작성해주세요.")
    private String applyReason;
}
