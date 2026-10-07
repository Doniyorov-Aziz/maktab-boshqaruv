package uz.azizbek.maktabboshqaruv.exception;

/** The user is signed in but this data is not theirs to see (another school, another class's appeal) → 403. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
