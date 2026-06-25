package app.marca.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RecordPage {
    private long total;
    private int page;
    private int size;
    private List<RecordDto> items;
}
