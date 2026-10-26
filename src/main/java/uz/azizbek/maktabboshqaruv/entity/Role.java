package uz.azizbek.maktabboshqaruv.entity;

public enum Role {
    ADMIN,
    EDITOR,
    VIEWER,
    /** The public demo visitor: sees what an ADMIN sees, changes nothing (GuestGuardFilter). */
    GUEST
}