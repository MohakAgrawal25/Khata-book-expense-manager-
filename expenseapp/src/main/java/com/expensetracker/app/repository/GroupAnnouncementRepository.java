package com.expensetracker.app.repository;

import com.expensetracker.app.entity.GroupAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing GroupAnnouncement entities.
 * Extends JpaRepository to inherit standard CRUD operations.
 */
@Repository
public interface GroupAnnouncementRepository extends JpaRepository<GroupAnnouncement, Long> {

    /**
     * Custom query method to find all announcements belonging to a specific group,
     * ordered by creation date descending (most recent first).
     *
     * @param groupId The ID of the Group.
     * @return A list of GroupAnnouncement entities.
     */
    List<GroupAnnouncement> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    /**
     * Custom query method to find all announcements created by a specific user,
     * ordered by creation date descending.
     *
     * @param createdByUserId The ID of the User who created the announcement.
     * @return A list of GroupAnnouncement entities.
     */
    List<GroupAnnouncement> findByCreatedByIdOrderByCreatedAtDesc(Long createdByUserId);
}