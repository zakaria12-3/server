package com.example.service;

import com.example.model.User;
import com.example.model.Role;
import com.example.model.Job;
import com.example.repository.UserRepository;
import com.example.repository.ApplicationRepository;
import com.example.repository.QuizResultRepository;
import com.example.dto.UserProfileDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final QuizResultRepository quizResultRepository;
    private final JobService jobService;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository, ApplicationRepository applicationRepository,
                       QuizResultRepository quizResultRepository, JobService jobService,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.quizResultRepository = quizResultRepository;
        this.jobService = jobService;
        this.notificationService = notificationService;
    }

    public List<User> allUsers(){
        List<User> users=new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users;

    }
    public User findByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findById(id);
        if (user.getRole() == Role.ROLE_CANDIDATE) {
            applicationRepository.deleteByCandidateId(id);
            quizResultRepository.deleteByCandidateId(id);
        } else if (user.getRole() == Role.ROLE_RECRUITER) {
            List<Job> recruiterJobs = jobService.getJobsByRecruiter(id);
            for(Job job : recruiterJobs) {
                jobService.deleteJob(job.getId());
            }
        }
        userRepository.delete(user);
    }

    public User updateUserRole(Long id, Role newRole) {
        User user = findById(id);
        user.setRole(newRole);
        return userRepository.save(user);
    }

    public User updateUserStatus(Long id, boolean enabled) {
        User user = findById(id);
        user.setEnabled(enabled);
        if (user.getRole() == Role.ROLE_RECRUITER) {
            user.setApprovalStatus(enabled ? "APPROVED" : "REJECTED");
        }
        User savedUser = userRepository.save(user);
        if (savedUser.getRole() == Role.ROLE_RECRUITER) {
            jobService.setJobsActiveByRecruiter(id, enabled);
        }
        return savedUser;
    }

    public User approveRecruiter(Long id) {
        User user = findById(id);
        if (user.getRole() != Role.ROLE_RECRUITER) {
            throw new RuntimeException("Only recruiter accounts can be approved");
        }
        if (!user.isEmailVerified()) {
            throw new RuntimeException("Recruiter must verify email before approval");
        }
        user.setEnabled(true);
        user.setApprovalStatus("APPROVED");
        User savedUser = userRepository.save(user);
        notificationService.create(
                savedUser,
                "ACCOUNT_STATUS",
                "Compte recruteur approuve",
                "Votre compte recruteur est maintenant approuve. Vous pouvez publier des offres et gerer vos candidatures.",
                "/recruiter"
        );
        return savedUser;
    }

    public User rejectRecruiter(Long id) {
        User user = findById(id);
        if (user.getRole() != Role.ROLE_RECRUITER) {
            throw new RuntimeException("Only recruiter accounts can be rejected");
        }
        user.setEnabled(false);
        user.setApprovalStatus("REJECTED");
        jobService.setJobsActiveByRecruiter(id, false);
        User savedUser = userRepository.save(user);
        notificationService.create(
                savedUser,
                "ACCOUNT_STATUS",
                "Compte recruteur refuse",
                "Votre compte recruteur a ete refuse par l'administration. Contactez le support si vous pensez qu'il s'agit d'une erreur.",
                "/profile"
        );
        return savedUser;
    }

    public UserProfileDto getUserProfile(Long id) {
        User user = findById(id);
        return mapToProfileDto(user);
    }

    public UserProfileDto updateUserProfile(Long id, UserProfileDto dto) {
        User user = findById(id);
        user.setBio(dto.getBio());
        user.setHeadline(dto.getHeadline());
        user.setLocation(dto.getLocation());
        user.setAvatarUrl(dto.getAvatarUrl());
        User savedUser = userRepository.save(user);
        return mapToProfileDto(savedUser);
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
        if (user.getCompany() != null) {
            dto.setCompanyName(user.getCompany().getName());
        }
        dto.setReported(user.isReported());
        dto.setSuspended(user.isSuspended());
        return dto;
    }
}
