package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.telegram.TelegramApiException;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

/** Streams a photo a parent sent to the bot; 404 when there is none or Telegram no longer has it. */
final class PhotoResponses {

    private PhotoResponses() {
    }

    static ResponseEntity<byte[]> of(TelegramClient client, String fileId) {
        if (fileId == null) return ResponseEntity.notFound().build();
        try {
            byte[] bytes = client.downloadFile(fileId);
            MediaType type = bytes.length > 3 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
            return ResponseEntity.ok().contentType(type).cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate()).body(bytes);
        } catch (TelegramApiException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
