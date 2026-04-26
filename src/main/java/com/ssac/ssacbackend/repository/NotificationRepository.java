package com.ssac.ssacbackend.repository;

import com.ssac.ssacbackend.domain.notification.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 알림 데이터 접근 인터페이스.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserEmailOrderByCreatedAtDesc(String email);

    long countByUserEmailAndIsReadFalse(String email);

    Optional<Notification> findByIdAndUserEmail(Long id, String email);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.email = :email")
    void markAllAsReadByUserEmail(@Param("email") String email);
}
