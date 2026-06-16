package com.example.service;

import com.example.dto.CreateQuizDto;
import com.example.dto.QuestionDto;
import com.example.dto.QuizDto;
import com.example.dto.QuizSubmissionResultDto;
import com.example.model.*;
import com.example.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class QuizService {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuizService.class);

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final QuizResultRepository resultRepository;
    private final ApplicationRepository applicationRepository;

    public QuizService(QuizRepository quizRepository,
                       QuestionRepository questionRepository,
                       JobRepository jobRepository,
                       ApplicationRepository applicationRepository,
                       UserRepository userRepository,
                       QuizResultRepository resultRepository, AIService aiService) {

        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.resultRepository = resultRepository;
        this.applicationRepository = applicationRepository;
        this.aiService = aiService;
    }

    public List<Quiz> getAllQuizzes() {
        return quizRepository.findAll();
    }

    public Quiz getQuizEntityByJob(Long jobId) {
        return quizRepository.findByJobId(jobId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
    }

    public QuizDto getQuizDtoByJob(Long jobId) {
        Quiz quiz = getQuizEntityByJob(jobId);

        List<QuestionDto> questionDTOs = quiz.getQuestions().stream()
                .map(q -> new QuestionDto(
                        q.getId(),
                        q.getQuestionText(),
                        q.getOptionA(),
                        q.getOptionB(),
                        q.getOptionC(),
                        q.getOptionD()
                ))
                .toList();

        return new QuizDto(
                quiz.getId(),
                questionDTOs,
                quiz.getPassingScore() != null ? quiz.getPassingScore() : 50,
                quiz.getJob().getTitle()
        );
    }

    public CreateQuizDto getEditableQuizDtoByJob(Long jobId) {
        Quiz quiz = getQuizEntityByJob(jobId);

        CreateQuizDto dto = new CreateQuizDto();
        dto.setJobId(jobId);
        dto.setPassingScore(quiz.getPassingScore() != null ? quiz.getPassingScore() : 50);
        dto.setQuestions(quiz.getQuestions().stream()
                .map(q -> {
                    QuestionDto questionDto = new QuestionDto();
                    questionDto.setId(q.getId());
                    questionDto.setQuestionText(q.getQuestionText());
                    questionDto.setOptionA(q.getOptionA());
                    questionDto.setOptionB(q.getOptionB());
                    questionDto.setOptionC(q.getOptionC());
                    questionDto.setOptionD(q.getOptionD());
                    questionDto.setCorrectAnswer(q.getCorrectAnswer());
                    return questionDto;
                })
                .toList());

        return dto;
    }

    public QuizSubmissionResultDto submitQuiz(Long jobId, String email, Map<?, ?> payload) {

        Quiz quiz = getQuizEntityByJob(jobId);
        QuizSubmission submission = parseSubmissionPayload(payload);

        int correctAnswers = 0;
        int totalQuestions = quiz.getQuestions().size();

        for (Question q : quiz.getQuestions()) {
            String userAnswer = submission.answers().get(q.getId());

            if (userAnswer != null &&
                    q.getCorrectAnswer() != null &&
                    q.getCorrectAnswer().trim().equalsIgnoreCase(userAnswer.trim())) {
                correctAnswers++;
            }
        }
        int score = (totalQuestions == 0)
                ? 0
                : (correctAnswers * 100 / totalQuestions);

        User user = userRepository.findByEmail(email).orElseThrow();
        Job job = jobRepository.findById(jobId).orElseThrow();

        IntegrityDecision integrityDecision = evaluateSubmissionIntegrity(totalQuestions, submission);
        long previousCheatingSubmissions = resultRepository.countByCandidateIdAndCheatingSuspectedTrue(user.getId());
        boolean repeatedCheating = integrityDecision.cheatingSuspected() && previousCheatingSubmissions > 0;

        if (resultRepository.existsByJobIdAndCandidateId(jobId, user.getId())) {
            if (integrityDecision.cheatingSuspected()) {
                suspendCandidate(user, "Repeated quiz integrity violation");
                return new QuizSubmissionResultDto(
                        score,
                        quiz.getPassingScore() != null ? quiz.getPassingScore() : 50,
                        false,
                        true,
                        true,
                        "Repeated quiz integrity violation. Candidate account suspended."
                );
            }
            throw new RuntimeException("Quiz already submitted for this job");
        }

        QuizResult result = new QuizResult();
        result.setScore(score);
        result.setCandidate(user);
        result.setJob(job);
        result.setQuiz(quiz);
        result.setDurationSeconds(submission.durationSeconds());
        result.setIntegrityEventCount(submission.integrityEventCount());
        result.setCheatingSuspected(integrityDecision.cheatingSuspected());
        result.setIntegrityReason(integrityDecision.reason());
        result.setSubmittedAt(LocalDateTime.now());
        resultRepository.save(result);
        Application app = applicationRepository
                .findByJobIdAndCandidateId(jobId, user.getId())
                .orElseThrow(() -> new RuntimeException("Application not found"));

        int passingScore = (quiz.getPassingScore() != null)
                ? quiz.getPassingScore()
                : 50;
        boolean passed = score >= passingScore;

        if (repeatedCheating) {
            suspendCandidate(user, "Repeated quiz integrity violation");
        }

        app.setQuizScore(score);
        app.setQuizPassed(integrityDecision.cheatingSuspected() ? !repeatedCheating : passed);
        app.setCheatingSuspected(integrityDecision.cheatingSuspected());
        app.setQuizDurationSeconds(submission.durationSeconds());
        app.setQuizIntegrityEventCount(submission.integrityEventCount());
        app.setQuizIntegrityReason(integrityDecision.reason());

        if (repeatedCheating) {
            app.setStatus("REJECTED_CHEATING");
        } else if (integrityDecision.cheatingSuspected()) {
            app.setStatus("PENDING");
        } else if (!passed) {
            app.setStatus("REJECTED");
        } else {
            app.setStatus("PENDING");
        }

        applicationRepository.save(app);

        String message;
        if (repeatedCheating) {
            message = "Repeated quiz integrity violation. Candidate account suspended.";
        } else if (integrityDecision.cheatingSuspected()) {
            message = "Quiz submitted with integrity warnings. This is a first warning; another cheating attempt will suspend the account.";
        } else if (!passed) {
            message = "Quiz failed.";
        } else {
            message = "Quiz passed.";
        }

        return new QuizSubmissionResultDto(
                score,
                passingScore,
                integrityDecision.cheatingSuspected() ? !repeatedCheating : passed,
                integrityDecision.cheatingSuspected(),
                repeatedCheating,
                message
        );
    }

    private void suspendCandidate(User user, String reason) {
        user.setEnabled(false);
        user.setSuspended(true);
        user.setReported(true);
        user.setRiskScore(Math.max(user.getRiskScore(), 90));
        user.setSuspensionReason(reason);
        userRepository.save(user);
    }

    @SuppressWarnings("unchecked")
    private QuizSubmission parseSubmissionPayload(Map<?, ?> payload) {
        Map<Long, String> answers = new HashMap<>();
        int durationSeconds = 0;
        int integrityEvents = 0;

        Object rawAnswers = payload.get("answers");
        Map<?, ?> answerMap = rawAnswers instanceof Map<?, ?> map ? map : payload;

        answerMap.forEach((key, value) -> {
            if (value == null) return;
            try {
                Long questionId = key instanceof Number number
                        ? number.longValue()
                        : Long.valueOf(String.valueOf(key));
                answers.put(questionId, String.valueOf(value));
            } catch (NumberFormatException ignored) {
            }
        });

        durationSeconds = readInt(payload.get("durationSeconds"));
        integrityEvents = readInt(payload.get("integrityEventCount"));
        if (payload.get("integrityEvents") instanceof List<?> events) {
            integrityEvents = Math.max(integrityEvents, events.size());
        }

        return new QuizSubmission(answers, Math.max(0, durationSeconds), Math.max(0, integrityEvents));
    }

    private IntegrityDecision evaluateSubmissionIntegrity(int totalQuestions, QuizSubmission submission) {
        List<String> reasons = new ArrayList<>();
        int minimumSeconds = Math.max(20, totalQuestions * 8);

        if (submission.durationSeconds() > 0 && submission.durationSeconds() < minimumSeconds) {
            reasons.add("Assessment submitted too quickly");
        }
        if (submission.integrityEventCount() >= 3) {
            reasons.add("Multiple tab switches, copy/paste, or focus-loss events detected");
        }

        return new IntegrityDecision(!reasons.isEmpty(), reasons.isEmpty() ? null : String.join("; ", reasons));
    }

    private int readInt(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) return 0;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private record QuizSubmission(Map<Long, String> answers, int durationSeconds, int integrityEventCount) {
    }

    private record IntegrityDecision(boolean cheatingSuspected, String reason) {
    }

    public Quiz createQuiz(CreateQuizDto dto) {


        if (quizRepository.findByJobId(dto.getJobId()).isPresent()) {
            throw new RuntimeException("Quiz already exists for this job");
        }

        Job job = jobRepository.findById(dto.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found"));

        Quiz quiz = new Quiz();
        quiz.setJob(job);
        quiz.setPassingScore(dto.getPassingScore() != null ? dto.getPassingScore() : 50);

        List<Question> questionList = new ArrayList<>();

        if (dto.getQuestions() == null || dto.getQuestions().isEmpty()) {
            throw new RuntimeException("Add at least one question");
        }

        for (QuestionDto qDto : dto.getQuestions()) {
            Question q = new Question();

            q.setQuestionText(qDto.getQuestionText());
            q.setOptionA(qDto.getOptionA());
            q.setOptionB(qDto.getOptionB());
            q.setOptionC(qDto.getOptionC());
            q.setOptionD(qDto.getOptionD());
            q.setCorrectAnswer(resolveCorrectAnswer(qDto));
            validateQuestion(q);

            q.setQuiz(quiz);
            questionList.add(q);
        }

        quiz.setQuestions(questionList);

        return quizRepository.save(quiz);
    }
    public Quiz createQuiz(Quiz quiz) {
        for (Question q : quiz.getQuestions()) {
            q.setQuiz(quiz);
        }
        return quizRepository.save(quiz);
    }

    public Quiz updateQuiz(Long jobId, CreateQuizDto dto) {
        Quiz quiz = getQuizEntityByJob(jobId);
        
        quiz.setPassingScore(dto.getPassingScore() != null ? dto.getPassingScore() : 50);

        quiz.getQuestions().clear();

        List<Question> newQuestions = new ArrayList<>();
        if (dto.getQuestions() == null || dto.getQuestions().isEmpty()) {
            throw new RuntimeException("Add at least one question");
        }

        for (QuestionDto qDto : dto.getQuestions()) {
            Question q = new Question();
            q.setQuestionText(qDto.getQuestionText());
            q.setOptionA(qDto.getOptionA());
            q.setOptionB(qDto.getOptionB());
            q.setOptionC(qDto.getOptionC());
            q.setOptionD(qDto.getOptionD());
            q.setCorrectAnswer(resolveCorrectAnswer(qDto));
            validateQuestion(q);
            q.setQuiz(quiz);
            newQuestions.add(q);
        }

        quiz.getQuestions().addAll(newQuestions);

        return quizRepository.save(quiz);
    }

    public void deleteQuizForJob(Long jobId) {
        if (quizRepository.findByJobId(jobId).isEmpty()) {
            throw new RuntimeException("Quiz not found");
        }
        List<Application> waitingApplications = applicationRepository.findByJobIdAndStatus(jobId, "PENDING_QUIZ");
        for (Application application : waitingApplications) {
            application.setStatus("PENDING");
            application.setQuiz(null);
            application.setQuizPassed(false);
        }
        applicationRepository.saveAll(waitingApplications);
        quizRepository.deleteByJobId(jobId);
    }

    public CreateQuizDto generateQuizDraft(String topic, Long jobId) throws Exception {
        Quiz quiz = buildQuizFromAI(topic, jobId, requestAIQuiz(topic));

        CreateQuizDto dto = new CreateQuizDto();
        dto.setJobId(jobId);
        dto.setPassingScore(quiz.getPassingScore());
        dto.setQuestions(quiz.getQuestions().stream()
                .map(q -> {
                    QuestionDto questionDto = new QuestionDto();
                    questionDto.setQuestionText(q.getQuestionText());
                    questionDto.setOptionA(q.getOptionA());
                    questionDto.setOptionB(q.getOptionB());
                    questionDto.setOptionC(q.getOptionC());
                    questionDto.setOptionD(q.getOptionD());
                    questionDto.setCorrectAnswer(q.getCorrectAnswer());
                    return questionDto;
                })
                .toList());

        return dto;
    }

    public Quiz createQuizFromAI(String topic, Long jobId, String aiResponse) throws Exception {

        if (quizRepository.findByJobId(jobId).isPresent()) {
            throw new RuntimeException("Quiz already exists for this job");
        }

        return quizRepository.save(buildQuizFromAI(topic, jobId, aiResponse));
    }

    @SuppressWarnings("unchecked")
    private Quiz buildQuizFromAI(String topic, Long jobId, String aiResponse) throws Exception {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        int startObj = aiResponse.indexOf("{");
        int endObj = aiResponse.lastIndexOf("}") + 1;
        int startArr = aiResponse.indexOf("[");
        int endArr = aiResponse.lastIndexOf("]") + 1;

        String jsonOnly;
        boolean isArray = false;

        if (startArr != -1 && endArr != 0 && startArr < endArr && (startObj == -1 || startArr < startObj)) {
            jsonOnly = aiResponse.substring(startArr, endArr);
            isArray = true;
        } else if (startObj != -1 && endObj != 0 && startObj < endObj) {
            jsonOnly = aiResponse.substring(startObj, endObj);
        } else {
            throw new RuntimeException("AI did not return valid JSON. Raw: "
                    + aiResponse.substring(0, Math.min(200, aiResponse.length())));
        }

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> questions = null;

        if (isArray) {
            questions = mapper.readValue(jsonOnly, List.class);
        } else {
            Map<String, Object> parsed = mapper.readValue(jsonOnly, Map.class);
            if (parsed.get("questions") instanceof List) {
                questions = (List<Map<String, Object>>) parsed.get("questions");
            } else if (parsed.get("question") instanceof List) {
                questions = (List<Map<String, Object>>) parsed.get("question");
            } else if (parsed.get("quiz") instanceof List) {
                questions = (List<Map<String, Object>>) parsed.get("quiz");
            }
        }

        if (questions == null || questions.isEmpty()) {
            throw new RuntimeException("AI JSON has no questions array. Found: " + jsonOnly);
        }

        Quiz quiz = new Quiz();
        quiz.setJob(job);
        quiz.setPassingScore(50);

        List<Question> questionList = new ArrayList<>();

        for (Map<String, Object> q : questions) {

            String questionText = q.get("question") != null
                    ? q.get("question").toString() : null;

            List<String> options = new ArrayList<>();
            Object rawOptions = q.get("options");

            if (rawOptions instanceof List<?> rawList) {
                for (Object item : rawList) {
                    if (item instanceof String s) {
                        options.add(s);
                    } else if (item instanceof Map<?, ?> itemMap) {
                        Object val = itemMap.values().stream().findFirst().orElse(null);
                        if (val != null) options.add(val.toString());
                    } else if (item != null) {
                        options.add(item.toString());
                    }
                }
            }

            String correct = null;
            Object rawCorrect = q.get("correctAnswer");
            if (rawCorrect == null) rawCorrect = q.get("answer");

            if (rawCorrect instanceof String s) {
                correct = s;
            } else if (rawCorrect instanceof Map<?, ?> m) {
                Object val = m.values().stream().findFirst().orElse(null);
                if (val != null) correct = val.toString();
            } else if (rawCorrect != null) {
                correct = rawCorrect.toString();
            }

            if (questionText == null || options.size() < 4) {
                LOGGER.warn("Skipping malformed AI quiz question: {}", q);
                continue;
            }

            if (correct != null && correct.length() == 1 && Character.isLetter(correct.charAt(0))) {
                int index = correct.toUpperCase().charAt(0) - 'A';
                if (index >= 0 && index < options.size()) {
                    correct = options.get(index);
                }
            }

            String matchedCorrect = null;
            if (correct != null) {
                for (String opt : options) {
                    if (opt.trim().equalsIgnoreCase(correct.trim())) {
                        matchedCorrect = opt;
                        break;
                    }
                }
            }
            if (matchedCorrect != null) {
                correct = matchedCorrect;
            } else {
                correct = options.get(0);
            }

            Question question = new Question();
            question.setQuestionText(questionText);
            question.setOptionA(options.get(0));
            question.setOptionB(options.get(1));
            question.setOptionC(options.get(2));
            question.setOptionD(options.get(3));
            question.setCorrectAnswer(correct);
            question.setQuiz(quiz);
            questionList.add(question);
        }

        if (questionList.isEmpty()) {
            throw new RuntimeException("All questions were malformed. Check AI response format.");
        }

        quiz.setQuestions(questionList);
        return quiz;
    }
    private final AIService aiService;


    public Quiz generateAndSaveQuiz(String topic, Long jobId) throws Exception {
        return createQuizFromAI(topic, jobId, requestAIQuiz(topic));
    }

    private String requestAIQuiz(String topic) {
        String prompt = """
        Generate exactly 5 multiple choice questions about: %s

        Return a JSON object with this exact structure:
        {
          "questions": [
            {
              "question": "question text",
              "options": ["option1", "option2", "option3", "option4"],
              "correctAnswer": "option1"
            }
          ]
        }

        Rules:
        - Each question must have exactly 4 options
        - correctAnswer must be copied exactly from the options array
        - Return nothing but the JSON object
        """.formatted(topic);

        String response = aiService.askAI(prompt, true);
        if (response == null || response.isBlank() || response.startsWith("AI error:")) {
            throw new RuntimeException(response == null || response.isBlank()
                    ? "AI service returned an empty response"
                    : response);
        }
        return response;
    }

    private void validateQuestion(Question question) {
        if (isBlank(question.getQuestionText())
                || isBlank(question.getOptionA())
                || isBlank(question.getOptionB())
                || isBlank(question.getOptionC())
                || isBlank(question.getOptionD())
                || isBlank(question.getCorrectAnswer())) {
            throw new RuntimeException("Every question needs text, four options, and a correct answer");
        }
    }

    private String resolveCorrectAnswer(QuestionDto question) {
        String correct = question.getCorrectAnswer();
        if (correct == null) {
            return null;
        }

        String trimmed = correct.trim();
        if (trimmed.length() == 1 && Character.isLetter(trimmed.charAt(0))) {
            return switch (Character.toUpperCase(trimmed.charAt(0))) {
                case 'A' -> question.getOptionA();
                case 'B' -> question.getOptionB();
                case 'C' -> question.getOptionC();
                case 'D' -> question.getOptionD();
                default -> trimmed;
            };
        }

        return trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
