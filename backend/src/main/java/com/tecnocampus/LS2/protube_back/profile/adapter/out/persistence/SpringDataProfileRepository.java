package com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpringDataProfileRepository extends JpaRepository<ProfileJpaEntity, Long> {
    @Query("""
            SELECT profile
            FROM ProfileJpaEntity profile
            JOIN profile.user user
            WHERE user.username = :username
            """)
    Optional<ProfileJpaEntity> findByUsername(@Param("username") String username);

    @Query("""
             SELECT CASE WHEN COUNT(profile) > 0 THEN true ELSE false END
             FROM ProfileJpaEntity profile
             JOIN profile.user user
             WHERE user.username = :username
             AND profile.userId <> :userId
            """)
    boolean existsByUsernameNotID(@Param("username") String username, @Param("userId") Long id);


}
