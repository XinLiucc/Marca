package app.marca.controller;

import app.marca.dto.ImageUploadResponse;
import app.marca.dto.RecordDto;
import app.marca.dto.RecordPage;
import app.marca.dto.SaveRecordRequest;
import app.marca.dto.VoiceUploadResponse;
import app.marca.security.UserPrincipal;
import app.marca.service.RecordService;
import app.marca.service.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final RecordService recordService;
    private final StorageService storageService;

    @PostMapping(value = "/voice", consumes = "multipart/form-data")
    public VoiceUploadResponse uploadVoice(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "duration", required = false) Integer duration
    ) {
        var stored = storageService.storeVoice(user.id(), file);
        return new VoiceUploadResponse(stored.url(), duration, stored.bytes());
    }

    @PostMapping(value = "/image", consumes = "multipart/form-data")
    public ImageUploadResponse uploadImage(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestPart("file") MultipartFile file
    ) {
        var stored = storageService.storeImage(user.id(), file);
        return new ImageUploadResponse(stored.url(), stored.width(), stored.height(), stored.bytes());
    }

    @PostMapping
    public RecordDto save(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody SaveRecordRequest req
    ) {
        return RecordDto.from(recordService.save(user.id(), req));
    }

    @GetMapping("/today")
    public ResponseEntity<RecordDto> today(@AuthenticationPrincipal UserPrincipal user) {
        LocalDate today = LocalDate.now(ZONE);
        return recordService.findToday(user.id(), today)
                .map(RecordDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping
    public RecordPage list(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var p = recordService.list(user.id(), page, size);
        var items = p.getContent().stream().map(RecordDto::from).toList();
        return new RecordPage(p.getTotalElements(), page, size, items);
    }

    @GetMapping("/random")
    public ResponseEntity<RecordDto> random(@AuthenticationPrincipal UserPrincipal user) {
        LocalDate today = LocalDate.now(ZONE);
        return recordService.random(user.id(), today)
                .map(RecordDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{date}")
    public ResponseEntity<RecordDto> byDate(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable LocalDate date
    ) {
        return recordService.findByDate(user.id(), date)
                .map(RecordDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
