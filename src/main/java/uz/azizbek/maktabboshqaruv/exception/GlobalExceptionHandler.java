package uz.azizbek.maktabboshqaruv.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(409).body(e.getMessage());
    }

    /** Another school's data, someone else's appeal, or a role that may not do this — 403, not 500. */
    @ExceptionHandler({ForbiddenException.class, org.springframework.security.access.AccessDeniedException.class})
    public ResponseEntity<String> handleForbidden(RuntimeException e) {
        String message = e instanceof ForbiddenException ? e.getMessage() : "Bu amal uchun ruxsat yo'q";
        return ResponseEntity.status(403).body(message);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleTooBig(Exception e) {
        return ResponseEntity.status(413).body("Fayl juda katta (ilova — 20 MB gacha, kutubxona kitobi — 50 MB gacha)");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<String> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.status(400).body(e.getParameterName() + " parametri kiritilishi shart");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst().orElse("Noto'g'ri ma'lumot");
        return ResponseEntity.status(400).body(message);
    }

    /** A JSON body sent where files are expected (e.g. an appeal reply). */
    @ExceptionHandler(org.springframework.web.multipart.MultipartException.class)
    public ResponseEntity<String> handleNotMultipart(Exception e) {
        return ResponseEntity.status(415).body("So'rov multipart/form-data bo'lishi kerak");
    }

    /**
     * An address nothing answers (e.g. a new page talking to an older backend): a plain 404 for
     * the user instead of Spring's internal "No static resource …" text.
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<java.util.Map<String, String>> handleNoResource(Exception e) {
        log.warn("Manzil topilmadi: {}", e.getMessage());
        return ResponseEntity.status(404).body(java.util.Map.of("message", "Manzil topilmadi"));
    }

    /**
     * A database rule said no (a delete that other rows still point to, a duplicate, a too long
     * text): 409 / 400 with a plain reason instead of 500 with the SQL and constraint names.
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException e) {
        java.sql.SQLException sql = causeOf(e, java.sql.SQLException.class);
        String state = sql == null || sql.getSQLState() == null ? "" : sql.getSQLState();
        String text = sql == null || sql.getMessage() == null ? "" : sql.getMessage();
        log.warn("Baza cheklovi ({}): {}", state, text);
        return switch (state) {
            case "23503" -> {
                // a new or changed row names a record that does not exist
                if (text.contains("is not present")) {
                    yield ResponseEntity.status(404).body("Ko'rsatilgan bog'liq yozuv topilmadi");
                }
                java.util.regex.Matcher m = LAST_TABLE.matcher(text);
                String table = m.find() ? m.group(1) : null;
                String what = table == null ? null : TABLE_NAMES.get(table);
                yield ResponseEntity.status(409).body(what == null
                        ? "Bu yozuvga boshqa ma'lumotlar bog'langan, shuning uchun o'chirib bo'lmaydi"
                        : "Bu yozuvga " + what + " bog'langan — avval ularni o'chiring yoki boshqasiga o'tkazing");
            }
            case "23505" -> ResponseEntity.status(409).body("Bunday ma'lumot allaqachon mavjud");
            case "22001" -> {
                java.util.regex.Matcher m = MAX_LENGTH.matcher(text);
                yield ResponseEntity.status(400).body(m.find()
                        ? "Matn juda uzun: ko'pi bilan " + m.group(1) + " belgi bo'lishi mumkin"
                        : "Matn juda uzun");
            }
            case "23502" -> ResponseEntity.status(400).body("Majburiy maydon to'ldirilmagan");
            default -> ResponseEntity.status(409).body("Ma'lumot bazadagi qoidaga zid");
        };
    }

    /** "on table "room"" — the last one names the table whose rows still point here. */
    private static final java.util.regex.Pattern LAST_TABLE = java.util.regex.Pattern.compile("on table \"(\\w+)\"(?!.*on table)", java.util.regex.Pattern.DOTALL);
    private static final java.util.regex.Pattern MAX_LENGTH = java.util.regex.Pattern.compile("varying\\((\\d+)\\)");
    private static final java.util.Map<String, String> TABLE_NAMES = java.util.Map.ofEntries(
            java.util.Map.entry("room", "xonalar"), java.util.Map.entry("building", "binolar"),
            java.util.Map.entry("students", "o'quvchilar"), java.util.Map.entry("employee", "xodimlar"),
            java.util.Map.entry("school_class", "sinflar"), java.util.Map.entry("academic_year", "o'quv yillari"),
            java.util.Map.entry("subject", "fanlar"), java.util.Map.entry("lesson_slot", "dars jadvali"),
            java.util.Map.entry("grade", "baholar"), java.util.Map.entry("attendance", "davomat yozuvlari"),
            java.util.Map.entry("behavior_record", "xulq yozuvlari"), java.util.Map.entry("app_user", "foydalanuvchilar"),
            java.util.Map.entry("announcement", "e'lonlar"), java.util.Map.entry("calendar_event", "tadbirlar"),
            java.util.Map.entry("absence_request", "arizalar"), java.util.Map.entry("appeal", "murojaatlar"),
            java.util.Map.entry("parent_telegram_link", "ota-ona Telegram bog'lanishlari"),
            java.util.Map.entry("book", "kitoblar"), java.util.Map.entry("broadcast", "xabarnomalar"));

    /** A number where text was sent (?schoolId=abc, /students/abc). */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(400).body(e.getName() + " noto'g'ri qiymat: " + e.getValue());
    }

    /** A body that is not valid JSON, a missing body, or a field of the wrong kind (a date, a role). */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleNotReadable(org.springframework.http.converter.HttpMessageNotReadableException e) {
        tools.jackson.databind.DatabindException bad = causeOf(e, tools.jackson.databind.DatabindException.class);
        if (bad != null && !bad.getPath().isEmpty()) {
            String field = bad.getPath().stream().map(r -> r.getPropertyName() != null ? r.getPropertyName() : "[" + r.getIndex() + "]")
                    .collect(java.util.stream.Collectors.joining("."));
            return ResponseEntity.status(400).body(field + ": noto'g'ri qiymat");
        }
        if (e.getMessage() != null && e.getMessage().startsWith("Required request body is missing")) {
            return ResponseEntity.status(400).body("So'rov ma'lumoti yuborilmagan");
        }
        return ResponseEntity.status(400).body("So'rov ma'lumoti noto'g'ri (JSON)");
    }

    /** A date or month in the wrong format (?month=2026-13). */
    @ExceptionHandler(java.time.format.DateTimeParseException.class)
    public ResponseEntity<String> handleDateFormat(java.time.format.DateTimeParseException e) {
        return ResponseEntity.status(400).body("Sana noto'g'ri formatda: " + e.getParsedString());
    }

    /** ?sort= with a field the list does not have. */
    @ExceptionHandler({org.springframework.dao.InvalidDataAccessApiUsageException.class,
            org.springframework.data.core.PropertyReferenceException.class})
    public ResponseEntity<String> handleBadSort(RuntimeException e) {
        if (e instanceof org.springframework.data.core.PropertyReferenceException p) {
            return ResponseEntity.status(400).body("Saralash maydoni noto'g'ri: " + p.getPropertyName());
        }
        if (causeOf(e, org.hibernate.query.sqm.PathElementException.class) != null
                || causeOf(e, org.hibernate.query.sqm.UnknownPathException.class) != null) {
            return ResponseEntity.status(400).body("Saralash maydoni noto'g'ri");
        }
        return handleGeneral(e);
    }

    private static <T extends Throwable> T causeOf(Throwable e, Class<T> type) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (type.isInstance(t)) return type.cast(t);
        }
        return null;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneral(Exception e) {
        // Spring's own errors (404, 405, 415, ...) keep their status instead of becoming 500
        if (e instanceof org.springframework.web.ErrorResponse er) {
            return ResponseEntity.status(er.getStatusCode()).body(er.getBody().getDetail());
        }
        // the details (SQL, class names) go to the log only, never to the browser
        log.error("Kutilmagan xato", e);
        return ResponseEntity.status(500).body("Kutilmagan xato yuz berdi");
    }
}