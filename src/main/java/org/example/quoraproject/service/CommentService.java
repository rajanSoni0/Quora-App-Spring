package org.example.quoraproject.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.quoraproject.dto.request.CreateCommentRequest;
import org.example.quoraproject.dto.response.CommentResponse;
import org.example.quoraproject.entity.Answer;
import org.example.quoraproject.entity.Comment;
import org.example.quoraproject.entity.User;
import org.example.quoraproject.exception.ForbiddenException;
import org.example.quoraproject.exception.ResourceNotFoundException;
import org.example.quoraproject.exception.UserNotFoundException;
import org.example.quoraproject.mapper.CommentMapper;
import org.example.quoraproject.repository.AnswerRepository;
import org.example.quoraproject.repository.CommentRepository;
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
public class CommentService {

    private final CommentRepository commentRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getCommentsByAnswerId(Long answerId, Pageable pageable) {
        if (!answerRepository.existsById(answerId)) {
            throw new ResourceNotFoundException("Answer", answerId);
        }
        return PageResponse.from(
                commentRepository.findByAnswerIdAndParentCommentIsNull(answerId, pageable),
                commentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getRepliesByCommentId(Long commentId, Pageable pageable) {
        if (!commentRepository.existsById(commentId)) {
            throw new ResourceNotFoundException("Comment", commentId);
        }
        return PageResponse.from(
                commentRepository.findByParentCommentId(commentId, pageable),
                commentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CommentResponse getCommentById(Long id) {
        return commentMapper.toResponse(requireComment(id));
    }

    @Transactional
    public CommentResponse createComment(CreateCommentRequest request) {
        CustomUserDetails principal = SecurityUtils.getCurrentUser();
        User author = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UserNotFoundException(principal.getId()));
        Answer answer = answerRepository.findById(request.answerId())
                .orElseThrow(() -> new ResourceNotFoundException("Answer", request.answerId()));

        Comment comment = new Comment();
        comment.setContent(request.content());
        comment.setAnswer(answer);
        comment.setUser(author);

        if (request.parentCommentId() != null) {
            Comment parent = commentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", request.parentCommentId()));
            if (!parent.getAnswer().getId().equals(answer.getId())) {
                throw new ForbiddenException("Parent comment does not belong to the same answer");
            }
            comment.setParentComment(parent);
        }

        Comment saved = commentRepository.save(comment);
        log.info("User id {} commented on answer id {}", author.getId(), answer.getId());
        return commentMapper.toResponse(saved);
    }

    @Transactional
    public void deleteComment(Long id) {
        Comment comment = requireComment(id);
        CustomUserDetails principal = SecurityUtils.getCurrentUser();
        boolean isOwner = comment.getUser().getId().equals(principal.getId());
        if (!isOwner && !principal.isAdmin()) {
            throw new ForbiddenException("You can only delete your own comments");
        }
        commentRepository.delete(comment);
        log.info("Comment id {} deleted by user id {}", id, principal.getId());
    }

    private Comment requireComment(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", id));
    }
}
