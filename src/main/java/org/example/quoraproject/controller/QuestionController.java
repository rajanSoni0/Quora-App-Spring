package org.example.quoraproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.quoraproject.constant.AppConstants;
import org.example.quoraproject.dto.request.CreateQuestionRequest;
import org.example.quoraproject.dto.request.UpdateQuestionRequest;
import org.example.quoraproject.dto.response.QuestionResponse;
import org.example.quoraproject.response.ApiResponse;
import org.example.quoraproject.response.PageResponse;
import org.example.quoraproject.service.QuestionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Tag(name = "Questions", description = "Question CRUD with search, filtering and pagination")
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "List questions with optional keyword search and tag filter")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<QuestionResponse>>> getQuestions(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long tagId,
            @PageableDefault(size = AppConstants.DEFAULT_PAGE_SIZE,
                    sort = AppConstants.DEFAULT_SORT_FIELD,
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(questionService.getQuestions(search, tagId, pageable)));
    }

    @Operation(summary = "Get a question by id")
    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> getQuestionById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(questionService.getQuestionById(id)));
    }

    @Operation(summary = "Ask a question (author = authenticated user)")
    @PostMapping
    public ResponseEntity<ApiResponse<QuestionResponse>> createQuestion(
            @Valid @RequestBody CreateQuestionRequest request) {
        QuestionResponse created = questionService.createQuestion(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Question created", created));
    }

    @Operation(summary = "Update a question (owner or admin)")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> updateQuestion(
            @PathVariable Long id, @Valid @RequestBody UpdateQuestionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Question updated", questionService.updateQuestion(id, request)));
    }

    @Operation(summary = "Delete a question (owner or admin)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.ok(ApiResponse.success("Question deleted"));
    }
}
