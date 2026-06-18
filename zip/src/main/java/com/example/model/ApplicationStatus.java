package com.example.model;

public enum ApplicationStatus {
    PENDING_QUIZ,
    APPLIED,
    INTERVIEW_SCHEDULED,
    INTERVIEW_COMPLETED,
    FINAL_ACCEPTED,
    REJECTED,
    REJECTED_CHEATING;

    public static String normalize(String status) {
        if (status == null || status.isBlank()) {
            return APPLIED.name();
        }

        return switch (status.trim().toUpperCase()) {
            case "PENDING" -> APPLIED.name();
            case "PRESELECTED_FOR_INTERVIEW" -> INTERVIEW_SCHEDULED.name();
            case "ACCEPTED" -> FINAL_ACCEPTED.name();
            default -> status.trim().toUpperCase();
        };
    }

    public static boolean isQuizPending(String status) {
        return PENDING_QUIZ.name().equals(normalize(status));
    }

    public static boolean isAccepted(String status) {
        return FINAL_ACCEPTED.name().equals(normalize(status));
    }

    public static boolean isRejected(String status) {
        String normalized = normalize(status);
        return REJECTED.name().equals(normalized) || REJECTED_CHEATING.name().equals(normalized);
    }
}
