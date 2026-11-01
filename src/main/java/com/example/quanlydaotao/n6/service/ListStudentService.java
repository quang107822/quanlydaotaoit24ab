package com.example.quanlydaotao.n6.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.quanlydaotao.n6.dto.RegistrationDtos.ImportResult;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.ListStudentRequest;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.ListStudentResponse;
import com.example.quanlydaotao.n6.entity.ListStudent;
import com.example.quanlydaotao.n6.entity.RegistrationPeriod;
import com.example.quanlydaotao.n6.entity.StudentLookup;
import com.example.quanlydaotao.n6.repository.ListStudentRepository;
import com.example.quanlydaotao.n6.repository.RegistrationPeriodRepository;
import com.example.quanlydaotao.n6.repository.StudentLookupRepository;

@Service
public class ListStudentService {

    private static final long MAX_IMPORT_BYTES = 5L * 1024 * 1024;

    private final ListStudentRepository listRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final StudentLookupRepository studentRepository;

    public ListStudentService(
            ListStudentRepository listRepository,
            RegistrationPeriodRepository periodRepository,
            StudentLookupRepository studentRepository) {
        this.listRepository = listRepository;
        this.periodRepository = periodRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<ListStudentResponse> list(UUID periodId) {
        requireActivePeriod(periodId);
        return listRepository.findByRegistrationPeriodIdAndIsActiveTrueAndDeletedAtIsNullOrderByCreatedAtDesc(periodId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ListStudentResponse add(UUID periodId, ListStudentRequest request) {
        requireActivePeriod(periodId);
        StudentLookup student = findStudent(request.studentId());
        ListStudent record = listRepository.findByRegistrationPeriodIdAndStudentIdAndDeletedAtIsNull(
                periodId, student.getId()).orElseGet(ListStudent::new);
        if (record.getId() != null && Boolean.TRUE.equals(record.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sinh viên đã có quyền tham gia đợt này.");
        }
        record.setRegistrationPeriodId(periodId);
        record.setStudentId(student.getId());
        record.setMaxAllowedCredits(request.maxAllowedCredits());
        record.setIsActive(true);
        record.setDeletedAt(null);
        return toResponse(listRepository.save(record));
    }

    @Transactional
    public ImportResult importExcel(UUID periodId, MultipartFile file) {
        requireActivePeriod(periodId);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn tệp Excel.");
        }
        if (file.getSize() > MAX_IMPORT_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tệp Excel không được vượt quá 5 MB.");
        }

        List<String> errors = new ArrayList<>();
        int added = 0;
        int skipped = 0;
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tệp Excel không có dữ liệu.");
            }
            DataFormatter formatter = new DataFormatter();
            Row header = sheet.getRow(sheet.getFirstRowNum());
            int codeColumn = findColumn(header, formatter, "studentcode", "student_code", "masinhvien", "ma_sinh_vien");
            int creditsColumn = findColumn(header, formatter, "maxallowedcredits", "max_allowed_credits", "hanmuctinchi");
            if (codeColumn < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tệp Excel phải có cột mã sinh viên: studentCode hoặc student_code.");
            }

            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                int displayRow = rowIndex + 1;
                String code = formatter.formatCellValue(row.getCell(codeColumn)).trim();
                if (code.isEmpty()) {
                    continue;
                }
                try {
                    StudentLookup student = studentRepository.findFirstByCodeIgnoreCase(code)
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã sinh viên " + code));
                    if (listRepository.existsByRegistrationPeriodIdAndStudentIdAndIsActiveTrueAndDeletedAtIsNull(
                            periodId, student.getId())) {
                        skipped++;
                        continue;
                    }
                    Integer credits = readCredits(row, creditsColumn, formatter);
                    ListStudent record = listRepository.findByRegistrationPeriodIdAndStudentIdAndDeletedAtIsNull(
                                    periodId, student.getId())
                            .orElseGet(ListStudent::new);
                    record.setRegistrationPeriodId(periodId);
                    record.setStudentId(student.getId());
                    record.setMaxAllowedCredits(credits);
                    record.setIsActive(true);
                    record.setDeletedAt(null);
                    listRepository.save(record);
                    added++;
                } catch (IllegalArgumentException exception) {
                    skipped++;
                    errors.add("Dòng " + displayRow + ": " + exception.getMessage());
                }
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không đọc được tệp Excel.", exception);
        } catch (org.apache.poi.EncryptedDocumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tệp tải lên không phải Excel hợp lệ.", exception);
        }
        return new ImportResult(added, skipped, errors);
    }

    @Transactional
    public void revoke(UUID periodId, UUID studentId) {
        requireActivePeriod(periodId);
        ListStudent record = listRepository.findByRegistrationPeriodIdAndStudentIdAndDeletedAtIsNull(
                        periodId, studentId)
                .filter(ListStudent::getIsActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy quyền đăng ký."));
        record.setIsActive(false);
        record.setDeletedAt(LocalDateTime.now());
        listRepository.save(record);
    }

    private ListStudentResponse toResponse(ListStudent record) {
        StudentLookup student = findStudent(record.getStudentId());
        return new ListStudentResponse(
                record.getId(),
                record.getRegistrationPeriodId(),
                student.getId(),
                student.getCode(),
                student.getFullName(),
                record.getMaxAllowedCredits(),
                record.getIsActive());
    }

    private RegistrationPeriod requireActivePeriod(UUID id) {
        return periodRepository.findByIdAndDeletedAtIsNull(id)
                .filter(period -> Boolean.TRUE.equals(period.getIsActive()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đợt đăng ký."));
    }

    private StudentLookup findStudent(UUID id) {
        return studentRepository.findFirstById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sinh viên."));
    }

    private static int findColumn(Row header, DataFormatter formatter, String... names) {
        if (header == null) {
            return -1;
        }
        for (int column = 0; column < header.getLastCellNum(); column++) {
            String normalized = formatter.formatCellValue(header.getCell(column))
                    .replaceAll("\\s+", "")
                    .toLowerCase(java.util.Locale.ROOT);
            for (String name : names) {
                if (normalized.equals(name.toLowerCase(java.util.Locale.ROOT))) {
                    return column;
                }
            }
        }
        return -1;
    }

    private static Integer readCredits(Row row, int column, DataFormatter formatter) {
        if (column < 0) {
            return null;
        }
        String value = formatter.formatCellValue(row.getCell(column)).trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            int credits = Integer.parseInt(value);
            if (credits < 0 || credits > 255) {
                throw new NumberFormatException();
            }
            return credits;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Hạn mức tín chỉ phải là số từ 0 đến 255.");
        }
    }
}
