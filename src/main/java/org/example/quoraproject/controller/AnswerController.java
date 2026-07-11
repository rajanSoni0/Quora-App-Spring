package org.example.quoraproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.quoraproject.constant.AppConstants;
import org.example.quoraproject.dto.request.CreateAnswerRequest;
import org.example.quoraproject.dto.request.UpdateAnswerRequest;
import org.example.quoraproject.dto.response.AnswerResponse;
import org.example.quoraproject.response.ApiResponse;
import org.example.quoraproject.response.PageResponse;
import org.example.quoraproject.service.AnswerService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/answers")
@RequiredArgsConstructor
@Tag(name = "Answers", description = "Answer CRUD with pagination")
public class AnswerController {

    private final AnswerService answerService;

    @Operation(summary = "List answers for a question")
    @SecurityRequirements
    @GetMapping("/question/{questionId}")
    public ResponseEntity<ApiResponse<PageResponse<AnswerResponse>>> getAnswersByQuestionId(
            @PathVariable Long questionId,
            @PageableDefault(size = AppConstants.DEFAULT_PAGE_SIZE,
                    sort = AppConstants.DEFAULT_SORT_FIELD,
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(answerService.getAnswersByQuestionId(questionId, pageable)));
    }

    @Operation(summary = "Get an answer by id")
    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AnswerResponse>> getAnswerById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(answerService.getAnswerById(id)));
    }

    @Operation(summary = "Post an answer (author = authenticated user)")
    @PostMapping
    public ResponseEntity<ApiResponse<AnswerResponse>> createAnswer(
            @Valid @RequestBody CreateAnswerRequest request) {
        AnswerResponse created = answerService.createAnswer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Answer created", created));
    }

    @Operation(summary = "Update an answer (owner or admin)")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AnswerResponse>> updateAnswer(
            @PathVariable Long id, @Valid @RequestBody UpdateAnswerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Answer updated", answerService.updateAnswer(id, request)));
    }

    @Operation(summary = "Delete an answer (owner or admin)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAnswer(@PathVariable Long id) {
        answerService.deleteAnswer(id);
        return ResponseEntity.ok(ApiResponse.success("Answer deleted"));
    }
}
