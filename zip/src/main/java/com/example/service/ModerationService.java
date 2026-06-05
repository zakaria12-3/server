package com.example.service;

import com.example.model.Job;
import com.example.model.ReportTargetType;
import com.example.model.User;
import com.example.repository.JobRepository;
import com.example.repository.ReportRepository;
import com.example.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ModerationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModerationService.class);

    private static final Pattern SCAM_PATTERN = Pattern.compile("(?i)(travail facile|gagne\\s*\\d+|sans exp[eé]rience|argent facile(?:ment)?|revenu garanti|crypto|investissement|telegram|whatsapp|western union|moneygram|frais d[' ]?inscription|paiement avant|commission uniquement)");
    private static final Pattern PHISHING_PATTERN = Pattern.compile("(?i)(http://|https://|bit\\.ly|tinyurl|t\\.me/|wa\\.me/)[^\\s]*");
    private static final Pattern TOXIC_PATTERN = Pattern.compile("(?i)(arnaque|fraud|harc[eè]lement|toxic|hate|haine|violence|menace|humiliation|exploitation)");
    private static final Pattern PERSONAL_DATA_PATTERN = Pattern.compile("(?i)(envoyez.*(cin|passport|passeport|rib|carte bancaire)|numero de carte|num[eé]ro de carte|mot de passe|code bancaire)");
    private static final Pattern DISCRIMINATION_PATTERN = Pattern.compile("(?i)(homme uniquement|femme uniquement|moins de \\d+ ans|nationalit[eé] exig[eé]e|religion|enceinte|handicap)");

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ReportRepository reportRepository;
    private final AIService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ModerationService(UserRepository userRepository,
                             JobRepository jobRepository,
                             ReportRepository reportRepository,
                             AIService aiService) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.reportRepository = reportRepository;
        this.aiService = aiService;
    }

    public void evaluateUser(User user) {
        int score = 0;

        long reportCount = reportRepository.countByTargetTypeAndTargetId(ReportTargetType.USER, user.getId());
        score += (int) reportCount;

        if (user.getBio() == null || user.getBio().isBlank()) score += 1;
        if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) score += 1;

        if ("REJECTED".equalsIgnoreCase(user.getApprovalStatus())) score += 10;
        else if ("PENDING".equalsIgnoreCase(user.getApprovalStatus())) score += 2;

        user.setRiskScore(score);

        if (score >= 15) {
            user.setSuspended(true);
            user.setSuspensionReason("Automatic suspension due to high risk score (" + score + ")");
            user.setReported(true);
        } else if (score >= 10) {
            user.setReported(true);
            user.setSuspended(false);
            user.setSuspensionReason(null);
        } else {
            user.setReported(false);
            user.setSuspended(false);
            user.setSuspensionReason(null);
        }

        userRepository.save(user);
        LOGGER.info("Evaluated user {}, new risk score: {}", user.getEmail(), score);
    }

    public void evaluateJob(Job job) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        String text = String.join(" ",
                safe(job.getTitle()),
                safe(job.getCompany()),
                safe(job.getLocation()),
                safe(job.getDescription())
        ).toLowerCase();

        if (SCAM_PATTERN.matcher(text).find()) {
            score += 5;
            reasons.add("Scam-like wording or unrealistic payment promise");
        }
        if (TOXIC_PATTERN.matcher(text).find()) {
            score += 3;
            reasons.add("Harmful, abusive, or fraudulent wording");
        }
        if (PHISHING_PATTERN.matcher(text).find()) {
            score += 3;
            reasons.add("Suspicious external contact or link");
        }
        if (PERSONAL_DATA_PATTERN.matcher(text).find()) {
            score += 5;
            reasons.add("Requests sensitive identity or banking data");
        }
        if (DISCRIMINATION_PATTERN.matcher(text).find()) {
            score += 4;
            reasons.add("Potentially discriminatory requirement");
        }

        long emojiCount = text.codePoints().filter(c -> c >= 0x1F300 && c <= 0x1FAFF).count();
        if (emojiCount > 3) {
            score += 2;
            reasons.add("Spam-like emoji usage");
        }

        if (job.getId() != null) {
            long duplicates = jobRepository.countByTitleAndDescription(job.getTitle(), job.getDescription());
            if (duplicates > 1) {
                score += 3;
                reasons.add("Duplicate job content");
            }
        }

        if (job.getRecruiter() != null && !"APPROVED".equalsIgnoreCase(job.getRecruiter().getApprovalStatus())) {
            score += 4;
            reasons.add("Recruiter account is not fully approved");
        }

        AiModerationResult aiResult = evaluateJobWithAI(job);
        if (aiResult != null) {
            score += aiResult.score();
            reasons.addAll(aiResult.reasons());
        }

        job.setRiskScore(score);
        job.setSuspicious(score >= 7);
        job.setModerationStatus(score >= 10 ? "BLOCKED" : score >= 7 ? "REVIEW" : "APPROVED");
        job.setModerationReason(reasons.isEmpty() ? null : String.join("; ", reasons));

        LOGGER.info("Evaluated job '{}', risk score {}, status {}", job.getTitle(), score, job.getModerationStatus());
    }

    public void approveJob(Job job) {
        job.setRiskScore(0);
        job.setSuspicious(false);
        job.setModerationStatus("APPROVED");
        job.setModerationReason("Approved by admin review");
    }

    public void rejectJob(Job job, String reason) {
        job.setActive(false);
        job.setSuspicious(true);
        job.setModerationStatus("BLOCKED");
        job.setModerationReason(reason == null || reason.isBlank() ? "Blocked by admin review" : reason.trim());
    }

    private AiModerationResult evaluateJobWithAI(Job job) {
        if (!aiService.hasConfiguredApiKey()) {
            return null;
        }

        String prompt = """
        You are a strict recruitment platform safety moderator.
        Check whether this job post is a scam, harmful, discriminatory, asks for money, asks for sensitive personal data, or pushes candidates to unsafe external contact.
        Return only JSON:
        {"riskScore": 0, "reasons": ["short reason"]}

        Job title: %s
        Company: %s
        Location: %s
        Description: %s
        """.formatted(safe(job.getTitle()), safe(job.getCompany()), safe(job.getLocation()), safe(job.getDescription()));

        String response = aiService.askAI(prompt, true);
        if (response == null || response.startsWith("AI error:")) {
            LOGGER.warn("AI job moderation skipped: {}", response);
            return null;
        }

        try {
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}") + 1;
            if (start < 0 || end <= start) return null;
            Map<?, ?> parsed = objectMapper.readValue(response.substring(start, end), Map.class);
            int aiScore = parsed.get("riskScore") instanceof Number number
                    ? Math.max(0, Math.min(5, number.intValue()))
                    : 0;
            List<String> aiReasons = new ArrayList<>();
            if (parsed.get("reasons") instanceof List<?> list) {
                list.stream()
                        .map(Object::toString)
                        .filter(reason -> !reason.isBlank())
                        .limit(3)
                        .forEach(aiReasons::add);
            }
            return new AiModerationResult(aiScore, aiReasons);
        } catch (Exception e) {
            LOGGER.warn("Could not parse AI moderation response", e);
            return null;
        }
    }

    private String safe(String value) {
        if (value == null) return "";
        return value.length() > 2500 ? value.substring(0, 2500) : value;
    }

    private record AiModerationResult(int score, List<String> reasons) {
    }
}
