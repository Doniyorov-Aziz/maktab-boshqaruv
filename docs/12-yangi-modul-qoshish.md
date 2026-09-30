# 12 — Yangi modul qo'shish

[← 11 — Muammolar va yechimlar](11-muammolar-va-yechimlar.md) · Keyingisi: [13 — Lug'at →](13-lugat.md)

## Mundarija
- [Misol: "Kutubxona kitoblari" moduli](#misol-kutubxona-kitoblari-moduli)
- [1. Entity](#1-entity)
- [2. Repository](#2-repository)
- [3. DTO](#3-dto)
- [4. Service](#4-service)
- [5. Controller (xavfsizlik va ruxsatlar shu yerda)](#5-controller-xavfsizlik-va-ruxsatlar-shu-yerda)
- [6. Frontend: modules.js — bitta konfiguratsiya, yangi sahifa kerak emas](#6-frontend-modulesjs-bitta-konfiguratsiya-yangi-sahifa-kerak-emas)
- [7. Router va sidebar — hech narsa qilish shart emas](#7-router-va-sidebar-hech-narsa-qilish-shart-emas)
- [8. Seed (ixtiyoriy)](#8-seed-ixtiyoriy)
- [9. Test](#9-test)
- [Tekshirish ro'yxati](#tekshirish-royxati)

## Misol: "Kutubxona kitoblari" moduli

Har bir sinf/maktab uchun kutubxona kitoblari ro'yxatini boshqaradigan yangi modul qo'shamiz: sarlavha, muallif, jami nusxa soni, mavjud nusxa soni. Bu — loyihadagi **oddiy CRUD modul** (Rooms/Buildings kabi) qanday qo'shilishining to'liq amaliy misoli, mavjud uslubga qat'iy rioya qilgan holda.

**Migratsiya kerak emas**: bu loyihada Flyway/Liquibase yo'q — yangi `@Entity` yozilib, ilova qayta ishga tushirilishi bilan (`ddl-auto=update`) jadval avtomatik yaratiladi ([05-malumotlar-bazasi.md](05-malumotlar-bazasi.md#migratsiyalar-bu-loyihada-qanday-ishlaydi)).

## 1. Entity

`src/main/java/uz/azizbek/maktabboshqaruv/entity/LibraryBook.java`:

```java
package uz.azizbek.maktabboshqaruv.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "library_book")
public class LibraryBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private Integer totalCopies;

    @Column(nullable = false)
    private Integer availableCopies;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public School getSchool() { return school; }
    public void setSchool(School school) { this.school = school; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public Integer getTotalCopies() { return totalCopies; }
    public void setTotalCopies(Integer totalCopies) { this.totalCopies = totalCopies; }

    public Integer getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(Integer availableCopies) { this.availableCopies = availableCopies; }
}
```

> **Diqqat**: yangi maydonlar hech biri `nullable=false` bilan **allaqachon to'la jadvalga** qo'shilmayapti (yangi jadval, bo'sh boshlanadi), shuning uchun bu xavfsiz. Agar kelajakda **mavjud, to'la** jadvalga yangi majburiy ustun qo'shsangiz, [05-malumotlar-bazasi.md](05-malumotlar-bazasi.md#migratsiyalar-bu-loyihada-qanday-ishlaydi)dagi ogohlantirishga rioya qiling.

## 2. Repository

`src/main/java/uz/azizbek/maktabboshqaruv/repository/LibraryBookRepository.java`:

```java
package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.LibraryBook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryBookRepository extends JpaRepository<LibraryBook, Long> {
    Page<LibraryBook> findBySchoolId(Long schoolId, Pageable pageable);
}
```

## 3. DTO

`src/main/java/uz/azizbek/maktabboshqaruv/dto/LibraryBookRequestDto.java`:

```java
package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class LibraryBookRequestDto {

    @NotNull(message = "Maktab tanlanishi shart")
    private Long schoolId;

    @NotBlank(message = "Kitob nomi kiritilishi shart")
    private String title;

    @NotBlank(message = "Muallif kiritilishi shart")
    private String author;

    @NotNull @Min(value = 1, message = "Nusxalar soni kamida 1 bo'lishi kerak")
    private Integer totalCopies;

    // getter/setter — BuildingRequestDto.java uslubida
}
```

`src/main/java/uz/azizbek/maktabboshqaruv/dto/LibraryBookResponseDto.java` — tekis (flat) shakl, `schoolName` kabi qo'shimcha o'qish uchun qulay maydonlar bilan:

```java
package uz.azizbek.maktabboshqaruv.dto;

public class LibraryBookResponseDto {
    private Long id;
    private String title;
    private String author;
    private Integer totalCopies;
    private Integer availableCopies;
    private Long schoolId;
    private String schoolName;
    // getter/setter — BuildingResponseDto.java uslubida
}
```

## 4. Service

`src/main/java/uz/azizbek/maktabboshqaruv/service/LibraryBookService.java` — mavjud `BuildingService.java` bilan **bir xil naqsh**:

```java
package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.LibraryBookRequestDto;
import uz.azizbek.maktabboshqaruv.dto.LibraryBookResponseDto;
import uz.azizbek.maktabboshqaruv.entity.LibraryBook;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.LibraryBookRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibraryBookService {

    @Autowired private LibraryBookRepository libraryBookRepository;
    @Autowired private SchoolRepository schoolRepository;

    public Page<LibraryBookResponseDto> getAllBooks(Long schoolId, Pageable pageable) {
        return libraryBookRepository.findBySchoolId(schoolId, pageable).map(this::toResponseDto);
    }

    public LibraryBookResponseDto getBookById(Long id) {
        return toResponseDto(libraryBookRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday kitob topilmadi: " + id)));
    }

    @Transactional
    public LibraryBookResponseDto createBook(LibraryBookRequestDto request) {
        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));

        LibraryBook book = new LibraryBook();
        book.setSchool(school);
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setTotalCopies(request.getTotalCopies());
        book.setAvailableCopies(request.getTotalCopies());   // yangi kitob — hammasi mavjud

        return toResponseDto(libraryBookRepository.save(book));
    }

    @Transactional
    public LibraryBookResponseDto updateBook(Long id, LibraryBookRequestDto request) {
        LibraryBook book = libraryBookRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday kitob topilmadi: " + id));
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setTotalCopies(request.getTotalCopies());
        return toResponseDto(libraryBookRepository.save(book));
    }

    @Transactional
    public void deleteBook(Long id) {
        if (!libraryBookRepository.existsById(id)) {
            throw new IllegalStateException("Bunday kitob topilmadi: " + id);
        }
        libraryBookRepository.deleteById(id);
    }

    private LibraryBookResponseDto toResponseDto(LibraryBook book) {
        LibraryBookResponseDto dto = new LibraryBookResponseDto();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setTotalCopies(book.getTotalCopies());
        dto.setAvailableCopies(book.getAvailableCopies());
        dto.setSchoolId(book.getSchool().getId());
        dto.setSchoolName(book.getSchool().getName());
        return dto;
    }
}
```

## 5. Controller (xavfsizlik va ruxsatlar shu yerda)

`src/main/java/uz/azizbek/maktabboshqaruv/controller/LibraryBookController.java`:

```java
package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.LibraryBookRequestDto;
import uz.azizbek.maktabboshqaruv.dto.LibraryBookResponseDto;
import uz.azizbek.maktabboshqaruv.service.LibraryBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/library-books")
public class LibraryBookController {

    @Autowired private LibraryBookService libraryBookService;

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public Page<LibraryBookResponseDto> getAllBooks(@RequestParam Long schoolId, Pageable pageable) {
        return libraryBookService.getAllBooks(schoolId, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/{id}")
    public ResponseEntity<LibraryBookResponseDto> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(libraryBookService.getBookById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping
    public ResponseEntity<LibraryBookResponseDto> createBook(@Valid @RequestBody LibraryBookRequestDto request) {
        return ResponseEntity.status(201).body(libraryBookService.createBook(request));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}")
    public ResponseEntity<LibraryBookResponseDto> updateBook(@PathVariable Long id, @Valid @RequestBody LibraryBookRequestDto request) {
        return ResponseEntity.ok(libraryBookService.updateBook(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable Long id) {
        libraryBookService.deleteBook(id);
        return ResponseEntity.ok("ID " + id + " bilan kitob muvaffaqiyatli o'chirildi");
    }
}
```

Ruxsat naqshi loyihaning har bir oddiy CRUD moduli bilan bir xil: o'qish — hammaga, yaratish/yangilash — `ADMIN`/`EDITOR`, o'chirish — faqat `ADMIN` ([06-backend.md — Rollar va ruxsatlar](06-backend.md#rollar-va-ruxsatlar)).

Backend endi tayyor. `./gradlew bootRun` bilan qayta ishga tushirilganda `library_book` jadvali avtomatik yaratiladi.

## 6. Frontend: modules.js — bitta konfiguratsiya, yangi sahifa kerak emas

Bu — loyihaning eng katta qulayligi: `pages/CrudPage.vue` **universal** bo'lgani uchun ([08-frontend.md — CrudPage](08-frontend.md#crudpage-universal-komponent)), yangi `.vue` fayl yozish shart **emas**. Faqat `frontend/src/config/modules.js`dagi `modules` massiviga yangi obyekt qo'shiladi:

```js
{
  key: 'library-books',
  title: 'Kutubxona kitoblari',
  icon: 'menu_book',
  color: '#7c3aed',
  group: 'Kundalik hayot',
  endpoint: '/api/library-books',
  schoolScoped: true,
  columns: [
    { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left' },
    { name: 'title', label: 'Nomi', field: 'title', sortable: true, align: 'left' },
    { name: 'author', label: 'Muallif', field: 'author', align: 'left' },
    { name: 'availableCopies', label: 'Mavjud', field: 'availableCopies', align: 'left' },
    { name: 'totalCopies', label: 'Jami', field: 'totalCopies', align: 'left' }
  ],
  fields: [
    { key: 'title', label: 'Nomi', type: 'text', required: true },
    { key: 'author', label: 'Muallif', type: 'text', required: true },
    { key: 'totalCopies', label: 'Nusxalar soni', type: 'number', required: true },
    { key: 'schoolId', label: 'Maktab', autoSchool: true, required: true }
  ]
}
```

Tushuntirish: `group: 'Kundalik hayot'` — sidebar'da mavjud "Kundalik hayot" bo'limiga (E'lonlar, Tadbirlar, Xulq yozuvlari bilan bir qatorda) avtomatik qo'shiladi. `schoolScoped: true` — ro'yxat va forma avtomatik joriy maktabga bog'lanadi (`schoolId: autoSchool: true` maydoni forma yuborilganda avtomatik to'ldiriladi, foydalanuvchi tanlamaydi).

## 7. Router va sidebar — hech narsa qilish shart emas

- **Router**: `/app/:moduleKey` marshruti allaqachon `library-books` kalitini ham avtomatik ushlaydi (`router/routes.js`ga hech narsa qo'shilmaydi).
- **Sidebar**: `MainLayout.vue`dagi `groupedModules` computed'i `modules.js`ni o'qib, avtomatik guruhlaydi — hech qanday qo'lda qo'shish kerak emas ([08-frontend.md — Layout](08-frontend.md#layout-mainlayout-va-pagelayout)).

## 8. Seed (ixtiyoriy)

Agar demo ma'lumot kerak bo'lsa, `config/OperationalDataSeeder.java`ga (yoki yangi, alohida `@Order`li seederga) o'xshash naqshda qo'shiladi — mavjudligini tekshirib, bo'lmasa yaratadi (idempotent, batafsil: [05-malumotlar-bazasi.md — Seed ma'lumotlar](05-malumotlar-bazasi.md#seed-malumotlar)).

## 9. Test

`src/test/java/uz/azizbek/maktabboshqaruv/service/LibraryBookServiceTest.java` — `BuildingServiceTest.java` bilan bir xil Mockito naqshi:

```java
package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.LibraryBookRequestDto;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.LibraryBookRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryBookServiceTest {

    @Mock private LibraryBookRepository libraryBookRepository;
    @Mock private SchoolRepository schoolRepository;
    @InjectMocks private LibraryBookService libraryBookService;

    @Test
    void createBook_unknownSchool_throws() {
        LibraryBookRequestDto request = new LibraryBookRequestDto();
        request.setSchoolId(1L);
        when(schoolRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> libraryBookService.createBook(request));
        verify(libraryBookRepository, never()).save(any());
    }

    @Test
    void deleteBook_notFound_throws() {
        when(libraryBookRepository.existsById(1L)).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> libraryBookService.deleteBook(1L));
    }
}
```

## Tekshirish ro'yxati

| Qadam | Fayl | Bajarilganmi |
|---|---|---|
| Entity | `entity/LibraryBook.java` | ☐ |
| Repository | `repository/LibraryBookRepository.java` | ☐ |
| Request/Response DTO | `dto/LibraryBookRequestDto.java`, `dto/LibraryBookResponseDto.java` | ☐ |
| Service (`@Transactional` yozuvchi metodlarda) | `service/LibraryBookService.java` | ☐ |
| Controller (`@PreAuthorize` har bir metodda) | `controller/LibraryBookController.java` | ☐ |
| Frontend konfiguratsiyasi | `frontend/src/config/modules.js` | ☐ |
| Backend qayta ishga tushirildi, jadval yaratilganini tekshirish | — | ☐ |
| Brauzerda: sidebar'da ko'rinishi, CRUD amallarni qo'lda sinab ko'rish | — | ☐ |
| Test | `test/.../service/LibraryBookServiceTest.java` | ☐ |
| `./gradlew test`, `npm run lint`, `npm run build` xatosiz o'tishi | — | ☐ |

---
Keyingisi: [13 — Lug'at →](13-lugat.md)
