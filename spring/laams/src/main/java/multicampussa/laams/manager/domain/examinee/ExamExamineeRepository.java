package multicampussa.laams.manager.domain.examinee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamExamineeRepository extends JpaRepository<ExamExaminee, Long> {

    // 시험_응시자 엔티티에서 시험 no에 해당하는 응시자 전체 조회
    List<ExamExaminee> findByExamNo(Long examNo);

    // 출석이 true인 응시자 전체 조회
    List<ExamExaminee> findByAttendance(Boolean attendance);

    // 보상대상 여부가 true인 응시자 전체 조회
    List<ExamExaminee> findByCompensation(Boolean compensation);

    // 시험 응시자 상세 조회
    ExamExaminee findByExamNoAndExamineeNo(Long examNo, Long examineeNo);

    // 시험 응시자 수 카운트
    @Query(value = "SELECT count(ee) FROM ExamExaminee ee WHERE ee.exam.no = :examNo")
    int countByExamineeNo(Long examNo);

    // 시험 출결 여부 카운트
    @Query(value = "SELECT count(ee) FROM ExamExaminee ee WHERE ee.exam.no = :examNo AND ee.attendance = true")
    int countByAttendance(Long examNo);

    // 시험 서류 지참 카운트
    @Query(value = "SELECT count(ee) FROM ExamExaminee ee WHERE ee.exam.no = :examNo AND ee.document = true")
    int countByDocument(Long examNo);
}

