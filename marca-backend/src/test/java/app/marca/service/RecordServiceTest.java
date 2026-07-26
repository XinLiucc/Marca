package app.marca.service;

import app.marca.config.ApiException;
import app.marca.dto.AnswerInput;
import app.marca.dto.ImageInput;
import app.marca.dto.SaveRecordRequest;
import app.marca.entity.Question;
import app.marca.entity.Record;
import app.marca.entity.RecordAnswer;
import app.marca.entity.RecordImage;
import app.marca.repository.RecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Mock
    private RecordRepository recordRepository;

    @InjectMocks
    private RecordService recordService;

    @Test
    void save_emptyRecord_throwsEmptyRecordException() {
        // given：六个内容来源（answers/voice/images/freeText/weather/moods 里
        // 真正判断的是前四个）全是空的请求，只填了必填的 recordDate
        SaveRecordRequest req = new SaveRecordRequest();
        req.setRecordDate(LocalDate.now());

        // when + then：调用 save 应该抛出 ApiException，而不是真的存进数据库
        ApiException ex = assertThrows(ApiException.class, () -> recordService.save(1L, req));
        assertEquals("EMPTY_RECORD", ex.getCode());
    }

    @Test
    void save_futureDate_throwsFutureDateException() {
        SaveRecordRequest req = new SaveRecordRequest();
        req.setRecordDate(LocalDate.now(ZONE).plusYears(1));
        req.setFreeText("明年的事");

        ApiException ex = assertThrows(ApiException.class, () -> recordService.save(1L, req));
        assertEquals("FUTURE_DATE", ex.getCode());
    }

    @Test
    void save_beyondBackfillWindow_throwsOutOfWindowException() {
        SaveRecordRequest req = new SaveRecordRequest();
        // WINDOW_DAYS = 3，往前推10天必然超窗口，不受测试运行当天日期影响
        req.setRecordDate(LocalDate.now(ZONE).minusDays(10));
        req.setFreeText("很久以前的事");

        ApiException ex = assertThrows(ApiException.class, () -> recordService.save(1L, req));
        assertEquals("OUT_OF_BACKFILL_WINDOW", ex.getCode());
    }

    @Test
    void save_weatherWithWhitespace_trimsBeforeStoring() {
        SaveRecordRequest req = newValidRequest();
        req.setWeather("  sunny  ");
        Record saved = doSave(req);

        assertEquals("sunny", saved.getWeather());
    }

    @Test
    void save_blankWeather_storesNull() {
        SaveRecordRequest req = newValidRequest();
        req.setWeather("   ");
        Record saved = doSave(req);

        assertNull(saved.getWeather());
    }

    @Test
    void save_moodsWithDuplicatesAndBlanks_dedupesTrimsAndFiltersBlank() {
        SaveRecordRequest req = newValidRequest();
        req.setMoods(List.of(" happy ", "happy", "  ", "tired"));
        Record saved = doSave(req);

        assertEquals(List.of("happy", "tired"), saved.getMoods());
    }

    @Test
    void save_moodsAllBlankAfterFiltering_storesNull() {
        SaveRecordRequest req = newValidRequest();
        req.setMoods(List.of("  ", ""));
        Record saved = doSave(req);

        assertNull(saved.getMoods());
    }

    @Test
    void save_existingRecord_replacesOldAnswersAndImages() {
        Record existing = Record.builder()
                .id(9L)
                .userId(1L)
                .recordDate(LocalDate.now(ZONE))
                .build();
        existing.getAnswers().add(RecordAnswer.builder().id(100L).record(existing).questionId(1L)
                .question("旧问题").answer("旧回答").sortOrder(0).build());
        existing.getImages().add(RecordImage.builder().id(200L).record(existing).url("old.jpg").sortOrder(0).build());

        when(recordRepository.findByUserIdAndRecordDate(any(), any())).thenReturn(Optional.of(existing));
        when(recordRepository.save(any(Record.class))).thenAnswer(inv -> inv.getArgument(0));

        SaveRecordRequest req = new SaveRecordRequest();
        req.setRecordDate(LocalDate.now(ZONE));
        AnswerInput answer = new AnswerInput();
        answer.setQuestionId(2L);
        answer.setQuestion("新问题");
        answer.setCategory(Question.Category.event);
        answer.setAnswer("新回答");
        req.setAnswers(List.of(answer));
        ImageInput image = new ImageInput();
        image.setUrl("new.jpg");
        req.setImages(List.of(image));

        Record saved = recordService.save(1L, req);

        assertEquals(1, saved.getAnswers().size());
        assertEquals("新回答", saved.getAnswers().get(0).getAnswer());
        assertEquals(1, saved.getImages().size());
        assertEquals("new.jpg", saved.getImages().get(0).getUrl());
    }

    private SaveRecordRequest newValidRequest() {
        SaveRecordRequest req = new SaveRecordRequest();
        req.setRecordDate(LocalDate.now(ZONE));
        req.setFreeText("占位内容，确保不触发 EMPTY_RECORD");
        return req;
    }

    private Record doSave(SaveRecordRequest req) {
        when(recordRepository.findByUserIdAndRecordDate(any(), any())).thenReturn(Optional.empty());
        ArgumentCaptor<Record> captor = ArgumentCaptor.forClass(Record.class);
        when(recordRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        recordService.save(1L, req);
        return captor.getValue();
    }
}
