package uz.azizbek.maktabboshqaruv.entity;

/** Enums of the school library ("📚 Kitob mutolaasi"). */
public final class LibraryEnums {

    private LibraryEnums() {
    }

    /**
     * Why the school may hand this book out. Required on every book: a book without
     * a legal basis is never saved.
     */
    public enum LegalBasis {
        /** the author's rights have expired */
        JAMOAT_MULKI,
        /** a state textbook */
        DAVLAT_DARSLIGI,
        /** written permission of the author / publisher */
        MUALLIF_RUXSATI,
        /** an open or bought licence */
        LITSENZIYA,
        /** the school's own material */
        MAKTAB_MATERIALI
    }

    public enum AgeGroup {
        KATTALAR, OSMIRLAR, BOLALAR
    }

    /** Where in the bot the parent found the book they downloaded. */
    public enum DownloadSource {
        SEARCH, CATEGORY, RECOMMENDATION, TOP, NEW, SAVED;

        /** one-letter code used inside callback_data (64-byte limit) */
        public String code() {
            return name().substring(0, 1) + (this == SAVED ? "V" : "");
        }

        public static DownloadSource fromCode(String code) {
            if (code != null) {
                for (DownloadSource s : values()) if (s.code().equals(code)) return s;
            }
            return CATEGORY;
        }
    }
}
