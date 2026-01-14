package ru.practicum.comment.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.comment.model.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long>, QuerydslPredicateExecutor<Comment> {
    List<Comment> findByEventIdOrderByCreatedOnDesc(Long eventId, Pageable pageable);

    List<Comment> findByUserIdOrderByCreatedOnDesc(Long userId, Pageable pageable);
}
