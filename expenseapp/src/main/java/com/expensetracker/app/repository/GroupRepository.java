package com.expensetracker.app.repository;

import com.expensetracker.app.entity.Group;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    
    /**
     * Retrieves all groups for a given memberId, eagerly fetching both the 'createdBy'
     * user and all 'members' of the group in a single query to solve the N+1 problem.
     * * NOTE: The crucial fix is ensuring 'DISTINCT' is used and simplifying the join
     * structure to perform the necessary inner join for filtering AND the fetch join.
     */
    @Query("SELECT DISTINCT g FROM Group g " +
           // Inner join to filter groups the user is a member of (aliased 'm')
           "JOIN g.members m " + 
           // Fetch join to load the creator (EAGER, but good practice to explicitly fetch)
           "LEFT JOIN FETCH g.createdBy cb " +
           // Fetch join to load ALL members for each group (solves N+1)
           "LEFT JOIN FETCH g.members " + 
           "WHERE m.id = :memberId")
    List<Group> findGroupsByMemberIdWithDetails(@Param("memberId") Long memberId);

    // This method is now obsolete for fetching the group list due to the N+1 issue:
    // List<Group> findGroupsByMembers_Id(Long memberId);
    
    // Find groups created by a specific user
    List<Group> findByCreatedById(Long userId);

    @Override
    @EntityGraph(attributePaths = {"members"})
    Optional<Group> findById(Long id);

    // Eagerly fetch group with members for authorization checks
    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.members WHERE g.id = :groupId")
    Optional<Group> findByIdWithMembers(@Param("groupId") Long groupId);
}