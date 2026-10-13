package uz.azizbek.maktabboshqaruv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.azizbek.maktabboshqaruv.entity.Author;

import java.util.List;
import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    List<Author> findAllByOrderByFullNameAsc();

    Optional<Author> findByFullNameIgnoreCase(String fullName);

    /** Books (of any school) linked to each author: author id, count. */
    @Query(value = "select author_id, count(*) from book_author group by author_id", nativeQuery = true)
    List<Object[]> bookCounts();

    @Query(value = "select count(*) from book_author where author_id = :id", nativeQuery = true)
    long bookCount(@Param("id") Long id);
}
