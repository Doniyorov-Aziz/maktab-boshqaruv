package uz.azizbek.maktabboshqaruv.entity;

/**
 * Where an attendance mark (or a grade: MANUAL / DEMO_SIM) came from. MANUAL wins over everything: no automatic
 * process changes or deletes it (see AttendanceWriter).
 * <ul>
 *   <li>MANUAL — a staff member marked it (or confirmed an automatic mark);</li>
 *   <li>AUTO_CARRY — copied from the student's previous lesson of the same day;</li>
 *   <li>DEMO_SIM — the demo simulator (demo installations only);</li>
 *   <li>DEVICE — a turnstile / QR / face reader reported the arrival.</li>
 * </ul>
 */
public enum AttendanceSource {
    MANUAL, AUTO_CARRY, DEMO_SIM, DEVICE;

    /** Marked by a person or a device — what "attendance taken" means for teacher discipline. */
    public boolean isReal() {
        return this == MANUAL || this == DEVICE;
    }

    public boolean isAutomatic() {
        return this == AUTO_CARRY || this == DEMO_SIM;
    }
}
