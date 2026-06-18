package com.example.repository;

import com.example.model.PlatformRating;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformRatingRepository extends CrudRepository<PlatformRating, Long> {
    Optional<PlatformRating> findByUserId(Long userId);

    @Query("select avg(r.score) from PlatformRating r")
    Double findAverageScore();
}
