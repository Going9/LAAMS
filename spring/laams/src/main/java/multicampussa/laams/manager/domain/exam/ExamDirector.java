package multicampussa.laams.manager.domain.exam;

import lombok.Getter;
import lombok.NoArgsConstructor;
import multicampussa.laams.director.domain.director.Director;
import multicampussa.laams.director.dto.director.DirectorAttendanceDto;
import multicampussa.laams.global.BaseTimeEntity;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor
public class ExamDirector extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @ManyToOne
    @JoinColumn(name = "exam_no")
    private Exam exam;

    @ManyToOne
    @JoinColumn(name = "director_no")
    private Director director;

    private Boolean directorAttendance = false;
    private Boolean confirm = false;

    public void setExam(Exam exam, Director director) {
        this.exam = exam;
        this.director = director;
    }

    public void updateAttendance(DirectorAttendanceDto directorAttendanceDto) {
        this.directorAttendance = directorAttendanceDto.getDirectorAttendance();
    }

//    public void setDirector(Director director) {
//        this.director = director;
//    }
    public void confirmDirector() {
        this.confirm = true;
    }

}

