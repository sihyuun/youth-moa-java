package io.github.sihyuuun.youthmoa.user;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAgreementRepository extends JpaRepository<UserAgreement, Long> {

  /** 특정 사용자의 동의 이력 전체 조회 (감사·분쟁 대응). 최근순 필요 시 sort 파라미터로 처리. */
  List<UserAgreement> findByUser(User user);

  /**
   * 회원 탈퇴 시 FK 제약 회피용 — User 삭제 전에 이력 청소. 실제 서비스 운용상 이력을 지우는 것이 옳은지는 별도 정책 결정 사안이지만, 현재
   * UserService.withdraw 가 관련 하위 데이터를 모두 삭제하는 방식과 정합하도록 우선 삭제 시맨틱을 따른다.
   */
  void deleteAllByUser(User user);
}
