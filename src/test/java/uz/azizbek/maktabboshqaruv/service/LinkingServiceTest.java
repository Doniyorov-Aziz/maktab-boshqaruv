package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.entity.ParentTelegramLink;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static uz.azizbek.maktabboshqaruv.service.TelegramTestFixtures.*;

@ExtendWith(MockitoExtension.class)
class LinkingServiceTest {

    private static final long CHAT = 555L;
    private static final TelegramModels.User FROM = new TelegramModels.User(CHAT, false, "Ota", "ota_user");

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private uz.azizbek.maktabboshqaruv.service.BotCache botCache;

    @Mock
    private ParentTelegramLinkRepository linkRepository;

    @InjectMocks
    private LinkingService linking;

    private Student ali;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(linking, "clock", Clock.fixed(Instant.parse("2026-09-30T05:00:00Z"), ZoneId.of("Asia/Tashkent")));
        ali = student(schoolClass(school(), 5L, 5, "A"), 100L, "Ali", "Valiyev");
        ali.setGuardianPhone("+998 90 123-45-67");
    }

    private TelegramModels.Message contact(String phone, Long owner) {
        return new TelegramModels.Message(1L, FROM, new TelegramModels.Chat(CHAT, "private"), null,
                new TelegramModels.Contact(phone, "Ota", owner));
    }

    @Test
    void validCode_linksActiveParent() {
        when(studentRepository.findByTelegramLinkCode("Abc23456XY")).thenReturn(Optional.of(ali));
        when(linkRepository.findByStudentIdAndChatId(100L, CHAT)).thenReturn(Optional.empty());

        LinkingService.Result r = linking.byCode(CHAT, FROM, "Abc23456XY");

        assertTrue(r.ok());
        assertEquals(List.of(ali), r.linked());
        ArgumentCaptor<ParentTelegramLink> saved = ArgumentCaptor.forClass(ParentTelegramLink.class);
        verify(linkRepository).save(saved.capture());
        assertTrue(saved.getValue().getActive());
        assertEquals("ota_user", saved.getValue().getTelegramUsername());
    }

    @Test
    void wrongCode_sameAnswerAsMalformed_andNothingSaved() {
        when(studentRepository.findByTelegramLinkCode("WrongCode99")).thenReturn(Optional.empty());
        assertEquals("link.code_not_found", linking.byCode(CHAT, FROM, "WrongCode99").errorKey());
        assertEquals("link.code_not_found", linking.byCode(CHAT, FROM, "' or 1=1 --").errorKey());
        verify(studentRepository, times(1)).findByTelegramLinkCode(anyString()); // malformed never queried
        verify(linkRepository, never()).save(any());
    }

    @Test
    void repeatedWrongCodes_areRateLimited() {
        when(studentRepository.findByTelegramLinkCode(anyString())).thenReturn(Optional.empty());
        for (int i = 0; i < LinkingService.MAX_FAILED_CODES; i++) linking.byCode(CHAT, FROM, "Guess000" + i + "X");
        assertEquals("link.rate_limited", linking.byCode(CHAT, FROM, "Guess9999X").errorKey());
        verify(studentRepository, times(LinkingService.MAX_FAILED_CODES)).findByTelegramLinkCode(anyString());
    }

    @Test
    void ownContact_linksEveryChildWithThatNumber_normalized() {
        Student sibling = student(schoolClass(school(), 7L, 7, "B"), 101L, "Vali", "Valiyev");
        sibling.setGuardianPhone("901234567");
        Student stranger = student(schoolClass(school(), 7L, 7, "B"), 102L, "Boshqa", "Bola");
        stranger.setGuardianPhone("+7 990 123 45 67"); // same last 9 digits, other country
        when(studentRepository.findByGuardianPhoneLast9("901234567")).thenReturn(List.of(ali, sibling, stranger));
        when(linkRepository.findByStudentIdAndChatId(anyLong(), eq(CHAT))).thenReturn(Optional.empty());

        LinkingService.Result r = linking.byContact(CHAT, contact("998901234567", CHAT));

        assertTrue(r.ok());
        assertEquals(List.of(ali, sibling), r.linked());
        verify(linkRepository, times(2)).save(any());
    }

    @Test
    void someoneElsesContact_isRefused() {
        assertEquals("link.not_own_contact", linking.byContact(CHAT, contact("998901234567", 999L)).errorKey());
        verifyNoInteractions(studentRepository);
    }

    @Test
    void unknownNumber() {
        when(studentRepository.findByGuardianPhoneLast9("991112233")).thenReturn(List.of());
        assertEquals("link.phone_not_found", linking.byContact(CHAT, contact("+998991112233", CHAT)).errorKey());
    }

    @Test
    void unlink_onlyTouchesThisChatsLink() {
        ParentTelegramLink l = link(ali, CHAT);
        when(linkRepository.findByStudentIdAndChatId(100L, CHAT)).thenReturn(Optional.of(l));
        linking.unlink(CHAT, 100L);
        assertFalse(l.getActive());
    }
}
