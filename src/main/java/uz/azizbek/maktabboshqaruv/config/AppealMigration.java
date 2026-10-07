package uz.azizbek.maktabboshqaruv.config;

import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.entity.AppealEnums.*;
import uz.azizbek.maktabboshqaruv.repository.AppealMessageRepository;
import uz.azizbek.maktabboshqaruv.repository.AppealRepository;
import uz.azizbek.maktabboshqaruv.repository.ParentMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * One-time move of the old "💬 Maktabga yozish" messages (parent_message) into appeals,
 * so the new "Murojaatlar" page shows the whole history: the parent's text (and photo)
 * as the first message, the school's reply as the answer. Each migrated row is marked
 * (created_by = "pm#<id>"), so running it again adds nothing.
 */
@Component
@Order(5)
public class AppealMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AppealMigration.class);

    private final ParentMessageRepository oldMessages;
    private final AppealRepository appeals;
    private final AppealMessageRepository messages;

    public AppealMigration(ParentMessageRepository oldMessages, AppealRepository appeals, AppealMessageRepository messages) {
        this.oldMessages = oldMessages;
        this.appeals = appeals;
        this.messages = messages;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Set<String> done = new HashSet<>(appeals.migratedMarkers());
        int moved = 0;
        for (ParentMessage pm : oldMessages.findAll()) {
            String marker = "pm#" + pm.getId();
            if (done.contains(marker)) continue;
            Appeal a = new Appeal();
            a.setSchool(pm.getSchool());
            a.setStudent(pm.getStudent());
            a.setChatId(pm.getChatId());
            a.setParentName(pm.getParentName());
            a.setParentUsername(pm.getParentUsername());
            a.setTarget(pm.getRecipient() == ParentMessageRecipient.CLASS_TEACHER ? Target.CLASS_TEACHER : Target.ADMINISTRATION);
            boolean answered = pm.getReplyText() != null;
            a.setStatus(answered ? Status.ANSWERED : Status.NEW);
            a.setSource(Source.BOT);
            a.setCreatedAt(pm.getCreatedAt());
            a.setLastMessageAt(answered && pm.getRepliedAt() != null ? pm.getRepliedAt() : pm.getCreatedAt());
            a.setUnreadCount(answered ? 0 : 1);
            a.setCreatedBy(marker);
            String text = pm.getText();
            a.setLastPreview(text == null ? null : (text.length() > 190 ? text.substring(0, 189) + "…" : text));
            a = appeals.save(a);

            AppealMessage in = new AppealMessage();
            in.setAppeal(a);
            in.setDirection(Direction.IN);
            if (pm.getPhotoFileId() != null) {
                in.setKind(Kind.PHOTO);
                in.setFileId(pm.getPhotoFileId());
                in.setMimeType("image/jpeg");
                in.setFileState(FileState.PENDING); // fetched by AppealFiles' retry job
            } else {
                in.setKind(Kind.TEXT);
            }
            in.setText("📎".equals(text) ? null : text);
            in.setCreatedAt(pm.getCreatedAt());
            messages.save(in);
            if (answered) {
                AppealMessage out = new AppealMessage();
                out.setAppeal(a);
                out.setDirection(Direction.OUT);
                out.setKind(Kind.TEXT);
                out.setText(pm.getReplyText());
                out.setSentBy(pm.getRepliedBy());
                out.setCreatedAt(pm.getRepliedAt() != null ? pm.getRepliedAt() : pm.getCreatedAt());
                messages.save(out);
            }
            moved++;
        }
        if (moved > 0) log.info("Eski murojaatlar ko'chirildi: {} ta", moved);
    }
}
