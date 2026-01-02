package com.expensetracker.app.repository;

import com.expensetracker.app.entity.Group;
import com.expensetracker.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupListingRepository extends JpaRepository<Group, Long> {

    /**
     * Finds all Group entities where the given User is present in the 'members' collection.
     * This is the core query method for the listing feature.
     */
    List<Group> findAllByMembersContaining(User user);
}