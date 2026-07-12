package app.marca.service;

import app.marca.config.ApiException;
import app.marca.dto.AnswerInput;
import app.marca.dto.ImageInput;
import app.marca.dto.SaveRecordRequest;
import app.marca.entity.Record;
import app.marca.entity.RecordAnswer;
import app.marca.entity.RecordImage;
import app.marca.repository.RecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecordService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final RecordRepository recordRepository;

    @Transactional
    public Record save(Long userId, SaveRecordRequest req) {
        LocalDate today = LocalDate.now(ZONE);
        if (req.getRecordDate().isAfter(today)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FUTURE_DATE", "还没发生的日子写不了");
        }
        if (!BackfillPolicy.isWithinWindow(today, req.getRecordDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OUT_OF_BACKFILL_WINDOW",
                    "太久之前的日子不能补写了");
        }

        boolean hasAnswer = req.getAnswers() != null && !req.getAnswers().isEmpty();
        boolean hasVoice = req.getVoiceUrl() != null && !req.getVoiceUrl().isBlank();
        boolean hasImage = req.getImages() != null && !req.getImages().isEmpty();
        boolean hasFreeText = req.getFreeText() != null && !req.getFreeText().isBlank();
        if (!hasAnswer && !hasVoice && !hasImage && !hasFreeText) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_RECORD",
                    "今天总得留下点什么");
        }

        Record record = recordRepository.findByUserIdAndRecordDate(userId, req.getRecordDate())
                .orElseGet(() -> Record.builder()
                        .userId(userId)
                        .recordDate(req.getRecordDate())
                        .build());

        // 全删重插：简单可靠，避免 diff 三向合并
        record.getAnswers().clear();
        if (hasAnswer) {
            int order = 0;
            for (AnswerInput in : req.getAnswers()) {
                RecordAnswer answer = RecordAnswer.builder()
                        .record(record)
                        .questionId(in.getQuestionId())
                        .question(in.getQuestion())
                        .category(in.getCategory())
                        .answer(in.getAnswer())
                        .sortOrder(order++)
                        .build();
                record.getAnswers().add(answer);
            }
        }

        record.getImages().clear();
        if (hasImage) {
            int order = 0;
            for (ImageInput in : req.getImages()) {
                RecordImage img = RecordImage.builder()
                        .record(record)
                        .url(in.getUrl())
                        .width(in.getWidth())
                        .height(in.getHeight())
                        .bytes(in.getBytes())
                        .sortOrder(order++)
                        .build();
                record.getImages().add(img);
            }
        }

        record.setVoiceUrl(req.getVoiceUrl());
        record.setVoiceDuration(req.getVoiceDuration());
        record.setFreeText(hasFreeText ? req.getFreeText().trim() : null);
        record.setWeather(normalizeWeather(req.getWeather()));
        record.setMoods(normalizeMoods(req.getMoods()));

        // 显式刷新，不能只靠 @PreUpdate：这次编辑如果只动了 answers/images 子表，
        // Record 自身标量字段没变化，Hibernate 脏检查不会给它发 UPDATE，updatedAt 就刷不出来
        record.setUpdatedAt(LocalDateTime.now());

        return recordRepository.save(record);
    }

    private String normalizeWeather(String w) {
        if (w == null) return null;
        String trimmed = w.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<String> normalizeMoods(List<String> moods) {
        if (moods == null || moods.isEmpty()) return null;
        List<String> cleaned = moods.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        return cleaned.isEmpty() ? null : cleaned;
    }

    public Optional<Record> findToday(Long userId, LocalDate today) {
        return recordRepository.findByUserIdAndRecordDate(userId, today);
    }

    public Optional<Record> findByDate(Long userId, LocalDate date) {
        return recordRepository.findByUserIdAndRecordDate(userId, date);
    }

    // 删除不受补写窗口限制：删除只是移除已写内容，不会像补写/编辑那样伪造发生时间
    @Transactional
    public void deleteByDate(Long userId, LocalDate date) {
        Record record = recordRepository.findByUserIdAndRecordDate(userId, date)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RECORD_NOT_FOUND", "这条记录不存在"));
        recordRepository.delete(record);
    }

    public Page<Record> list(Long userId, int page, int size) {
        return recordRepository.findByUserIdOrderByRecordDateDesc(userId, PageRequest.of(page, size));
    }

    public List<Record> byMonth(Long userId, YearMonth month) {
        return recordRepository.findByUserIdAndRecordDateBetweenOrderByRecordDateDesc(
                userId, month.atDay(1), month.atEndOfMonth());
    }

    public Optional<Record> random(Long userId, LocalDate today, LocalDate extraExclude) {
        // 永远排除「今天」（回看是面向历史的）；可再追加一个 extraExclude
        // （比如从某天详情页点🎲，把当前看的那条也排除掉，避免抽到自己）
        Set<LocalDate> excludes = new LinkedHashSet<>();
        excludes.add(today);
        if (extraExclude != null && !extraExclude.equals(today)) {
            excludes.add(extraExclude);
        }
        return recordRepository.findRandomExcluding(userId, excludes);
    }
}
