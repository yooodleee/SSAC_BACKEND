package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.auth.AdminCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminCodeRepository extends JpaRepository<AdminCode, Long> {

    Optional<AdminCode> findByCodeHash(String codeHash);
}
