package ru.timchat.user.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.timchat.common.error.NotFoundException;
import ru.timchat.user.api.UpdateProfileRequest;
import ru.timchat.user.api.UserProfileResponse;
import ru.timchat.user.domain.User;
import ru.timchat.user.domain.UserRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  @Transactional
  public User getOrCreateUser(String externalId, String username,
      String email) {
    return userRepository.findByExternalId(externalId)
        .orElseGet(() -> {
          log.info("Provisioning new user: externalId={}, username={}",
              externalId, username);
          var user = new User(externalId, username, email);
          return userRepository.save(user);
        });
  }

  @Transactional(readOnly = true)
  public UserProfileResponse getProfile(UUID userId) {
    var user = findUserOrThrow(userId);
    return toProfileResponse(user);
  }

  @Transactional
  public UserProfileResponse updateProfile(UUID userId,
      UpdateProfileRequest request) {
    var user = findUserOrThrow(userId);
    var profile = user.getProfile();
    if (request.displayName() != null) {
      profile.updateDisplayName(request.displayName());
    }
    if (request.avatarUrl() != null) {
      profile.updateAvatarUrl(request.avatarUrl());
    }
    if (request.statusText() != null) {
      profile.updateStatusText(request.statusText());
    }
    userRepository.save(user);
    return toProfileResponse(user);
  }

  @Transactional(readOnly = true)
  public User findByExternalId(String externalId) {
    return userRepository.findByExternalId(externalId)
        .orElseThrow(() -> new NotFoundException(
            "error.user.not-found"));
  }

  @Transactional(readOnly = true)
  public User findById(UUID userId) {
    return findUserOrThrow(userId);
  }

  private User findUserOrThrow(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException(
            "error.user.not-found"));
  }

  private UserProfileResponse toProfileResponse(User user) {
    var profile = user.getProfile();
    return new UserProfileResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        profile != null ? profile.getDisplayName() : user.getUsername(),
        profile != null ? profile.getAvatarUrl() : null,
        profile != null ? profile.getStatusText() : null
    );
  }
}
