package com.example.repository;

import com.example.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    Optional<Quiz> findByJobId(Long jobId);
    void deleteByJobId(Long jobId);

}