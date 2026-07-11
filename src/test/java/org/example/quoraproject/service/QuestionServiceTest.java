package org.example.quoraproject.service;

import org.example.quoraproject.dto.request.CreateQuestionRequest;
import org.example.quoraproject.dto.response.QuestionResponse;
import org.example.quoraproject.entity.Question;
import org.example.quoraproject.entity.Role;
import org.example.quoraproject.entity.Tag;
import org.example.quoraproject.entity.User;
import org.example.quoraproject.exception.ForbiddenException;
import org.example.quoraproject.exception.ResourceNotFoundException;
import org.example.quoraproject.mapper.QuestionMapper;
import org.example.quoraproject.repository.QuestionRepository;
import org.example.quoraproject.repository.TagRepository;
import org.example.quoraproject.repository.UserRepository;
import org.example.quoraproject.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private UserRepository userRepository;
    @Mock private TagRepository tagRepository;
    @Mock private QuestionMapper questionMapper;

    @InjectMocks
    private QuestionService questionService;

    private User owner;
    private User otherUser;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setUsername("owner");
        owner.setPassword("hash");
        owner.setRole(Role.USER);

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("intruder");
        otherUser.setPassword("hash");
        otherUser.setRole(Role.USER);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        CustomUserDetails details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    @Test
    @DisplayName("createQuestion assigns authorship from the SecurityContext, not the request")
    void createQuestionUsesAuthenticatedUser() {
        authenticateAs(owner);
        Tag tag = new Tag();
        tag.setId(10L);
        tag.setName("java");

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(tagRepository.findById(10L)).thenReturn(Optional.of(tag));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        when(questionMapper.toResponse(any(Question.class)))
                .thenReturn(new QuestionResponse(1L, "t", "c", null, Set.of(), null, null));

        questionService.createQuestion(new CreateQuestionRequest(
                "How does JPA work?", "A sufficiently long question body here.", Set.of(10L)));

        verify(questionRepository).save(any(Question.class));
    }

    @Test
    @DisplayName("createQuestion fails loudly on unknown tag ids")
    void createQuestionFailsOnUnknownTag() {
        authenticateAs(owner);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.createQuestion(new CreateQuestionRequest(
                "How does JPA work?", "A sufficiently long question body here.", Set.of(99L))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteQuestion forbids non-owners who are not admins")
    void deleteQuestionForbidsNonOwner() {
        authenticateAs(otherUser);
        Question question = new Question();
        question.setId(5L);
        question.setUser(owner);
        when(questionRepository.findById(5L)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> questionService.deleteQuestion(5L))
                .isInstanceOf(ForbiddenException.class);

        verify(questionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteQuestion allows admins to delete any question")
    void deleteQuestionAllowsAdmin() {
        otherUser.setRole(Role.ADMIN);
        authenticateAs(otherUser);
        Question question = new Question();
        question.setId(5L);
        question.setUser(owner);
        when(questionRepository.findById(5L)).thenReturn(Optional.of(question));

        questionService.deleteQuestion(5L);

        verify(questionRepository).delete(question);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }
}
