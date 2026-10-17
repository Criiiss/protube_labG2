package com.tecnocampus.LS2.protube_back.profile.adapter.in.web;

import com.tecnocampus.LS2.protube_back.profile.application.port.in.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final GetMyProfileUseCase getMyProfileUseCase;
    private final GetPublicProfileUseCase getPublicProfileUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final ChangeUserNameUseCase changeUserNameUseCase;
    private final UpdatePrivacySettingsUseCase updatePrivacySettingsUseCase;
    private final ProfileWebMapper mapper;

    public ProfileController(GetMyProfileUseCase getMyProfileUseCase, GetPublicProfileUseCase getPublicProfileUseCase, UpdateProfileUseCase updateProfileUseCase, ChangeUserNameUseCase changeUserNameUseCase, UpdatePrivacySettingsUseCase updatePrivacySettingsUseCase, ProfileWebMapper mapper) {
        this.getMyProfileUseCase = getMyProfileUseCase;
        this.getPublicProfileUseCase = getPublicProfileUseCase;
        this.updateProfileUseCase = updateProfileUseCase;
        this.changeUserNameUseCase = changeUserNameUseCase;
        this.updatePrivacySettingsUseCase = updatePrivacySettingsUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/me")
    public ResponseEntity<MyProfileResponseDTO> getMyProfile(@RequestParam Long userId) {
        return ResponseEntity.ok(mapper.toMyProfileResponse(getMyProfileUseCase.getMyProfile(userId)));
    }

    @GetMapping("/{username}")
    public ResponseEntity<ProfileResponseDTO> getPublicProfile(@PathVariable String username) {
        return ResponseEntity.ok(mapper.toProfileResponse(getPublicProfileUseCase.getPublicProfile(username)));
    }

    @PatchMapping("/me")
    public ResponseEntity<MyProfileResponseDTO> updateProfile(@RequestParam Long userId, @RequestBody UpdateProfileRequestDTO request) {
        return ResponseEntity.ok(mapper.toMyProfileResponse(updateProfileUseCase.updateProfile(userId, mapper.toUpdateProfileCommand(request))));
    }

    @PatchMapping("/me/username")
    public ResponseEntity<MyProfileResponseDTO> changeUserName(@RequestParam Long userId, @RequestBody ChangeUsernameRequestDTO request) {
        return ResponseEntity.ok(mapper.toMyProfileResponse(changeUserNameUseCase.changeUserName(userId, mapper.toChangeUserNameCommand(request))));
    }

    @PatchMapping("/me/privacy")
    public ResponseEntity<MyProfileResponseDTO> updatePrivacySettings(@RequestParam Long userId, @RequestBody UpdatePrivacySettingsRequestDTO request) {
        return ResponseEntity.ok(mapper.toMyProfileResponse(updatePrivacySettingsUseCase.updatePrivacySettings(userId, mapper.toUpdatePrivacySettingsCommand(request))));
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<MyProfileResponseDTO> removeAvatar(@RequestParam Long userId) {
        return ResponseEntity.ok(mapper.toMyProfileResponse(updateProfileUseCase.updateProfile(userId, mapper.toUpdateProfileCommand(new UpdateProfileRequestDTO(null, null, null, null)))));
    }

}
