package org.example.quoraproject.service;

import lombok.RequiredArgsConstructor;
import org.example.quoraproject.dto.response.QuestionResponse;
import org.example.quoraproject.entity.Tag;
import org.example.quoraproject.entity.User;
import org.example.quoraproject.exception.UserNotFoundException;
import org.example.quoraproject.mapper.QuestionMapper;
import org.example.quoraproject.repository.QuestionRepository;
import org.example.quoraproject.repository.UserRepository;
import org.example.quoraproject.response.PageResponse;
import org.example.quoraproject.security.CustomUserDetails;
import org.example.quoraproject.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Personalized feed for the authenticated user. The old endpoint accepted
 * any userId in the path, letting anyone read anyone else's feed; the feed
 * is now derived from the SecurityContext.
 */
@Service
@RequiredArgsConstructor
public class UserFeedService {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;

    @Transactional(readOnly = true)
    public PageResponse<QuestionResponse> getCurrentUserFeed(Pageable pageable) {
        CustomUserDetails principal = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UserNotFoundException(principal.getId()));

        Set<Long> tagIds = user.getFollowedTags().stream()
                .map(Tag::getId)
                .collect(Collectors.toSet());

        if (tagIds.isEmpty()) {
            return PageResponse.from(Page.empty(pageable), questionMapper::toResponse);
        }
        return PageResponse.from(questionRepository.findQuestionsByTags(tagIds, pageable),
                questionMapper::toResponse);
    }
}
