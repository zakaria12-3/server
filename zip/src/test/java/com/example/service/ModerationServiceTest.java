package com.example.service;

import com.example.model.Job;
import com.example.model.User;
import com.example.model.ReportTargetType;
import com.example.repository.JobRepository;
import com.example.repository.ReportRepository;
import com.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ModerationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private AIService aiService;

    @InjectMocks
    private ModerationService moderationService;

    @BeforeEach
    void setUp() {
        lenient().when(aiService.hasConfiguredApiKey()).thenReturn(false);
    }

    @Test
    void evaluateUser_NoRisks_ScoreIsZero() {
        User user = new User();
        user.setId(1L);
        user.setBio("My valid bio");
        user.setAvatarUrl("http://example.com/avatar.jpg");
        user.setApprovalStatus("APPROVED");

        when(reportRepository.countByTargetTypeAndTargetId(ReportTargetType.USER, 1L)).thenReturn(0L);

        moderationService.evaluateUser(user);

        assertEquals(0, user.getRiskScore());
        assertFalse(user.isReported());
        assertFalse(user.isSuspended());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void evaluateUser_HighRisk_Suspended() {
        User user = new User();
        user.setId(1L);
        // Missing bio and avatar adds 2 points
        // Rejected approval adds 10 points
        user.setApprovalStatus("REJECTED");
        
        // 5 reports
        when(reportRepository.countByTargetTypeAndTargetId(ReportTargetType.USER, 1L)).thenReturn(5L);

        moderationService.evaluateUser(user);

        // Score should be: 5 (reports) + 1 (bio) + 1 (avatar) + 10 (rejected) = 17
        assertEquals(17, user.getRiskScore());
        assertTrue(user.isReported());
        assertTrue(user.isSuspended());
        assertNotNull(user.getSuspensionReason());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void evaluateJob_ScamAndToxic_Suspicious() {
        Job job = new Job();
        job.setTitle("Travail facile et Gagne 3000 euros sans expérience !");
        job.setDescription("Rejoignez notre arnaque et hate group http://scam.com \uD83D\uDE00\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00");
        
        User recruiter = new User();
        recruiter.setApprovalStatus("PENDING");
        job.setRecruiter(recruiter);

        moderationService.evaluateJob(job);

        // Scam words (+5)
        // Toxic/arnaque (+3)
        // Links (+2)
        // Emojis > 3 (+2)
        // Recruiter pending (+4)
        // Duplicate check bypassed since job.id is null
        
        assertTrue(job.getRiskScore() >= 10);
        assertTrue(job.isSuspicious());
        assertEquals("BLOCKED", job.getModerationStatus());
        assertNotNull(job.getModerationReason());
    }

    @Test
    void evaluateJob_NullDescription_DoesNotCrash() {
        Job job = new Job();
        job.setTitle("Valid Job");
        job.setDescription(null);

        User recruiter = new User();
        recruiter.setApprovalStatus("APPROVED");
        job.setRecruiter(recruiter);

        assertDoesNotThrow(() -> moderationService.evaluateJob(job));
        assertEquals(0, job.getRiskScore());
        assertFalse(job.isSuspicious());
        assertEquals("APPROVED", job.getModerationStatus());
    }
}
