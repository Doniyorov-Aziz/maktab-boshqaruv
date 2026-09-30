package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.ClassTelegramCodesDto;
import uz.azizbek.maktabboshqaruv.dto.StudentTelegramDto;
import uz.azizbek.maktabboshqaruv.dto.TelegramSimulateRequestDto;
import uz.azizbek.maktabboshqaruv.dto.TelegramStatusDto;
import uz.azizbek.maktabboshqaruv.service.TelegramBotService;
import uz.azizbek.maktabboshqaruv.service.TelegramLinkService;
import uz.azizbek.maktabboshqaruv.telegram.MockTelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/telegram")
public class TelegramController {

    private static final AtomicLong SIMULATED_UPDATE_ID = new AtomicLong(1);

    @Autowired
    private TelegramLinkService telegramLinkService;

    @Autowired
    private TelegramBotService telegramBotService;

    @Autowired
    private TelegramClient telegramClient;

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/status")
    public TelegramStatusDto getStatus() {
        return telegramLinkService.getStatus();
    }

    // Link codes let whoever holds them subscribe to a child's data — not for VIEWERs.
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @GetMapping("/students/{studentId}")
    public StudentTelegramDto getStudentTelegram(@PathVariable Long studentId) {
        return telegramLinkService.getStudentTelegram(studentId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping("/students/{studentId}/regenerate-code")
    public StudentTelegramDto regenerateCode(@PathVariable Long studentId) {
        return telegramLinkService.regenerateCode(studentId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @DeleteMapping("/links/{linkId}")
    public ResponseEntity<String> unlink(@PathVariable Long linkId) {
        telegramLinkService.unlink(linkId);
        return ResponseEntity.ok("Ota-ona bog'lanishi uzildi");
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @GetMapping("/classes/{schoolClassId}/codes")
    public ClassTelegramCodesDto getClassCodes(@PathVariable Long schoolClassId) {
        return telegramLinkService.getClassCodes(schoolClassId);
    }

    /** telegram.mock=true only: feeds a fake parent message to the bot and returns its replies. */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/mock/updates")
    public List<MockTelegramClient.SentMessage> simulateUpdate(@Valid @RequestBody TelegramSimulateRequestDto request) {
        if (!(telegramClient instanceof MockTelegramClient mock)) {
            throw new IllegalStateException("Bu endpoint faqat telegram.mock=true rejimida ishlaydi");
        }
        TelegramModels.User from = new TelegramModels.User(request.getChatId(), false,
                request.getFirstName(), request.getUsername());
        TelegramModels.Contact contact = request.getContactPhone() == null ? null
                : new TelegramModels.Contact(request.getContactPhone(), request.getFirstName(),
                request.getContactUserId() != null ? request.getContactUserId() : request.getChatId());
        TelegramModels.Message message = new TelegramModels.Message(1L, from,
                new TelegramModels.Chat(request.getChatId(), "private"), request.getText(), contact);

        int before = mock.size();
        telegramBotService.handleUpdate(new TelegramModels.Update(SIMULATED_UPDATE_ID.getAndIncrement(), message));
        return mock.since(before, request.getChatId());
    }
}
