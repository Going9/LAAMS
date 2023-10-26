package multicampussa.laams.manager.domain.examinee;

import multicampussa.laams.manager.domain.exam.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamineeRepository extends JpaRepository<Examinee, Long> {

    // 응시자 no로 조회
    Examinee findByNo(Long no);
}
