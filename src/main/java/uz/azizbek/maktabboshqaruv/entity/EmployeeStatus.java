package uz.azizbek.maktabboshqaruv.entity;

/** Ishda / Ta'tilda / Ishdan ketgan. A user linked to a non-working employee cannot sign in. */
public enum EmployeeStatus {
    ACTIVE,
    ON_LEAVE,
    DISMISSED
}
