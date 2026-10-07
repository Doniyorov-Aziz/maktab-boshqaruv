package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.service.SchoolAccessService;
import uz.azizbek.maktabboshqaruv.service.TelegramParentsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 8.1 "Sinf bo'yicha ota-onalar" and the parent picker of "Ota-onalarga xabar". */
@RestController
@RequestMapping("/api/telegram/parents")
public class TelegramParentsController {

    private final TelegramParentsService parents;
    private final SchoolAccessService access;

    public TelegramParentsController(TelegramParentsService parents, SchoolAccessService access) {
        this.parents = parents;
        this.access = access;
    }

    /** status: ALL (default), LINKED, NOT_LINKED. */
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public TelegramParentsService.ClassParents byClass(@RequestParam Long classId,
                                                       @RequestParam(defaultValue = "ALL") String status) {
        return parents.byClass(access.caller(), classId, status);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/choices")
    public List<TelegramParentsService.Choice> choices(@RequestParam Long schoolId,
                                                       @RequestParam(required = false) Long classId,
                                                       @RequestParam(required = false) String q) {
        access.requireSchool(schoolId);
        return parents.choices(schoolId, classId, q);
    }
}
