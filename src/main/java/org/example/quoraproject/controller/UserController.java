package org.example.quoraproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.quoraproject.constant.AppConstants;
import org.example.quoraproject.dto.request.UpdateUserRequest;
import org.example.quoraproject.dto.response.UserResponse;
import org.example.quoraproject.response.ApiResponse;
import org.example.quoraproject.response.PageResponse;
import org.example.quoraproject.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Note the removed endpoints compared to the original:
 * - POST /users (open user creation) -> replaced by /api/v1/auth/signup
 * - POST /users/{userId}/followTag/{tagId} -> replaced by /users/me/tags/{tagId};
 *   the acting user always comes from the SecurityContext, never the URL.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profiles and tag subscriptions")
public class UserController {

    private final UserService userService;

    @Operation(summary = "List all users (admin only)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @PageableDefault(size = AppConstants.DEFAULT_PAGE_SIZE,
                    sort = AppConstants.DEFAULT_SORT_FIELD,
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(pageable)));
    }

    @Operation(summary = "Get the authenticated user's profile")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrentUser()));
    }

    @Operation(summary = "Update the authenticated user's profile")
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated", userService.updateCurrentUser(request)));
    }

    @Operation(summary = "Get a user's public profile")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @Operation(summary = "Delete a user (admin only)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted"));
    }

    @Operation(summary = "Follow a tag as the authenticated user")
    @PostMapping("/me/tags/{tagId}")
    public ResponseEntity<ApiResponse<Void>> followTag(@PathVariable Long tagId) {
        userService.followTag(tagId);
        return ResponseEntity.ok(ApiResponse.success("Tag followed"));
    }

    @Operation(summary = "Unfollow a tag as the authenticated user")
    @DeleteMapping("/me/tags/{tagId}")
    public ResponseEntity<ApiResponse<Void>> unfollowTag(@PathVariable Long tagId) {
        userService.unfollowTag(tagId);
        return ResponseEntity.ok(ApiResponse.success("Tag unfollowed"));
    }
}
