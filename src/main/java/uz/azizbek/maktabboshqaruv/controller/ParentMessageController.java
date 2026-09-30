package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.ParentMessageDto;
import uz.azizbek.maktabboshqaruv.dto.TextRequestDto;
import uz.azizbek.maktabboshqaruv.entity.ParentMessageStatus;
import uz.azizbek.maktabboshqaruv.service.ParentMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.security.Principal;

/** "Murojaatlar": messages parents wrote from the bot, and replying to them. */
@RestController
@RequestMapping("/api/parent-messages")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
public class ParentMessageController {

    @Autowired
    private ParentMessageService parentMessageService;

    @Autowired
    private uz.azizbek.maktabboshqaruv.telegram.TelegramClient telegramClient;

    @GetMapping
    public Page<ParentMessageDto> list(@RequestParam Long schoolId,
                                       @RequestParam(required = false) ParentMessageStatus status,
                                       Pageable pageable) {
        return parentMessageService.list(schoolId, status, pageable);
    }

    @GetMapping("/count-new")
    public long countNew(@RequestParam Long schoolId) {
        return parentMessageService.countNew(schoolId);
    }

    /** The photo the parent attached, streamed from Telegram (the file id itself is never exposed). */
    @GetMapping("/{id}/photo")
    public org.springframework.http.ResponseEntity<byte[]> photo(@PathVariable Long id) {
        String fileId = parentMessageService.get(id).getPhotoFileId();
        return PhotoResponses.of(telegramClient, fileId);
    }

    @PostMapping("/{id}/reply")
    public ParentMessageDto reply(@PathVariable Long id, @Valid @RequestBody TextRequestDto request, Principal principal) {
        return parentMessageService.reply(id, request.getText(), principal == null ? null : principal.getName());
    }
}
