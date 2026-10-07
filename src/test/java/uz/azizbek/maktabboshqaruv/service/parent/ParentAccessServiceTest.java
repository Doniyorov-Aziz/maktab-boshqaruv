package uz.azizbek.maktabboshqaruv.service.parent;

import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.ParentSessionRepository;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** The gate every callback / Mini App request goes through: foreign student ids are refused. */
@ExtendWith(MockitoExtension.class)
class ParentAccessServiceTest {

    private static final long CHAT = 555L;

    @Mock
    private ParentTelegramLinkRepository linkRepository;
    @Mock
    private ParentSessionRepository sessionRepository;
    /** the real cache: it must call the loader on a miss */
    @org.mockito.Spy
    private uz.azizbek.maktabboshqaruv.service.BotCache cache = new uz.azizbek.maktabboshqaruv.service.BotCache();

    @InjectMocks
    private ParentAccessService access;

    private Student own;

    @BeforeEach
    void setUp() {
        own = new Student();
        own.setId(100L);
        own.setFirstName("Ali");
        own.setBirthDate(LocalDate.of(2015, 1, 1));
        when(linkRepository.findLinkedStudents(CHAT)).thenReturn(List.of(own));
    }

    @Test
    void ownChild_isReturned() {
        assertSame(own, access.requireLinked(CHAT, 100L));
    }

    @Test
    void someoneElsesChild_isRefused() {
        assertThrows(ParentAccessService.AccessDenied.class, () -> access.requireLinked(CHAT, 27L));
        assertThrows(ParentAccessService.AccessDenied.class, () -> access.requireLinked(CHAT, null));
    }

    @Test
    void selected_fallsBackToALinkedChild_whenTheRememberedOneIsNoLongerLinked() {
        ParentSession session = new ParentSession();
        session.setChatId(CHAT);
        session.setSelectedStudentId(27L); // e.g. unlinked meanwhile, or tampered
        assertSame(own, access.selected(session));
        assertEquals(100L, session.getSelectedStudentId());
        verify(sessionRepository).save(any());
    }
}
