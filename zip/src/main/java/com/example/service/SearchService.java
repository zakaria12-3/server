package com.example.service;

import com.example.dto.JobDto;
import com.example.dto.PostDto;
import com.example.dto.SearchResponseDto;
import com.example.dto.UserProfileDto;
import com.example.model.Job;
import com.example.model.Post;
import com.example.model.User;
import com.example.repository.JobRepository;
import com.example.repository.PostRepository;
import com.example.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final PostRepository postRepository;

    public SearchService(UserRepository userRepository, JobRepository jobRepository, PostRepository postRepository) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.postRepository = postRepository;
    }

    public SearchResponseDto globalSearch(String query, Long currentUserId) {
        SearchResponseDto response = new SearchResponseDto();

        List<User> userResults = userRepository.findByUsernameContainingIgnoreCaseOrHeadlineContainingIgnoreCase(query, query);
        List<UserProfileDto> userDtos = userResults.stream().map(this::mapToProfileDto).collect(Collectors.toList());

        List<Job> jobResults = jobRepository.searchVisibleJobs(query, LocalDate.now());
        List<JobDto> jobDtos = jobResults.stream().map(this::mapToJobDto).collect(Collectors.toList());

        List<Post> postResults = postRepository.findByContentContainingIgnoreCase(query);
        List<PostDto> postDtos = postResults.stream().map(post -> mapToPostDto(post, currentUserId)).collect(Collectors.toList());

        response.setUsers(userDtos);
        response.setJobs(jobDtos);
        response.setPosts(postDtos);

        return response;
    }

    private UserProfileDto mapToProfileDto(User user) {
        UserProfileDto dto = new UserProfileDto();
        dto.setId(user.getId());
        dto.setUsername(user.getRealUsername());
        dto.setEmail(user.getEmail());
        dto.setBio(user.getBio());
        dto.setHeadline(user.getHeadline());
        dto.setLocation(user.getLocation());
        dto.setAvatarUrl(user.getAvatarUrl());
        return dto;
    }

    private JobDto mapToJobDto(Job job) {
        JobDto dto = new JobDto();
        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setDescription(job.getDescription());
        dto.setLocation(job.getLocation());
        dto.setCompany(job.getCompany());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setExpirationDate(job.getExpirationDate());
        dto.setActive(job.getActive());
        return dto;
    }

    private PostDto mapToPostDto(Post post, Long currentUserId) {
        PostDto dto = new PostDto();
        dto.setId(post.getId());
        dto.setAuthorId(post.getAuthor().getId());
        dto.setAuthorName(post.getAuthor().getRealUsername());
        dto.setAuthorAvatar(post.getAuthor().getAvatarUrl());
        dto.setContent(post.getContent());
        dto.setCreatedAt(post.getCreatedAt());
        
        int likeCount = post.getLikes() != null ? post.getLikes().size() : 0;
        dto.setLikesCount(likeCount);
        
        boolean liked = post.getLikes() != null && currentUserId != null && post.getLikes().stream()
                .anyMatch(like -> like.getUser().getId().equals(currentUserId));
        dto.setLikedByCurrentUser(liked);
        return dto;
    }
}
