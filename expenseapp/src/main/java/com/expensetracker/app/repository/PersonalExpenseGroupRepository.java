package com.expensetracker.app.repository;

import com.expensetracker.app.entity.PersonalExpenseGroup;
import com.expensetracker.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalExpenseGroupRepository extends JpaRepository<PersonalExpenseGroup, Long> {
    
    List<PersonalExpenseGroup> findByUser(User user);
    
    List<PersonalExpenseGroup> findByUserId(Long userId);
    
    List<PersonalExpenseGroup> findByUserIdAndFromDateAfterAndToDateBefore(
            Long userId, OffsetDateTime startDate, OffsetDateTime endDate);
    
    @Query("SELECT peg FROM PersonalExpenseGroup peg LEFT JOIN FETCH peg.expenses WHERE peg.id = :groupId")
    Optional<PersonalExpenseGroup> findByIdWithExpenses(@Param("groupId") Long groupId);
    
    @Query("SELECT peg FROM PersonalExpenseGroup peg LEFT JOIN FETCH peg.expenses WHERE peg.user.id = :userId")
    List<PersonalExpenseGroup> findByUserIdWithExpenses(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(peg) FROM PersonalExpenseGroup peg WHERE peg.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    Boolean existsByUserIdAndTitle(Long userId, String title);
    
    List<PersonalExpenseGroup> findByUserIdAndTitleContainingIgnoreCase(Long userId, String title);
}