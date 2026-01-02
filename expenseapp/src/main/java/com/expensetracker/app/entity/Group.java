package com.expensetracker.app.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "groups")
@Getter // Use explicit Getters
@Setter // Use explicit Setters
@NoArgsConstructor
@AllArgsConstructor
@Builder
// *** CRUCIAL FIX: Only include the ID for equality/hashing ***
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true) // Optional: makes logging safer
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include // <-- Only the ID is used for hash/equals
    @ToString.Include
    private Long id;

    @Column(nullable = false, length = 100)
    @ToString.Include
    private String name;

    @Column(length = 500)
    @ToString.Include
    private String description;

    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        // Ensuring timezone is set correctly, though best practice is usually UTC and
        // then handle TZ in presentation layer
        this.createdAt = OffsetDateTime.now(ZoneOffset.ofHoursMinutes(5, 30));
    }

    // The user who created this group
    // The User entity must also have its equals/hashCode fixed as previously
    // discussed.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    // Many-to-many with users (group members)
    // Removed CascadeType.PERSIST for safety, as we fetch existing members.
    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE })
    @JoinTable(name = "group_members", joinColumns = @JoinColumn(name = "group_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<User> members = new HashSet<>();

    // Group.java (Entity)

    public boolean isMember(String username) {
        if (username == null || members == null) {
            return false;
        }

        // 1. Normalize the incoming username from the JWT/Controller
        final String normalizedUsername = username.trim();

        // 2. Search the members, trimming their stored username as well
        return members.stream()
                .anyMatch(user -> {
                    String memberUsername = user != null ? user.getUsername() : null;

                    // CRITICAL FINAL CHECK: Ensure memberUsername is not null, then TRIM and
                    // compare case-insensitively.
                    return memberUsername != null &&
                            memberUsername.trim().equalsIgnoreCase(normalizedUsername);
                });
    }
}