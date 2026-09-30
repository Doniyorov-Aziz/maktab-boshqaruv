package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.ParentSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParentSessionRepository extends JpaRepository<ParentSession, Long> {
    Optional<ParentSession> findByChatId(Long chatId);

    List<ParentSession> findByChatIdIn(Collection<Long> chatIds);
}
