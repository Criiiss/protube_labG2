package com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence;

import com.tecnocampus.LS2.protube_back.profile.application.port.out.ProfileRepository;
import com.tecnocampus.LS2.protube_back.profile.domain.exception.ProfileNotFoundException;
import com.tecnocampus.LS2.protube_back.profile.domain.model.UserProfile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ProfilePersistenceAdapter implements ProfileRepository {
    private final ProfilePersistenceMapper mapper;
    private final SpringDataProfileRepository profileRepository;
    private final SpringDataUserRepository userRepository;

    public ProfilePersistenceAdapter(ProfilePersistenceMapper mapper, SpringDataProfileRepository springDataProfileRepository, SpringDataUserRepository springDataUserRepository) {
        this.mapper = mapper;
        this.profileRepository = springDataProfileRepository;
        this.userRepository = springDataUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfile> findByUsername(String username) {
        return profileRepository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfile> findById(Long id) {
        return profileRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsernameNotID(String username, Long id) {
        return profileRepository.existsByUsernameNotID(username, id);
    }

    @Override
    public UserProfile save(UserProfile userProfile) {
        UserJpaEntity user = userRepository.findById(userProfile.getUserId())
                .orElseThrow(() ->new ProfileNotFoundException(userProfile.getUserId()));
        user.changeUsername(userProfile.getUsername());
        userRepository.save(user);
        ProfileJpaEntity profileEntity = mapper.toEntity(userProfile,user);
        ProfileJpaEntity savedEntity = profileRepository.save(profileEntity);
        return mapper.toDomain(savedEntity);
    }
}
