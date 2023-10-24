package multicampussa.laams.manager.controller.exam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import multicampussa.laams.home.member.jwt.JwtTokenProvider;
import multicampussa.laams.manager.dto.exam.request.ExamCreateRequest;
import multicampussa.laams.manager.dto.exam.request.ExamUpdateRequest;
import multicampussa.laams.manager.dto.exam.response.ExamDetailResponse;
import multicampussa.laams.manager.dto.exam.response.ExamResponse;
import multicampussa.laams.manager.service.exam.ExamService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "시험 관련")
@RestController
public class ExamController {

    private final ExamService examService;
    private final JwtTokenProvider jwtTokenProvider;

    public ExamController(ExamService examService, JwtTokenProvider jwtTokenProvider) {
        this.examService = examService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // 시험 생성
    @ApiOperation("시험 생성")
    @PostMapping("/manager/exam")
    public void saveExam(@RequestBody ExamCreateRequest request) {
        examService.saveExam(request);
    }

    // 시험 목록 조회
    @ApiOperation("시험 목록 조회")
    @GetMapping("/manager/exams")
    public List<ExamResponse> getExams() {
        return examService.getExams();
    }

    // 시험 상세 조회
    @ApiOperation("시험 상세 조회")
    @GetMapping("/manager/exam/{no}")
    public ExamDetailResponse getExam(@PathVariable Long no) {
        return examService.getExam(no);
    }

    // 시험 수정
    @ApiOperation("시험 수정")
    @PutMapping("/manager/exam")
    public void updateExam(@RequestBody ExamUpdateRequest request) {
        examService.updateExam(request);
    }

    // 시험 삭제
    @ApiOperation("시험 삭제")
    @DeleteMapping("/manager/exam/{no}")
    public void deleteExam(@PathVariable Long no) {
        examService.deleteExam(no);
    }
}