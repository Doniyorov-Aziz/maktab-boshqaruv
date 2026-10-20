package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.StudentRequestDto;
import uz.azizbek.maktabboshqaruv.dto.StudentResponseDto;
import uz.azizbek.maktabboshqaruv.entity.District;
import uz.azizbek.maktabboshqaruv.entity.HousingType;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.service.pages.ListStatsService;
import uz.azizbek.maktabboshqaruv.util.PageSort;

import java.util.List;
import java.util.Map;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private BotCache botCache;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private DistrictService districtService;

    @Autowired
    private ListStatsService listStats;

    @Transactional(readOnly = true)
    public Page<StudentResponseDto> getAllStudents(Long schoolId, Pageable pageable) {
        return getAllStudents(schoolId, null, null, pageable);
    }

    /** What the list may sort by: its columns → the entity's fields. */
    private static final Map<String, List<String>> SORTABLE = Map.of(
            "id", List.of("id"),
            "fullName", List.of("lastName", "firstName"),
            "lastName", List.of("lastName", "firstName"),
            "firstName", List.of("firstName", "lastName"),
            "birthDate", List.of("birthDate"),
            "className", List.of("schoolClass.gradeNumber", "schoolClass.sectionLetter", "lastName"));

    /**
     * One page of students with their 30-day numbers: the page (class and places fetched with it),
     * its count, and one statement for every row's attendance, grades and Telegram — 3 in all.
     */
    @Transactional(readOnly = true)
    public Page<StudentResponseDto> getAllStudents(Long schoolId, Long schoolClassId, String q, Pageable pageable) {
        Page<StudentResponseDto> page = studentRepository.search(schoolId, schoolClassId, PageSort.like(q),
                PageSort.map(pageable, SORTABLE)).map(this::toResponseDto);
        Map<Long, ListStatsService.StudentStats> stats = listStats.students(schoolId,
                page.getContent().stream().map(StudentResponseDto::getId).toList());
        for (StudentResponseDto dto : page.getContent()) {
            ListStatsService.StudentStats st = stats.get(dto.getId());
            if (st == null) continue;
            dto.setAttendance30(st.attendance30());
            dto.setAverage30(st.average30());
            dto.setAveragePrev(st.averagePrev());
            dto.setRisk(st.risk());
            dto.setTelegram(st.telegram());
        }
        return page;
    }

    @Transactional(readOnly = true)
    public StudentResponseDto getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday o'quvchi topilmadi: " + id));
        return toResponseDto(student);
    }

    @Transactional
    public StudentResponseDto createStudent(StudentRequestDto request) {
        SchoolClass schoolClass = schoolClassRepository.findById(request.getSchoolClassId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday sinf mavjud emas"));

        Student student = new Student();
        student.setSchoolClass(schoolClass);
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setBirthDate(request.getBirthDate());
        student.setGuardianName(request.getGuardianName());
        student.setGuardianPhone(request.getGuardianPhone());
        student.setGuardianRelation(request.getGuardianRelation());
        applyAddress(student, request);

        Student saved = studentRepository.save(student);
        activityLogService.record(
                schoolClass.getAcademicYear().getSchool(),
                "person_add",
                student.getFirstName() + " " + student.getLastName() + " " + schoolClass.getGradeNumber()
                        + "-" + schoolClass.getSectionLetter() + " sinfiga qo'shildi");
        return toResponseDto(saved);
    }

    @Transactional
    public StudentResponseDto updateStudent(Long id, StudentRequestDto request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday o'quvchi topilmadi: " + id));

        SchoolClass schoolClass = schoolClassRepository.findById(request.getSchoolClassId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday sinf mavjud emas"));

        student.setSchoolClass(schoolClass);
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setBirthDate(request.getBirthDate());
        student.setGuardianName(request.getGuardianName());
        student.setGuardianPhone(request.getGuardianPhone());
        student.setGuardianRelation(request.getGuardianRelation());
        applyAddress(student, request);

        Student updated = studentRepository.save(student);
        botCache.evictAllChats();
        return toResponseDto(updated);
    }

    @Transactional
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday o'quvchi topilmadi: " + id);
        }
        studentRepository.deleteById(id);
        botCache.evictAllChats();
    }

    /**
     * Tug'ilgan joyi, yashash joyi va uy manzili — all optional, but what is given must fit together:
     * each district belongs to its chosen region; a dom/uy number needs the housing type; an
     * apartment (KVARTIRA) has a dom and a kvartira number, a hovli (HOVLI) only its house number.
     */
    private void applyAddress(Student student, StudentRequestDto request) {
        District birth = districtService.forAddress(request.getBirthDistrictId(), request.getBirthRegionId(), "Tug'ilgan joyi");
        District residence = districtService.forAddress(request.getResidenceDistrictId(), request.getResidenceRegionId(), "Yashash joyi");
        String house = blankToNull(request.getHouseNumber());
        String apartment = blankToNull(request.getApartmentNumber());
        HousingType type = request.getHousingType();
        if (type == null && (house != null || apartment != null)) {
            throw bad("Uy turini tanlang: ko'p qavatli uy yoki hovli");
        }
        if (type == HousingType.KVARTIRA && (house == null || apartment == null)) {
            throw bad("Ko'p qavatli uy uchun uy raqami va xonadon raqami kiritilishi shart");
        }
        if (type == HousingType.HOVLI) {
            if (house == null) throw bad("Hovli uchun uy raqami kiritilishi shart");
            if (apartment != null) throw bad("Hovlida xonadon raqami bo'lmaydi");
        }
        student.setBirthDistrict(birth);
        student.setResidenceDistrict(residence);
        student.setMahalla(blankToNull(request.getMahalla()));
        student.setStreet(blankToNull(request.getStreet()));
        student.setHousingType(type);
        student.setHouseNumber(house);
        student.setApartmentNumber(type == HousingType.KVARTIRA ? apartment : null);
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private StudentResponseDto toResponseDto(Student student) {
        StudentResponseDto dto = new StudentResponseDto();
        dto.setId(student.getId());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setFullName(student.getFirstName() + " " + student.getLastName());
        dto.setBirthDate(student.getBirthDate());
        dto.setSchoolClassId(student.getSchoolClass().getId());
        dto.setClassName(student.getSchoolClass().getGradeNumber() + "-" + student.getSchoolClass().getSectionLetter());
        dto.setGuardianName(student.getGuardianName());
        dto.setGuardianPhone(student.getGuardianPhone());
        dto.setGuardianRelation(student.getGuardianRelation() == null ? null : student.getGuardianRelation().name());
        District birth = student.getBirthDistrict();
        District residence = student.getResidenceDistrict();
        dto.setBirthRegionId(birth == null ? null : birth.getRegion().getId());
        dto.setBirthDistrictId(birth == null ? null : birth.getId());
        dto.setBirthPlace(birth == null ? null : birth.placeName());
        dto.setResidenceRegionId(residence == null ? null : residence.getRegion().getId());
        dto.setResidenceDistrictId(residence == null ? null : residence.getId());
        dto.setResidencePlace(residence == null ? null : residence.placeName());
        dto.setMahalla(student.getMahalla());
        dto.setStreet(student.getStreet());
        dto.setHousingType(student.getHousingType() == null ? null : student.getHousingType().name());
        dto.setHouseNumber(student.getHouseNumber());
        dto.setApartmentNumber(student.getApartmentNumber());
        dto.setAddress(student.addressLine());
        dto.setFullAddress(student.fullAddress());
        return dto;
    }
}
