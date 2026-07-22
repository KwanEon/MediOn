package com.example.medicalsearch.repository;

import com.example.medicalsearch.entity.UserFavorite;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {

    @Query("""
            select favorite.institutionId
            from UserFavorite favorite
            where favorite.userId = :userId
            order by favorite.createdAt desc
            """)
    List<Long> findInstitutionIdsByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndInstitutionId(Long userId, Long institutionId);

    void deleteByUserIdAndInstitutionId(Long userId, Long institutionId);
}
