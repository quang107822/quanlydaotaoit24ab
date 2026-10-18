const API_BASE = `${window.location.origin}/api`;
const state = {
    users: [],
    roles: [],
    userRolesByUser: new Map(),
    selectedUserId: null,
    assignments: [],
    selectedRoleIds: new Set(),
    requestVersion: 0,
    userPage: 0,
    userPageSize: 12,
    totalPages: 0,
    totalUsers: 0,
    clientPaged: false,
    searchTimer: null,
};

const userList = document.querySelector("#userList");
const roleList = document.querySelector("#roleList");
const userSearch = document.querySelector("#userSearch");
const userStatusFilter = document.querySelector("#userStatusFilter");
const roleSearch = document.querySelector("#roleSearch");
const roleFilter = document.querySelector("#roleFilter");
const saveButton = document.querySelector("#saveRoles");
const pageAlert = document.querySelector("#pageAlert");
const saveStatus = document.querySelector("#saveStatus");
const userDetails = document.querySelector("#userDetails");

async function requestJson(path, options = {}) {
    const response = await fetch(`${API_BASE}${path}`, {
        ...options,
        headers: {
            Accept: "application/json",
            ...(options.body ? { "Content-Type": "application/json" } : {}),
            ...options.headers,
        },
    });
    const contentType = response.headers.get("content-type") || "";
    const body = contentType.includes("application/json")
        ? await response.json()
        : await response.text();

    if (!response.ok) {
        const message = typeof body === "string" ? body : body.message || body.error;
        throw new Error(message || `Yêu cầu thất bại (HTTP ${response.status})`);
    }
    return body;
}

function showAlert(message, kind = "danger") {
    pageAlert.className = `alert alert-${kind}`;
    pageAlert.textContent = message;
}

function hideAlert() {
    pageAlert.className = "alert d-none";
    pageAlert.textContent = "";
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, (character) => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;",
    })[character]);
}

function initials(name) {
    return String(name || "?")
        .trim()
        .split(/\s+/)
        .slice(-2)
        .map((part) => part.charAt(0))
        .join("")
        .toUpperCase();
}

function formatDate(value) {
    if (!value) return "Chưa có dữ liệu";
    const date = new Date(value);
    return Number.isNaN(date.getTime())
        ? "Chưa có dữ liệu"
        : new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: "short" }).format(date);
}

function selectedUser() {
    return state.users.find((user) => user.id === state.selectedUserId) || null;
}

function renderUserDetails() {
    const user = selectedUser();
    if (!user) {
        userDetails.innerHTML = '<div class="empty-state">Chọn người dùng để xem hồ sơ chi tiết.</div>';
        document.querySelector("#selectedUserCaption").textContent = "Chọn người dùng để xem và cập nhật vai trò.";
        return;
    }

    const active = user.isActive !== false;
    const avatar = user.avatarUrl
        ? `<img class="profile-avatar" src="${escapeHtml(user.avatarUrl)}" alt="Ảnh đại diện ${escapeHtml(user.fullName)}" />`
        : `<span class="profile-avatar profile-avatar-fallback">${escapeHtml(initials(user.fullName || user.username))}</span>`;
    const details = [
        ["Mã người dùng", user.id],
        ["Tên đăng nhập", user.username],
        ["Email", user.email],
        ["Số điện thoại", user.phone],
        ["Đăng nhập gần nhất", formatDate(user.lastLoginAt)],
        ["Ngày tạo tài khoản", formatDate(user.createdAt)],
        ["Cập nhật lần cuối", formatDate(user.updatedAt)],
        ["Người tạo", user.createdBy],
        ["Người cập nhật", user.updatedBy],
        ["Trạng thái tài khoản", user.deletedAt ? "Đã xóa mềm" : active ? "Đang hoạt động" : "Đã khóa"],
    ];
    if (user.deletedAt) {
        details.push(["Thời điểm xóa", formatDate(user.deletedAt)]);
        details.push(["Người xóa", user.deletedBy]);
    }

    userDetails.innerHTML = `
        <div class="profile-heading">
            ${avatar}
            <div class="profile-main">
                <div class="profile-name">${escapeHtml(user.fullName || "Chưa cập nhật họ tên")}</div>
                <div class="profile-subtitle">@${escapeHtml(user.username || "—")}</div>
            </div>
            <span class="badge ${user.deletedAt ? "text-bg-danger" : active ? "text-bg-success" : "text-bg-secondary"}">${user.deletedAt ? "Đã xóa mềm" : active ? "Đang hoạt động" : "Đã khóa"}</span>
        </div>
        <dl class="profile-grid">
            ${details.map(([label, value], index) => `
                <div class="profile-field ${index === 0 ? "profile-id" : ""}">
                    <dt>${escapeHtml(label)}</dt>
                    <dd>${escapeHtml(value || "Chưa có dữ liệu")}</dd>
                </div>`).join("")}
        </dl>
        <section class="effective-permissions" aria-labelledby="effectivePermissionsTitle">
            <div class="effective-permissions-heading">
                <div>
                    <h3 id="effectivePermissionsTitle" class="h6 fw-bold mb-1">Quyền chức năng hiệu lực</h3>
                    <p class="small text-secondary mb-0">API tổng hợp permission hiệu lực theo user hiện chưa có trong backend.</p>
                </div>
                <span class="badge text-bg-secondary">Chưa khả dụng</span>
            </div>
            <div class="alert alert-warning mt-3 mb-0">Trang vẫn quản lý role được gán ở phần bên dưới, nhưng chưa thể tổng hợp các permission do backend chưa có endpoint tương ứng.</div>
        </section>
        <div class="profile-actions mt-3 d-flex flex-wrap gap-2">
            ${user.deletedAt
                ? '<button class="btn btn-sm btn-success" type="button" data-user-action="restore"><i class="bi bi-arrow-counterclockwise me-1"></i>Khôi phục</button>'
                : `<button class="btn btn-sm btn-outline-primary" type="button" data-user-action="edit"><i class="bi bi-pencil me-1"></i>Sửa hồ sơ</button>
                   <button class="btn btn-sm btn-outline-warning" type="button" data-user-action="toggle-active"><i class="bi bi-person-lock me-1"></i>${active ? "Khóa tài khoản" : "Mở khóa"}</button>
                   <button class="btn btn-sm btn-outline-secondary" type="button" data-user-action="password"><i class="bi bi-key me-1"></i>Đổi mật khẩu</button>
                   <button class="btn btn-sm btn-outline-danger" type="button" data-user-action="delete"><i class="bi bi-trash me-1"></i>Xóa mềm</button>`}
        </div>`;
    document.querySelector("#selectedUserCaption").textContent =
        `Đang phân quyền cho ${user.fullName || user.username || "người dùng này"}`;
}

function renderUsers() {
    const filtered = state.clientPaged && userStatusFilter.value !== "deleted"
        ? state.users.filter((user) => userStatusFilter.value === "all"
            || (userStatusFilter.value === "active" && user.isActive !== false)
            || (userStatusFilter.value === "inactive" && user.isActive === false))
        : state.users;
    const pageCount = state.clientPaged
        ? Math.ceil(filtered.length / state.userPageSize)
        : state.totalPages;
    const page = state.clientPaged
        ? filtered.slice(state.userPage * state.userPageSize, (state.userPage + 1) * state.userPageSize)
        : filtered;

    if (!page.length) {
        userList.innerHTML = '<tr><td colspan="8" class="text-center text-secondary py-4">Không tìm thấy người dùng phù hợp.</td></tr>';
        document.querySelector("#userPageSummary").textContent = `0 / ${state.clientPaged ? filtered.length : state.totalUsers} người dùng`;
        document.querySelector("#userPrevPage").disabled = true;
        document.querySelector("#userNextPage").disabled = true;
        return;
    }

    state.userPage = Math.min(state.userPage, Math.max(0, pageCount - 1));
    userList.innerHTML = page.map((user) => {
        const active = user.isActive !== false;
        const userRoles = state.userRolesByUser.get(user.id) || [];
        const roleBadges = userRoles.length
            ? `${userRoles.slice(0, 2).map((assignment) =>
                `<span class="badge ${assignment.isActive === false ? "text-bg-secondary" : "text-bg-primary-subtle text-primary"} user-role-badge">${escapeHtml(assignment.roleCode || assignment.roleName)}</span>`
            ).join(" ")}${userRoles.length > 2 ? `<span class="more-roles" title="${escapeHtml(userRoles.slice(2).map((assignment) => assignment.roleName || assignment.roleCode).join(", "))}">+${userRoles.length - 2}</span>` : ""}`
            : '<span class="text-secondary small">Chưa gán role</span>';
        const deleted = Boolean(user.deletedAt);
        return `
            <tr class="user-row ${user.id === state.selectedUserId ? "user-row-selected" : ""}"
                data-user-id="${escapeHtml(user.id)}" tabindex="0" role="button"
                aria-label="Chọn tài khoản ${escapeHtml(user.fullName || user.username)}"
                aria-pressed="${user.id === state.selectedUserId}">
                <td>
                    <span class="user-name">${escapeHtml(user.fullName || "Chưa cập nhật họ tên")}</span>
                    <span class="user-meta">ID: ${escapeHtml(user.id || "—")}</span>
                </td>
                <td><span class="user-username">@${escapeHtml(user.username || "—")}</span></td>
                <td>${escapeHtml(user.email || "—")}</td>
                <td>${escapeHtml(user.phone || "—")}</td>
                <td>${roleBadges}</td>
                <td>${escapeHtml(formatDate(user.lastLoginAt))}</td>
                <td><span class="badge ${deleted ? "text-bg-danger" : active ? "text-bg-success" : "text-bg-secondary"}">${deleted ? "Đã xóa mềm" : active ? "Đang hoạt động" : "Đã khóa"}</span></td>
                <td class="text-end"><button class="btn btn-sm ${user.id === state.selectedUserId ? "btn-primary" : "btn-outline-primary"}"
                    type="button" data-user-id="${escapeHtml(user.id)}">
                    <i class="bi bi-person-lines-fill me-1"></i>${user.id === state.selectedUserId ? "Đang chọn" : "Chi tiết"}
                </button></td>
            </tr>`;
    }).join("");
    const total = state.clientPaged ? filtered.length : state.totalUsers;
    const startIndex = state.userPage * state.userPageSize;
    document.querySelector("#userPageSummary").textContent =
        `Trang ${state.userPage + 1}/${Math.max(pageCount, 1)} · Hiển thị ${startIndex + 1}–${Math.min(startIndex + page.length, total)} / ${total} người dùng`;
    document.querySelector("#userPrevPage").disabled = state.userPage <= 0;
    document.querySelector("#userNextPage").disabled = state.userPage + 1 >= pageCount;
}

function visibleUsersOnPage() {
    if (!state.clientPaged) return state.users;
    const status = userStatusFilter.value;
    const filtered = state.users.filter((user) => status === "deleted"
        || status === "all"
        || (status === "active" && user.isActive !== false)
        || (status === "inactive" && user.isActive === false));
    return filtered.slice(state.userPage * state.userPageSize, (state.userPage + 1) * state.userPageSize);
}

function updateRoleSummary() {
    const count = state.selectedRoleIds.size;
    document.querySelector("#assignedCount").textContent = state.selectedUserId ? String(count) : "—";
    document.querySelector("#selectionCount").textContent = state.selectedUserId
        ? `${count} / ${state.roles.length} vai trò được gán`
        : "Chưa chọn người dùng";
    saveButton.disabled = !state.selectedUserId || Boolean(selectedUser()?.deletedAt);
}

function filteredRoles() {
    const keyword = roleSearch.value.trim().toLocaleLowerCase("vi");
    const filter = roleFilter.value;
    return state.roles.filter((role) => {
        const matchesKeyword = [role.name, role.code, role.description]
            .some((value) => String(value || "").toLocaleLowerCase("vi").includes(keyword));
        const checked = state.selectedRoleIds.has(role.id);
        const active = role.isActive !== false;
        const matchesFilter = filter === "all"
            || (filter === "assigned" && checked)
            || (filter === "unassigned" && !checked)
            || (filter === "active" && active)
            || (filter === "inactive" && !active);
        return matchesKeyword && matchesFilter;
    });
}

function renderRoles() {
    if (!state.selectedUserId) {
        roleList.innerHTML = '<div class="empty-state">Chọn một người dùng ở danh sách bên trái.</div>';
        roleSearch.disabled = true;
        roleFilter.disabled = true;
        document.querySelector("#selectVisibleRoles").disabled = true;
        document.querySelector("#clearVisibleRoles").disabled = true;
        document.querySelector("#roleResultCount").textContent = "Chưa chọn người dùng";
        updateRoleSummary();
        return;
    }

    roleSearch.disabled = false;
    roleFilter.disabled = false;
    const filtered = filteredRoles();
    const activeFiltered = filtered.filter((role) => role.isActive !== false);
    document.querySelector("#roleResultCount").textContent =
        `Hiển thị ${filtered.length} / ${state.roles.length} vai trò · ${state.selectedRoleIds.size} đang gán`;
    document.querySelector("#selectVisibleRoles").disabled = activeFiltered.length === 0;
    document.querySelector("#clearVisibleRoles").disabled = filtered.length === 0
        || !filtered.some((role) => state.selectedRoleIds.has(role.id));

    if (!filtered.length) {
        roleList.innerHTML = '<div class="empty-state">Không có vai trò phù hợp bộ lọc.</div>';
        updateRoleSummary();
        return;
    }

    roleList.innerHTML = `
        <table class="table table-hover align-middle role-table mb-0">
            <thead class="table-light">
                <tr>
                    <th scope="col" class="role-select-column">Gán</th>
                    <th scope="col">Tên vai trò / Mã</th>
                    <th scope="col">Mô tả</th>
                    <th scope="col">Phân loại</th>
                    <th scope="col">Trạng thái</th>
                </tr>
            </thead>
            <tbody>${filtered.map((role) => {
        const checked = state.selectedRoleIds.has(role.id);
        const roleActive = role.isActive !== false;
        const assignment = state.assignments.find((item) => item.roleId === role.id);
        const assignmentActive = assignment?.isActive !== false;
        const disabled = !roleActive && !checked;
        return `
            <tr class="${checked ? "role-row-selected" : ""}">
                <td class="text-center">
                    <input class="form-check-input role-checkbox" type="checkbox"
                           aria-label="Gán vai trò ${escapeHtml(role.name)}"
                           data-role-id="${escapeHtml(role.id)}"
                           ${checked ? "checked" : ""} ${disabled ? "disabled" : ""} />
                </td>
                <td>
                    <span class="role-title">${escapeHtml(role.name || "Chưa đặt tên")}</span>
                    <span class="role-code">${escapeHtml(role.code || "—")}</span>
                </td>
                <td class="role-description">${escapeHtml(role.description || "Chưa có mô tả")}</td>
                <td>${role.isSystem
                    ? '<span class="badge text-bg-info-subtle text-info-emphasis">Hệ thống</span>'
                    : '<span class="badge text-bg-light text-secondary">Nghiệp vụ</span>'}
                    ${checked && !assignmentActive
                    ? '<span class="badge text-bg-warning ms-1">Phân công đang tắt</span>' : ""}
                </td>
                <td><span class="badge ${roleActive ? "text-bg-success" : "text-bg-secondary"}">${roleActive ? "Đang hoạt động" : "Ngừng hoạt động"}</span></td>
            </tr>`;
    }).join("")}</tbody>
        </table>`;
    updateRoleSummary();
}

async function selectUser(userId) {
    state.selectedUserId = userId;
    const requestVersion = ++state.requestVersion;
    const user = state.users.find((item) => item.id === userId);
    state.assignments = [];
    state.selectedRoleIds = new Set();
    saveButton.disabled = true;
    saveStatus.textContent = "";
    hideAlert();
    renderUserDetails();
    roleSearch.disabled = true;
    roleFilter.disabled = true;
    renderUsers();

    if (user?.deletedAt) {
        roleList.innerHTML = '<div class="empty-state">Tài khoản đã xóa mềm; hãy khôi phục trước khi quản lý vai trò.</div>';
        updateRoleSummary();
        return;
    }
    roleList.innerHTML = '<div class="empty-state">Đang tải vai trò...</div>';
    try {
        // Dùng API chi tiết để hồ sơ luôn mới nhất thay vì chỉ dựa vào dữ liệu trang danh sách.
        const [userDetailsResult, assignments] = await Promise.all([
            requestJson(`/users/${encodeURIComponent(userId)}`),
            requestJson(`/user-roles/user/${encodeURIComponent(userId)}`),
        ]);
        if (requestVersion !== state.requestVersion) return;
        state.users = state.users.map((item) => item.id === userId ? userDetailsResult : item);
        state.assignments = assignments;
        state.selectedRoleIds = new Set(assignments.map((assignment) => assignment.roleId));
        renderUsers();
        renderUserDetails();
        renderRoles();
    } catch (error) {
        if (requestVersion !== state.requestVersion) return;
        roleList.innerHTML = '<div class="empty-state">Không thể tải phân quyền của người dùng.</div>';
        showAlert(`Không tải được role của người dùng: ${error.message}`);
    }
}

// Tìm kiếm/xóa mềm dùng các API riêng; danh sách thường dùng API phân trang phía server.
async function loadUsersPage(page = state.userPage) {
    const requestVersion = ++state.requestVersion;
    const keyword = userSearch.value.trim();
    const status = userStatusFilter.value;
    const pageIndex = Math.max(0, page);
    let actualPage = pageIndex;
    state.userPage = pageIndex;
    state.clientPaged = Boolean(keyword) || status === "deleted";

    try {
        let users;
        if (status === "deleted") {
            users = await requestJson("/users/deleted");
            state.users = users.filter((user) => !keyword || [
                user.id, user.fullName, user.username, user.email, user.phone,
            ].some((value) => String(value || "").toLocaleLowerCase("vi").includes(keyword.toLocaleLowerCase("vi"))));
            state.totalUsers = state.users.length;
            state.totalPages = Math.ceil(state.totalUsers / state.userPageSize);
        } else if (keyword) {
            users = await requestJson(`/users/search?keyword=${encodeURIComponent(keyword)}`);
            state.users = users.filter((user) => status === "all"
                || (status === "active" && user.isActive !== false)
                || (status === "inactive" && user.isActive === false));
            state.totalUsers = state.users.length;
            state.totalPages = Math.ceil(state.totalUsers / state.userPageSize);
        } else {
            const active = status === "all" ? "" : `&active=${status === "active"}`;
            let result = await requestJson(`/users/page?page=${pageIndex}&size=${state.userPageSize}${active}`);
            if (requestVersion !== state.requestVersion) return;
            if (result.totalPages > 0 && pageIndex >= result.totalPages) {
                actualPage = result.totalPages - 1;
                result = await requestJson(`/users/page?page=${actualPage}&size=${state.userPageSize}${active}`);
                if (requestVersion !== state.requestVersion) return;
            }
            state.users = result.content || [];
            state.totalUsers = Number(result.totalElements || 0);
            state.totalPages = Number(result.totalPages || 0);
        }

        if (requestVersion !== state.requestVersion) return;
        state.userPage = Math.min(actualPage, Math.max(0, state.totalPages - 1));
        document.querySelector("#userCount").textContent = String(state.totalUsers);
        document.querySelector("#activeUserCount").textContent = String(
            state.users.filter((user) => !user.deletedAt && user.isActive !== false).length,
        );
        renderUsers();
    } catch (error) {
        if (requestVersion !== state.requestVersion) return;
        state.users = [];
        state.totalUsers = 0;
        state.totalPages = 0;
        state.userPage = 0;
        userList.innerHTML = '<tr><td colspan="8" class="text-center text-danger py-4">Không thể tải danh sách người dùng.</td></tr>';
        showAlert(`Không tải được danh sách người dùng: ${error.message}`);
    }
}

async function loadPage(preferredUserId = null) {
    saveButton.disabled = true;
    try {
        const [rolesResult, assignmentsResult] = await Promise.allSettled([
            requestJson("/roles"),
            requestJson("/user-roles"),
        ]);
        state.roles = rolesResult.status === "fulfilled" ? rolesResult.value : [];
        if (rolesResult.status === "rejected") {
            showAlert(`Không tải được danh sách role: ${rolesResult.reason.message}`, "warning");
        }
        state.userRolesByUser = new Map();
        if (assignmentsResult.status === "fulfilled") {
            for (const assignment of assignmentsResult.value) {
                const userAssignments = state.userRolesByUser.get(assignment.userId) || [];
                userAssignments.push(assignment);
                state.userRolesByUser.set(assignment.userId, userAssignments);
            }
        } else {
            showAlert(`Không tải được role hiện tại cho danh sách: ${assignmentsResult.reason.message}`, "warning");
        }
        document.querySelector("#roleCount").textContent = String(state.roles.length);
        await loadUsersPage(0);

        const visibleUsers = visibleUsersOnPage();
        const userToSelect = visibleUsers.find((user) => user.id === preferredUserId) || visibleUsers[0];
        if (userToSelect) {
            await selectUser(userToSelect.id);
        } else {
            state.selectedUserId = null;
            userDetails.innerHTML = '<div class="empty-state">Không có tài khoản phù hợp để hiển thị.</div>';
            roleList.innerHTML = '<div class="empty-state">Chọn một người dùng ở danh sách.</div>';
            updateRoleSummary();
        }
    } catch (error) {
        userList.innerHTML = '<tr><td colspan="8" class="text-center text-danger py-4">Không thể tải danh sách người dùng.</td></tr>';
        showAlert(`Không tải được dữ liệu người dùng hoặc vai trò: ${error.message}`);
    }
}

userList.addEventListener("click", (event) => {
    const button = event.target.closest("[data-user-id]");
    if (button) selectUser(button.dataset.userId);
    else {
        const row = event.target.closest("tr[data-user-id]");
        if (row) selectUser(row.dataset.userId);
    }
});

userList.addEventListener("keydown", (event) => {
    const row = event.target.closest("tr[data-user-id]");
    if (row && (event.key === "Enter" || event.key === " ")) {
        event.preventDefault();
        selectUser(row.dataset.userId);
    }
});

function updateVisibleRoles(checked) {
    for (const role of filteredRoles()) {
        if (checked && role.isActive !== false) {
            state.selectedRoleIds.add(role.id);
        } else if (!checked) {
            state.selectedRoleIds.delete(role.id);
        }
    }
    renderRoles();
}

roleList.addEventListener("change", (event) => {
    const checkbox = event.target.closest("[data-role-id]");
    if (!checkbox) return;
    if (checkbox.checked) {
        state.selectedRoleIds.add(checkbox.dataset.roleId);
    } else {
        state.selectedRoleIds.delete(checkbox.dataset.roleId);
    }
    renderRoles();
});

userSearch.addEventListener("input", () => {
    window.clearTimeout(state.searchTimer);
    state.searchTimer = window.setTimeout(async () => {
        await loadUsersPage(0);
        const visibleUsers = visibleUsersOnPage();
        const selected = visibleUsers.find((user) => user.id === state.selectedUserId) || visibleUsers[0];
        if (selected) await selectUser(selected.id);
        else clearSelectedUser();
    }, 250);
});
userStatusFilter.addEventListener("change", async () => {
    await loadUsersPage(0);
    const visibleUsers = visibleUsersOnPage();
    const selected = visibleUsers.find((user) => user.id === state.selectedUserId) || visibleUsers[0];
    if (selected) await selectUser(selected.id);
    else clearSelectedUser();
});
document.querySelector("#userPrevPage").addEventListener("click", async () => {
    if (state.userPage > 0) await loadUsersPage(state.userPage - 1);
    const visibleUsers = visibleUsersOnPage();
    const selected = visibleUsers.find((user) => user.id === state.selectedUserId) || visibleUsers[0];
    if (selected) await selectUser(selected.id);
    else clearSelectedUser();
});
document.querySelector("#userNextPage").addEventListener("click", async () => {
    if (state.userPage + 1 < state.totalPages) await loadUsersPage(state.userPage + 1);
    const visibleUsers = visibleUsersOnPage();
    const selected = visibleUsers.find((user) => user.id === state.selectedUserId) || visibleUsers[0];
    if (selected) await selectUser(selected.id);
    else clearSelectedUser();
});
roleSearch.addEventListener("input", renderRoles);
roleFilter.addEventListener("change", renderRoles);
document.querySelector("#selectVisibleRoles").addEventListener("click", () => updateVisibleRoles(true));
document.querySelector("#clearVisibleRoles").addEventListener("click", () => updateVisibleRoles(false));

const addUserForm = document.querySelector("#addUserForm");
const createUserButton = document.querySelector("#createUserButton");
const addUserError = document.querySelector("#addUserError");
const addUserModal = bootstrap.Modal.getOrCreateInstance(document.querySelector("#addUserModal"));
const editUserForm = document.querySelector("#editUserForm");
const editUserModal = bootstrap.Modal.getOrCreateInstance(document.querySelector("#editUserModal"));
const passwordUserForm = document.querySelector("#passwordUserForm");
const passwordUserModal = bootstrap.Modal.getOrCreateInstance(document.querySelector("#passwordUserModal"));

addUserForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!addUserForm.reportValidity()) return;

    const formData = new FormData(addUserForm);
    const fullName = String(formData.get("fullName") || "").trim();
    const username = String(formData.get("username") || "").trim();
    const email = String(formData.get("email") || "").trim();
    const phone = String(formData.get("phone") || "").trim();
    const avatarUrl = String(formData.get("avatarUrl") || "").trim();
    const password = String(formData.get("password") || "");
    createUserButton.disabled = true;
    createUserButton.innerHTML = '<span class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>Đang tạo...';
    addUserError.classList.add("d-none");

    try {
        const createdUser = await requestJson("/users", {
            method: "POST",
            body: JSON.stringify({
                fullName,
                username,
                email: email || null,
                phone: phone || null,
                avatarUrl: avatarUrl || null,
                passwordHash: password,
                isActive: true,
            }),
        });
        addUserForm.reset();
        addUserModal.hide();
        userStatusFilter.value = "all";
        userSearch.value = "";
        showAlert(`Đã tạo tài khoản ${createdUser.fullName || fullName} thành công.`, "success");
        await loadPage(createdUser.id);
    } catch (error) {
        addUserError.textContent = `Không thể tạo người dùng: ${error.message}`;
        addUserError.classList.remove("d-none");
    } finally {
        createUserButton.disabled = false;
        createUserButton.innerHTML = '<i class="bi bi-person-plus me-1"></i>Tạo người dùng';
    }
});

document.querySelector("#addUserModal").addEventListener("hidden.bs.modal", () => {
    addUserForm.reset();
    addUserError.classList.add("d-none");
    addUserError.textContent = "";
});

function clearSelectedUser() {
    state.selectedUserId = null;
    state.assignments = [];
    state.selectedRoleIds.clear();
    saveStatus.textContent = "";
    renderUserDetails();
    roleList.innerHTML = '<div class="empty-state">Chọn một người dùng ở danh sách.</div>';
    renderUsers();
    renderRoles();
}

async function refreshUserList(preferredUserId = state.selectedUserId) {
    const requestedPage = state.userPage;
    await loadUsersPage(requestedPage);
    const visibleUsers = visibleUsersOnPage();
    const user = visibleUsers.find((item) => item.id === preferredUserId) || visibleUsers[0];
    if (user) await selectUser(user.id);
    else clearSelectedUser();
}

userDetails.addEventListener("click", async (event) => {
    const button = event.target.closest("[data-user-action]");
    if (!button) return;
    const user = selectedUser();
    if (!user) return;

    switch (button.dataset.userAction) {
        case "edit":
            editUserForm.elements.fullName.value = user.fullName || "";
            editUserForm.elements.username.value = user.username || "";
            editUserForm.elements.email.value = user.email || "";
            editUserForm.elements.phone.value = user.phone || "";
            editUserForm.elements.avatarUrl.value = user.avatarUrl || "";
            document.querySelector("#editUserError").classList.add("d-none");
            editUserModal.show();
            break;
        case "password":
            passwordUserForm.reset();
            document.querySelector("#passwordUserError").classList.add("d-none");
            passwordUserModal.show();
            break;
        case "toggle-active":
            try {
                const active = user.isActive === false;
                await requestJson(`/users/${encodeURIComponent(user.id)}/active?active=${active}`, { method: "PATCH" });
                showAlert(`Đã ${active ? "mở khóa" : "khóa"} tài khoản ${user.username}.`, "success");
                await refreshUserList(user.id);
            } catch (error) {
                showAlert(`Không thể đổi trạng thái tài khoản: ${error.message}`);
            }
            break;
        case "delete":
            if (!window.confirm(`Xóa mềm tài khoản ${user.username}?`)) return;
            try {
                await requestJson(`/users/${encodeURIComponent(user.id)}`, { method: "DELETE" });
                showAlert(`Đã xóa mềm tài khoản ${user.username}.`, "success");
                await refreshUserList(null);
            } catch (error) {
                showAlert(`Không thể xóa tài khoản: ${error.message}`);
            }
            break;
        case "restore":
            try {
                await requestJson(`/users/${encodeURIComponent(user.id)}/restore`, { method: "PATCH" });
                userStatusFilter.value = "all";
                userSearch.value = "";
                showAlert(`Đã khôi phục tài khoản ${user.username}.`, "success");
                await loadUsersPage(0);
                const visibleUsers = visibleUsersOnPage();
                const restored = visibleUsers.find((item) => item.id === user.id) || visibleUsers[0];
                if (restored) await selectUser(restored.id);
            } catch (error) {
                showAlert(`Không thể khôi phục tài khoản: ${error.message}`);
            }
            break;
        default:
            break;
    }
});

editUserForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!editUserForm.reportValidity() || !state.selectedUserId) return;
    const form = new FormData(editUserForm);
    const errorBox = document.querySelector("#editUserError");
    const submitButton = document.querySelector("#updateUserButton");
    submitButton.disabled = true;
    errorBox.classList.add("d-none");
    try {
        const updated = await requestJson(`/users/${encodeURIComponent(state.selectedUserId)}`, {
            method: "PUT",
            body: JSON.stringify({
                fullName: String(form.get("fullName") || "").trim(),
                username: String(form.get("username") || "").trim(),
                email: String(form.get("email") || "").trim(),
                phone: String(form.get("phone") || "").trim(),
                avatarUrl: String(form.get("avatarUrl") || "").trim(),
            }),
        });
        editUserModal.hide();
        showAlert(`Đã cập nhật hồ sơ ${updated.fullName || updated.username}.`, "success");
        await refreshUserList(updated.id);
    } catch (error) {
        errorBox.textContent = `Không thể cập nhật hồ sơ: ${error.message}`;
        errorBox.classList.remove("d-none");
    } finally {
        submitButton.disabled = false;
    }
});

passwordUserForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!passwordUserForm.reportValidity() || !state.selectedUserId) return;
    const passwordHash = String(new FormData(passwordUserForm).get("passwordHash") || "");
    const errorBox = document.querySelector("#passwordUserError");
    const submitButton = document.querySelector("#updatePasswordButton");
    submitButton.disabled = true;
    errorBox.classList.add("d-none");
    try {
        await requestJson(`/users/${encodeURIComponent(state.selectedUserId)}/password`, {
            method: "PATCH",
            body: JSON.stringify({ passwordHash }),
        });
        passwordUserModal.hide();
        passwordUserForm.reset();
        showAlert("Đã cập nhật mật khẩu theo API hiện tại.", "success");
    } catch (error) {
        errorBox.textContent = `Không thể cập nhật mật khẩu: ${error.message}`;
        errorBox.classList.remove("d-none");
    } finally {
        submitButton.disabled = false;
    }
});

saveButton.addEventListener("click", async () => {
    if (!state.selectedUserId) return;
    const userId = state.selectedUserId;
    const roleIds = Array.from(state.selectedRoleIds);
    saveButton.disabled = true;
    saveStatus.textContent = "Đang lưu...";
    saveStatus.className = "text-secondary";
    hideAlert();
    try {
        const result = await requestJson(`/user-roles/user/${encodeURIComponent(userId)}`, {
            method: "PUT",
            body: JSON.stringify({ roleIds }),
        });
        state.userRolesByUser.set(userId, result);
        renderUsers();
        if (state.selectedUserId === userId) {
            state.assignments = result;
            state.selectedRoleIds = new Set(result.map((assignment) => assignment.roleId));
            renderRoles();
            saveStatus.textContent = "Đã lưu phân quyền.";
            saveStatus.className = "text-success";
            showAlert("Cập nhật phân quyền thành công.", "success");
        }
    } catch (error) {
        saveStatus.textContent = "";
        showAlert(`Không thể lưu phân quyền: ${error.message}`);
    } finally {
        saveButton.disabled = !state.selectedUserId;
    }
});

loadPage();
