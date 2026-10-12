package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.BookFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookFavoriteRepository extends JpaRepository<BookFavorite, Long> {

    Optional<BookFavorite> findByBookIdAndChatId(Long bookId, Long chatId);
}
