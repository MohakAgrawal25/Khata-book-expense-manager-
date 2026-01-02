package com.expensetracker.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "group_announcements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupAnnouncement {

    /**
     * Unique identifier for the announcement.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The Group this announcement belongs to.
     * Many announcements can belong to one group (ManyToOne).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    /**
     * The User who created the announcement.
     * Many announcements can be created by one user (ManyToOne).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    /**
     * The content of the announcement.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * A short, optional title for the announcement.
     */
    @Column(length = 255)
    private String title;

    /**
     * Timestamp for when the announcement was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Timestamp for the last time the announcement was updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    // You could also add fields like:
    // private boolean isPinned; // To keep important announcements at the top
}