package uz.azizbek.maktabboshqaruv.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import uz.azizbek.maktabboshqaruv.entity.AppealEnums.Status;
import uz.azizbek.maktabboshqaruv.entity.AppealEnums.Target;
import uz.azizbek.maktabboshqaruv.entity.AppealMessage;
import uz.azizbek.maktabboshqaruv.exception.ForbiddenException;
import uz.azizbek.maktabboshqaruv.service.AccountStatusService;
import uz.azizbek.maktabboshqaruv.service.SchoolAccessService;
import uz.azizbek.maktabboshqaruv.service.appeal.AppealService;
import uz.azizbek.maktabboshqaruv.service.appeal.AppealViews;
import uz.azizbek.maktabboshqaruv.service.appeal.AttachmentLinks;
import uz.azizbek.maktabboshqaruv.service.appeal.AttachmentPolicy;
import uz.azizbek.maktabboshqaruv.service.appeal.FileStorage;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * "Murojaatlar": parents' appeals (from the bot) and appeals written down by staff.
 * Who sees what is decided in {@link AppealService}: admins of the school see every
 * appeal, a class teacher sees the appeals addressed to them for their own classes.
 */
@RestController
@RequestMapping("/api/appeals")
public class AppealController {

    private final AppealService appeals;
    private final SchoolAccessService access;
    private final AttachmentLinks links;
    private final FileStorage storage;
    private final AccountStatusService accountStatus;

    public AppealController(AppealService appeals, SchoolAccessService access, AttachmentLinks links,
                            FileStorage storage, AccountStatusService accountStatus) {
        this.appeals = appeals;
        this.access = access;
        this.links = links;
        this.storage = storage;
        this.accountStatus = accountStatus;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public Page<AppealViews.Row> list(@RequestParam Long schoolId,
                                      @RequestParam(required = false) Status status,
                                      @RequestParam(required = false) Target target,
                                      @RequestParam(required = false) Long classId,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                      @RequestParam(required = false) String q,
                                      Pageable pageable) {
        return appeals.list(access.caller(), schoolId, status, target, classId, from, to, q, pageable);
    }

    /** The sidebar badge: unread parent messages this user may see. */
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/unread")
    public long unread(@RequestParam Long schoolId) {
        return appeals.unread(access.caller(), schoolId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/{id}")
    public AppealViews.Chat chat(@PathVariable Long id) {
        return appeals.chat(access.caller(), id);
    }

    /** A reply: text and/or up to 10 files (photo, video, audio, document; 20 MB each). */
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping(value = "/{id}/reply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AppealViews.Chat reply(@PathVariable Long id,
                                  @RequestParam(required = false) String text,
                                  @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        return appeals.reply(access.caller(), id, text, files);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping("/{id}/close")
    public AppealViews.Row close(@PathVariable Long id) {
        return appeals.close(access.caller(), id);
    }

    /** An appeal received by phone or in person, with the date and time it really came in. */
    public record ManualAppeal(@NotNull Long studentId, Target target,
                               @NotBlank @Size(max = 4000) String text,
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at,
                               @Size(max = 120) String parentName) {
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping
    public ResponseEntity<AppealViews.Row> create(@Valid @RequestBody ManualAppeal body) {
        return ResponseEntity.status(201).body(appeals.createManual(access.caller(), body.studentId(), body.target(),
                body.text(), body.at(), body.parentName()));
    }

    /**
     * A file of an appeal. Authorised by the signed link the chat page received
     * (?t=…, for {@code <img>/<video>/<audio>}) or by the usual JWT; either way the
     * same visibility check runs again. Served from our own disk with a whitelisted
     * Content-Type; ranges are supported (video/audio seeking).
     */
    @GetMapping("/{id}/attachments/{messageId}")
    public ResponseEntity<Resource> attachment(@PathVariable Long id, @PathVariable Long messageId,
                                               @RequestParam(value = "t", required = false) String token,
                                               @RequestParam(value = "download", defaultValue = "false") boolean download) {
        String username = links.verify(id, messageId, token);
        if (username == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                throw new ForbiddenException("Havola eskirgan — sahifani yangilang");
            }
            username = auth.getName();
        }
        if (!accountStatus.isUsable(username)) throw new ForbiddenException(AccountStatusService.BLOCKED_MESSAGE);
        AppealMessage m = appeals.attachment(access.caller(username), id, messageId);
        String ext = AttachmentPolicy.extensionOf(m.getKind(), m.getOriginalName(), m.getMimeType());
        String name = m.getOriginalName() != null ? m.getOriginalName() : "murojaat-" + id + "-" + messageId + "." + ext;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(AttachmentPolicy.contentType(ext)))
                .header(HttpHeaders.CONTENT_DISPOSITION, (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                        .filename(name, StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePrivate())
                .body(new FileSystemResource(storage.resolve(m.getStoragePath())));
    }
}
