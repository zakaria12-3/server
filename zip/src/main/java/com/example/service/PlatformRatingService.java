package com.example.service;

import com.example.dto.PlatformRatingDto;
import com.example.dto.PlatformRatingSummaryDto;
import com.example.model.PlatformRating;
import com.example.model.User;
import com.example.repository.PlatformRatingRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PlatformRatingService {
    private final PlatformRatingRepository platformRatingRepository;
    private final UserService userService;

    public PlatformRatingService(PlatformRatingRepository platformRatingRepository, UserService userService) {
        this.platformRatingRepository = platformRatingRepository;
        this.userService = userService;
    }

    public Optional<PlatformRatingDto> getMyRating(String email) {
        User user = userService.findByEmail(email);
        return platformRatingRepository.findByUserId(user.getId()).map(this::toDto);
    }

    public PlatformRatingDto saveMyRating(String email, PlatformRatingDto request) {
        User user = userService.findByEmail(email);
        PlatformRating rating = platformRatingRepository.findByUserId(user.getId()).orElseGet(PlatformRating::new);

        rating.setUser(user);
        rating.setScore(request.getScore());
        rating.setComment(normalizeComment(request.getComment()));

        return toDto(platformRatingRepository.save(rating));
    }

    public PlatformRatingSummaryDto getSummary() {
        Double average = platformRatingRepository.findAverageScore();
        long count = platformRatingRepository.count();
        return new PlatformRatingSummaryDto(average == null ? 0.0 : average, count);
    }

    private PlatformRatingDto toDto(PlatformRating rating) {
        PlatformRatingDto dto = new PlatformRatingDto();
        dto.setId(rating.getId());
        dto.setScore(rating.getScore());
        dto.setComment(rating.getComment());
        dto.setCreatedAt(rating.getCreatedAt());
        dto.setUpdatedAt(rating.getUpdatedAt());
        return dto;
    }

    private String normalizeComment(String comment) {
        if (comment == null) {
            return null;
        }

        String normalized = comment.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
