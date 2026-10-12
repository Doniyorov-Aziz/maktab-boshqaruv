package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.BookRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookRatingRepository extends JpaRepository<BookRating, Long> {

    Optional<BookRating> findByBookIdAndChatId(Long bookId, Long chatId);
}
