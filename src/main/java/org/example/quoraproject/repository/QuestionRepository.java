package org.example.quoraproject.repository;

import org.example.quoraproject.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("SELECT DISTINCT q FROM Question q JOIN q.tags t WHERE t.id IN :tagIds")
    Page<Question> findQuestionsByTags(@Param("tagIds") Set<Long> tagIds, Pageable pageable);

    @Query("""
            SELECT DISTINCT q FROM Question q
            LEFT JOIN q.tags t
            WHERE (:search IS NULL
                   OR LOWER(q.title) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(q.content) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:tagId IS NULL OR t.id = :tagId)
            """)
    Page<Question> search(@Param("search") String search,
                          @Param("tagId") Long tagId,
                          Pageable pageable);
}
