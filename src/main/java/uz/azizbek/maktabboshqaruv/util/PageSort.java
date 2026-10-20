package uz.azizbek.maktabboshqaruv.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The sort a list page asks for, in the entity's terms: a column the page shows ("fullName",
 * "className") becomes the entity's own fields (lastName + firstName, the class's grade + letter).
 * Anything else is passed on as it came, so an unknown property still gets the usual
 * 400 "Saralash ..." answer (GlobalExceptionHandler) instead of being silently ignored.
 */
public final class PageSort {

    private PageSort() {
    }

    /** sortable: what the page may send → the entity properties to sort by, in order. */
    public static Pageable map(Pageable pageable, Map<String, List<String>> sortable) {
        if (pageable.isUnpaged() || pageable.getSort().isUnsorted()) return pageable;
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order o : pageable.getSort()) {
            List<String> to = sortable.get(o.getProperty());
            if (to == null) {
                orders.add(o);
                continue;
            }
            for (String property : to) orders.add(new Sort.Order(o.getDirection(), property));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                orders.isEmpty() ? Sort.unsorted() : Sort.by(orders));
    }

    /** "  Ali  Valiyev " → "%ali valiyev%" for a lower(...) like :q search; blank → null (no filter). */
    public static String like(String q) {
        return q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase().replaceAll("\\s+", " ") + "%";
    }
}
