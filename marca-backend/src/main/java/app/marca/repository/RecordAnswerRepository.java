package app.marca.repository;

import app.marca.entity.RecordAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RecordAnswerRepository extends JpaRepository<RecordAnswer, Long> {

    /** 该用户最近 since 之后（含当天）答过的所有 question_id，用于出题去重。 */
    @Query("SELECT ra.questionId FROM RecordAnswer ra "
            + "WHERE ra.record.userId = :userId AND ra.record.recordDate >= :since")
    List<Long> findRecentQuestionIds(@Param("userId") long userId, @Param("since") LocalDate since);
}
