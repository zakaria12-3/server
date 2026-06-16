package com.example.service;

import com.example.dto.CommentDto;
import com.example.dto.PostDto;
import com.example.model.Comment;
import com.example.model.Post;
import com.example.model.PostLike;
import com.example.model.User;
import com.example.repository.CommentRepository;
import com.example.repository.PostLikeRepository;
import com.example.repository.PostRepository;
import com.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository,
                       PostLikeRepository postLikeRepository,
                       CommentRepository commentRepository,
                       UserRepository userRepository) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    public PostDto createPost(String email, String content) {
        User author = userRepository.findByEmail(email).orElseThrow();
        Post post = new Post(author, content);
        post = postRepository.save(post);
        return mapToDto(post, author.getId());
    }

    public List<PostDto> getAllPosts(String email) {
        User currentUser = userRepository.findByEmail(email).orElseThrow();
        List<Post> posts = (List<Post>) postRepository.findAll();
        posts.sort((a,b)-> b.getCreatedAt().compareTo(a.getCreatedAt()));
        
        return posts.stream()
                .map(post -> mapToDto(post, currentUser.getId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public PostDto toggleLike(Long postId, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();
        
        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, user.getId());
        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
        } else {
            postLikeRepository.save(new PostLike(post, user));
        }
        
        Post updatedPost = postRepository.findById(postId).orElseThrow();
        return mapToDto(updatedPost, user.getId());
    }

    @Transactional
    public PostDto addComment(Long postId, String email, String content) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();
        
        Comment comment = new Comment(post, user, content);
        commentRepository.save(comment);
        
        Post updatedPost = postRepository.findById(postId).orElseThrow();
        return mapToDto(updatedPost, user.getId());
    }

    private PostDto mapToDto(Post post, Long currentUserId) {
        PostDto dto = new PostDto();
        dto.setId(post.getId());
        dto.setAuthorId(post.getAuthor().getId());
        dto.setAuthorName(post.getAuthor().getRealUsername());
        dto.setAuthorAvatar(post.getAuthor().getAvatarUrl());
        dto.setContent(post.getContent());
        dto.setCreatedAt(post.getCreatedAt());
        
        int likeCount = post.getLikes() != null ? post.getLikes().size() : 0;
        dto.setLikesCount(likeCount);
        
        boolean liked = post.getLikes() != null && post.getLikes().stream()
                .anyMatch(like -> like.getUser().getId().equals(currentUserId));
        dto.setLikedByCurrentUser(liked);
        
        if (post.getComments() != null) {
            List<CommentDto> commentDtos = post.getComments().stream().map(c -> {
                CommentDto cdto = new CommentDto();
                cdto.setId(c.getId());
                cdto.setAuthorId(c.getAuthor().getId());
                cdto.setAuthorName(c.getAuthor().getRealUsername());
                cdto.setAuthorAvatar(c.getAuthor().getAvatarUrl());
                cdto.setContent(c.getContent());
                cdto.setCreatedAt(c.getCreatedAt());
                return cdto;
            }).collect(Collectors.toList());
            dto.setComments(commentDtos);
        }
        return dto;
    }
}
