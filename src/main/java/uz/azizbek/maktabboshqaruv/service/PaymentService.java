package uz.azizbek.maktabboshqaruv.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.telegram.MessageFormatter;

import java.time.*;
import java.util.*;

/**
 * To'lovlar: the school's fee settings, a pupil's own fee, the payments, each pupil's months (paid / part paid /
 * unpaid / not due yet) and the debtors. A month is a debt from the day after its due day. The months counted are
 * those of the pupil's school year (O'quv yillari) up to the current month.
 */
@Service
public class PaymentService {

    public record SettingsDto(boolean enabled, long monthlyFee, int dueDay, boolean remindParents, String payInfo) {
    }

    public record SettingsRequest(@NotNull Boolean enabled,
                                  @NotNull @Min(value = 0, message = "To'lov manfiy bo'lmasin") Long monthlyFee,
                                  @NotNull @Min(value = 1, message = "Kun 1 dan 28 gacha") @Max(value = 28, message = "Kun 1 dan 28 gacha") Integer dueDay,
                                  Boolean remindParents,
                                  @Size(max = 500, message = "500 belgidan oshmasin") String payInfo) {
    }

    public record PaymentRequest(@NotNull(message = "O'quvchi tanlanishi shart") Long studentId,
                                 @NotNull(message = "Oy tanlanishi shart") YearMonth month,
                                 @NotNull(message = "Summa kiritilishi shart") @Min(value = 1, message = "Summa 0 dan katta bo'lsin") Long amount,
                                 LocalDate paidOn,
                                 Payment.Method method,
                                 @Size(max = 200, message = "Izoh 200 belgidan oshmasin") String note) {
    }

    public record PaymentDto(Long id, Long studentId, String studentName, Long schoolClassId, String className,
                             YearMonth month, long amount, LocalDate paidOn, Payment.Method method, String note,
                             String createdBy, LocalDateTime createdAt) {
    }

    public record FeeRequest(@NotNull Long studentId, @Min(0) Long monthlyFee, @Size(max = 200) String note) {
    }

    public enum MonthStatus { PAID, PARTIAL, UNPAID, UPCOMING, FREE }

    public record MonthRow(YearMonth month, long fee, long paid, MonthStatus status, LocalDate dueDate) {
    }

    public record StudentAccount(Long studentId, String name, String className, long monthlyFee, boolean ownFee,
                                 String feeNote, List<MonthRow> months, long due, long paid, long debt,
                                 List<PaymentDto> payments, String payInfo) {
    }

    public record Debtor(Long studentId, String name, Long schoolClassId, String className, String guardianName,
                         String guardianPhone, long debt, int monthsUnpaid, YearMonth oldestUnpaid, LocalDate lastPaidOn,
                         boolean telegram) {
    }

    public record Summary(YearMonth month, long expected, long collected, long debt, int debtors, int students,
                          List<Debtor> rows) {
    }

    private final PaymentRepository payments;
    private final PaymentSettingsRepository settingsRepo;
    private final StudentFeeRepository fees;
    private final StudentRepository students;
    private final SchoolRepository schools;
    private final NotificationService notifications;
    private final SchoolAccessService access;
    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public PaymentService(PaymentRepository payments, PaymentSettingsRepository settingsRepo, StudentFeeRepository fees,
                          StudentRepository students, SchoolRepository schools, NotificationService notifications,
                          SchoolAccessService access, NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.payments = payments;
        this.settingsRepo = settingsRepo;
        this.fees = fees;
        this.students = students;
        this.schools = schools;
        this.notifications = notifications;
        this.access = access;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ settings

    @Transactional(readOnly = true)
    public SettingsDto settings(Long schoolId) {
        access.requireSchool(schoolId);
        return settingsRepo.findBySchoolId(schoolId).map(PaymentService::toDto)
                .orElse(new SettingsDto(false, 0, 10, true, null));
    }

    /** Whether the parents' bot shows "💳 To'lovlar" for this school. */
    @Transactional(readOnly = true)
    public boolean enabled(Long schoolId) {
        return settingsRepo.findBySchoolId(schoolId).map(PaymentSettings::isEnabled).orElse(false);
    }

    @Transactional
    public SettingsDto saveSettings(Long schoolId, SettingsRequest r) {
        access.requireSchool(schoolId);
        PaymentSettings s = settingsRepo.findBySchoolId(schoolId).orElseGet(() -> {
            PaymentSettings n = new PaymentSettings();
            n.setSchool(schools.findById(schoolId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday maktab topilmadi")));
            return n;
        });
        s.setEnabled(r.enabled());
        s.setMonthlyFee(r.monthlyFee());
        s.setDueDay(r.dueDay());
        s.setRemindParents(!Boolean.FALSE.equals(r.remindParents()));
        s.setPayInfo(r.payInfo() == null || r.payInfo().isBlank() ? null : r.payInfo().strip());
        s.setUpdatedAt(LocalDateTime.now(clock));
        s.setUpdatedBy(access.caller().username());
        settingsRepo.save(s);
        return toDto(s);
    }

    @Transactional
    public StudentAccount setFee(FeeRequest r) {
        Student st = student(r.studentId());
        StudentFee f = fees.findByStudentId(st.getId()).orElse(null);
        if (r.monthlyFee() == null) {
            if (f != null) fees.delete(f);
        } else {
            if (f == null) {
                f = new StudentFee();
                f.setStudent(st);
            }
            f.setMonthlyFee(r.monthlyFee());
            f.setNote(r.note() == null || r.note().isBlank() ? null : r.note().strip());
            fees.save(f);
        }
        return account(st.getId());
    }

    // ------------------------------------------------------------------ payments

    @Transactional(readOnly = true)
    public Page<PaymentDto> list(Long schoolId, Long classId, Long studentId, YearMonth month, Pageable pageable) {
        access.requireSchool(schoolId);
        return payments.search(schoolId, classId, studentId, month == null ? null : month.atDay(1), pageable)
                .map(PaymentService::toDto);
    }

    @Transactional
    public PaymentDto create(PaymentRequest r) {
        Student st = student(r.studentId());
        Payment p = new Payment();
        p.setSchool(NotificationService.schoolOf(st));
        p.setStudent(st);
        apply(p, r);
        p.setCreatedBy(access.caller().username());
        p.setCreatedAt(LocalDateTime.now(clock));
        payments.save(p);
        return toDto(p);
    }

    @Transactional
    public PaymentDto update(Long id, PaymentRequest r) {
        Payment p = find(id);
        if (!p.getStudent().getId().equals(r.studentId())) {
            Student st = student(r.studentId());
            p.setStudent(st);
            p.setSchool(NotificationService.schoolOf(st));
        }
        apply(p, r);
        return toDto(p);
    }

    @Transactional
    public void delete(Long id) {
        payments.delete(find(id));
    }

    private void apply(Payment p, PaymentRequest r) {
        LocalDate today = LocalDate.now(clock);
        LocalDate paidOn = r.paidOn() != null ? r.paidOn() : today;
        if (paidOn.isAfter(today)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "To'lov sanasi kelajakda bo'lmasin");
        p.setMonth(r.month().atDay(1));
        p.setAmount(r.amount());
        p.setPaidOn(paidOn);
        p.setMethod(r.method() != null ? r.method() : Payment.Method.CASH);
        p.setNote(r.note() == null || r.note().isBlank() ? null : r.note().strip());
    }

    // ------------------------------------------------------------------ accounts

    /** One pupil: the months of the school year with their fee and what was paid, and every payment. */
    @Transactional(readOnly = true)
    public StudentAccount account(Long studentId) {
        Student st = student(studentId);
        School school = NotificationService.schoolOf(st);
        PaymentSettings s = settingsRepo.findBySchoolId(school.getId()).orElse(null);
        long schoolFee = s == null ? 0 : s.getMonthlyFee();
        int dueDay = s == null ? 10 : s.getDueDay();
        StudentFee own = fees.findByStudentId(st.getId()).orElse(null);
        long fee = own != null ? own.getMonthlyFee() : schoolFee;

        List<YearMonth> months = monthsOf(st);
        LocalDate from = months.isEmpty() ? LocalDate.now(clock).withDayOfMonth(1) : months.get(0).atDay(1);
        LocalDate to = months.isEmpty() ? from : months.get(months.size() - 1).atDay(1);
        List<Payment> list = payments.findByStudentIdAndMonthBetweenOrderByMonthAscPaidOnAsc(st.getId(), from.minusYears(1), to.plusYears(1));
        Map<YearMonth, Long> paidBy = new HashMap<>();
        for (Payment p : list) paidBy.merge(YearMonth.from(p.getMonth()), p.getAmount(), Long::sum);

        LocalDate today = LocalDate.now(clock);
        List<MonthRow> rows = new ArrayList<>();
        long due = 0;
        long paidDue = 0;
        for (YearMonth m : months) {
            long paid = paidBy.getOrDefault(m, 0L);
            LocalDate dueDate = m.atDay(Math.min(dueDay, m.lengthOfMonth()));
            MonthStatus status;
            if (fee == 0) status = MonthStatus.FREE;
            else if (paid >= fee) status = MonthStatus.PAID;
            else if (!today.isAfter(dueDate)) status = paid > 0 ? MonthStatus.PARTIAL : MonthStatus.UPCOMING;
            else status = paid > 0 ? MonthStatus.PARTIAL : MonthStatus.UNPAID;
            if (today.isAfter(dueDate)) {
                due += fee;
                paidDue += Math.min(paid, fee);
            }
            rows.add(new MonthRow(m, fee, paid, status, dueDate));
        }
        long totalPaid = list.stream().mapToLong(Payment::getAmount).sum();
        List<PaymentDto> dtos = new ArrayList<>(list.stream().map(PaymentService::toDto).toList());
        Collections.reverse(dtos);
        return new StudentAccount(st.getId(), NotificationService.fullName(st), NotificationService.className(st), fee,
                own != null, own == null ? null : own.getNote(), rows, due, totalPaid, Math.max(0, due - paidDue), dtos,
                s == null ? null : s.getPayInfo());
    }

    /**
     * The school's month: what is expected and collected this month, and every pupil with a debt (unpaid months
     * whose due day has passed), the largest first. One query for the payments, one for the pupils.
     */
    @Transactional(readOnly = true)
    public Summary summary(Long schoolId, Long classId, YearMonth month) {
        access.requireSchool(schoolId);
        return summaryOf(schoolId, classId, month);
    }

    Summary summaryOf(Long schoolId, Long classId, YearMonth month) {
        YearMonth ym = month != null ? month : YearMonth.now(clock);
        PaymentSettings s = settingsRepo.findBySchoolId(schoolId).orElse(null);
        long schoolFee = s == null ? 0 : s.getMonthlyFee();
        int dueDay = s == null ? 10 : s.getDueDay();
        Map<Long, Long> ownFees = new HashMap<>();
        for (StudentFee f : fees.ofSchool(schoolId)) ownFees.put(f.getStudent().getId(), f.getMonthlyFee());
        LocalDate today = LocalDate.now(clock);

        record Pupil(long id, String name, long classId, String className, String guardian, String phone,
                     LocalDate yearFrom, LocalDate yearTo, boolean telegram) {
        }
        MapSqlParameterSource p = new MapSqlParameterSource("s", schoolId).addValue("c", classId).addValue("d", today);
        List<Pupil> pupils = jdbc.query("""
                select st.id, st.last_name || ' ' || st.first_name, c.id, c.grade_number || '-' || c.section_letter,
                       st.guardian_name, st.guardian_phone, ay.start_date, ay.end_date,
                       exists(select 1 from parent_telegram_link l where l.student_id = st.id and l.active = true)
                  from students st join school_class c on c.id = st.class_id join academic_year ay on ay.id = c.academic_year_id
                 where ay.school_id = :s and (cast(:c as bigint) is null or c.id = :c)
                   and ay.start_date <= :d and ay.end_date >= :d - 100
                 order by c.grade_number, c.section_letter, st.last_name, st.first_name""", p,
                (rs, i) -> new Pupil(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4), rs.getString(5),
                        rs.getString(6), rs.getDate(7).toLocalDate(), rs.getDate(8).toLocalDate(), rs.getBoolean(9)));

        Map<Long, Map<YearMonth, Long>> paid = new HashMap<>();
        Map<Long, LocalDate> lastPaid = new HashMap<>();
        jdbc.query("""
                select p.student_id, p.month, sum(p.amount), max(p.paid_on) from payment p where p.school_id = :s
                 group by p.student_id, p.month""", p, rs -> {
            paid.computeIfAbsent(rs.getLong(1), k -> new HashMap<>()).put(YearMonth.from(rs.getDate(2).toLocalDate()), rs.getLong(3));
            LocalDate d = rs.getDate(4).toLocalDate();
            lastPaid.merge(rs.getLong(1), d, (a, b) -> a.isAfter(b) ? a : b);
        });

        long expected = 0;
        long collected = 0;
        long totalDebt = 0;
        List<Debtor> debtors = new ArrayList<>();
        for (Pupil u : pupils) {
            long fee = ownFees.getOrDefault(u.id(), schoolFee);
            Map<YearMonth, Long> mine = paid.getOrDefault(u.id(), Map.of());
            expected += fee;
            collected += mine.getOrDefault(ym, 0L);
            if (fee == 0) continue;
            long debt = 0;
            int unpaid = 0;
            YearMonth oldest = null;
            for (YearMonth m : months(u.yearFrom(), u.yearTo(), today)) {
                LocalDate dueDate = m.atDay(Math.min(dueDay, m.lengthOfMonth()));
                if (!today.isAfter(dueDate)) continue;
                long short_ = fee - Math.min(fee, mine.getOrDefault(m, 0L));
                if (short_ > 0) {
                    debt += short_;
                    unpaid++;
                    if (oldest == null) oldest = m;
                }
            }
            if (debt > 0) {
                totalDebt += debt;
                debtors.add(new Debtor(u.id(), u.name(), u.classId(), u.className(), u.guardian(), u.phone(), debt, unpaid,
                        oldest, lastPaid.get(u.id()), u.telegram()));
            }
        }
        debtors.sort(Comparator.comparingLong(Debtor::debt).reversed().thenComparing(Debtor::name));
        return new Summary(ym, expected, collected, totalDebt, debtors.size(), pupils.size(), debtors);
    }

    /**
     * "💳 To'lov eslatmasi" to the parents of every debtor (or of the given pupils), at most once a day per pupil
     * and parent. Used by the button on the page and by the monthly job.
     */
    @Transactional
    public int remind(Long schoolId, Collection<Long> onlyStudents) {
        access.requireSchool(schoolId);
        return remindOf(schoolId, onlyStudents);
    }

    /** The monthly job (no signed-in user). */
    @Transactional
    public int remindAsJob(Long schoolId) {
        return remindOf(schoolId, null);
    }

    private int remindOf(Long schoolId, Collection<Long> onlyStudents) {
        Summary sum = summaryOf(schoolId, null, null);
        PaymentSettings s = settingsRepo.findBySchoolId(schoolId).orElse(null);
        if (s == null || !s.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "To'lovlar bo'limi o'chirilgan");
        }
        String payInfo = s.getPayInfo();
        String schoolName = s.getSchool().getName();
        LocalDate today = LocalDate.now(clock);
        int queued = 0;
        for (Debtor d : sum.rows()) {
            if (onlyStudents != null && !onlyStudents.contains(d.studentId())) continue;
            Student st = students.findById(d.studentId()).orElse(null);
            if (st == null) continue;
            long debt = d.debt();
            int months = d.monthsUnpaid();
            for (NotificationService.Recipient r : notifications.recipientsOf(st)) {
                queued += notifications.enqueueDirect(s.getSchool(), st, r.chatId(), NotificationType.PAYMENT_REMINDER, st.getId(),
                        today, lang -> BotI18n.get().t(lang, "notif.payment_reminder",
                                "name", MessageFormatter.escape(d.name()), "debt", money(debt), "months", months,
                                "info", payInfo == null ? "" : "\n\n" + MessageFormatter.escape(payInfo))
                                + MessageFormatter.footer(lang, schoolName),
                        null);
            }
        }
        return queued;
    }

    /** "1 250 000 so'm". */
    public static String money(long amount) {
        String digits = Long.toString(Math.abs(amount));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) sb.append(' ');
            sb.append(digits.charAt(i));
        }
        return (amount < 0 ? "-" : "") + sb + " so'm";
    }

    // ------------------------------------------------------------------ helpers

    private List<YearMonth> monthsOf(Student st) {
        AcademicYear y = st.getSchoolClass().getAcademicYear();
        return months(y.getStartDate(), y.getEndDate(), LocalDate.now(clock));
    }

    /** The months of a school year from its start up to the current month (all of them once the year is over). */
    static List<YearMonth> months(LocalDate yearFrom, LocalDate yearTo, LocalDate today) {
        List<YearMonth> out = new ArrayList<>();
        if (yearFrom == null || yearTo == null) return out;
        YearMonth last = YearMonth.from(today.isBefore(yearTo) ? today : yearTo);
        for (YearMonth m = YearMonth.from(yearFrom); !m.isAfter(last); m = m.plusMonths(1)) out.add(m);
        return out;
    }

    private Student student(Long id) {
        return students.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday o'quvchi topilmadi"));
    }

    private Payment find(Long id) {
        return payments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday to'lov topilmadi"));
    }

    static SettingsDto toDto(PaymentSettings s) {
        return new SettingsDto(s.isEnabled(), s.getMonthlyFee(), s.getDueDay(), s.isRemindParents(), s.getPayInfo());
    }

    static PaymentDto toDto(Payment p) {
        Student st = p.getStudent();
        return new PaymentDto(p.getId(), st.getId(), NotificationService.fullName(st), st.getSchoolClass().getId(),
                NotificationService.className(st), YearMonth.from(p.getMonth()), p.getAmount(), p.getPaidOn(), p.getMethod(),
                p.getNote(), p.getCreatedBy(), p.getCreatedAt());
    }
}
