package com.example.quanlydaotao.n6.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.quanlydaotao.n6.dto.RegistrationDtos.CourseRegistrationResponse;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.DepartmentStatistics;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.PeriodSummary;
import com.example.quanlydaotao.n6.entity.CourseLookup;
import com.example.quanlydaotao.n6.entity.CourseRegistration;
import com.example.quanlydaotao.n6.entity.EcourseSection;
import com.example.quanlydaotao.n6.entity.RegistrationPeriod;
import com.example.quanlydaotao.n6.entity.StudentLookup;
import com.example.quanlydaotao.n6.repository.CourseLookupRepository;
import com.example.quanlydaotao.n6.repository.CourseRegistrationRepository;
import com.example.quanlydaotao.n6.repository.DepartmentLookupRepository;
import com.example.quanlydaotao.n6.repository.EcourseSectionRepository;
import com.example.quanlydaotao.n6.repository.ListStudentRepository;
import com.example.quanlydaotao.n6.repository.RegistrationPeriodRepository;
import com.example.quanlydaotao.n6.repository.SemesterLookupRepository;
import com.example.quanlydaotao.n6.repository.StudentLookupRepository;

@Service
public class RegistrationReportService {

    private final JdbcTemplate jdbcTemplate;
    private final CourseRegistrationRepository registrationRepository;
    private final EcourseSectionRepository sectionRepository;
    private final CourseLookupRepository courseRepository;
    private final StudentLookupRepository studentRepository;
    private final DepartmentLookupRepository departmentRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final ListStudentRepository listStudentRepository;
    private final SemesterLookupRepository semesterRepository;

    public RegistrationReportService(
            JdbcTemplate jdbcTemplate,
            CourseRegistrationRepository registrationRepository,
            EcourseSectionRepository sectionRepository,
            CourseLookupRepository courseRepository,
            StudentLookupRepository studentRepository,
            DepartmentLookupRepository departmentRepository,
            RegistrationPeriodRepository periodRepository,
            ListStudentRepository listStudentRepository,
            SemesterLookupRepository semesterRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.registrationRepository = registrationRepository;
        this.sectionRepository = sectionRepository;
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
        this.periodRepository = periodRepository;
        this.listStudentRepository = listStudentRepository;
        this.semesterRepository = semesterRepository;
    }

    @Transactional(readOnly = true)
    public byte[] export(UUID periodId, UUID sectionId) {
        if (periodId == null && sectionId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cần truyền periodId hoặc sectionId để giới hạn dữ liệu xuất.");
        }
        List<CourseRegistration> registrations = periodId == null
                ? registrationRepository.findByEcourseSectionIdAndStatusAndDeletedAtIsNullOrderByRegisteredAtDesc(
                        sectionId, 1)
                : registrationRepository.findActiveByPeriod(periodId);
        if (sectionId != null) {
            registrations = registrations.stream()
                    .filter(registration -> sectionId.equals(registration.getEcourseSectionId()))
                    .toList();
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Registrations");
            Row header = sheet.createRow(0);
            String[] columns = {
                    "Mã sinh viên", "Họ tên", "Mã lớp học phần", "Mã môn", "Tên môn",
                    "Loại đăng ký", "Đã đóng học phí", "Trạng thái", "Thời gian đăng ký"
            };
            for (int column = 0; column < columns.length; column++) {
                header.createCell(column).setCellValue(columns[column]);
            }

            int rowNumber = 1;
            for (CourseRegistration registration : registrations) {
                CourseRegistrationResponse row = toResponse(registration);
                Row excelRow = sheet.createRow(rowNumber++);
                excelRow.createCell(0).setCellValue(nullToEmpty(row.studentCode()));
                excelRow.createCell(1).setCellValue(nullToEmpty(row.studentName()));
                excelRow.createCell(2).setCellValue(nullToEmpty(row.sectionCode()));
                excelRow.createCell(3).setCellValue(nullToEmpty(row.courseCode()));
                excelRow.createCell(4).setCellValue(nullToEmpty(row.courseName()));
                excelRow.createCell(5).setCellValue(registration.getRegistrationType() == null
                        ? ""
                        : registration.getRegistrationType().toString());
                excelRow.createCell(6).setCellValue(Boolean.TRUE.equals(registration.getIsPaid()) ? "Đã đóng" : "Chưa đóng");
                excelRow.createCell(7).setCellValue(registration.getStatus() == null
                        ? ""
                        : registration.getStatus().toString());
                excelRow.createCell(8).setCellValue(registration.getRegisteredAt() == null
                        ? ""
                        : registration.getRegisteredAt().toString());
            }
            for (int column = 0; column < columns.length; column++) {
                sheet.autoSizeColumn(column);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo tệp Excel.", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<DepartmentStatistics> statistics(UUID semesterId, UUID periodId) {
        StringBuilder sql = new StringBuilder("""
                SELECT c.department_id,
                       d.name AS department_name,
                       COUNT(*) AS registration_count,
                       SUM(CASE WHEN r.registration_type = 2 THEN 1 ELSE 0 END) AS retake_count,
                       SUM(CASE WHEN r.registration_type = 3 THEN 1 ELSE 0 END) AS improvement_count
                FROM course_registrations r
                JOIN ecourse_sections s ON s.id = r.ecourse_section_id
                JOIN courses c ON c.id = s.course_id
                LEFT JOIN departments d ON d.id = c.department_id
                WHERE r.deleted_at IS NULL AND r.status = 1
                """);
        List<Object> args = new ArrayList<>();
        if (semesterId != null) {
            sql.append(" AND s.semester_id = ?");
            args.add(semesterId);
        }
        if (periodId != null) {
            sql.append(" AND r.registration_period_id = ?");
            args.add(periodId);
        }
        sql.append(" GROUP BY c.department_id, d.name ORDER BY d.name");
        return jdbcTemplate.query(sql.toString(), (result, rowNumber) -> new DepartmentStatistics(
                result.getObject("department_id", UUID.class),
                result.getString("department_name"),
                result.getLong("registration_count"),
                result.getLong("retake_count"),
                result.getLong("improvement_count")), args.toArray());
    }

    @Transactional(readOnly = true)
    public PeriodSummary summary(UUID periodId) {
        RegistrationPeriod period = periodRepository.findByIdAndDeletedAtIsNull(periodId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đợt đăng ký."));
        long eligible = listStudentRepository
                .countByRegistrationPeriodIdAndIsActiveTrueAndDeletedAtIsNull(periodId);
        long registeredStudents = registrationRepository.countDistinctRegisteredStudents(periodId);
        long registrationCount = registrationRepository
                .countByRegistrationPeriodIdAndStatusAndDeletedAtIsNull(periodId, 1);
        long paidCount = registrationRepository
                .countByRegistrationPeriodIdAndStatusAndIsPaidTrueAndDeletedAtIsNull(periodId, 1);
        List<EcourseSection> sections = sectionRepository
                .findByDeletedAtIsNullAndSemesterIdOrderByCode(period.getSemesterId()).stream()
                .filter(section -> section.getStatus() == 1)
                .toList();
        long seats = sections.stream()
                .mapToLong(section -> Math.max(0, section.getMaxCapacity() - section.getCurrentEnrolled()))
                .sum();
        return new PeriodSummary(
                period.getId(),
                period.getName(),
                eligible,
                registeredStudents,
                registrationCount,
                paidCount,
                sections.size(),
                seats);
    }

    private CourseRegistrationResponse toResponse(CourseRegistration registration) {
        StudentLookup student = studentRepository.findFirstById(registration.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Đăng ký đang tham chiếu tới sinh viên không còn tồn tại."));
        EcourseSection section = registration.getEcourseSectionId() == null
                ? null
                : sectionRepository.findById(registration.getEcourseSectionId()).orElse(null);
        return new CourseRegistrationResponse(
                registration.getId(),
                registration.getRegistrationPeriodId(),
                student.getId(),
                student.getCode(),
                student.getFullName(),
                registration.getEcourseSectionId(),
                section == null ? null : section.getCode(),
                section == null ? null : section.getCourseCode(),
                section == null ? null : section.getCourseName(),
                registration.getRegistrationType(),
                registration.getReplacedGradeId(),
                registration.getIsPaid(),
                registration.getStatus(),
                registration.getRegisteredAt());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
