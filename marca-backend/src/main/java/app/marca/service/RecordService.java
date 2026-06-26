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
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final RecordRepository recordRepository;

    @Transactional
    public Record save(Long userId, SaveRecordRequest req) {
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

        return recordRepository.save(record);
    }

    public Optional<Record> findToday(Long userId, LocalDate today) {
        return recordRepository.findByUserIdAndRecordDate(userId, today);
    }

    public Optional<Record> findByDate(Long userId, LocalDate date) {
        return recordRepository.findByUserIdAndRecordDate(userId, date);
    }

    public Page<Record> list(Long userId, int page, int size) {
        return recordRepository.findByUserIdOrderByRecordDateDesc(userId, PageRequest.of(page, size));
    }

    public Optional<Record> random(Long userId, LocalDate today) {
        return recordRepository.findRandomExcluding(userId, today);
    }
}
