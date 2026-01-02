package com.expensetracker.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet; // Import for initializing Set
import java.util.Set; 

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
// Keep @EqualsAndHashCode(onlyExplicitlyIncluded = true) for JPA safety
@EqualsAndHashCode(onlyExplicitlyIncluded = true) 
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include // <-- Only the ID is used for hash/equals
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    // 🛑 CRITICAL FIX: UNCOMMENT/ADD THE RELATIONSHIP FIELD 🛑
    // 'mappedBy' points to the 'members' field in the Group entity.
    @ManyToMany(mappedBy = "members", fetch = FetchType.LAZY) 
    private Set<Group> groups = new HashSet<>(); 

    // --- Lombok generates the Getter/Setter ---
    // Lombok's @Getter and @Setter annotations at the class level automatically generate
    // the required public Set<Group> getGroups() and public void setGroups(Set<Group> groups).
    // If you were not using Lombok, you'd have to write these methods manually.
    // Since you are using @Getter, the 'getGroups()' method IS defined now.
    
    // --- UserDetails Implementation (Required for Spring Security) ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}