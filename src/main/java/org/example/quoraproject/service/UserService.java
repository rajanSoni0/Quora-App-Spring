package org.example.quoraproject.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.quoraproject.dto.request.UpdateUserRequest;
import org.example.quoraproject.dto.response.UserResponse;
import org.example.quoraproject.entity.Tag;
import org.example.quoraproject.entity.User;
import org.example.quoraproject.exception.DuplicateResourceException;
import org.example.quoraproject.exception.ResourceNotFoundException;
import org.example.quoraproject.exception.UserNotFoundException;
import org.example.quoraproject.mapper.UserMapper;
import org.example.quoraproject.repository.TagRepository;
import org.example.quoraproject.repository.UserRepository;
import org.example.quoraproject.response.PageResponse;
import org.example.quoraproject.util.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User management. Note that user creation now lives exclusively in
 * AuthService.signup; there is no unauthenticated "create user" CRUD path.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final UserMapper userMapper;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable), userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        return getUserById(SecurityUtils.getCurrentUser().getId());
    }

    /**
     * Users update only their own profile; the target is always the
     * authenticated principal.
     */
    @Transactional
    public UserResponse updateCurrentUser(UpdateUserRequest request) {
        User user = requireUser(SecurityUtils.getCurrentUser().getId());

        if (request.email() != null && !request.email().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new DuplicateResourceException("Email is already registered");
            }
            user.setEmail(request.email());
        }
        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Destructive account removal is an admin-only operation.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteUser(Long id) {
        User user = requireUser(id);
        userRepository.delete(user);
        log.info("Admin {} deleted user id {}", SecurityUtils.getCurrentUsername().orElse("?"), id);
    }

    @Transactional
    public void followTag(Long tagId) {
        User user = requireUser(SecurityUtils.getCurrentUser().getId());
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", tagId));
        user.getFollowedTags().add(tag);
        userRepository.save(user);
        log.debug("User id {} followed tag id {}", user.getId(), tagId);
    }

    @Transactional
    public void unfollowTag(Long tagId) {
        User user = requireUser(SecurityUtils.getCurrentUser().getId());
        user.getFollowedTags().removeIf(tag -> tag.getId().equals(tagId));
        userRepository.save(user);
    }

    private User requireUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
