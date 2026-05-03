package com.example.service;

import com.example.dto.CreateQuizDto;
import com.example.dto.QuestionDto;
import com.example.dto.QuizDto;
import com.example.model.*;
import com.example.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class QuizService {

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

    // 🔍 GET QUIZ ENTITY
    public Quiz getQuizEntityByJob(Long jobId) {
        return quizRepository.findByJobId(jobId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
    }

    // 🔍 GET QUIZ DTO
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

    // 🧠 SUBMIT QUIZ (FIXED)
    public int submitQuiz(Long jobId, String email, Map<Long, String> answers) {

        Quiz quiz = getQuizEntityByJob(jobId);

        int correctAnswers = 0;
        int totalQuestions = quiz.getQuestions().size();

        for (Question q : quiz.getQuestions()) {
            String userAnswer = answers.get(q.getId());
            if (userAnswer == null) {
                Object val = ((Map<?, ?>) answers).get(String.valueOf(q.getId()));
                if (val instanceof String) {
                    userAnswer = (String) val;
                }
            }

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

        QuizResult result = new QuizResult();
        result.setScore(score);
        result.setCandidate(user);
        result.setJob(job);
        resultRepository.save(result);
        Application app = applicationRepository
                .findByJobIdAndCandidateId(jobId, user.getId())
                .orElseThrow(() -> new RuntimeException("Application not found"));

        int passingScore = (quiz.getPassingScore() != null)
                ? quiz.getPassingScore()
                : 50;

        app.setQuizScore(score);
        app.setQuizPassed(score >= passingScore);

        if (!app.getQuizPassed()) {
            app.setStatus("REJECTED");
        }

        applicationRepository.save(app);

        return score;
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

        for (QuestionDto qDto : dto.getQuestions()) {
            Question q = new Question();

            q.setQuestionText(qDto.getQuestionText());
            q.setOptionA(qDto.getOptionA());
            q.setOptionB(qDto.getOptionB());
            q.setOptionC(qDto.getOptionC());
            q.setOptionD(qDto.getOptionD());
            q.setCorrectAnswer(qDto.getCorrectAnswer());

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

        // Clear existing questions (orphanRemoval handles deletion)
        quiz.getQuestions().clear();

        List<Question> newQuestions = new ArrayList<>();
        for (QuestionDto qDto : dto.getQuestions()) {
            Question q = new Question();
            q.setQuestionText(qDto.getQuestionText());
            q.setOptionA(qDto.getOptionA());
            q.setOptionB(qDto.getOptionB());
            q.setOptionC(qDto.getOptionC());
            q.setOptionD(qDto.getOptionD());
            q.setCorrectAnswer(qDto.getCorrectAnswer());
            q.setQuiz(quiz);
            newQuestions.add(q);
        }

        quiz.getQuestions().addAll(newQuestions);

        return quizRepository.save(quiz);
    }
    public Quiz createQuizFromAI(String topic, Long jobId, String aiResponse) throws Exception {

        if (quizRepository.findByJobId(jobId).isPresent()) {
            throw new RuntimeException("Quiz already exists for this job");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        int startObj = aiResponse.indexOf("{");
        int endObj = aiResponse.lastIndexOf("}") + 1;
        int startArr = aiResponse.indexOf("[");
        int endArr = aiResponse.lastIndexOf("]") + 1;

        String jsonOnly;
        boolean isArray = false;

        if (startArr != -1 && endArr != 0 && startArr < endArr && (startObj == -1 || startArr < startObj)) {
            // It's an array
            jsonOnly = aiResponse.substring(startArr, endArr);
            isArray = true;
        } else if (startObj != -1 && endObj != 0 && startObj < endObj) {
            // It's an object
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
            // ✅ Handle both "questions" and "question" keys
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

            // ✅ Safe string extraction — toString() handles String, Map, Integer, etc.
            String questionText = q.get("question") != null
                    ? q.get("question").toString() : null;

            // ✅ Safe options extraction — each option may be a String or a Map
            List<String> options = new ArrayList<>();
            Object rawOptions = q.get("options");

            if (rawOptions instanceof List<?> rawList) {
                for (Object item : rawList) {
                    if (item instanceof String s) {
                        options.add(s);
                    } else if (item instanceof Map<?, ?> itemMap) {
                        // AI returned {"text": "option"} instead of "option"
                        Object val = itemMap.values().stream().findFirst().orElse(null);
                        if (val != null) options.add(val.toString());
                    } else if (item != null) {
                        options.add(item.toString());
                    }
                }
            }

            // ✅ Safe correctAnswer extraction
            String correct = null;
            Object rawCorrect = q.get("correctAnswer");
            if (rawCorrect == null) rawCorrect = q.get("answer"); // fallback key

            if (rawCorrect instanceof String s) {
                correct = s;
            } else if (rawCorrect instanceof Map<?, ?> m) {
                // AI returned {"text": "answer"} instead of "answer"
                Object val = m.values().stream().findFirst().orElse(null);
                if (val != null) correct = val.toString();
            } else if (rawCorrect != null) {
                correct = rawCorrect.toString();
            }

            // Skip this question if it's too broken to use
            if (questionText == null || options.size() < 4) {
                System.err.println("Skipping malformed question: " + q);
                continue;
            }

            // Resolve letter (A/B/C/D) to actual option text
            if (correct != null && correct.length() == 1 && Character.isLetter(correct.charAt(0))) {
                int index = correct.toUpperCase().charAt(0) - 'A';
                if (index >= 0 && index < options.size()) {
                    correct = options.get(index);
                }
            }

            // Robust exact/ignorecase/trim fallback
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
        return quizRepository.save(quiz);
    }
    @Autowired private final AIService aiService;


    public Quiz generateAndSaveQuiz(String topic, Long jobId) throws Exception {
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

        String aiResponse = aiService.askAI(prompt);
        return createQuizFromAI(topic, jobId, aiResponse);
    }
}