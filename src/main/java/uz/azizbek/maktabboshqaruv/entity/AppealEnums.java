package uz.azizbek.maktabboshqaruv.entity;

/** Enums of the parent appeals ("✉️ Ma'muriyatga xat"). */
public final class AppealEnums {

    private AppealEnums() {
    }

    /** Who the parent writes to. */
    public enum Target {
        ADMINISTRATION, CLASS_TEACHER
    }

    /**
     * DRAFT — the parent is still sending messages (not visible to staff);
     * NEW → SEEN (opened) → ANSWERED (a reply went out) → CLOSED. A new parent
     * message turns any of them back to NEW.
     */
    public enum Status {
        DRAFT, NEW, SEEN, ANSWERED, CLOSED
    }

    /** IN — from the parent; OUT — the school's reply. */
    public enum Direction {
        IN, OUT
    }

    public enum Kind {
        TEXT, PHOTO, VIDEO, VOICE, AUDIO, DOCUMENT, VIDEO_NOTE
    }

    /** Where the file is: PENDING — being downloaded from Telegram; STORED — on our disk. */
    public enum FileState {
        NONE, PENDING, STORED, FAILED
    }

    /** How the appeal reached the school: the bot, or registered by staff (phone call, visit). */
    public enum Source {
        BOT, MANUAL
    }
}
