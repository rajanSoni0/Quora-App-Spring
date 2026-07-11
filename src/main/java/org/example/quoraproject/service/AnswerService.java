package org.example.quoraproject.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.quoraproject.dto.request.CreateAnswerRequest;
import org.example.quoraproject.dto.request.UpdateAnswerRequest;
import org.example.quoraproject.dto.response.AnswerResponse;
import org.example.quoraproject.entity.Answer;
import org.example.quoraproject.entity.Question;
import org.example.quoraproject.entity.User;
import org.example.quoraproject.exception.ForbiddenException;
import org.example.quoraproject.exception.ResourceNotFoundException;
import org.example.quoraproject.exception.UserNotFoundException;
import org.example.quoraproject.mapper.AnswerMapper;
import org.example.quoraproject.repository.AnswerRepository;
import org.example.quoraproject.repository.QuestionRepository;
import org.example.quoraproject.repository.UserRepository;
import org.example.quoraproject.response.PageResponse;
import org.example.quoraproject.security.CustomUserDetails;
import org.example.quoraproject.util.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final AnswerMapper answerMapper;

    @Transactional(readOnly = true)
    public PageResponse<AnswerResponse> getAnswersByQuestionId(Long questionId, Pageable pageable) {
        if (!questionRepository.existsById(questionId)) {
            throw new ResourceNotFoundException("Question", questionId);
        }
        return PageResponse.from(answerRepository.findByQuestionId(questionId, pageable), answerMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AnswerResponse getAnswerById(Long id) {
        return answerMapper.toResponse(requireAnswer(id));
    }

    @Transactional
    public AnswerResponse createAnswer(CreateAnswerRequest request) {
        CustomUserDetails principal = SecurityUtils.getCurrentUser();
        User author = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UserNotFoundException(principal.getId()));
        Question question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question", request.questionId()));

        Answer answer = new Answer();
        answer.setContent(request.content());
        answer.setQuestion(question);
        answer.setUser(author);

        Answer saved = answerRepository.save(answer);
        log.info("User id {} answered question id {}", author.getId(), question.getId());
        return answerMapper.toResponse(saved);
    }

    @Transactional
    public AnswerResponse updateAnswer(Long id, UpdateAnswerRequest request) {
        Answer answer = requireAnswer(id);
        assertOwnerOrAdmin(answer);
        answer.setContent(request.content());
        return answerMapper.toResponse(answerRepository.save(answer));
    }

    @Transactional
    public void deleteAnswer(Long id) {
        Answer answer = requireAnswer(id);
        assertOwnerOrAdmin(answer);
        answerRepository.delete(answer);
        log.info("Answer id {} deleted by user id {}", id, SecurityUtils.getCurrentUser().getId());
    }

    private Answer requireAnswer(Long id) {
        return answerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Answer", id));
    }

    private void assertOwnerOrAdmin(Answer answer) {
        CustomUserDetails principal = SecurityUtils.getCurrentUser();
        boolean isOwner = answer.getUser().getId().equals(principal.getId());
        if (!isOwner && !principal.isAdmin()) {
            throw new ForbiddenException("You can only modify your own answers");
        }
    }
}
