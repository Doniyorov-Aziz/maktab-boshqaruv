package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.bot.BotRouter;
import uz.azizbek.maktabboshqaruv.dto.ClassTelegramCodesDto;
import uz.azizbek.maktabboshqaruv.dto.StudentTelegramDto;
import uz.azizbek.maktabboshqaruv.dto.TelegramSimulateRequestDto;
import uz.azizbek.maktabboshqaruv.dto.TelegramStatusDto;
import uz.azizbek.maktabboshqaruv.service.TelegramLinkService;
import uz.azizbek.maktabboshqaruv.telegram.MockTelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramApiException;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/telegram")
public class TelegramController {

    private static final AtomicLong SIMULATED_UPDATE_ID = new AtomicLong(1);

    @Autowired
    private TelegramLinkService telegramLinkService;

    @Autowired
    private BotRouter botRouter;

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

    /** A photo a parent attached (to a message or an absence request), streamed through the bot. */
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @GetMapping("/files/{fileId}")
    public ResponseEntity<byte[]> file(@PathVariable String fileId) {
        try {
            byte[] bytes = telegramClient.downloadFile(fileId);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(bytes);
        } catch (TelegramApiException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * telegram.mock=true only: feeds a fake parent update (message, contact,
     * photo or button tap) to the bot and returns everything the bot did —
     * sent/edited messages with their keyboards, and photo ids.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/mock/updates")
    public List<MockTelegramClient.SentMessage> simulateUpdate(@Valid @RequestBody TelegramSimulateRequestDto request) {
        MockTelegramClient mock = requireMock();
        TelegramModels.User from = new TelegramModels.User(request.getChatId(), false,
                request.getFirstName(), request.getUsername(), request.getLanguageCode());
        TelegramModels.Chat chat = new TelegramModels.Chat(request.getChatId(), "private");

        TelegramModels.Update update;
        if (request.getCallbackData() != null) {
            TelegramModels.Message tapped = new TelegramModels.Message(request.getMessageId(), from, chat, null, null);
            update = new TelegramModels.Update(SIMULATED_UPDATE_ID.getAndIncrement(), null,
                    new TelegramModels.CallbackQuery("sim-" + SIMULATED_UPDATE_ID.get(), from, tapped, request.getCallbackData()));
        } else {
            TelegramModels.Contact contact = request.getContactPhone() == null ? null
                    : new TelegramModels.Contact(request.getContactPhone(), request.getFirstName(),
                    request.getContactUserId() != null ? request.getContactUserId() : request.getChatId());
            List<TelegramModels.PhotoSize> photo = null;
            String text = request.getText();
            String caption = null;
            if (request.getPhotoBase64() != null) {
                String fileId = mock.storeIncomingFile(Base64.getDecoder().decode(request.getPhotoBase64()));
                photo = List.of(new TelegramModels.PhotoSize(fileId, 800, 600));
                caption = text;
                text = null;
            }
            update = new TelegramModels.Update(SIMULATED_UPDATE_ID.getAndIncrement(),
                    new TelegramModels.Message(1L, from, chat, text, contact, photo, caption));
        }

        int before = mock.size();
        botRouter.handle(update);
        return mock.since(before, request.getChatId());
    }

    /**
     * telegram.mock=true only: everything sent to a chat since position {@code from}
     * — including automatic notifications delivered later by the outbox sender.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/mock/messages")
    public java.util.Map<String, Object> mockMessages(@RequestParam Long chatId, @RequestParam(defaultValue = "0") int from) {
        MockTelegramClient mock = requireMock();
        int next = mock.size();
        return java.util.Map.of("next", next, "messages", mock.since(from, chatId));
    }

    /** telegram.mock=true only: a PNG the bot "sent", so the demo can show it. */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/mock/photos/{fileId}")
    public ResponseEntity<byte[]> mockPhoto(@PathVariable String fileId) {
        MockTelegramClient mock = requireMock();
        try {
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(mock.downloadFile(fileId));
        } catch (TelegramApiException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private MockTelegramClient requireMock() {
        if (!(telegramClient instanceof MockTelegramClient mock)) {
            throw new IllegalStateException("Bu endpoint faqat telegram.mock=true rejimida ishlaydi");
        }
        return mock;
    }
}
