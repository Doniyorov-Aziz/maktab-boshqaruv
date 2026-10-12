package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.BookCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookCategoryRepository extends JpaRepository<BookCategory, Long> {

    List<BookCategory> findBySchoolIdOrderByPositionAscIdAsc(Long schoolId);

    long countBySchoolId(Long schoolId);

    @Query("select coalesce(max(c.position), 0) from BookCategory c where c.school.id = :schoolId")
    int maxPosition(@Param("schoolId") Long schoolId);

    /** Active shelves with the number of books parents can see on each — one query for the bot's library page. */
    @Query("select c, count(b) from BookCategory c left join Book b on b.category = c and b.active = true and b.draft = false "
            + "where c.school.id = :schoolId and c.active = true group by c order by c.position, c.id")
    List<Object[]> activeWithCounts(@Param("schoolId") Long schoolId);
}
