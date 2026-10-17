package com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence;

import com.tecnocampus.LS2.protube_back.profile.application.port.out.ProfileRepository;
import com.tecnocampus.LS2.protube_back.profile.domain.model.UserProfile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ProfilePersistenceAdapter implements ProfileRepository {
    private final ProfilePersistenceMapper mapper;
    private final SpringDataProfileRepository repository;

    public ProfilePersistenceAdapter(ProfilePersistenceMapper mapper, SpringDataProfileRepository springDataProfileRepository) {
        this.mapper = mapper;
        this.repository = springDataProfileRepository;
    }

    @Override
    public Optional<UserProfile> findByUsername(String username) {
        return repository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public Optional<UserProfile> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUsernameNotID(String username, Long id) {
        return repository.existsByUsernameNotID(username, id);
    }

    @Override
    public UserProfile save(UserProfile userProfile) {
        ProfileJpaEntity entity = mapper.toEntity(userProfile);
        ProfileJpaEntity savedEntity = repository.save(entity);
        return mapper.toDomain(savedEntity);
    }
}
