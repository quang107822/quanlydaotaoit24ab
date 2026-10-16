import { API_ENDPOINTS } from './config.js';

// ==========================================
// STATE MANAGEMENT
// ==========================================
const state = {
    employees: [],
    filteredEmployees: [],
    departments: [],
    positions: [],
    assignments: [],
    currentTab: 'employees',
    currentEmployee: null,
};

// ==========================================
// INITIALIZATION
// ==========================================
document.addEventListener('DOMContentLoaded', async () => {
    initTabRouting();
    setupEventListeners();
    await loadInitialData();
});

// Setup tab routing based on URL param or clicks
function initTabRouting() {
    const params = new URLSearchParams(window.location.search);
    const tabParam = params.get('tab') || 'employees';
    switchTab(tabParam, false);

    document.querySelectorAll('[data-bs-toggle="pill"]').forEach(tabBtn => {
        tabBtn.addEventListener('shown.bs.tab', (e) => {
            const targetId = e.target.getAttribute('data-bs-target').replace('#tab-', '');
            switchTab(targetId, true);
        });
    });
}

function switchTab(tabName, updateUrl = true) {
    state.currentTab = tabName;
    const tabTrigger = document.querySelector(`[data-bs-target="#tab-${tabName}"]`);
    if (tabTrigger && window.bootstrap && window.bootstrap.Tab) {
        const bsTab = bootstrap.Tab.getOrCreateInstance(tabTrigger);
        bsTab.show();
    } else if (tabTrigger) {
        document.querySelectorAll('.tab-pane').forEach(el => el.classList.remove('show', 'active'));
        document.querySelectorAll('.nav-pills-custom .nav-link').forEach(el => el.classList.remove('active'));
        tabTrigger.classList.add('active');
        const targetPane = document.getElementById(`tab-${tabName}`);
        if (targetPane) targetPane.classList.add('show', 'active');
    }

    if (updateUrl) {
        const newUrl = `${window.location.pathname}?tab=${tabName}`;
        window.history.replaceState(null, '', newUrl);
    }

    // Refresh data corresponding to active tab
    if (tabName === 'departments') renderDepartments();
    else if (tabName === 'positions') renderPositions();
    else if (tabName === 'assignments') loadAllAssignments();
    else renderEmployees();
}

async function loadInitialData() {
    try {
        await Promise.all([
            loadDepartments(),
            loadPositions(),
            loadEmployees()
        ]);
        populateDropdowns();
        updateKPIStats();
    } catch (err) {
        console.error('Lỗi khi tải dữ liệu ban đầu:', err);
        showToast('Lỗi', 'Không thể kết nối tới máy chủ API. Vui lòng kiểm tra backend.', 'error');
    }
}

// ==========================================
// TOAST NOTIFICATIONS
// ==========================================
export function showToast(title, message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `custom-toast toast-${type}`;

    let iconClass = 'bi-info-circle-fill';
    if (type === 'success') iconClass = 'bi-check-circle-fill';
    if (type === 'error') iconClass = 'bi-exclamation-triangle-fill';
    if (type === 'warning') iconClass = 'bi-exclamation-circle-fill';

    toast.innerHTML = `
        <div class="toast-icon"><i class="bi ${iconClass}"></i></div>
        <div class="toast-content">
            <div class="toast-title">${title}</div>
            <div class="toast-msg">${message}</div>
        </div>
        <button type="button" class="toast-close"><i class="bi bi-x-lg"></i></button>
    `;

    toast.querySelector('.toast-close').addEventListener('click', () => toast.remove());
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(60px)';
        setTimeout(() => toast.remove(), 300);
    }, 4500);
}

// ==========================================
// 1. QUẢN LÝ GIẢNG VIÊN (EMPLOYEES)
// ==========================================
export async function loadEmployees() {
    const tbody = document.getElementById('employeeTableBody');
    if (tbody) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Đang tải dữ liệu giảng viên...</td></tr>`;
    }

    try {
        const res = await fetch(API_ENDPOINTS.EMPLOYEES);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const result = await res.json();
        state.employees = result.data || [];
        applyEmployeeFilters();
        updateKPIStats();
    } catch (err) {
        console.error('Lỗi khi tải danh sách giảng viên:', err);
        if (tbody) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-danger"><i class="bi bi-exclamation-octagon me-2"></i>Không thể tải dữ liệu: ${err.message}</td></tr>`;
        }
    }
}

export function applyEmployeeFilters() {
    const keyword = (document.getElementById('empSearchKeyword')?.value || '').trim().toLowerCase();
    const deptId = document.getElementById('empFilterDept')?.value || '';
    const degree = document.getElementById('empFilterDegree')?.value || '';
    const status = document.getElementById('empFilterStatus')?.value || '';

    state.filteredEmployees = state.employees.filter(emp => {
        const matchKeyword = !keyword || 
            (emp.code && emp.code.toLowerCase().includes(keyword)) ||
            (emp.fullName && emp.fullName.toLowerCase().includes(keyword)) ||
            (emp.email && emp.email.toLowerCase().includes(keyword)) ||
            (emp.phone && emp.phone.includes(keyword)) ||
            (emp.specialization && emp.specialization.toLowerCase().includes(keyword));

        const matchDept = !deptId || (emp.department && emp.department.id === deptId);
        const matchDegree = !degree || (emp.academicDegree === degree);
        const matchStatus = !status || (status === 'active' ? emp.isActive : !emp.isActive);

        return matchKeyword && matchDept && matchDegree && matchStatus;
    });

    renderEmployees();
}

function renderEmployees() {
    const tbody = document.getElementById('employeeTableBody');
    if (!tbody) return;

    if (!state.filteredEmployees.length) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7">
                    <div class="empty-state">
                        <i class="bi bi-person-x"></i>
                        <h5>Không tìm thấy giảng viên nào</h5>
                        <p class="text-muted small">Thử thay đổi bộ lọc tìm kiếm hoặc thêm giảng viên mới</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = state.filteredEmployees.map(emp => {
        const initials = getInitials(emp.fullName || 'GV');
        const deptName = emp.department ? emp.department.name : '<span class="text-muted fst-italic">Chưa phân khoa</span>';
        const posTitle = emp.position ? emp.position.title : '<span class="text-muted">Chưa gán</span>';
        const degreeBadge = emp.academicDegree ? `<span class="badge-degree">${emp.academicTitle ? emp.academicTitle + ' ' : ''}${emp.academicDegree}</span>` : '<span class="text-muted small">--</span>';
        const statusBadge = emp.isActive 
            ? `<span class="badge-status-active">Đang công tác</span>`
            : `<span class="badge-status-inactive">Đã nghỉ</span>`;

        return `
            <tr>
                <td>
                    <span class="fw-bold text-primary font-monospace">${emp.code || '--'}</span>
                </td>
                <td>
                    <div class="staff-info-cell">
                        <div class="staff-avatar">${initials}</div>
                        <div>
                            <div class="fw-bold text-dark">${emp.fullName || 'Chưa có tên'}</div>
                            <div class="small text-muted">${emp.email || 'Chưa có email'}</div>
                        </div>
                    </div>
                </td>
                <td>${degreeBadge}</td>
                <td><span class="badge-dept">${deptName}</span></td>
                <td><span class="badge-position">${posTitle}</span></td>
                <td>
                    <button class="btn btn-link p-0 text-decoration-none" onclick="window.toggleEmployeeStatus('${emp.id}', ${!emp.isActive})">
                        ${statusBadge}
                    </button>
                </td>
                <td class="text-end">
                    <div class="action-btn-group">
                        <button class="action-btn btn-view" title="Xem chi tiết hồ sơ" onclick="window.viewEmployeeDetail('${emp.id}')">
                            <i class="bi bi-eye"></i>
                        </button>
                        <button class="action-btn btn-assign" title="Phân công chức vụ" onclick="window.openAssignModal('${emp.id}')">
                            <i class="bi bi-person-badge"></i>
                        </button>
                        <button class="action-btn btn-edit" title="Chỉnh sửa thông tin" onclick="window.editEmployee('${emp.id}')">
                            <i class="bi bi-pencil-square"></i>
                        </button>
                        <button class="action-btn btn-delete" title="Xóa hồ sơ" onclick="window.deleteEmployee('${emp.id}', '${escapeHtml(emp.fullName)}')">
                            <i class="bi bi-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

// Thêm mới giảng viên
async function handleCreateEmployee(e) {
    e.preventDefault();
    const form = e.target;
    const submitBtn = form.querySelector('button[type="submit"]');
    const originalText = submitBtn.innerHTML;

    const payload = {
        code: form.code.value.trim(),
        full_name: form.fullName.value.trim(),
        date_of_birth: form.dateOfBirth.value || null,
        gender: form.gender.value,
        email: form.email.value.trim(),
        phone: form.phone.value.trim(),
        address: form.address.value.trim(),
        department_id: form.departmentId.value,
        position_id: form.positionId.value || null,
        hire_date: form.hireDate.value || null,
        contract_type: form.contractType.value,
        salary_coefficient: form.salaryCoefficient.value ? parseFloat(form.salaryCoefficient.value) : 2.34,
        academic_degree: form.academicDegree.value,
        academic_title: form.academicTitle.value,
        specialization: form.specialization.value.trim(),
        start_date: form.hireDate.value ? `${form.hireDate.value}T08:00:00` : null
    };

    if (!payload.code || !payload.full_name || !payload.email || !payload.department_id) {
        showToast('Cảnh báo', 'Vui lòng điền đủ Mã GV, Họ tên, Email và Khoa/Phòng ban!', 'warning');
        return;
    }

    try {
        submitBtn.disabled = true;
        submitBtn.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span>Đang lưu...`;

        const res = await fetch(API_ENDPOINTS.EMPLOYEES, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.message || `HTTP ${res.status}`);

        showToast('Thành công', 'Đã thêm mới hồ sơ giảng viên/nhân sự thành công!', 'success');
        form.reset();
        
        // Hide modal
        const modalEl = document.getElementById('addEmployeeModal');
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();

        await loadEmployees();
    } catch (err) {
        console.error('Lỗi khi thêm giảng viên:', err);
        showToast('Lỗi', `Thêm mới thất bại: ${err.message}`, 'error');
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;
    }
}

// Cập nhật giảng viên
async function handleUpdateEmployee(e) {
    e.preventDefault();
    const form = e.target;
    const empId = form.employeeId.value;
    const submitBtn = form.querySelector('button[type="submit"]');
    const originalText = submitBtn.innerHTML;

    const payload = {
        code: form.code.value.trim(),
        fullName: form.fullName.value.trim(),
        dateOfBirth: form.dateOfBirth.value || null,
        gender: form.gender.value,
        email: form.email.value.trim(),
        phone: form.phone.value.trim(),
        address: form.address.value.trim(),
        hireDate: form.hireDate.value || null,
        contractType: form.contractType.value,
        salaryCoefficient: form.salaryCoefficient.value ? parseFloat(form.salaryCoefficient.value) : 2.34,
        academicDegree: form.academicDegree.value,
        academicTitle: form.academicTitle.value,
        specialization: form.specialization.value.trim(),
        department: { id: form.departmentId.value },
        position: form.positionId.value ? { id: form.positionId.value } : null
    };

    try {
        submitBtn.disabled = true;
        submitBtn.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span>Đang cập nhật...`;

        const res = await fetch(`${API_ENDPOINTS.EMPLOYEES}/${empId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.message || `HTTP ${res.status}`);

        showToast('Thành công', 'Cập nhật thông tin giảng viên thành công!', 'success');
        
        const modalEl = document.getElementById('editEmployeeModal');
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();

        await loadEmployees();
    } catch (err) {
        console.error('Lỗi khi cập nhật giảng viên:', err);
        showToast('Lỗi', `Cập nhật thất bại: ${err.message}`, 'error');
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;
    }
}

// Xóa giảng viên
window.deleteEmployee = async function(id, name) {
    if (!confirm(`Bạn có chắc chắn muốn xóa hồ sơ giảng viên "${name}"?`)) return;

    try {
        const res = await fetch(`${API_ENDPOINTS.EMPLOYEES}/${id}`, {
            method: 'DELETE'
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        showToast('Thành công', `Đã xóa giảng viên "${name}" thành công`, 'success');
        await loadEmployees();
    } catch (err) {
        console.error('Lỗi khi xóa:', err);
        showToast('Lỗi', `Không thể xóa giảng viên: ${err.message}`, 'error');
    }
};

// Đổi trạng thái hoạt động
window.toggleEmployeeStatus = async function(id, newStatus) {
    try {
        const res = await fetch(`${API_ENDPOINTS.EMPLOYEES}/${id}/status`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ is_active: newStatus })
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        showToast('Đã đổi trạng thái', newStatus ? 'Đã kích hoạt trạng thái Đang công tác' : 'Đã chuyển sang Đã nghỉ việc', 'info');
        await loadEmployees();
    } catch (err) {
        console.error('Lỗi khi đổi trạng thái:', err);
        showToast('Lỗi', `Không thể đổi trạng thái: ${err.message}`, 'error');
    }
};

// Mở modal sửa giảng viên
window.editEmployee = function(id) {
    const emp = state.employees.find(e => e.id === id);
    if (!emp) return;

    const form = document.getElementById('editEmployeeForm');
    form.employeeId.value = emp.id;
    form.code.value = emp.code || '';
    form.fullName.value = emp.fullName || '';
    form.dateOfBirth.value = emp.dateOfBirth || '';
    form.gender.value = emp.gender || 'Nam';
    form.email.value = emp.email || '';
    form.phone.value = emp.phone || '';
    form.address.value = emp.address || '';
    form.departmentId.value = emp.department ? emp.department.id : '';
    form.positionId.value = emp.position ? emp.position.id : '';
    form.hireDate.value = emp.hireDate || '';
    form.contractType.value = emp.contractType || 'Cơ hữu';
    form.salaryCoefficient.value = emp.salaryCoefficient || '2.34';
    form.academicDegree.value = emp.academicDegree || '';
    form.academicTitle.value = emp.academicTitle || '';
    form.specialization.value = emp.specialization || '';

    const modal = new bootstrap.Modal(document.getElementById('editEmployeeModal'));
    modal.show();
};

// Mở modal xem chi tiết giảng viên & lịch sử chức vụ
window.viewEmployeeDetail = async function(id) {
    const emp = state.employees.find(e => e.id === id);
    if (!emp) return;

    state.currentEmployee = emp;
    const modalEl = document.getElementById('detailEmployeeModal');
    
    // Fill main details
    document.getElementById('detFullName').textContent = emp.fullName || '---';
    document.getElementById('detCode').textContent = emp.code || '---';
    document.getElementById('detEmail').textContent = emp.email || '---';
    document.getElementById('detPhone').textContent = emp.phone || '---';
    document.getElementById('detGender').textContent = emp.gender || '---';
    document.getElementById('detDob').textContent = emp.dateOfBirth ? formatDate(emp.dateOfBirth) : '---';
    document.getElementById('detAddress').textContent = emp.address || '---';
    document.getElementById('detDept').textContent = emp.department ? emp.department.name : '---';
    document.getElementById('detPosition').textContent = emp.position ? emp.position.title : 'Chưa có chức vụ chính';
    document.getElementById('detDegree').textContent = emp.academicDegree || '---';
    document.getElementById('detTitle').textContent = emp.academicTitle || '---';
    document.getElementById('detSpecialization').textContent = emp.specialization || '---';
    document.getElementById('detContract').textContent = emp.contractType || '---';
    document.getElementById('detSalary').textContent = emp.salaryCoefficient ? `${emp.salaryCoefficient}` : '---';
    document.getElementById('detHireDate').textContent = emp.hireDate ? formatDate(emp.hireDate) : '---';
    document.getElementById('detStatus').innerHTML = emp.isActive 
        ? `<span class="badge-status-active">Đang công tác</span>`
        : `<span class="badge-status-inactive">Đã nghỉ</span>`;

    // Fetch and render assignment history
    const historyTbody = document.getElementById('detHistoryTableBody');
    historyTbody.innerHTML = `<tr><td colspan="5" class="text-center py-3 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Đang tải lịch sử công tác...</td></tr>`;

    const modal = new bootstrap.Modal(modalEl);
    modal.show();

    try {
        const res = await fetch(`${API_ENDPOINTS.EMPLOYEES}/${emp.id}/positions`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        const positions = data.data || [];

        if (!positions.length) {
            historyTbody.innerHTML = `<tr><td colspan="5" class="text-center py-3 text-muted">Chưa có bản ghi phân công chức vụ nào</td></tr>`;
            return;
        }

        historyTbody.innerHTML = positions.map(p => {
            const posTitle = p.position ? p.position.title : 'Chức vụ';
            const startDate = p.startDate ? formatDateTime(p.startDate) : '--';
            const endDate = p.endDate ? formatDateTime(p.endDate) : '<span class="badge bg-success-subtle text-success border border-success-subtle">Hiện tại</span>';
            const desc = p.description || p.note || '--';

            return `
                <tr>
                    <td><strong class="text-primary">${posTitle}</strong></td>
                    <td>${startDate}</td>
                    <td>${endDate}</td>
                    <td>${desc}</td>
                    <td class="text-end">
                        <button class="btn btn-sm btn-outline-danger" title="Gỡ chức vụ này" onclick="window.unassignPosition('${emp.id}', '${p.position ? p.position.id : ''}')">
                            <i class="bi bi-x-circle"></i>
                        </button>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        historyTbody.innerHTML = `<tr><td colspan="5" class="text-center py-3 text-muted">Không thể tải lịch sử: ${err.message}</td></tr>`;
    }
};

// ==========================================
// 2. QUẢN LÝ KHOA - PHÒNG BAN (DEPARTMENTS)
// ==========================================
export async function loadDepartments() {
    try {
        const res = await fetch(API_ENDPOINTS.DEPARTMENTS);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const result = await res.json();
        state.departments = result.data || [];
        populateDropdowns();
        renderDepartments();
    } catch (err) {
        console.error('Lỗi tải danh sách khoa:', err);
    }
}

function renderDepartments() {
    const tbody = document.getElementById('deptTableBody');
    if (!tbody) return;

    if (!state.departments.length) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">Chưa có khoa / phòng ban nào</td></tr>`;
        return;
    }

    tbody.innerHTML = state.departments.map(d => {
        // Count employees in this dept
        const empCount = state.employees.filter(e => e.department && e.department.id === d.id).length;
        const estDate = d.establishedDate ? formatDate(d.establishedDate) : '--';

        return `
            <tr>
                <td><span class="fw-bold font-monospace text-primary">${d.code}</span></td>
                <td>
                    <div class="fw-bold text-dark">${d.name}</div>
                </td>
                <td>${d.phone || '--'}</td>
                <td>${d.email || '--'}</td>
                <td><span class="badge bg-primary-subtle text-primary border border-primary-subtle px-2 py-1">${empCount} giảng viên</span></td>
                <td class="text-end">
                    <div class="action-btn-group">
                        <button class="action-btn btn-edit" title="Sửa khoa" onclick="window.editDepartment('${d.id}')">
                            <i class="bi bi-pencil-square"></i>
                        </button>
                        <button class="action-btn btn-delete" title="Xóa khoa" onclick="window.deleteDepartment('${d.id}', '${escapeHtml(d.name)}')">
                            <i class="bi bi-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

async function handleCreateDepartment(e) {
    e.preventDefault();
    const form = e.target;
    const submitBtn = form.querySelector('button[type="submit"]');

    const payload = {
        code: form.code.value.trim().toUpperCase(),
        name: form.name.value.trim(),
        phone: form.phone.value.trim(),
        email: form.email.value.trim(),
        establishedDate: form.establishedDate.value || null
    };

    try {
        submitBtn.disabled = true;
        const res = await fetch(API_ENDPOINTS.DEPARTMENTS, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || `HTTP ${res.status}`);

        showToast('Thành công', 'Thêm mới Khoa / Phòng ban thành công!', 'success');
        form.reset();
        bootstrap.Modal.getInstance(document.getElementById('addDeptModal'))?.hide();
        await loadDepartments();
    } catch (err) {
        showToast('Lỗi', `Không thể thêm khoa: ${err.message}`, 'error');
    } finally {
        submitBtn.disabled = false;
    }
}

window.editDepartment = function(id) {
    const dept = state.departments.find(d => d.id === id);
    if (!dept) return;

    const form = document.getElementById('editDeptForm');
    form.deptId.value = dept.id;
    form.code.value = dept.code || '';
    form.name.value = dept.name || '';
    form.phone.value = dept.phone || '';
    form.email.value = dept.email || '';
    form.establishedDate.value = dept.establishedDate || '';

    const modal = new bootstrap.Modal(document.getElementById('editDeptModal'));
    modal.show();
};

async function handleUpdateDepartment(e) {
    e.preventDefault();
    const form = e.target;
    const deptId = form.deptId.value;

    const payload = {
        code: form.code.value.trim().toUpperCase(),
        name: form.name.value.trim(),
        phone: form.phone.value.trim(),
        email: form.email.value.trim(),
        establishedDate: form.establishedDate.value || null
    };

    try {
        const res = await fetch(`${API_ENDPOINTS.DEPARTMENTS}/${deptId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);

        showToast('Thành công', 'Cập nhật Khoa / Phòng ban thành công!', 'success');
        bootstrap.Modal.getInstance(document.getElementById('editDeptModal'))?.hide();
        await loadDepartments();
    } catch (err) {
        showToast('Lỗi', `Cập nhật thất bại: ${err.message}`, 'error');
    }
}

window.deleteDepartment = async function(id, name) {
    if (!confirm(`Bạn có chắc muốn xóa khoa "${name}"?`)) return;
    try {
        const res = await fetch(`${API_ENDPOINTS.DEPARTMENTS}/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        showToast('Thành công', `Đã xóa khoa "${name}"!`, 'success');
        await loadDepartments();
    } catch (err) {
        showToast('Lỗi', `Không thể xóa khoa: ${err.message}`, 'error');
    }
};

// ==========================================
// 3. QUẢN LÝ CHỨC VỤ / VỊ TRÍ (POSITIONS)
// ==========================================
export async function loadPositions() {
    try {
        const res = await fetch(API_ENDPOINTS.POSITIONS);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const result = await res.json();
        state.positions = result.data || [];
        populateDropdowns();
        renderPositions();
    } catch (err) {
        console.error('Lỗi tải danh sách chức vụ:', err);
    }
}

function renderPositions() {
    const tbody = document.getElementById('positionTableBody');
    if (!tbody) return;

    if (!state.positions.length) {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-muted">Chưa có chức vụ nào</td></tr>`;
        return;
    }

    tbody.innerHTML = state.positions.map(p => {
        const deptName = p.department ? p.department.name : '<span class="text-muted fst-italic">Áp dụng toàn trường</span>';
        const empCount = state.employees.filter(e => e.position && e.position.id === p.id).length;

        return `
            <tr>
                <td><span class="fw-bold font-monospace text-primary">${p.code}</span></td>
                <td><strong class="text-dark">${p.title}</strong></td>
                <td><span class="badge bg-secondary-subtle text-secondary border px-2 py-1">Cấp ${p.level || 1}</span></td>
                <td><span class="badge-dept">${deptName}</span></td>
                <td class="text-end">
                    <div class="action-btn-group">
                        <button class="action-btn btn-edit" title="Sửa chức vụ" onclick="window.editPosition('${p.id}')">
                            <i class="bi bi-pencil-square"></i>
                        </button>
                        <button class="action-btn btn-delete" title="Xóa chức vụ" onclick="window.deletePosition('${p.id}', '${escapeHtml(p.title)}')">
                            <i class="bi bi-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

async function handleCreatePosition(e) {
    e.preventDefault();
    const form = e.target;
    const payload = {
        code: form.code.value.trim().toUpperCase(),
        title: form.title.value.trim(),
        level: parseInt(form.level.value) || 1,
        department: form.departmentId.value ? { id: form.departmentId.value } : null
    };

    try {
        const res = await fetch(API_ENDPOINTS.POSITIONS, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || `HTTP ${res.status}`);

        showToast('Thành công', 'Thêm mới chức vụ thành công!', 'success');
        form.reset();
        bootstrap.Modal.getInstance(document.getElementById('addPositionModal'))?.hide();
        await loadPositions();
    } catch (err) {
        showToast('Lỗi', `Không thể thêm chức vụ: ${err.message}`, 'error');
    }
}

window.editPosition = function(id) {
    const pos = state.positions.find(p => p.id === id);
    if (!pos) return;

    const form = document.getElementById('editPositionForm');
    form.positionId.value = pos.id;
    form.code.value = pos.code || '';
    form.title.value = pos.title || '';
    form.level.value = pos.level || 1;
    form.departmentId.value = pos.department ? pos.department.id : '';

    const modal = new bootstrap.Modal(document.getElementById('editPositionModal'));
    modal.show();
};

async function handleUpdatePosition(e) {
    e.preventDefault();
    const form = e.target;
    const posId = form.positionId.value;

    const payload = {
        code: form.code.value.trim().toUpperCase(),
        title: form.title.value.trim(),
        level: parseInt(form.level.value) || 1,
        department: form.departmentId.value ? { id: form.departmentId.value } : null
    };

    try {
        const res = await fetch(`${API_ENDPOINTS.POSITIONS}/${posId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);

        showToast('Thành công', 'Cập nhật chức vụ thành công!', 'success');
        bootstrap.Modal.getInstance(document.getElementById('editPositionModal'))?.hide();
        await loadPositions();
    } catch (err) {
        showToast('Lỗi', `Cập nhật chức vụ thất bại: ${err.message}`, 'error');
    }
}

window.deletePosition = async function(id, title) {
    if (!confirm(`Bạn có chắc muốn xóa chức vụ "${title}"?`)) return;
    try {
        const res = await fetch(`${API_ENDPOINTS.POSITIONS}/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        showToast('Thành công', `Đã xóa chức vụ "${title}"!`, 'success');
        await loadPositions();
    } catch (err) {
        showToast('Lỗi', `Không thể xóa chức vụ: ${err.message}`, 'error');
    }
};

// ==========================================
// 4. PHÂN CÔNG CHỨC VỤ (ASSIGNMENTS)
// ==========================================
window.openAssignModal = function(empId) {
    const emp = state.employees.find(e => e.id === empId);
    if (!emp) return;

    const form = document.getElementById('assignPositionForm');
    form.employeeId.value = emp.id;
    document.getElementById('assignEmpName').textContent = `${emp.fullName} (${emp.code})`;
    form.positionId.value = emp.position ? emp.position.id : '';
    form.startDate.value = new Date().toISOString().slice(0, 16);
    form.description.value = '';
    form.note.value = '';

    const modal = new bootstrap.Modal(document.getElementById('assignPositionModal'));
    modal.show();
};

async function handleAssignPosition(e) {
    e.preventDefault();
    const form = e.target;
    const empId = form.employeeId.value;
    const posId = form.positionId.value;
    const submitBtn = form.querySelector('button[type="submit"]');

    if (!posId) {
        showToast('Cảnh báo', 'Vui lòng chọn chức vụ cần phân công!', 'warning');
        return;
    }

    const payload = {
        startDate: form.startDate.value ? `${form.startDate.value}:00` : new Date().toISOString(),
        description: form.description.value.trim(),
        note: form.note.value.trim()
    };

    try {
        submitBtn.disabled = true;
        const res = await fetch(`${API_ENDPOINTS.EMPLOYEES}/${empId}/positions/${posId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || `HTTP ${res.status}`);

        showToast('Thành công', 'Phân công chức vụ thành công!', 'success');
        bootstrap.Modal.getInstance(document.getElementById('assignPositionModal'))?.hide();

        await loadEmployees();
        if (state.currentTab === 'assignments') {
            await loadAllAssignments();
        }
    } catch (err) {
        showToast('Lỗi', `Phân công thất bại: ${err.message}`, 'error');
    } finally {
        submitBtn.disabled = false;
    }
}

window.unassignPosition = async function(empId, posId) {
    if (!posId) return;
    if (!confirm('Bạn có chắc muốn gỡ bỏ chức vụ này khỏi giảng viên?')) return;

    try {
        const res = await fetch(`${API_ENDPOINTS.EMPLOYEES}/${empId}/positions/${posId}`, {
            method: 'DELETE'
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);

        showToast('Thành công', 'Đã gỡ chức vụ thành công!', 'success');
        if (state.currentEmployee && state.currentEmployee.id === empId) {
            window.viewEmployeeDetail(empId);
        }
        await loadEmployees();
        if (state.currentTab === 'assignments') {
            await loadAllAssignments();
        }
    } catch (err) {
        showToast('Lỗi', `Không thể gỡ chức vụ: ${err.message}`, 'error');
    }
};

async function loadAllAssignments() {
    const tbody = document.getElementById('assignmentTableBody');
    if (!tbody) return;

    tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Đang tổng hợp dữ liệu phân công...</td></tr>`;

    try {
        // Collect assignments across active employees
        const assignmentList = [];
        for (const emp of state.employees) {
            if (emp.position) {
                assignmentList.push({
                    emp: emp,
                    position: emp.position,
                    startDate: emp.hireDate,
                    endDate: null,
                    description: 'Chức danh công tác chính'
                });
            }
        }

        if (!assignmentList.length) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">Chưa có bản ghi phân công nào</td></tr>`;
            return;
        }

        tbody.innerHTML = assignmentList.map(a => {
            const initials = getInitials(a.emp.fullName);
            const deptName = a.emp.department ? a.emp.department.name : '--';
            const startDate = a.startDate ? formatDate(a.startDate) : '--';
            const endDate = a.endDate ? formatDate(a.endDate) : '<span class="badge bg-success-subtle text-success border border-success-subtle">Đang đảm nhiệm</span>';

            return `
                <tr>
                    <td>
                        <div class="staff-info-cell">
                            <div class="staff-avatar">${initials}</div>
                            <div>
                                <div class="fw-bold">${a.emp.fullName}</div>
                                <span class="small text-muted font-monospace">${a.emp.code}</span>
                            </div>
                        </div>
                    </td>
                    <td><span class="badge-dept">${deptName}</span></td>
                    <td><strong class="text-primary">${a.position.title}</strong></td>
                    <td>${startDate}</td>
                    <td>${endDate}</td>
                    <td>${a.description || '--'}</td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-danger">Lỗi khi tải phân công: ${err.message}</td></tr>`;
    }
}

// ==========================================
// HELPER FUNCTIONS & DROPDOWNS
// ==========================================
function populateDropdowns() {
    // Dept dropdowns
    const deptOptions = `<option value="">-- Chọn Khoa / Viện --</option>` +
        state.departments.map(d => `<option value="${d.id}">${d.name} (${d.code})</option>`).join('');

    const filterDept = document.getElementById('empFilterDept');
    if (filterDept) {
        filterDept.innerHTML = `<option value="">Tất cả khoa / phòng ban</option>` +
            state.departments.map(d => `<option value="${d.id}">${d.name}</option>`).join('');
    }

    const addEmpDept = document.getElementById('addEmpDepartmentId');
    if (addEmpDept) addEmpDept.innerHTML = deptOptions;

    const editEmpDept = document.getElementById('editEmpDepartmentId');
    if (editEmpDept) editEmpDept.innerHTML = deptOptions;

    const addPosDept = document.getElementById('addPosDepartmentId');
    if (addPosDept) {
        addPosDept.innerHTML = `<option value="">-- Toàn trường / Chung --</option>` +
            state.departments.map(d => `<option value="${d.id}">${d.name}</option>`).join('');
    }

    const editPosDept = document.getElementById('editPosDepartmentId');
    if (editPosDept) {
        editPosDept.innerHTML = `<option value="">-- Toàn trường / Chung --</option>` +
            state.departments.map(d => `<option value="${d.id}">${d.name}</option>`).join('');
    }

    // Position dropdowns
    const posOptions = `<option value="">-- Chọn Chức vụ / Vị trí --</option>` +
        state.positions.map(p => `<option value="${p.id}">${p.title} (${p.code})</option>`).join('');

    const addEmpPos = document.getElementById('addEmpPositionId');
    if (addEmpPos) addEmpPos.innerHTML = posOptions;

    const editEmpPos = document.getElementById('editEmpPositionId');
    if (editEmpPos) editEmpPos.innerHTML = posOptions;

    const assignPos = document.getElementById('assignPositionSelect');
    if (assignPos) assignPos.innerHTML = posOptions;
}

function updateKPIStats() {
    const totalStaff = state.employees.length;
    const totalPhd = state.employees.filter(e => e.academicDegree === 'Tiến sĩ' || (e.academicTitle && (e.academicTitle.includes('GS') || e.academicTitle.includes('Phó')))).length;
    const totalDepts = state.departments.length;
    const activeStaff = state.employees.filter(e => e.isActive).length;

    const kpiTotal = document.getElementById('kpiTotalStaff');
    if (kpiTotal) kpiTotal.textContent = totalStaff;

    const kpiPhd = document.getElementById('kpiTotalPhd');
    if (kpiPhd) kpiPhd.textContent = totalPhd;

    const kpiDepts = document.getElementById('kpiTotalDepts');
    if (kpiDepts) kpiDepts.textContent = totalDepts;

    const kpiActive = document.getElementById('kpiActiveRate');
    if (kpiActive) {
        const rate = totalStaff > 0 ? Math.round((activeStaff / totalStaff) * 100) : 100;
        kpiActive.textContent = `${rate}%`;
    }
}

function getInitials(name) {
    if (!name) return 'GV';
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) {
        return (parts[parts.length - 2][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return parts[0].slice(0, 2).toUpperCase();
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    try {
        const d = new Date(dateStr);
        return d.toLocaleDateString('vi-VN');
    } catch {
        return dateStr;
    }
}

function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '';
    try {
        const d = new Date(dateTimeStr);
        return `${d.toLocaleDateString('vi-VN')} ${d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}`;
    } catch {
        return dateTimeStr;
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/'/g, "\\'").replace(/"/g, '&quot;');
}

// ==========================================
// EVENT LISTENERS BINDING
// ==========================================
function setupEventListeners() {
    // Search & Filter
    document.getElementById('empSearchKeyword')?.addEventListener('input', debounce(applyEmployeeFilters, 250));
    document.getElementById('empFilterDept')?.addEventListener('change', applyEmployeeFilters);
    document.getElementById('empFilterDegree')?.addEventListener('change', applyEmployeeFilters);
    document.getElementById('empFilterStatus')?.addEventListener('change', applyEmployeeFilters);
    document.getElementById('btnResetFilters')?.addEventListener('click', () => {
        document.getElementById('empSearchKeyword').value = '';
        document.getElementById('empFilterDept').value = '';
        document.getElementById('empFilterDegree').value = '';
        document.getElementById('empFilterStatus').value = '';
        applyEmployeeFilters();
    });

    // Forms
    document.getElementById('addEmployeeForm')?.addEventListener('submit', handleCreateEmployee);
    document.getElementById('editEmployeeForm')?.addEventListener('submit', handleUpdateEmployee);
    document.getElementById('addDeptForm')?.addEventListener('submit', handleCreateDepartment);
    document.getElementById('editDeptForm')?.addEventListener('submit', handleUpdateDepartment);
    document.getElementById('addPositionForm')?.addEventListener('submit', handleCreatePosition);
    document.getElementById('editPositionForm')?.addEventListener('submit', handleUpdatePosition);
    document.getElementById('assignPositionForm')?.addEventListener('submit', handleAssignPosition);

    // Refresh buttons
    document.getElementById('btnRefreshEmployees')?.addEventListener('click', async () => {
        showToast('Đang tải', 'Làm mới danh sách giảng viên...', 'info');
        await loadEmployees();
    });
}

function debounce(fn, ms) {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), ms);
    };
}
