package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.user.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사용자 데이터 접근 인터페이스.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);
}
