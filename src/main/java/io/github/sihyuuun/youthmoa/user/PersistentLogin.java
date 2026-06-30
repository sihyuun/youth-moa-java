package io.github.sihyuuun.youthmoa.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Spring Security remember-me (PersistentTokenBasedRememberMeServices) 의 토큰 저장 테이블.
 * <p>
 * Spring 표준 스키마 (JdbcTokenRepositoryImpl) 와 동일한 컬럼·타입.
 * JdbcTokenRepositoryImpl 이 SQL 로 직접 read/write 하지만, ddl-auto 자동 관리를 위해
 * Entity 로 모델링.
 * </p>
 * <ul>
 *   <li>series — PRIMARY KEY. 사용자별 고유 series</li>
 *   <li>token — 매 사용 시 새로 발급 (rotation)</li>
 *   <li>같은 series 의 옛 token 사용 시 도난 의심 → 전체 무효화 (Spring 자동 처리)</li>
 * </ul>
 */
@Entity
@Table(name = "persistent_logins")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersistentLogin {

    @Id
    @Column(length = 64)
    private String series;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 64)
    private String token;

    @Column(name = "last_used", nullable = false)
    private LocalDateTime lastUsed;
}
