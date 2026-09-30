package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.entity.ParentTelegramLink;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
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
class TelegramBotServiceTest {

    private static final long CHAT = 555L;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ParentTelegramLinkRepository linkRepository;

    @Mock
    private TelegramClient telegramClient;

    @InjectMocks
    private TelegramBotService botService;

    private Student ali;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(botService, "clock",
                Clock.fixed(Instant.parse("2026-09-30T05:00:00Z"), ZoneId.of("Asia/Tashkent")));
        ali = student(schoolClass(school(), 5L, 5, "A"), 100L, "Ali", "Valiyev");
        ali.setGuardianPhone("+998 90 123-45-67");
    }

    private TelegramModels.Update text(String text) {
        return new TelegramModels.Update(1, new TelegramModels.Message(1L,
                new TelegramModels.User(CHAT, false, "Ota", "ota_user"),
                new TelegramModels.Chat(CHAT, "private"), text, null));
    }

    private TelegramModels.Update contact(String phone, Long ownerId) {
        return new TelegramModels.Update(1, new TelegramModels.Message(1L,
                new TelegramModels.User(CHAT, false, "Ota", "ota_user"),
                new TelegramModels.Chat(CHAT, "private"), null,
                new TelegramModels.Contact(phone, "Ota", ownerId)));
    }

    private String lastReply() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telegramClient, atLeastOnce()).sendMessage(eq(CHAT), captor.capture(), any());
        return captor.getValue();
    }

    @Test
    void deepLinkCode_linksParentAndConfirms() {
        when(studentRepository.findByTelegramLinkCode("Abc23456XY")).thenReturn(Optional.of(ali));
        when(linkRepository.findByStudentIdAndChatId(100L, CHAT)).thenReturn(Optional.empty());

        botService.handleUpdate(text("/start Abc23456XY"));

        ArgumentCaptor<ParentTelegramLink> saved = ArgumentCaptor.forClass(ParentTelegramLink.class);
        verify(linkRepository).save(saved.capture());
        assertEquals(CHAT, saved.getValue().getChatId());
        assertTrue(saved.getValue().getActive());
        assertEquals("ota_user", saved.getValue().getTelegramUsername());
        assertTrue(lastReply().startsWith("✅ Farzandingiz <b>Ali Valiyev</b>, 5-A sinfiga ulandingiz."));
    }

    @Test
    void wrongCode_saysNotFound_andRevealsNothing() {
        when(studentRepository.findByTelegramLinkCode("WrongCode99")).thenReturn(Optional.empty());

        botService.handleUpdate(text("/start WrongCode99"));

        assertTrue(lastReply().startsWith("❌ Kod topilmadi."));
        verify(linkRepository, never()).save(any());
    }

    @Test
    void malformedCode_neverReachesDatabase() {
        botService.handleUpdate(text("/start ' or 1=1 --"));

        assertTrue(lastReply().startsWith("❌ Kod topilmadi."));
        verifyNoInteractions(studentRepository);
    }

    @Test
    void repeatedWrongCodes_areRateLimited() {
        when(studentRepository.findByTelegramLinkCode(anyString())).thenReturn(Optional.empty());
        for (int i = 0; i < TelegramBotService.MAX_FAILED_CODES; i++) {
            botService.handleUpdate(text("/start Guess000" + i + "X"));
        }
        botService.handleUpdate(text("/start Guess9999X"));

        assertTrue(lastReply().startsWith("⏳"));
        verify(studentRepository, times(TelegramBotService.MAX_FAILED_CODES)).findByTelegramLinkCode(anyString());
    }

    @Test
    void startWithoutCode_offersShareContactButton() {
        botService.handleUpdate(text("/start"));

        verify(telegramClient).sendMessage(eq(CHAT), contains("Raqamni ulashish"),
                isA(TelegramModels.ReplyKeyboardMarkup.class));
    }

    @Test
    void ownContact_linksAllChildrenWithThatGuardianPhone() {
        Student sibling = student(schoolClass(school(), 7L, 7, "B"), 101L, "Vali", "Valiyev");
        sibling.setGuardianPhone("901234567");
        Student stranger = student(schoolClass(school(), 7L, 7, "B"), 102L, "Boshqa", "Bola");
        stranger.setGuardianPhone("+7 990 123 45 67"); // same last 9 digits, different country
        when(studentRepository.findByGuardianPhoneLast9("901234567")).thenReturn(List.of(ali, sibling, stranger));
        when(linkRepository.findByStudentIdAndChatId(anyLong(), eq(CHAT))).thenReturn(Optional.empty());

        botService.handleUpdate(contact("998901234567", CHAT));

        verify(linkRepository, times(2)).save(any());
        String reply = lastReply();
        assertTrue(reply.contains("2 ta farzand ulandi"));
        assertTrue(reply.contains("Ali Valiyev"));
        assertTrue(reply.contains("Vali Valiyev"));
        assertFalse(reply.contains("Boshqa"));
    }

    @Test
    void someoneElsesContact_isRejected() {
        botService.handleUpdate(contact("998901234567", 999L));

        assertTrue(lastReply().contains("faqat <b>o'zingizning</b> raqamingizni"));
        verifyNoInteractions(studentRepository);
        verify(linkRepository, never()).save(any());
    }

    @Test
    void unknownPhone_saysNotFound() {
        when(studentRepository.findByGuardianPhoneLast9("991112233")).thenReturn(List.of());

        botService.handleUpdate(contact("+998991112233", CHAT));

        assertTrue(lastReply().contains("topilmadi"));
    }

    @Test
    void farzandlarim_listsOnlyThisChatsChildren() {
        when(linkRepository.findByChatIdAndActiveTrue(CHAT)).thenReturn(List.of(link(ali, CHAT)));

        botService.handleUpdate(text("/farzandlarim"));

        String reply = lastReply();
        assertTrue(reply.contains("1. <b>Ali Valiyev</b> — 5-A · 1-maktab"));
    }

    @Test
    void stop_deactivatesAllLinksOfChat() {
        when(linkRepository.deactivateByChatId(CHAT)).thenReturn(2);

        botService.handleUpdate(text("/stop"));

        assertTrue(lastReply().startsWith("🔕 Obuna bekor qilindi (2 ta farzand)"));
    }

    @Test
    void unknownText_repliesWithHelp() {
        botService.handleUpdate(text("salom"));
        assertTrue(lastReply().contains("/farzandlarim"));
    }

    @Test
    void groupChats_areIgnored() {
        botService.handleUpdate(new TelegramModels.Update(1, new TelegramModels.Message(1L,
                new TelegramModels.User(CHAT, false, "Ota", null),
                new TelegramModels.Chat(-100L, "group"), "/start Abc23456XY", null)));

        verifyNoInteractions(studentRepository, linkRepository, telegramClient);
    }
}
