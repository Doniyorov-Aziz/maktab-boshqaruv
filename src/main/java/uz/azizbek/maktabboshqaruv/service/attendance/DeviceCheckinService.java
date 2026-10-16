package uz.azizbek.maktabboshqaruv.service.attendance;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.entity.AttendanceStatus;
import uz.azizbek.maktabboshqaruv.entity.DeviceApiKey;
import uz.azizbek.maktabboshqaruv.entity.LessonSlot;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.DeviceApiKeyRepository;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.service.SchoolAccessService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/**
 * Arrivals reported by a device (turnstile, QR reader, face ID) — POST /api/attendance/checkin.
 * Before the first lesson starts → PRESENT; later → LATE by the minutes after that start (the
 * lesson the student walked into), and PRESENT for the lessons after it. Lessons that had already
 * ended stay unmarked. A MANUAL mark is never changed (AttendanceWriter.saveDevice).
 */
@Service
public class DeviceCheckinService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Tashkent");
    private static final SecureRandom RANDOM = new SecureRandom();

    public record CheckinRequest(String studentExternalId, String cardId, String timestamp, String deviceId) {
    }

    public record CheckinResult(long studentId, String studentName, String status, Integer lateMinutes,
                                int lessonsMarked, int lessonsKeptManual) {
    }

    public record KeyView(long id, String name, String prefix, LocalDateTime createdAt, String createdBy,
                          LocalDateTime lastUsedAt, LocalDateTime revokedAt) {
    }

    /** The new key — shown once, never stored in clear. */
    public record CreatedKey(KeyView key, String apiKey) {
    }

    private final DeviceApiKeyRepository keys;
    private final StudentRepository students;
    private final LessonSlotRepository lessons;
    private final SchoolRepository schools;
    private final AttendanceWriter writer;
    private final SchoolDays schoolDays;
    private final SchoolAccessService access;
    private final Clock clock;

    public DeviceCheckinService(DeviceApiKeyRepository keys, StudentRepository students, LessonSlotRepository lessons,
                                SchoolRepository schools, AttendanceWriter writer, SchoolDays schoolDays,
                                SchoolAccessService access, Clock clock) {
        this.keys = keys;
        this.students = students;
        this.lessons = lessons;
        this.schools = schools;
        this.writer = writer;
        this.schoolDays = schoolDays;
        this.access = access;
        this.clock = clock;
    }

    @Transactional
    public CheckinResult checkin(String apiKey, CheckinRequest req) {
        DeviceApiKey key = (apiKey == null || apiKey.isBlank()) ? null
                : keys.findByKeyHashAndRevokedAtIsNull(sha256(apiKey.trim())).orElse(null);
        if (key == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "API kalit noto'g'ri yoki bekor qilingan");
        long schoolId = key.getSchool().getId();
        key.setLastUsedAt(LocalDateTime.now(clock));

        Student student = findStudent(req);
        if (student == null || student.getSchoolClass().getAcademicYear().getSchool().getId() != schoolId) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "O'quvchi topilmadi");
        }
        LocalDateTime at = parseTime(req.timestamp());
        LocalDate date = at.toLocalDate();
        if (!schoolDays.isSchoolDay(schoolId, date)) {
            throw new IllegalStateException("Bu kun dam olish kuni — dars yo'q");
        }
        String weekday = SchoolDays.WEEKDAY_UZ.get(date.getDayOfWeek());
        List<LessonSlot> day = lessons.findBySchoolClassId(student.getSchoolClass().getId()).stream()
                .filter(l -> weekday.equals(l.getWeekday()))
                .sorted(Comparator.comparing(LessonSlot::getStartTime)).toList();
        if (day.isEmpty()) throw new IllegalStateException("Bu kunda sinfning darsi yo'q");

        LocalDateTime firstStart = date.atTime(day.get(0).getStartTime());
        boolean onTime = !at.isAfter(firstStart);
        Integer lateMinutes = onTime ? null : (int) Duration.between(firstStart, at).toMinutes();
        String device = req.deviceId() == null || req.deviceId().isBlank() ? "qurilma" : req.deviceId().trim();
        String note = "Qurilma " + shorten(device) + ": " + at.toLocalTime().withNano(0).toString().substring(0, 5);

        int marked = 0;
        int kept = 0;
        boolean lateGiven = onTime;
        for (LessonSlot l : day) {
            if (!date.atTime(l.getEndTime()).isAfter(at)) continue; // over before the student came
            AttendanceStatus status = lateGiven ? AttendanceStatus.PRESENT : AttendanceStatus.LATE;
            Integer minutes = lateGiven ? null : lateMinutes;
            lateGiven = true;
            Long id = writer.saveDevice(schoolId, new AttendanceWriter.AutoMark(l.getId(), student.getId(), date, status, note, minutes));
            if (id == null) kept++;
            else marked++;
        }
        return new CheckinResult(student.getId(), student.getFirstName() + " " + student.getLastName(),
                onTime ? "PRESENT" : "LATE", lateMinutes, marked, kept);
    }

    // ------------------------------------------------------------------ keys (ADMIN)

    @Transactional(readOnly = true)
    public List<KeyView> list(long schoolId) {
        access.requireSchool(schoolId);
        return keys.findBySchoolIdOrderByCreatedAtDesc(schoolId).stream().map(DeviceCheckinService::view).toList();
    }

    @Transactional
    public CreatedKey create(long schoolId, String name, String username) {
        access.requireSchool(schoolId);
        if (name == null || name.isBlank()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Qurilma nomini kiriting");
        School school = schools.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday maktab topilmadi: " + schoolId));
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String apiKey = "mk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        DeviceApiKey k = new DeviceApiKey();
        k.setSchool(school);
        k.setName(name.trim());
        k.setKeyHash(sha256(apiKey));
        k.setPrefix(apiKey.substring(0, 10));
        k.setCreatedAt(LocalDateTime.now(clock));
        k.setCreatedBy(username);
        return new CreatedKey(view(keys.save(k)), apiKey);
    }

    @Transactional
    public KeyView revoke(long id) {
        DeviceApiKey k = keys.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kalit topilmadi"));
        access.requireSchool(k.getSchool().getId());
        if (k.getRevokedAt() == null) k.setRevokedAt(LocalDateTime.now(clock));
        return view(k);
    }

    private Student findStudent(CheckinRequest req) {
        if (req.cardId() != null && !req.cardId().isBlank()) {
            return students.findByCardId(req.cardId().trim()).orElse(null);
        }
        if (req.studentExternalId() != null && req.studentExternalId().trim().matches("\\d{1,18}")) {
            return students.findById(Long.parseLong(req.studentExternalId().trim())).orElse(null);
        }
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "studentExternalId yoki cardId kerak");
    }

    private LocalDateTime parseTime(String ts) {
        if (ts == null || ts.isBlank()) return LocalDateTime.now(clock);
        try {
            return OffsetDateTime.parse(ts.trim()).atZoneSameInstant(ZONE).toLocalDateTime();
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(ts.trim()); // no offset: Tashkent time
            } catch (DateTimeParseException e2) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "timestamp ISO-8601 bo'lishi kerak, masalan 2026-10-16T08:47:00+05:00");
            }
        }
    }

    private static String shorten(String s) {
        return s.length() > 40 ? s.substring(0, 40) : s;
    }

    private static KeyView view(DeviceApiKey k) {
        return new KeyView(k.getId(), k.getName(), k.getPrefix(), k.getCreatedAt(), k.getCreatedBy(), k.getLastUsedAt(),
                k.getRevokedAt());
    }

    static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
