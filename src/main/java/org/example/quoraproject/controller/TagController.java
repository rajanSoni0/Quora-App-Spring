package org.example.quoraproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.quoraproject.constant.AppConstants;
import org.example.quoraproject.dto.request.CreateTagRequest;
import org.example.quoraproject.dto.response.TagResponse;
import org.example.quoraproject.response.ApiResponse;
import org.example.quoraproject.response.PageResponse;
import org.example.quoraproject.service.TagService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags", description = "Tag taxonomy")
public class TagController {

    private final TagService tagService;

    @Operation(summary = "List tags with optional name filter")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TagResponse>>> getAllTags(
            @RequestParam(required = false) String name,
            @PageableDefault(size = AppConstants.DEFAULT_PAGE_SIZE, sort = "name",
                    direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(tagService.getAllTags(name, pageable)));
    }

    @Operation(summary = "Get a tag by id")
    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TagResponse>> getTagById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(tagService.getTagById(id)));
    }

    @Operation(summary = "Create a tag (authenticated)")
    @PostMapping
    public ResponseEntity<ApiResponse<TagResponse>> createTag(@Valid @RequestBody CreateTagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tag created", tagService.createTag(request)));
    }

    @Operation(summary = "Delete a tag (admin only)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.ok(ApiResponse.success("Tag deleted"));
    }
}
