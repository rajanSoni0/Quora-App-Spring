package org.example.quoraproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.quoraproject.constant.AppConstants;
import org.example.quoraproject.dto.request.CreateCommentRequest;
import org.example.quoraproject.dto.response.CommentResponse;
import org.example.quoraproject.response.ApiResponse;
import org.example.quoraproject.response.PageResponse;
import org.example.quoraproject.service.CommentService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Comments and threaded replies")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "List top-level comments for an answer")
    @SecurityRequirements
    @GetMapping("/answer/{answerId}")
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getCommentsByAnswerId(
            @PathVariable Long answerId,
            @PageableDefault(size = AppConstants.DEFAULT_PAGE_SIZE,
                    sort = AppConstants.DEFAULT_SORT_FIELD,
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getCommentsByAnswerId(answerId, pageable)));
    }

    @Operation(summary = "List replies to a comment")
    @SecurityRequirements
    @GetMapping("/comment/{commentId}")
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getRepliesByCommentId(
            @PathVariable Long commentId,
            @PageableDefault(size = AppConstants.DEFAULT_PAGE_SIZE,
                    sort = AppConstants.DEFAULT_SORT_FIELD,
                    direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getRepliesByCommentId(commentId, pageable)));
    }

    @Operation(summary = "Get a comment by id")
    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentResponse>> getCommentById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getCommentById(id)));
    }

    @Operation(summary = "Post a comment or reply (author = authenticated user)")
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse created = commentService.createComment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment created", created));
    }

    @Operation(summary = "Delete a comment (owner or admin)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.ok(ApiResponse.success("Comment deleted"));
    }
}
