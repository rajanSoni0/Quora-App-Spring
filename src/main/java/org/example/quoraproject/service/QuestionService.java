package org.example.quoraproject.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.quoraproject.dto.request.CreateQuestionRequest;
import org.example.quoraproject.dto.request.UpdateQuestionRequest;
import org.example.quoraproject.dto.response.QuestionResponse;
import org.example.quoraproject.entity.Question;
import org.example.quoraproject.entity.Tag;
import org.example.quoraproject.entity.User;
import org.example.quoraproject.exception.ForbiddenException;
import org.example.quoraproject.exception.ResourceNotFoundException;
import org.example.quoraproject.exception.UserNotFoundException;
import org.example.quoraproject.mapper.QuestionMapper;
import org.example.quoraproject.repository.QuestionRepository;
import org.example.quoraproject.repository.TagRepository;
import org.example.quoraproject.repository.UserRepository;
import org.example.quoraproject.response.PageResponse;
import org.example.quoraproject.security.CustomUserDetails;
import org.example.quoraproject.util.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final QuestionMapper questionMapper;

    /**
     * Paginated listing with optional full-text-ish search and tag filter.
     */
    @Transactional(readOnly = true)
    public PageResponse<QuestionResponse> getQuestions(String search, Long tagId, Pageable pageable) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        return PageResponse.from(
                questionRepository.search(normalizedSearch, tagId, pageable),
                questionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public QuestionResponse getQuestionById(Long id) {
        return questionMapper.toResponse(requireQuestion(id));
    }

    /**
     * The author is the authenticated principal - never a client-supplied id.
     */
    @Transactional
    public QuestionResponse createQuestion(CreateQuestionRequest request) {
        CustomUserDetails principal = SecurityUtils.getCurrentUser();
        User author = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UserNotFoundException(principal.getId()));

        Question question = new Question();
        question.setTitle(request.title());
        question.setContent(request.content());
        question.setUser(author);
        question.setTags(resolveTags(request.tagIds()));

        Question saved = questionRepository.save(question);
        log.info("User id {} created question id {}", author.getId(), saved.getId());
        return questionMapper.toResponse(saved);
    }

    @Transactional
    public QuestionResponse updateQuestion(Long id, UpdateQuestionRequest request) {
        Question question = requireQuestion(id);
        assertOwnerOrAdmin(question);

        if (request.title() != null) {
            question.setTitle(request.title());
        }
        if (request.content() != null) {
            question.setContent(request.content());
        }
        if (request.tagIds() != null && !request.tagIds().isEmpty()) {
            question.setTags(resolveTags(request.tagIds()));
        }
        return questionMapper.toResponse(questionRepository.save(question));
    }

    @Transactional
    public void deleteQuestion(Long id) {
        Question question = requireQuestion(id);
        assertOwnerOrAdmin(question);
        questionRepository.delete(question);
        log.info("Question id {} deleted by user id {}", id, SecurityUtils.getCurrentUser().getId());
    }

    private Question requireQuestion(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question", id));
    }

    /**
     * Ownership rule: only the author or an admin may modify a question.
     */
    private void assertOwnerOrAdmin(Question question) {
        CustomUserDetails principal = SecurityUtils.getCurrentUser();
        boolean isOwner = question.getUser().getId().equals(principal.getId());
        if (!isOwner && !principal.isAdmin()) {
            throw new ForbiddenException("You can only modify your own questions");
        }
    }

    /**
     * Unlike the original silent-skip behavior, unknown tag ids now fail
     * loudly so clients are not left with partially-tagged questions.
     */
    private Set<Tag> resolveTags(Set<Long> tagIds) {
        return tagIds.stream()
                .map(tagId -> tagRepository.findById(tagId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tag", tagId)))
                .collect(java.util.stream.Collectors.toSet());
    }
}
