package uz.azizbek.maktabboshqaruv.service;

/**
 * One colour per subject on every page and in the PDF: the colour picked on the subjects page,
 * else one of 12 from the subject's id (frontend: src/utils/subjectColor.js, the same order).
 */
public final class SubjectColors {

    /** The light-theme palette (the PDF is printed on white). */
    public static final String[] PALETTE = {"#6d28d9", "#0284c7", "#059669", "#d97706", "#db2777", "#4f46e5",
            "#0d9488", "#dc2626", "#2563eb", "#65a30d", "#c2410c", "#9333ea"};

    private SubjectColors() {
    }

    /** 0..11 for an automatic colour. */
    public static int index(Long subjectId) {
        return subjectId == null ? 0 : (int) Math.floorMod(subjectId, (long) PALETTE.length);
    }

    public static String of(Long subjectId, String chosen) {
        return chosen != null && !chosen.isBlank() ? chosen : PALETTE[index(subjectId)];
    }
}
