package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.entity.NotificationLog;
import uz.azizbek.maktabboshqaruv.repository.AppealMessageRepository;
import uz.azizbek.maktabboshqaruv.service.appeal.FileStorage;
import uz.azizbek.maktabboshqaruv.telegram.TelegramApiException;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient.MediaKind;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient.SentMedia;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** A 429 between the album and the text: the retry sends only the text, never the album again. */
class OutboxMediaTest {

    private final TelegramClient telegram = mock(TelegramClient.class);

    private OutboxMedia media(int photos) {
        OutboxMedia.Resolver resolver = new OutboxMedia.Resolver() {
            @Override
            public boolean supports(String m) {
                return m.startsWith("broadcast:");
            }

            @Override
            public List<OutboxMedia.File> files(String m) {
                List<OutboxMedia.File> files = new ArrayList<>();
                for (int i = 0; i < photos; i++) {
                    files.add(new OutboxMedia.File(MediaKind.PHOTO, "file-" + i, () -> new byte[0], "r" + i + ".jpg", id -> {
                    }));
                }
                return files;
            }
        };
        return new OutboxMedia(telegram, mock(AppealMessageRepository.class), mock(FileStorage.class), List.of(resolver));
    }

    private static NotificationLog row() {
        NotificationLog n = new NotificationLog();
        n.setChatId(42L);
        n.setText("Ertaga ochiq dars");
        n.setMedia("broadcast:1");
        return n;
    }

    @Test
    void retryAfter429_continuesWithTheText_withoutResendingTheAlbum() {
        OutboxMedia media = media(3);
        when(telegram.sendMediaGroup(anyLong(), anyList()))
                .thenReturn(List.of(new SentMedia(1L, "file-0"), new SentMedia(2L, "file-1"), new SentMedia(3L, "file-2")));
        when(telegram.sendMessage(anyLong(), anyString(), any()))
                .thenThrow(new TelegramApiException(429, "Too Many Requests", 5))
                .thenReturn(10L);
        NotificationLog n = row();
        List<Integer> saved = new ArrayList<>();

        assertThrows(TelegramApiException.class, () -> media.send(n, saved::add));
        assertEquals(1, n.getMediaParts(), "the album was delivered");
        assertEquals(List.of(1), saved, "progress stored before the text was tried");

        media.send(n, saved::add);
        verify(telegram, times(1)).sendMediaGroup(anyLong(), anyList());
        verify(telegram, times(2)).sendMessage(anyLong(), anyString(), any());
        assertEquals(2, n.getMediaParts());
    }

    @Test
    void oneFileWithShortText_isOnePartWithCaption() {
        OutboxMedia media = media(1);
        when(telegram.sendMedia(anyLong(), any(), any())).thenReturn(new SentMedia(1L, "file-0"));
        NotificationLog n = row();
        media.send(n, p -> fail("no intermediate progress for a single part"));
        verify(telegram).sendMedia(eq(42L), argThat(i -> "Ertaga ochiq dars".equals(i.captionHtml())), any());
        verify(telegram, never()).sendMessage(anyLong(), anyString(), any());
        assertEquals(1, n.getMediaParts());
    }
}
