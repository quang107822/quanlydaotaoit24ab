package com.example.quanlydaotao.n1.service;

import com.example.quanlydaotao.n1.entity.User;
import com.example.quanlydaotao.n1.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Lấy danh sách tất cả người dùng chưa bị xóa mềm.
     *
     * @return danh sách người dùng đang hoạt động và còn hiệu lực
     */
    public List<User> getAllUsers() {
        return userRepository.findByDeletedAtIsNull();
    }

    /**
     * Lấy thông tin chi tiết của một người dùng theo ID.
     * Nếu người dùng không tồn tại hoặc đã bị xóa mềm thì trả về lỗi 404.
     *
     * @param id ID của người dùng cần tìm
     * @return đối tượng User tương ứng
     */
    public User getUserById(UUID id) {

        return userRepository.findById(id)
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy user: " + id));
    }

    /**
     * Tìm người dùng theo username đang hoạt động và chưa bị xóa mềm.
     *
     * @param username tên đăng nhập cần tra cứu
     * @return thông tin user khớp với username
     */
    public User getByUsername(String username) {

        return userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy username: " + username));
    }

    /**
     * Tìm kiếm người dùng theo từ khóa tên, email hoặc thông tin liên quan.
     * Nếu keyword rỗng thì trả về toàn bộ danh sách người dùng active.
     *
     * @param keyword từ khóa tìm kiếm
     * @return danh sách người dùng phù hợp
     */
    public List<User> searchUsers(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return getAllUsers();
        }

        return userRepository.searchActiveUsers(keyword.trim());
    }

    /**
     * Tạo mới một tài khoản người dùng với các kiểm tra dữ liệu bắt buộc.
     * Kiểm tra username, fullName, passwordHash, trùng username/email trước khi lưu.
     *
     * @param user thông tin người dùng cần tạo
     * @return user đã được lưu vào database
     */
    public User createUser(User user) {

        if (user == null
                || user.getUsername() == null || user.getUsername().isBlank()
                || user.getFullName() == null || user.getFullName().isBlank()
                || user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username, fullName và passwordHash là thông tin bắt buộc");
        }

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username đã tồn tại: " + user.getUsername());
        }

        if (user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email đã tồn tại: " + user.getEmail());
        }

        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }

        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }

        user.setUpdatedAt(LocalDateTime.now());

        if (user.getIsActive() == null) {
            user.setIsActive(true);
        }

        return userRepository.save(user);
    }

    /**
     * Cập nhật thông tin người dùng theo ID.
     * Chỉ cập nhật các trường được gửi lên, đồng thời kiểm tra dữ liệu hợp lệ
     * và tránh trùng username/email với người dùng khác.
     *
     * @param id ID của người dùng cần cập nhật
     * @param request dữ liệu mới cần thay đổi
     * @return user sau khi cập nhật
     */
    public User updateUser(UUID id, User request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thông tin cập nhật không được để trống");
        }

        User user = getUserById(id);

        if (request.getUsername() != null) {
            if (request.getUsername().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Username không được để trống");
            }
            if (!request.getUsername().equals(user.getUsername())
                    && userRepository.existsByUsernameAndIdNot(request.getUsername(), id)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Username đã tồn tại: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }
        if (request.getFullName() != null) {
            if (request.getFullName().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Full name không được để trống");
            }
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null) {
            if (!request.getEmail().equals(user.getEmail())
                    && userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Email đã tồn tại: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    /**
     * Xóa mềm người dùng bằng cách gán deletedAt và vô hiệu hóa tài khoản.
     * Không xóa bản ghi khỏi database, chỉ đánh dấu là đã xóa.
     *
     * @param id ID người dùng cần xóa mềm
     */
    public void deleteUser(UUID id) {

        User user = getUserById(id);
        LocalDateTime deletedAt = LocalDateTime.now();

        user.setDeletedAt(deletedAt);
        user.setUpdatedAt(deletedAt);
        user.setIsActive(false);

        userRepository.save(user);
    }

    /**
     * Cập nhật trạng thái hoạt động của người dùng.
     * Dùng cho chức năng kích hoạt hoặc vô hiệu hóa tài khoản.
     *
     * @param id ID người dùng
     * @param active trạng thái mới (true = active, false = inactive)
     * @return user sau khi cập nhật trạng thái
     */
    public User updateActive(UUID id, Boolean active) {

        if (active == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trạng thái active là thông tin bắt buộc");
        }

        User user = getUserById(id);

        user.setIsActive(active);
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }
}