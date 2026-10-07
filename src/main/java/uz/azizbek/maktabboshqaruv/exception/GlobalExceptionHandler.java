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
        return ResponseEntity.status(413).body("Fayl juda katta (ko'pi bilan 20 MB)");
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneral(Exception e) {
        // Spring's own errors (404, 405, 415, ...) keep their status instead of becoming 500
        if (e instanceof org.springframework.web.ErrorResponse er) {
            return ResponseEntity.status(er.getStatusCode()).body(er.getBody().getDetail());
        }
        log.error("Kutilmagan xato", e);
        return ResponseEntity.status(500).body("Kutilmagan xato: " + e.getMessage());
    }
}