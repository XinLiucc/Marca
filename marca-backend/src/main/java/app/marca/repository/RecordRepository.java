package app.marca.repository;

import app.marca.entity.Record;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

public interface RecordRepository extends JpaRepository<Record, Long> {

    Optional<Record> findByUserIdAndRecordDate(Long userId, LocalDate recordDate);

    Page<Record> findByUserIdOrderByRecordDateDesc(Long userId, Pageable pageable);

    /** 随机取一条该用户的历史记录，排除给定日期集合（如 today + 当前正在看的那条）。 */
    @Query(value = "SELECT * FROM record WHERE user_id = :userId AND record_date NOT IN (:excludeDates) ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<Record> findRandomExcluding(@Param("userId") Long userId,
                                         @Param("excludeDates") Collection<LocalDate> excludeDates);
}
