package com.expensetracker.app.repository;

import com.expensetracker.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);
    
    // ⚠️ Recommended: Remove this redundant line or the one below it.
    // List<User> findByUsernameIn(List<String> usernames); 

    /**
     * Finds all users whose usernames are in the provided collection/set.
     * This is the correct method for efficient batch lookup in the GroupService.
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) IN :usernames")
    List<User> findAllByUsernameIn(Collection<String> usernames);
    
    // Alternatively, if you prefer to be explicit about the Set:
    // List<User> findAllByUsernameIn(Set<String> usernames); 
}