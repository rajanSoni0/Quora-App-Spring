package org.example.quoraproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.quoraproject.constant.AppConstants;
import org.example.quoraproject.dto.response.QuestionResponse;
import org.example.quoraproject.response.ApiResponse;
import org.example.quoraproject.response.PageResponse;
import org.example.quoraproject.service.UserFeedService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Old contract: GET /api/v1/feed/{userId} - anyone could fetch anyone's
 * feed. New contract: GET /api/v1/feed - the feed of the authenticated user.
 */
@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
@Tag(name = "Feed", description = "Personalized question feed")
public class UserFeedController {

    private final UserFeedService userFeedService;

    @Operation(summary = "Get the authenticated user's feed based on followed tags")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<QuestionResponse>>> getFeed(
            @PageableDefault(size = AppConstants.DEFAULT_PAGE_SIZE,
                    sort = AppConstants.DEFAULT_SORT_FIELD,
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(userFeedService.getCurrentUserFeed(pageable)));
    }
}
