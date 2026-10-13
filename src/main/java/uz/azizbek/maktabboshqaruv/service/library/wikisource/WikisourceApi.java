package uz.azizbek.maktabboshqaruv.service.library.wikisource;

import java.util.List;

/**
 * The few MediaWiki API calls the Wikisource import needs. The real one talks to the internet
 * ({@link HttpWikisourceApi}); tests pass a fake with fixed pages.
 */
public interface WikisourceApi {

    /** The wikitext of a page, or null when there is no such page. */
    String wikitext(String host, String title);

    /** Page titles matching {@code query} in one namespace (e.g. 108 = "Author:" on wikisource.org). */
    List<String> search(String host, String query, int namespace, int limit);

    /** Titles of the pages starting with {@code prefix} (a work's chapter subpages "Title/…"). */
    List<String> pagesWithPrefix(String host, String prefix, int limit);
}
