package com.example.medicalsearch.repository;

import com.example.medicalsearch.entity.AppUser;
import com.example.medicalsearch.entity.UserRole;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    long countByRole(UserRole role);

    long countByCreatedAtAfter(LocalDateTime createdAt);

    @Query("""
            select user
            from AppUser user
            where :query = ''
               or lower(user.username) like lower(concat('%', :query, '%'))
               or lower(user.name) like lower(concat('%', :query, '%'))
               or lower(user.email) like lower(concat('%', :query, '%'))
               or user.phoneNumber like concat('%', :query, '%')
            """)
    Page<AppUser> searchForDeveloper(@Param("query") String query, Pageable pageable);
}
