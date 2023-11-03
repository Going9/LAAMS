package multicampussa.laams.manager.domain.exam;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExamDirectorRepository extends JpaRepository<ExamDirector, Long> {

    // 시험 정보로 ExamDirector 조회
    List<ExamDirector> findByExam(Exam exam);

    // 시험 no로 ExamDirector 조회
    List<ExamDirector> findByExamNo(Long examNo);

    // 시험 no과 감독 no로 조회
    ExamDirector findByExamNoAndDirectorNo(Long examNo, Long directorNo);

    // ExamDirector와 연결된 시험의 날짜와 confirm이 false인 것들 조회
    @Query("SELECT ed FROM ExamDirector ed WHERE DATE(ed.exam.examDate) = :targetDate AND ed.confirm = false")
    List<ExamDirector> findUnconfirmedByDate(@Param("targetDate") java.sql.Date targetDate);

    @Query("select count(*) from ExamDirector ed where ed.exam.no = :no and ed.confirm = true")
    int countByConfirm(Long no);
}
