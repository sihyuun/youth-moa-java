package io.github.sihyuuun.youthmoa.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {
  Optional<PhoneVerification> findByPhone(String phone);
}
