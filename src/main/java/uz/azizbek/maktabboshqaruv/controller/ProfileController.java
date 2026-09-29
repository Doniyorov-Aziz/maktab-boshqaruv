package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.ClassProfileDto;
import uz.azizbek.maktabboshqaruv.dto.StudentProfileDto;
import uz.azizbek.maktabboshqaruv.dto.TeacherProfileDto;
import uz.azizbek.maktabboshqaruv.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @GetMapping("/students/{id}")
    public StudentProfileDto getStudentProfile(@PathVariable Long id) {
        return profileService.getStudentProfile(id);
    }

    @GetMapping("/teachers/{id}")
    public TeacherProfileDto getTeacherProfile(@PathVariable Long id) {
        return profileService.getTeacherProfile(id);
    }

    @GetMapping("/classes/{id}")
    public ClassProfileDto getClassProfile(@PathVariable Long id) {
        return profileService.getClassProfile(id);
    }
}
