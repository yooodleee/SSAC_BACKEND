package com.ssac.ssacbackend.domain.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메뉴 클릭 이벤트 도메인 엔티티.
 */
@Entity
@Table(name = "menu_click_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, length = 100)
    private String menuId;

    @Column(nullable = false, length = 200)
    private String menuName;

    @Column(length = 100)
    private String userId;

    @Column(length = 100)
    private String guestId;

    @Column(nullable = false)
    private LocalDateTime clickedAt;

    @Column(nullable = false, length = 200)
    private String pageContext;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public MenuClickEvent(String eventType, String menuId, String menuName,
        String userId, String guestId, LocalDateTime clickedAt, String pageContext) {
        this.eventType = eventType;
        this.menuId = menuId;
        this.menuName = menuName;
        this.userId = userId;
        this.guestId = guestId;
        this.clickedAt = clickedAt;
        this.pageContext = pageContext;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
