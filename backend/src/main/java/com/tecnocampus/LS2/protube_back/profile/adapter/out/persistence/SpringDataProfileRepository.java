package com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataProfileRepository extends JpaRepository<ProfileJpaEntity, Long> {
    Optional<ProfileJpaEntity> findByUserName(String userName);

    boolean existsByUserNameNotID(String userName, Long id);
}
