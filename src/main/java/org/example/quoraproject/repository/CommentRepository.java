package org.example.quoraproject.repository;

import org.example.quoraproject.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByAnswerIdAndParentCommentIsNull(Long answerId, Pageable pageable);

    Page<Comment> findByParentCommentId(Long parentCommentId, Pageable pageable);
}
