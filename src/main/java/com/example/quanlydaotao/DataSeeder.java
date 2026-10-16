package com.example.quanlydaotao;

import com.example.quanlydaotao.n3.entity.Department;
import com.example.quanlydaotao.n3.entity.Employee;
import com.example.quanlydaotao.n3.entity.EmployeePosition;
import com.example.quanlydaotao.n3.entity.Position;
import com.example.quanlydaotao.n3.repository.DepartmentRepository;
import com.example.quanlydaotao.n3.repository.EmployeePositionRepository;
import com.example.quanlydaotao.n3.repository.EmployeeRepository;
import com.example.quanlydaotao.n3.repository.PositionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private PositionRepository positionRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private EmployeePositionRepository employeePositionRepository;

    @Override
    public void run(String... args) throws Exception {
        // Nếu database chưa có khoa nào thì mới nạp dữ liệu mẫu
        if (departmentRepository.count() == 0) {
            System.out.println("Dang nap du lieu gia (Mock Data) vao SQL Server...");

            // 1. Tạo Khoa (Department)
            Department dept1 = new Department();
            dept1.setCode("CNTT");
            dept1.setName("Khoa Công nghệ Thông tin");
            dept1.setPhone("02363123456");
            dept1.setEmail("cntt@donga.edu.vn");
            dept1.setEstablishedDate(LocalDate.of(2009, 5, 15));
            departmentRepository.save(dept1);

            Department dept2 = new Department();
            dept2.setCode("QTKD");
            dept2.setName("Khoa Quản trị Kinh doanh");
            dept2.setPhone("02363789789");
            dept2.setEmail("qtkd@donga.edu.vn");
            dept2.setEstablishedDate(LocalDate.of(2010, 8, 20));
            departmentRepository.save(dept2);

            // 2. Tạo Chức vụ (Position)
            Position pos1 = new Position();
            pos1.setCode("HEAD_DEPT");
            pos1.setTitle("Trưởng khoa");
            pos1.setLevel(1);
            pos1.setDepartment(dept1);
            positionRepository.save(pos1);

            Position pos2 = new Position();
            pos2.setCode("LECTURER");
            pos2.setTitle("Giảng viên cơ hữu");
            pos2.setLevel(3);
            pos2.setDepartment(dept1);
            positionRepository.save(pos2);

            // 3. Tạo Nhân viên/Giảng viên (Employee)
            Employee emp1 = new Employee();
            emp1.setCode("GV202601");
            emp1.setFullName("Nguyễn Văn Hoàng");
            emp1.setDateOfBirth(LocalDate.of(1985, 4, 12));
            emp1.setGender("Nam");
            emp1.setEmail("nguyenvanhoang@donga.edu.vn");
            emp1.setPhone("0905123999");
            emp1.setAddress("123 Điện Biên Phủ, Đà Nẵng");
            emp1.setDepartment(dept1);
            emp1.setPosition(pos1); // Vị trí chính
            emp1.setHireDate(LocalDate.of(2015, 8, 1));
            emp1.setContractType("Cơ hữu");
            emp1.setSalaryCoefficient(new BigDecimal("4.40"));
            emp1.setAcademicDegree("Tiến sĩ");
            emp1.setAcademicTitle("Phó Giáo sư");
            emp1.setSpecialization("Khoa học Dữ liệu");
            employeeRepository.save(emp1);

            Employee emp2 = new Employee();
            emp2.setCode("GV202602");
            emp2.setFullName("Trần Thị Mai Phương");
            emp2.setDateOfBirth(LocalDate.of(1992, 10, 25));
            emp2.setGender("Nữ");
            emp2.setEmail("tranmaiphuong@donga.edu.vn");
            emp2.setPhone("0934567888");
            emp2.setAddress("45 Nguyễn Văn Linh, Đà Nẵng");
            emp2.setDepartment(dept1);
            emp2.setPosition(pos2);
            emp2.setHireDate(LocalDate.of(2020, 3, 15));
            emp2.setContractType("Hợp đồng 3 năm");
            emp2.setSalaryCoefficient(new BigDecimal("3.00"));
            emp2.setAcademicDegree("Thạc sĩ");
            emp2.setSpecialization("Kỹ thuật phần mềm");
            employeeRepository.save(emp2);

            // 4. Tạo Lịch sử công tác (EmployeePosition)
            EmployeePosition ep1 = new EmployeePosition();
            ep1.setEmployee(emp1);
            ep1.setPosition(pos1);
            ep1.setStartDate(LocalDateTime.of(2015, 8, 1, 8, 0));
            ep1.setDescription("Bổ nhiệm chức danh Trưởng khoa CNTT");
            ep1.setNote("QĐ số 123/QĐ-ĐHA");
            employeePositionRepository.save(ep1);

            EmployeePosition ep2 = new EmployeePosition();
            ep2.setEmployee(emp2);
            ep2.setPosition(pos2);
            ep2.setStartDate(LocalDateTime.of(2020, 3, 15, 8, 0));
            ep2.setDescription("Ký hợp đồng giảng viên cơ hữu");
            employeePositionRepository.save(ep2);

            System.out.println("Da nap du lieu gia thanh cong! Ban co the mo Postman de test cac API GET.");
        }

        if (positionRepository.count() == 0) {
            Department defaultDept = departmentRepository.findAll().stream().findFirst().orElse(null);
            Position p1 = new Position();
            p1.setCode("TRUONG_KHOA");
            p1.setTitle("Trưởng khoa");
            p1.setLevel(1);
            p1.setDepartment(defaultDept);
            positionRepository.save(p1);

            Position p2 = new Position();
            p2.setCode("PHO_KHOA");
            p2.setTitle("Phó trưởng khoa");
            p2.setLevel(2);
            p2.setDepartment(defaultDept);
            positionRepository.save(p2);

            Position p3 = new Position();
            p3.setCode("GV_COHUU");
            p3.setTitle("Giảng viên cơ hữu");
            p3.setLevel(3);
            p3.setDepartment(defaultDept);
            positionRepository.save(p3);

            Position p4 = new Position();
            p4.setCode("GV_THINHG");
            p4.setTitle("Giảng viên thỉnh giảng");
            p4.setLevel(4);
            p4.setDepartment(defaultDept);
            positionRepository.save(p4);

            Position p5 = new Position();
            p5.setCode("TRO_GIANG");
            p5.setTitle("Trợ giảng");
            p5.setLevel(5);
            p5.setDepartment(defaultDept);
            positionRepository.save(p5);
            System.out.println("Da nap danh muc chuc vu mac dinh vao SQL Server!");
        }

        cleanVietnameseEncoding();
    }

    private void cleanVietnameseEncoding() {
        try {
            // 1. Clean departments
            for (Department d : departmentRepository.findAll()) {
                boolean changed = false;
                if ("CNTT".equalsIgnoreCase(d.getCode())) { d.setName("Khoa Công nghệ thông tin"); changed = true; }
                else if ("OTO".equalsIgnoreCase(d.getCode())) { d.setName("Khoa Công nghệ Ô tô"); changed = true; }
                else if ("MKT".equalsIgnoreCase(d.getCode())) { d.setName("Khoa Marketing"); changed = true; }
                else if (d.getCode() != null && d.getCode().contains("DA")) { d.setCode("DDT"); d.setName("Khoa Điện - Điện tử"); changed = true; }
                else if ("NNA".equalsIgnoreCase(d.getCode())) { d.setName("Khoa Ngôn ngữ Anh"); changed = true; }
                else if ("DD".equalsIgnoreCase(d.getCode())) { d.setName("Khoa Điều dưỡng"); changed = true; }
                else if ("DL".equalsIgnoreCase(d.getCode())) { d.setName("Khoa Du lịch"); changed = true; }
                else if ("KTE".equalsIgnoreCase(d.getCode())) { d.setName("Khoa Kinh tế"); changed = true; }
                if (changed) departmentRepository.save(d);
            }

            // 2. Clean employees
            for (Employee e : employeeRepository.findAll()) {
                boolean changed = false;
                
                // Clean Full Name
                if (e.getFullName() != null) {
                    String name = e.getFullName();
                    name = name.replace("Nguy?n", "Nguyễn")
                               .replace("Tr?n", "Trần")
                               .replace("Th?", "Thị")
                               .replace("Ð?ng", "Đặng")
                               .replace("?ng", "Đặng")
                               .replace("Ð?t", "Đạt")
                               .replace("C?m", "Cẩm")
                               .replace("V?n", "Văn")
                               .replace("Van", "Văn")
                               .replace("Hu?nh", "Huỳnh")
                               .replace("Ph?m", "Phạm")
                               .replace("BA1i", "Bùi")
                               .replace("Bùi Van", "Bùi Văn")
                               .replace("VA", "Võ")
                               .replace("VA", "Võ")
                               .replace("LA", "Lê")
                               .replace("LA", "Lê")
                               .replace("KhA'i", "Khải")
                               .replace("PhAc", "Phúc")
                               .replace("TA1ng", "Tùng")
                               .replace("Cu?ng", "Cường")
                               .replace("Ð", "Đ")
                               .replace("?", "");
                    name = name.trim().replaceAll("\\s+", " ");
                    if (!name.equals(e.getFullName())) {
                        e.setFullName(name);
                        changed = true;
                    }
                }

                // Clean Academic Degree
                if (e.getAcademicDegree() != null) {
                    String deg = e.getAcademicDegree();
                    if (deg.startsWith("Th") || deg.contains("Th")) deg = "Thạc sĩ";
                    else if (deg.startsWith("Ti") || deg.contains("Ti")) deg = "Tiến sĩ";
                    else if (deg.startsWith("C")) deg = "Cử nhân";
                    else if (deg.startsWith("K")) deg = "Kỹ sư";
                    if (!deg.equals(e.getAcademicDegree())) { e.setAcademicDegree(deg); changed = true; }
                }

                // Clean Academic Title
                if (e.getAcademicTitle() != null) {
                    String title = e.getAcademicTitle();
                    if (title.contains("Ph")) title = "Phó Giáo sư";
                    else if (title.contains("Giáo sư") || title.contains("GiAo s")) title = "Giáo sư";
                    else if (title.contains("Gi")) title = "Giảng viên";
                    if (!title.equals(e.getAcademicTitle())) { e.setAcademicTitle(title); changed = true; }
                }

                // Clean Specialization
                if (e.getSpecialization() != null) {
                    String spec = e.getSpecialization();
                    if (spec.contains("Khoa") || spec.contains("li")) spec = "Khoa học dữ liệu";
                    else if (spec.contains("thu?t ph") || spec.contains("thuật ph")) spec = "Kỹ thuật phần mềm";
                    else if (spec.contains("ph?n") || spec.contains("phần")) spec = "Công nghệ phần mềm";
                    else if (spec.contains("an to") || spec.contains("an toàn")) spec = "Mạng và an toàn thông tin";
                    else if (spec.contains("M?ng") || spec.contains("Mạng")) spec = "Mạng máy tính";
                    else if (spec.contains("tu?") || spec.contains("nhân") || spec.contains("Tr")) spec = "Trí tuệ nhân tạo";
                    else if (spec.contains("H?") || spec.contains("Hệ")) spec = "Hệ thống thông tin";
                    else if (spec.contains("my t") || spec.contains("máy")) spec = "Kỹ thuật máy tính";
                    else if (spec.contains("th?ng") || spec.contains("thông tin") || spec.contains("CA'ng") || spec.contains("Cng")) spec = "Công nghệ thông tin";
                    if (!spec.equals(e.getSpecialization())) { e.setSpecialization(spec); changed = true; }
                }

                // Clean Gender
                if (e.getGender() != null) {
                    String g = e.getGender();
                    if (g.contains("N") && !g.equalsIgnoreCase("Nam")) {
                        e.setGender("Nữ");
                        changed = true;
                    }
                }

                // Clean Address
                if (e.getAddress() != null && (e.getAddress().contains("N") || e.getAddress().contains("D"))) {
                    e.setAddress("Đà Nẵng");
                    changed = true;
                }

                // Clean Contract Type
                if (e.getContractType() != null) {
                    String ct = e.getContractType();
                    if (ct.contains("Vi")) ct = "Viên chức";
                    else if (ct.contains("C")) ct = "Cơ hữu";
                    if (!ct.equals(e.getContractType())) { e.setContractType(ct); changed = true; }
                }

                if (changed) {
                    employeeRepository.save(e);
                }
            }
            System.out.println("Da chuan hoa va sua loi phong chu Tieng Viet trong Database thanh cong!");
        } catch (Exception ex) {
            System.err.println("Loi khi chuan hoa phong chu: " + ex.getMessage());
        }
    }
}
