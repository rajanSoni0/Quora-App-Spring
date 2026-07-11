package org.example.quoraproject.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.quoraproject.dto.request.CreateTagRequest;
import org.example.quoraproject.dto.response.TagResponse;
import org.example.quoraproject.entity.Tag;
import org.example.quoraproject.exception.DuplicateResourceException;
import org.example.quoraproject.exception.ResourceNotFoundException;
import org.example.quoraproject.mapper.TagMapper;
import org.example.quoraproject.repository.TagRepository;
import org.example.quoraproject.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Transactional(readOnly = true)
    public PageResponse<TagResponse> getAllTags(String name, Pageable pageable) {
        if (name != null && !name.isBlank()) {
            return PageResponse.from(tagRepository.findByNameContainingIgnoreCase(name.trim(), pageable),
                    tagMapper::toResponse);
        }
        return PageResponse.from(tagRepository.findAll(pageable), tagMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TagResponse getTagById(Long id) {
        return tagRepository.findById(id)
                .map(tagMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", id));
    }

    @Transactional
    public TagResponse createTag(CreateTagRequest request) {
        tagRepository.findByNameIgnoreCase(request.name()).ifPresent(existing -> {
            throw new DuplicateResourceException("Tag already exists: " + existing.getName());
        });
        Tag tag = new Tag();
        tag.setName(request.name().trim());
        return tagMapper.toResponse(tagRepository.save(tag));
    }

    /**
     * Tags are shared taxonomy; deleting one affects every question using
     * it, so this is restricted to admins.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", id));
        // Detach from questions and followers to avoid FK violations.
        tag.getQuestions().forEach(question -> question.getTags().remove(tag));
        tag.getFollowers().forEach(user -> user.getFollowedTags().remove(tag));
        tagRepository.delete(tag);
        log.info("Tag id {} deleted", id);
    }
}
