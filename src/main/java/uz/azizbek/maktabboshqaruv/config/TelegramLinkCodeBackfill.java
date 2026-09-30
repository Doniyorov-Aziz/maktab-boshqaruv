package uz.azizbek.maktabboshqaruv.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.telegram.LinkCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * telegram_link_code was added as a nullable column (ddl-auto=update cannot
 * add NOT NULL to a table with rows), so students that existed before this
 * feature get their code here, once. Runs after the seeders.
 */
@Component
@Order(10)
public class TelegramLinkCodeBackfill implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TelegramLinkCodeBackfill.class);

    @Autowired
    private StudentRepository studentRepository;

    @Override
    public void run(String... args) {
        List<Student> missing = studentRepository.findByTelegramLinkCodeIsNull();
        if (missing.isEmpty()) return;

        Set<String> used = new HashSet<>();
        for (Student s : missing) {
            String code;
            do {
                code = LinkCodeGenerator.generate();
            } while (!used.add(code) || studentRepository.existsByTelegramLinkCode(code));
            s.setTelegramLinkCode(code);
        }
        studentRepository.saveAll(missing);
        log.info("Migratsiya: {} ta o'quvchi uchun Telegram bog'lash kodi yaratildi", missing.size());
    }
}
