package app.marca.service;

import app.marca.config.ApiException;
import app.marca.dto.SaveRecordRequest;
import app.marca.repository.RecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class RecordServiceTest {

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
}
