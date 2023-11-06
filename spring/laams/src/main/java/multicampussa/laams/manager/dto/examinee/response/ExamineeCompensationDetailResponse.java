package multicampussa.laams.manager.dto.examinee.response;

import lombok.Getter;
import multicampussa.laams.manager.domain.examinee.ExamExaminee;

import java.time.LocalDateTime;

@Getter
public class ExamineeCompensationDetailResponse {

    private Long examineeNo;
    private String examineeName;
    private String compensationReason;
    private String centerName;
    private LocalDateTime createdAt;
    private String imageUrl;
    private String imageReason;

    public ExamineeCompensationDetailResponse(ExamExaminee examExaminee) {
        this.examineeNo = examExaminee.getExaminee().getNo();
        this.examineeName = examExaminee.getExaminee().getName();
        this.compensationReason = examExaminee.getCompensationReason();
        this.centerName = examExaminee.getExam().getCenter().getName();
        this.createdAt = examExaminee.getCreatedAt();
        this.imageUrl = examExaminee.getImageUrl();
        this.imageReason = examExaminee.getImageReason();
    }

}
