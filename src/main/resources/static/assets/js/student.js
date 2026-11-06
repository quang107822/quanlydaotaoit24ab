import { API_ENDPOINTS } from './config.js';

const PAGE_SIZE = 8;
const courses = [];
const semesters = [];
let students = [];
let managers = [];
let registrationPeriods = [];
let equivalentCourses = [];
let eligibleStudents = [];
let managerStudents = [];
let sections = [];
let totalSections = 0;
let currentPage = 0;
let sectionModal;
let periodModal;
let equivalentModal;
let studentAddModal;
let managerAssignModal;
let registrationsDrawer;
let toastTimer;
let searchTimer;
let eligibleImportInput;

document.addEventListener('DOMContentLoaded', () => {
    bindModuleNavigation();
    showScreen(window.location.hash.slice(1) || 'sections', false);

    if (!window.bootstrap?.Modal || !window.bootstrap?.Offcanvas) {
        notify('Không tải được thành phần giao diện Bootstrap. Hãy kiểm tra kết nối rồi tải lại trang.', 'danger');
        return;
    }

    sectionModal = window.bootstrap.Modal.getOrCreateInstance(document.getElementById('sectionModal'));
    periodModal = window.bootstrap.Modal.getOrCreateInstance(document.getElementById('periodModal'));
    equivalentModal = window.bootstrap.Modal.getOrCreateInstance(document.getElementById('equivalentModal'));
    studentAddModal = window.bootstrap.Modal.getOrCreateInstance(document.getElementById('studentAddModal'));
    managerAssignModal = window.bootstrap.Modal.getOrCreateInstance(document.getElementById('managerAssignModal'));
    registrationsDrawer = window.bootstrap.Offcanvas.getOrCreateInstance(document.getElementById('registrationsDrawer'));
    bindEvents();
    initializeFromApi();
});

async function initializeFromApi() {
    clearPreviewContent();
    const results = await Promise.allSettled([
        loadLookups(),
        loadSections(),
        loadRegistrationPeriods(),
        loadEquivalentCourses(),
        loadManagerStudents()
    ]);
    const failed = results.filter((result) => result.status === 'rejected');
    const dependentResults = await Promise.allSettled([
        loadEligibleStudents(),
        loadReportSummary(),
        loadRetakeStatistics()
    ]);
    failed.push(...dependentResults.filter((result) => result.status === 'rejected'));
    if (failed.length > 0) showApiFailure(failed[0].reason);
}

async function loadLookups() {
    const results = await Promise.allSettled([
        apiRequest(API_ENDPOINTS.COURSES),
        apiRequest(API_ENDPOINTS.SEMESTERS),
        apiRequest(API_ENDPOINTS.STUDENTS),
        apiRequest(API_ENDPOINTS.LECTURERS)
    ]);
    const failures = [];
    const applyResult = (result, apply) => {
        if (result.status === 'fulfilled') apply(result.value);
        else failures.push(result.reason);
    };
    applyResult(results[0], (items) => courses.splice(0, courses.length, ...items));
    applyResult(results[1], (items) => semesters.splice(0, semesters.length, ...items));
    applyResult(results[2], (items) => { students = items; });
    applyResult(results[3], (items) => { managers = items; });
    populateLookups();
    populatePeopleSelectors();
    renderCourseCatalog();
    if (failures.length > 0) throw failures[0];
}

async function loadRegistrationPeriods() {
    try {
        const result = await apiRequest(`${API_ENDPOINTS.REGISTRATION_PERIODS}?page=0&size=100`);
        registrationPeriods = result.items;
        renderRegistrationPeriods();
        populatePeriodSelectors();
        updateOverview();
    } catch (error) {
        registrationPeriods = [];
        renderRegistrationPeriods();
        populatePeriodSelectors();
        throw error;
    }
}

async function loadEquivalentCourses() {
    try {
        equivalentCourses = await apiRequest(API_ENDPOINTS.EQUIVALENT_COURSES);
        renderEquivalentCourses();
    } catch (error) {
        equivalentCourses = [];
        renderEquivalentCourses();
        throw error;
    }
}

async function loadManagerStudents() {
    try {
        managerStudents = await apiRequest(API_ENDPOINTS.MANAGER_STUDENTS);
        renderManagerStudents();
    } catch (error) {
        managerStudents = [];
        renderManagerStudents();
        throw error;
    }
}

function clearPreviewContent() {
    document.querySelectorAll('#screen-periods .mini-stat strong').forEach((node) => { node.textContent = '—'; });
    document.querySelectorAll('#screen-reports .report-kpi strong').forEach((node) => { node.textContent = '—'; });
    document.querySelectorAll('#screen-periods tbody, #screen-equivalents tbody, #screen-eligible-students tbody, #screen-advisors tbody')
        .forEach((body) => { body.innerHTML = '<tr><td colspan="8" class="text-center text-body-secondary py-4">Đang tải dữ liệu từ API...</td></tr>'; });
    const reportCourses = document.querySelector('#screen-reports .col-xl-5 .card-body');
    if (reportCourses) reportCourses.innerHTML = '<p class="text-body-secondary mb-0">Đang tải dữ liệu từ API...</p>';
    document.getElementById('overviewOpenPeriods').textContent = '—';
    document.getElementById('overviewEligible').textContent = '—';
    document.getElementById('overviewActivePeriod').textContent = '—';
    document.getElementById('overviewActivePeriodDates').textContent = 'Dữ liệu từ API';
    const eligibilityContext = document.querySelector('.eligibility-context');
    eligibilityContext.querySelectorAll('strong').forEach((node) => { node.textContent = '—'; });
    const reportPeriodSelect = document.querySelector('.report-period-select');
    reportPeriodSelect.replaceChildren(new Option('Đang tải đợt đăng ký...', ''));
}

function populateLookups() {
    const semesterFilter = document.getElementById('semesterFilter');
    const semesterSelect = document.getElementById('semesterId');
    const currentSemester = semesterFilter.value;
    const currentSelection = semesterSelect.value;
    semesterFilter.replaceChildren(new Option('Tất cả học kỳ', ''));
    semesterSelect.replaceChildren(new Option('Chọn học kỳ', ''));
    semesters.forEach((semester) => {
        const label = `${semester.code} · ${semester.name}`;
        semesterFilter.add(new Option(label, semester.id));
        semesterSelect.add(new Option(label, semester.id));
    });
    semesterFilter.value = currentSemester;
    semesterSelect.value = currentSelection;

    const courseSelect = document.getElementById('courseId');
    const selectedCourse = courseSelect.value;
    courseSelect.replaceChildren(new Option('Chọn môn học', ''));
    courses.forEach((course) => {
        courseSelect.add(new Option(`${course.code} · ${course.name} (${course.credits} tín chỉ)`, course.id));
    });
    courseSelect.value = selectedCourse;

    ['originalCourseId', 'equivalentCourseId'].forEach((id) => {
        const select = document.getElementById(id);
        const selectedId = select.value;
        select.replaceChildren(new Option(id === 'originalCourseId' ? 'Chọn môn gốc' : 'Chọn môn thay thế', ''));
        courses.forEach((course) => select.add(new Option(`${course.code} · ${course.name}`, course.id)));
        select.value = selectedId;
    });

    const periodSemester = document.getElementById('periodSemesterId');
    const selectedPeriodSemester = periodSemester.value;
    periodSemester.replaceChildren(new Option('Chọn học kỳ', ''));
    semesters.forEach((semester) => periodSemester.add(new Option(`${semester.code} · ${semester.name}`, semester.id)));
    periodSemester.value = selectedPeriodSemester;
}

function populatePeopleSelectors() {
    const populate = (id, items, placeholder) => {
        const select = document.getElementById(id);
        const selectedId = select.value;
        select.replaceChildren(new Option(placeholder, ''));
        items.forEach((person) => {
            const label = person.code ? `${person.code} · ${person.fullName}` : person.fullName;
            select.add(new Option(label, person.id));
        });
        select.value = selectedId;
    };
    populate('eligibleStudentId', students, 'Chọn sinh viên');
    populate('managerStudentId', students, 'Chọn sinh viên');
    populate('managerEmployeeId', managers, 'Chọn cán bộ');
}

function populatePeriodSelectors() {
    const reportSelect = document.querySelector('.report-period-select');
    const selectedReportPeriod = reportSelect.value;
    reportSelect.replaceChildren(new Option('Chọn đợt đăng ký', ''));
    registrationPeriods.forEach((period) => reportSelect.add(new Option(period.name, period.id)));
    reportSelect.value = registrationPeriods.some((period) => period.id === selectedReportPeriod)
        ? selectedReportPeriod
        : registrationPeriods[0]?.id || '';

    let eligibleSelect = document.getElementById('eligiblePeriodSelect');
    if (!eligibleSelect) {
        const oldButton = document.querySelector('#screen-eligible-students [data-demo-action="period-select"]');
        eligibleSelect = document.createElement('select');
        eligibleSelect.id = 'eligiblePeriodSelect';
        eligibleSelect.className = 'form-select form-select-sm ms-auto';
        eligibleSelect.setAttribute('aria-label', 'Chọn đợt đăng ký');
        if (oldButton) oldButton.replaceWith(eligibleSelect);
        eligibleSelect.addEventListener('change', () => loadEligibleStudents().catch(showApiFailure));
    }
    const selectedEligiblePeriod = eligibleSelect.value;
    eligibleSelect.replaceChildren(new Option('Chọn đợt đăng ký', ''));
    registrationPeriods.forEach((period) => eligibleSelect.add(new Option(period.name, period.id)));
    eligibleSelect.value = registrationPeriods.some((period) => period.id === selectedEligiblePeriod)
        ? selectedEligiblePeriod
        : registrationPeriods[0]?.id || '';

    const periodStats = document.querySelectorAll('#screen-periods .mini-stat strong');
    periodStats[0].textContent = registrationPeriods.length;
    periodStats[1].textContent = registrationPeriods.filter((period) => period.isOpen).length;
    periodStats[2].textContent = registrationPeriods.filter((period) => new Date(period.startTime) > new Date()).length;
}

function renderRegistrationPeriods() {
    const body = document.querySelector('#screen-periods tbody');
    if (!body) return;
    if (registrationPeriods.length === 0) {
        body.innerHTML = '<tr><td colspan="7" class="text-center text-body-secondary py-4">Không có đợt đăng ký.</td></tr>';
        return;
    }
    body.innerHTML = registrationPeriods.map((period) => `<tr>
        <td><strong>${escapeHtml(period.name)}</strong><small>${period.allowRetake ? 'Học lại / cải thiện' : 'Học mới'}</small></td>
        <td>${escapeHtml(period.semesterName || semesterName(period.semesterId))}</td>
        <td>${formatDateTime(period.startTime)} – ${formatDateTime(period.endTime)}</td>
        <td>${period.minCredits} – ${period.maxCredits} tín chỉ</td>
        <td><span class="soft-tag">${period.allowRetake ? 'Cho phép học lại' : 'Không cho học lại'}</span></td>
        <td>${period.isOpen ? statusBadge(1) : statusBadge(3)}</td>
        <td class="text-end"><div class="btn-group btn-group-sm">
            <button class="btn btn-outline-secondary" data-api-action="period-edit" data-id="${escapeHtml(period.id)}" aria-label="Sửa đợt"><i class="bi bi-pencil"></i></button>
            <button class="btn btn-light" data-api-action="period-toggle" data-id="${escapeHtml(period.id)}">${period.isOpen ? 'Đóng cổng' : 'Mở cổng'}</button>
            <button class="btn btn-outline-danger" data-api-action="period-delete" data-id="${escapeHtml(period.id)}" aria-label="Xóa đợt"><i class="bi bi-trash"></i></button>
        </div></td>
    </tr>`).join('');
}

function renderEquivalentCourses() {
    const body = document.querySelector('#screen-equivalents tbody');
    if (!body) return;
    body.innerHTML = equivalentCourses.map((relation) => `<tr>
        <td><strong>${escapeHtml(relation.originalCourseCode)} · ${escapeHtml(relation.originalCourseName)}</strong></td>
        <td><i class="bi bi-arrow-right text-primary"></i></td>
        <td><strong>${escapeHtml(relation.equivalentCourseCode)} · ${escapeHtml(relation.equivalentCourseName)}</strong></td>
        <td><span class="soft-tag ${relation.equivalenceType === 2 ? 'soft-tag-purple' : ''}">${relation.equivalenceType === 2 ? 'Thay thế hoàn toàn' : 'Tương đương 1–1'}</span></td>
        <td>${escapeHtml(relation.effectDate || '—')}</td>
        <td><div class="btn-group btn-group-sm"><button class="btn btn-outline-secondary" data-api-action="equivalent-edit" data-id="${escapeHtml(relation.id)}" aria-label="Sửa quy tắc"><i class="bi bi-pencil"></i></button><button class="btn btn-outline-danger" data-api-action="equivalent-delete" data-id="${escapeHtml(relation.id)}" aria-label="Gỡ quy tắc"><i class="bi bi-trash"></i></button></div></td>
    </tr>`).join('') || '<tr><td colspan="6" class="text-center text-body-secondary py-4">Không có quy tắc tương đương.</td></tr>';
}

async function loadEligibleStudents() {
    const periodId = document.getElementById('eligiblePeriodSelect')?.value;
    const body = document.querySelector('#screen-eligible-students tbody');
    if (!periodId) {
        eligibleStudents = [];
        if (body) body.innerHTML = '<tr><td colspan="5" class="text-center text-body-secondary py-4">Chọn đợt đăng ký để tải danh sách.</td></tr>';
        document.getElementById('eligibleCount').textContent = 'Chưa chọn đợt đăng ký';
        return;
    }
    eligibleStudents = [];
    if (body) body.innerHTML = '<tr><td colspan="7" class="text-center text-body-secondary py-4">Đang tải danh sách...</td></tr>';
    try {
        eligibleStudents = await apiRequest(`${API_ENDPOINTS.REGISTRATION_PERIODS}/${encodeURIComponent(periodId)}/students`);
    } catch (error) {
        if (body) body.innerHTML = `<tr><td colspan="5" class="text-center text-danger py-4">${escapeHtml(error.message)}</td></tr>`;
        throw error;
    }
    const selected = registrationPeriods.find((period) => period.id === periodId);
    const context = document.querySelector('.eligibility-context');
    context.querySelector('strong').textContent = selected?.name || '';
    context.querySelectorAll('strong')[1].textContent = `${eligibleStudents.length} sinh viên`;
    document.getElementById('eligibleCount').textContent = `${eligibleStudents.length} sinh viên`;
    document.getElementById('overviewEligible').textContent = eligibleStudents.length;
    renderEligibleStudents();
}

function renderEligibleStudents() {
    const body = document.querySelector('#screen-eligible-students tbody');
    if (!body) return;
    const keyword = document.querySelector('#screen-eligible-students .list-search input').value.trim().toLocaleLowerCase('vi');
    const items = eligibleStudents.filter((student) =>
        `${student.studentCode} ${student.studentName}`.toLocaleLowerCase('vi').includes(keyword));
    body.innerHTML = items.map((student) => `<tr>
        <td><strong>${escapeHtml(student.studentCode)}</strong></td>
        <td>${escapeHtml(student.studentName)}</td>
        <td>${student.maxAllowedCredits == null ? '<span class="text-body-secondary">Theo giới hạn đợt</span>' : `${student.maxAllowedCredits} tín chỉ`}</td>
        <td><span class="status-badge status-open"><span></span>Đang tham gia</span></td>
        <td><button class="btn btn-sm btn-outline-danger" data-api-action="student-revoke" data-id="${escapeHtml(student.studentId)}">Thu hồi</button></td>
    </tr>`).join('') || '<tr><td colspan="5" class="text-center text-body-secondary py-4">Không có sinh viên phù hợp.</td></tr>';
}

function renderManagerStudents() {
    const body = document.querySelector('#screen-advisors tbody');
    if (!body) return;
    const keyword = document.querySelector('#screen-advisors .list-search input').value.trim().toLocaleLowerCase('vi');
    const items = managerStudents.filter((item) =>
        `${item.studentCode} ${item.studentName} ${item.managerCode} ${item.managerName}`.toLocaleLowerCase('vi').includes(keyword));
    body.innerHTML = items.map((item) => `<tr>
        <td><strong>${escapeHtml(item.studentCode)} · ${escapeHtml(item.studentName)}</strong></td>
        <td><strong>${escapeHtml(item.managerName)}</strong><small>${escapeHtml(item.managerCode)}</small></td>
        <td><span class="soft-tag ${item.roleType === 2 ? 'soft-tag-purple' : ''}">${item.roleType === 2 ? 'Cán bộ hỗ trợ' : 'Cố vấn học tập'}</span></td>
        <td>${item.isPrimary ? 'Có' : 'Không'}</td>
        <td><button class="btn btn-sm btn-outline-danger" data-api-action="manager-delete" data-id="${escapeHtml(item.id)}">Gỡ</button></td>
    </tr>`).join('') || '<tr><td colspan="5" class="text-center text-body-secondary py-4">Không có phân công phù hợp.</td></tr>';
}

async function loadReportSummary() {
    const periodId = document.querySelector('.report-period-select').value;
    if (!periodId) return;
    const summary = await apiRequest(`${API_ENDPOINTS.REPORTS}/period-summary/${encodeURIComponent(periodId)}`);
    const values = document.querySelectorAll('#screen-reports .report-kpi strong');
    values[0].textContent = summary.availableSeats;
    values[1].textContent = summary.registrationCount;
    values[2].textContent = summary.paidCount;
    values[3].textContent = summary.activeSections;
    const captions = document.querySelectorAll('#screen-reports .report-kpi small');
    captions[0].textContent = `Chỗ trống trong ${summary.activeSections} lớp đang hoạt động`;
    captions[1].textContent = `Trên ${summary.eligibleStudents} sinh viên đủ điều kiện`;
    captions[2].textContent = `Trên ${summary.registrationCount} lượt ghi danh`;
    captions[3].textContent = `${summary.registeredStudents} sinh viên đã ghi danh`;
}

async function loadRetakeStatistics() {
    const periodId = document.querySelector('.report-period-select').value;
    if (!periodId) return;
    const statistics = await apiRequest(`${API_ENDPOINTS.REPORTS}/retake-statistics?periodId=${encodeURIComponent(periodId)}`);
    const container = document.querySelector('#screen-reports .col-xl-5 .card-body');
    if (!container) return;
    container.innerHTML = statistics.map((item) => `<div class="report-course">
        <div><strong>${escapeHtml(item.departmentName || 'Chưa phân khoa')}</strong>
        <small>${item.registrationCount} lượt · ${item.retakeCount} học lại · ${item.improvementCount} cải thiện</small></div>
        <span>${item.registrationCount}</span>
        <div class="progress w-100"><div class="progress-bar bg-primary" style="width:100%"></div></div>
    </div>`).join('') || '<p class="text-body-secondary mb-0">Chưa có số liệu cho đợt này.</p>';
}

async function apiRequest(url, options = {}) {
    const headers = new Headers(options.headers);
    if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json');
    }
    const response = await fetch(url, {
        ...options,
        headers
    });
    if (response.status === 204) return null;
    const responseText = await response.text();
    let payload = null;
    if (responseText) {
        try {
            payload = JSON.parse(responseText);
        } catch (error) {
            if (!(error instanceof SyntaxError)) throw error;
            payload = responseText;
        }
    }
    if (!response.ok) {
        const message = typeof payload === 'string'
            ? payload
            : payload?.detail || payload?.message || payload?.error || `API trả về HTTP ${response.status}.`;
        throw new Error(message);
    }
    return payload;
}

function showApiFailure(error) {
    console.error('API request failed:', error);
    notify(`Không tải được dữ liệu từ API: ${error.message}`, 'danger');
}

async function loadSections() {
    const query = new URLSearchParams({
        page: String(currentPage),
        size: String(PAGE_SIZE)
    });
    const semesterId = document.getElementById('semesterFilter').value;
    const status = document.getElementById('statusFilter').value;
    const keyword = document.getElementById('keyword').value.trim();
    if (semesterId) query.set('semesterId', semesterId);
    if (status) query.set('status', status);
    if (keyword) query.set('keyword', keyword);

    try {
        const result = await apiRequest(`${API_ENDPOINTS.ECOURSE_SECTIONS}?${query}`);
        sections = result.items;
        totalSections = result.total;
        render(result);
        updateOverview();
    } catch (error) {
        sections = [];
        totalSections = 0;
        render({ items: [], total: 0, page: currentPage, size: PAGE_SIZE });
        throw error;
    }
}

function bindEvents() {
    document.getElementById('keyword').addEventListener('input', () => {
        window.clearTimeout(searchTimer);
        searchTimer = window.setTimeout(() => resetPageAndRender(), 300);
    });
    document.getElementById('semesterFilter').addEventListener('change', resetPageAndRender);
    document.getElementById('statusFilter').addEventListener('change', resetPageAndRender);
    document.getElementById('clearSearchButton').addEventListener('click', () => {
        document.getElementById('keyword').value = '';
        resetPageAndRender();
    });
    document.getElementById('previousPage').addEventListener('click', () => {
        currentPage = Math.max(0, currentPage - 1);
        loadSections().catch(showApiFailure);
    });
    document.getElementById('nextPage').addEventListener('click', () => {
        currentPage += 1;
        loadSections().catch(showApiFailure);
    });
    document.getElementById('addSectionButton').addEventListener('click', () => openSectionForm());
    document.getElementById('periodForm').addEventListener('submit', (event) => savePeriod(event).catch(showApiFailure));
    document.getElementById('equivalentForm').addEventListener('submit', (event) => saveEquivalentCourse(event).catch(showApiFailure));
    document.getElementById('studentAddForm').addEventListener('submit', (event) => saveEligibleStudent(event).catch(showApiFailure));
    document.getElementById('managerAssignForm').addEventListener('submit', (event) => saveManagerAssignment(event).catch(showApiFailure));
    document.getElementById('courseForm').addEventListener('submit', (event) => saveSection(event).catch(showApiFailure));
    document.getElementById('managerRoleType').addEventListener('change', syncPrimaryAdvisorControl);
    document.getElementById('courseTableBody').addEventListener('click', (event) => {
        handleSectionAction(event).catch(showApiFailure);
    });
    document.getElementById('exportButton').addEventListener('click', () => exportRegistrations().catch(showApiFailure));
    document.getElementById('reportExportButton').addEventListener('click', () => exportRegistrations().catch(showApiFailure));
    document.addEventListener('click', (event) => {
        const apiButton = event.target.closest('[data-api-action]');
        if (apiButton) {
            handleApiAction(apiButton).catch(showApiFailure);
            return;
        }
        const button = event.target.closest('[data-demo-action]');
        if (button) handleDemoAction(button.dataset.demoAction);
    });
    document.querySelector('.report-period-select').addEventListener('change', () => {
        Promise.all([loadReportSummary(), loadRetakeStatistics()]).catch(showApiFailure);
    });
    document.querySelector('#screen-eligible-students .list-search input').addEventListener('input', renderEligibleStudents);
    document.querySelector('#screen-advisors .list-search input').addEventListener('input', renderManagerStudents);
    eligibleImportInput = document.createElement('input');
    eligibleImportInput.type = 'file';
    eligibleImportInput.accept = '.xlsx,.xls';
    eligibleImportInput.hidden = true;
    eligibleImportInput.addEventListener('change', () => importEligibleStudents(eligibleImportInput).catch(showApiFailure));
    document.body.append(eligibleImportInput);
    const courseSearch = document.querySelector('#screen-courses .list-search input');
    courseSearch?.addEventListener('input', () => renderCourseCatalog(courseSearch.value));
}

async function handleApiAction(button) {
    const { apiAction, id } = button.dataset;
    if (apiAction === 'period-toggle') {
        const period = registrationPeriods.find((item) => item.id === id);
        if (!period) return;
        await apiRequest(`${API_ENDPOINTS.REGISTRATION_PERIODS}/${encodeURIComponent(id)}/toggle-open`, {
            method: 'PATCH',
            body: JSON.stringify({ isOpen: !period.isOpen })
        });
        await loadRegistrationPeriods();
        notify(`Đã ${period.isOpen ? 'đóng' : 'mở'} đợt đăng ký.`);
    } else if (apiAction === 'period-delete') {
        if (!window.confirm('Xóa mềm đợt đăng ký này?')) return;
        await apiRequest(`${API_ENDPOINTS.REGISTRATION_PERIODS}/${encodeURIComponent(id)}`, { method: 'DELETE' });
        await loadRegistrationPeriods();
        const dependentResults = await Promise.allSettled([loadEligibleStudents(), loadReportSummary(), loadRetakeStatistics()]);
        const failed = dependentResults.find((result) => result.status === 'rejected');
        if (failed) throw failed.reason;
        notify('Đã xóa mềm đợt đăng ký.');
    } else if (apiAction === 'period-edit') {
        const period = registrationPeriods.find((item) => item.id === id);
        if (period) openPeriodForm(period);
    } else if (apiAction === 'equivalent-delete') {
        if (!window.confirm('Gỡ quy tắc tương đương này?')) return;
        await apiRequest(`${API_ENDPOINTS.EQUIVALENT_COURSES}/${encodeURIComponent(id)}`, { method: 'DELETE' });
        await loadEquivalentCourses();
        notify('Đã gỡ quy tắc tương đương.');
    } else if (apiAction === 'equivalent-edit') {
        const relation = equivalentCourses.find((item) => item.id === id);
        if (relation) openEquivalentForm(relation);
    } else if (apiAction === 'student-revoke') {
        const periodId = document.getElementById('eligiblePeriodSelect').value;
        if (!window.confirm('Thu hồi quyền tham gia đợt của sinh viên này?')) return;
        await apiRequest(`${API_ENDPOINTS.REGISTRATION_PERIODS}/${encodeURIComponent(periodId)}/students/${encodeURIComponent(id)}`, {
            method: 'DELETE'
        });
        await loadEligibleStudents();
        notify('Đã thu hồi quyền tham gia.');
    } else if (apiAction === 'manager-delete') {
        if (!window.confirm('Gỡ phân công này?')) return;
        await apiRequest(`${API_ENDPOINTS.MANAGER_STUDENTS}/${encodeURIComponent(id)}`, { method: 'DELETE' });
        await loadManagerStudents();
        notify('Đã gỡ phân công.');
    }
}

async function saveEligibleStudent(event) {
    event.preventDefault();
    const form = event.currentTarget;
    if (!form.reportValidity()) return;
    const periodId = document.getElementById('eligiblePeriodSelect').value;
    const credits = document.getElementById('eligibleMaxCredits').value;
    await apiRequest(`${API_ENDPOINTS.REGISTRATION_PERIODS}/${encodeURIComponent(periodId)}/students`, {
        method: 'POST',
        body: JSON.stringify({
            studentId: document.getElementById('eligibleStudentId').value,
            maxAllowedCredits: credits === '' ? null : Number(credits)
        })
    });
    studentAddModal.hide();
    await loadEligibleStudents();
    notify('Đã thêm sinh viên vào danh sách tham gia.');
}

async function saveManagerAssignment(event) {
    event.preventDefault();
    const form = event.currentTarget;
    if (!form.reportValidity()) return;
    const roleType = Number(document.getElementById('managerRoleType').value);
    await apiRequest(API_ENDPOINTS.MANAGER_STUDENTS, {
        method: 'POST',
        body: JSON.stringify({
            studentId: document.getElementById('managerStudentId').value,
            managerId: document.getElementById('managerEmployeeId').value,
            roleType,
            isPrimary: roleType === 1 && document.getElementById('managerIsPrimary').checked
        })
    });
    managerAssignModal.hide();
    await loadManagerStudents();
    notify('Đã lưu phân công.');
}

function syncPrimaryAdvisorControl() {
    const isAdvisor = document.getElementById('managerRoleType').value === '1';
    const primaryCheckbox = document.getElementById('managerIsPrimary');
    primaryCheckbox.disabled = !isAdvisor;
    if (!isAdvisor) primaryCheckbox.checked = false;
}

function bindModuleNavigation() {
    document.querySelectorAll('[data-screen]').forEach((link) => {
        link.addEventListener('click', (event) => {
            event.preventDefault();
            navigateToScreen(link.dataset.screen);
        });
    });
    document.querySelectorAll('[data-go-screen]').forEach((button) => {
        button.addEventListener('click', () => navigateToScreen(button.dataset.goScreen));
    });
    window.addEventListener('hashchange', () => showScreen(window.location.hash.slice(1) || 'sections', false));
}

function navigateToScreen(name) {
    if (window.location.hash !== `#${name}`) {
        window.location.hash = name;
        return;
    }
    showScreen(name, true);
}

function showScreen(name, closeSidebar) {
    const screen = document.getElementById(`screen-${name}`);
    if (!screen) {
        name = 'sections';
    }
    document.querySelectorAll('.module-screen').forEach((element) => {
        const active = element.id === `screen-${name}`;
        element.classList.toggle('active', active);
        element.hidden = !active;
    });
    document.querySelectorAll('.sidebar-menu [data-screen]').forEach((link) => {
        const active = link.dataset.screen === name;
        link.classList.toggle('active', active);
        if (active) link.setAttribute('aria-current', 'page');
        else link.removeAttribute('aria-current');
    });
    if (closeSidebar && window.innerWidth < 992) {
        document.body.classList.remove('sidebar-open');
        document.body.classList.add('sidebar-collapse');
    }
}

function renderCourseCatalog(search = '') {
    const body = document.getElementById('courseCatalogBody');
    if (!body) return;
    const keyword = search.trim().toLocaleLowerCase('vi');
    const filtered = courses.filter((course) =>
        `${course.code} ${course.name}`.toLocaleLowerCase('vi').includes(keyword));
    body.innerHTML = filtered.map((course) => `<tr>
        <td><strong>${escapeHtml(course.code)}</strong></td>
        <td><strong>${escapeHtml(course.name)}</strong></td>
        <td>${course.credits} tín chỉ</td>
    </tr>`).join('') || '<tr><td colspan="3" class="text-center text-body-secondary py-4">Không tìm thấy môn học.</td></tr>';
}

function updateOverview() {
    const fullSections = sections.filter((section) => section.currentEnrolled >= section.maxCapacity).length;
    document.getElementById('overviewTotal').textContent = totalSections;
    document.getElementById('overviewFull').textContent = fullSections;
    document.getElementById('overviewOpenPeriods').textContent =
        registrationPeriods.filter((period) => period.isOpen).length;
    const activePeriod = registrationPeriods.find((period) => period.isOpen);
    document.getElementById('overviewActivePeriod').textContent = activePeriod?.name || 'Không có đợt đang mở';
    document.getElementById('overviewActivePeriodDates').textContent = activePeriod
        ? `${formatDateTime(activePeriod.startTime)} – ${formatDateTime(activePeriod.endTime)}`
        : 'Theo dữ liệu API';
}

function handleDemoAction(action) {
    if (action === 'period-create') {
        openPeriodForm();
        return;
    }
    if (action === 'equivalent-create') {
        openEquivalentForm();
        return;
    }
    if (action === 'student-import') {
        const periodId = document.getElementById('eligiblePeriodSelect')?.value;
        if (!periodId) {
            notify('Hãy chọn đợt đăng ký trước khi import danh sách.', 'warning');
            return;
        }
        eligibleImportInput?.click();
        return;
    }
    if (action === 'student-add') {
        if (!document.getElementById('eligiblePeriodSelect')?.value) {
            notify('Hãy chọn đợt đăng ký trước khi thêm sinh viên.', 'warning');
            return;
        }
        document.getElementById('studentAddForm').reset();
        studentAddModal.show();
        return;
    }
    if (action === 'advisor-assign') {
        document.getElementById('managerAssignForm').reset();
        syncPrimaryAdvisorControl();
        managerAssignModal.show();
        return;
    }
    if (action === 'period-select') {
        document.getElementById('eligiblePeriodSelect')?.focus();
        return;
    }
    if (action === 'report-retake') {
        loadRetakeStatistics().catch(showApiFailure);
        return;
    }
    if (action === 'report-summary') {
        loadReportSummary().catch(showApiFailure);
        return;
    }
    const messages = {
        'period-create': 'Tạo đợt đăng ký chưa được nối vào biểu mẫu API.',
        'period-edit': 'Chỉnh sửa đợt đăng ký chưa được nối vào biểu mẫu API.',
        'course-create': 'Danh mục môn học chỉ đọc; API tạo môn không thuộc module đăng ký học lại.',
        'course-detail': 'Chi tiết môn học chưa có API riêng trong module này.',
        'equivalent-create': 'Tạo quy tắc tương đương chưa được nối vào biểu mẫu API.',
        'equivalent-edit': 'Chỉnh sửa quy tắc tương đương chưa được nối vào biểu mẫu API.',
        'student-import': 'Import Excel cần chọn một đợt đăng ký trước.',
        'student-detail': 'Chi tiết sinh viên chưa có API riêng trong module này.',
        'period-select': 'Chọn một đợt đăng ký trong màn hình danh sách đợt.',
        'advisor-edit': 'Chỉnh sửa phân công chưa được nối vào biểu mẫu API.',
        'report-retake': 'Báo cáo học lại chưa tải dữ liệu.',
        'report-summary': 'Chọn đợt đăng ký có dữ liệu để tải báo cáo tổng hợp.'
    };
    notify(messages[action] || 'Chức năng này chưa được nối với API.', 'info');
}

function openPeriodForm(period = null) {
    const form = document.getElementById('periodForm');
    form.reset();
    document.getElementById('periodId').value = period?.id || '';
    document.getElementById('periodModalTitle').textContent = period ? 'Chỉnh sửa đợt đăng ký' : 'Tạo đợt đăng ký';
    document.getElementById('periodName').value = period?.name || '';
    document.getElementById('periodSemesterId').value = period?.semesterId || '';
    document.getElementById('periodStartTime').value = period ? period.startTime.slice(0, 16) : '';
    document.getElementById('periodEndTime').value = period ? period.endTime.slice(0, 16) : '';
    document.getElementById('periodMinCredits').value = period?.minCredits ?? 12;
    document.getElementById('periodMaxCredits').value = period?.maxCredits ?? 25;
    document.getElementById('periodAllowRetake').checked = period?.allowRetake ?? true;
    document.getElementById('periodIsOpen').checked = period?.isOpen ?? true;
    periodModal.show();
}

async function savePeriod(event) {
    event.preventDefault();
    const form = event.currentTarget;
    if (!form.reportValidity()) return;
    const id = document.getElementById('periodId').value;
    const payload = {
        name: document.getElementById('periodName').value.trim(),
        semesterId: document.getElementById('periodSemesterId').value,
        startTime: `${document.getElementById('periodStartTime').value}:00`,
        endTime: `${document.getElementById('periodEndTime').value}:00`,
        minCredits: Number(document.getElementById('periodMinCredits').value),
        maxCredits: Number(document.getElementById('periodMaxCredits').value),
        allowRetake: document.getElementById('periodAllowRetake').checked,
        isOpen: document.getElementById('periodIsOpen').checked
    };
    await apiRequest(id
        ? `${API_ENDPOINTS.REGISTRATION_PERIODS}/${encodeURIComponent(id)}`
        : API_ENDPOINTS.REGISTRATION_PERIODS, {
        method: id ? 'PUT' : 'POST',
        body: JSON.stringify(payload)
    });
    periodModal.hide();
    await loadRegistrationPeriods();
    const dependentResults = await Promise.allSettled([loadEligibleStudents(), loadReportSummary(), loadRetakeStatistics()]);
    const failed = dependentResults.find((result) => result.status === 'rejected');
    if (failed) throw failed.reason;
    notify(id ? 'Đã cập nhật đợt đăng ký.' : 'Đã tạo đợt đăng ký.');
}

function openEquivalentForm(relation = null) {
    const form = document.getElementById('equivalentForm');
    form.reset();
    document.getElementById('equivalentId').value = relation?.id || '';
    document.getElementById('equivalentModalTitle').textContent = relation ? 'Chỉnh sửa quy tắc tương đương' : 'Thêm quy tắc tương đương';
    document.getElementById('originalCourseId').value = relation?.originalCourseId || '';
    document.getElementById('equivalentCourseId').value = relation?.equivalentCourseId || '';
    document.getElementById('equivalenceType').value = String(relation?.equivalenceType || 1);
    document.getElementById('equivalenceEffectDate').value = relation?.effectDate || '';
    document.getElementById('equivalenceNote').value = relation?.note || '';
    equivalentModal.show();
}

async function saveEquivalentCourse(event) {
    event.preventDefault();
    const form = event.currentTarget;
    if (!form.reportValidity()) return;
    const id = document.getElementById('equivalentId').value;
    const originalCourseId = document.getElementById('originalCourseId').value;
    const equivalentCourseId = document.getElementById('equivalentCourseId').value;
    if (originalCourseId === equivalentCourseId) {
        notify('Môn gốc và môn thay thế phải là hai môn khác nhau.', 'warning');
        document.getElementById('equivalentCourseId').focus();
        return;
    }
    const effectDate = document.getElementById('equivalenceEffectDate').value;
    const payload = {
        originalCourseId,
        equivalentCourseId,
        equivalenceType: Number(document.getElementById('equivalenceType').value),
        effectDate: effectDate || null,
        note: document.getElementById('equivalenceNote').value.trim() || null
    };
    await apiRequest(id
        ? `${API_ENDPOINTS.EQUIVALENT_COURSES}/${encodeURIComponent(id)}`
        : API_ENDPOINTS.EQUIVALENT_COURSES, {
        method: id ? 'PUT' : 'POST',
        body: JSON.stringify(payload)
    });
    equivalentModal.hide();
    await loadEquivalentCourses();
    notify(id ? 'Đã cập nhật quy tắc tương đương.' : 'Đã tạo quy tắc tương đương.');
}

async function importEligibleStudents(input) {
    const file = input.files[0];
    const periodId = document.getElementById('eligiblePeriodSelect').value;
    if (!file || !periodId) return;
    const formData = new FormData();
    formData.append('file', file);
    const result = await apiRequest(`${API_ENDPOINTS.REGISTRATION_PERIODS}/${encodeURIComponent(periodId)}/students/import`, {
        method: 'POST',
        body: formData
    });
    await loadEligibleStudents();
    input.value = '';
    const firstError = result.errors?.[0];
    notify(`Đã thêm ${result.added}, bỏ qua ${result.skipped}.${firstError ? ` Lỗi đầu tiên: ${firstError}` : ''}`,
        result.errors?.length ? 'warning' : 'success');
}

function resetPageAndRender() {
    currentPage = 0;
    loadSections().catch(showApiFailure);
}

function render(result = { items: sections, total: totalSections, page: currentPage, size: PAGE_SIZE }) {
    renderSummary(result.items);
    const pageCount = Math.max(1, Math.ceil(result.total / result.size));
    currentPage = Math.min(currentPage, pageCount - 1);
    const first = result.total === 0 ? 0 : currentPage * result.size + 1;
    const last = Math.min(currentPage * result.size + result.items.length, result.total);
    renderTable(result.items, first - 1);
    document.getElementById('paginationSummary').textContent =
        `Hiển thị ${first}–${last} trong ${result.total} lớp học phần`;
    document.getElementById('previousPage').disabled = currentPage === 0;
    document.getElementById('nextPage').disabled = currentPage >= pageCount - 1;
}

function renderSummary(visible) {
    document.getElementById('totalSections').textContent = totalSections;
    document.getElementById('openSections').textContent = visible.filter((section) => section.status === 1).length;
    document.getElementById('fullSections').textContent = visible.filter(
        (section) => section.currentEnrolled >= section.maxCapacity
    ).length;
    document.getElementById('closedSections').textContent = visible.filter(
        (section) => section.status === 2 || section.status === 3
    ).length;
    updateOverview();
}

function renderTable(pageSections, offset) {
    const body = document.getElementById('courseTableBody');
    if (pageSections.length === 0) {
        body.innerHTML = `<tr><td colspan="7" class="empty-state">
            <i class="bi bi-search"></i><strong>${totalSections === 0 ? 'Chưa có dữ liệu lớp học phần' : 'Không tìm thấy lớp học phần'}</strong>
            <span>${totalSections === 0 ? 'Kiểm tra kết nối cơ sở dữ liệu hoặc điều chỉnh bộ lọc.' : 'Thử đổi từ khóa hoặc bộ lọc của bạn.'}</span></td></tr>`;
        return;
    }

    body.innerHTML = pageSections.map((section, index) => {
        const percentage = Math.min(100, Math.round(section.currentEnrolled / section.maxCapacity * 100));
        const isFull = section.currentEnrolled >= section.maxCapacity;
        const cancelled = section.status === 2;
        const barColor = isFull || cancelled ? 'bg-danger' : 'bg-success';
        const barDescription = cancelled ? 'Lớp đã hủy' : `${percentage}%`;
        return `<tr>
            <td class="text-body-secondary">${offset + index + 1}</td>
            <td><span class="section-code">${escapeHtml(section.code)}</span></td>
            <td><div class="course-name">${escapeHtml(section.courseName)}</div>
                <small class="text-body-secondary">${escapeHtml(section.courseCode)} · ${escapeHtml(semesterName(section.semesterId))}</small></td>
            <td class="text-center"><span class="credit-pill">${section.credits}</span></td>
            <td><div class="capacity-label"><span>${section.currentEnrolled} / ${section.maxCapacity}</span><span>${barDescription}</span></div>
                <div class="progress capacity-progress" role="progressbar" aria-label="Tỷ lệ ghi danh ${escapeHtml(section.code)}"
                    aria-valuenow="${percentage}" aria-valuemin="0" aria-valuemax="100">
                    <div class="progress-bar ${barColor}" style="width: ${percentage}%"></div>
                </div></td>
            <td>${statusBadge(section.status)}</td>
            <td class="text-end"><div class="table-actions">
                <button type="button" class="btn btn-outline-primary" data-action="view" data-id="${escapeHtml(section.id)}"
                    title="Xem danh sách sinh viên" aria-label="Xem danh sách sinh viên">
                    <i class="bi bi-eye"></i></button>
                <button type="button" class="btn btn-outline-secondary" data-action="edit" data-id="${escapeHtml(section.id)}"
                    title="Chỉnh sửa lớp" aria-label="Chỉnh sửa lớp"><i class="bi bi-pencil"></i></button>
                <div class="dropdown">
                    <button type="button" class="btn btn-outline-secondary" data-bs-toggle="dropdown" aria-expanded="false"
                        title="Thao tác khác" aria-label="Thao tác khác"><i class="bi bi-three-dots"></i></button>
                    <ul class="dropdown-menu dropdown-menu-end">
                        <li><button class="dropdown-item" type="button" data-action="status" data-id="${escapeHtml(section.id)}" data-status="1"
                            ${section.status === 1 || cancelled ? 'disabled' : ''}><i class="bi bi-unlock me-2"></i>Mở lớp</button></li>
                        <li><button class="dropdown-item" type="button" data-action="status" data-id="${escapeHtml(section.id)}" data-status="3"
                            ${section.status === 3 || cancelled ? 'disabled' : ''}><i class="bi bi-lock me-2"></i>Khóa lớp</button></li>
                        <li><hr class="dropdown-divider"></li>
                        <li><button class="dropdown-item text-danger" type="button" data-action="status" data-id="${escapeHtml(section.id)}" data-status="2"
                            ${cancelled ? 'disabled' : ''}><i class="bi bi-x-circle me-2"></i>Hủy lớp</button></li>
                    </ul>
                </div>
            </div></td>
        </tr>`;
    }).join('');
}

function statusBadge(status) {
    const details = {
        1: ['Đang mở', 'status-open'],
        2: ['Đã hủy', 'status-cancelled'],
        3: ['Đã khóa', 'status-locked']
    };
    const [label, className] = details[status] || ['Không xác định', 'status-locked'];
    return `<span class="status-badge ${className}"><span></span>${label}</span>`;
}

async function handleSectionAction(event) {
    const button = event.target.closest('button[data-action]');
    if (!button) return;
    const section = sections.find((item) => item.id === button.dataset.id);
    if (!section) return;
    if (button.dataset.action === 'view') await showRegistrations(section);
    if (button.dataset.action === 'edit') openSectionForm(section);
    if (button.dataset.action === 'status') await changeStatus(section, Number(button.dataset.status));
}

function openSectionForm(section = null) {
    const form = document.getElementById('courseForm');
    form.reset();
    document.getElementById('sectionId').value = section?.id || '';
    document.getElementById('courseId').disabled = false;
    document.getElementById('semesterId').disabled = false;
    document.getElementById('sectionCode').disabled = false;
    document.getElementById('sectionModalTitle').textContent = section ? 'Chỉnh sửa lớp học phần' : 'Mở lớp học phần mới';
    document.getElementById('submitButton').innerHTML = section
        ? '<i class="bi bi-check2 me-1"></i>Lưu thay đổi'
        : '<i class="bi bi-check2 me-1"></i>Lưu lớp học phần';
    if (section) {
        document.getElementById('courseId').value = section.courseId;
        document.getElementById('semesterId').value = section.semesterId;
        document.getElementById('sectionCode').value = section.code;
        document.getElementById('maxCapacity').value = section.maxCapacity;
    } else {
        document.getElementById('semesterId').value = document.getElementById('semesterFilter').value;
        document.getElementById('maxCapacity').value = 35;
    }
    sectionModal.show();
}

async function saveSection(event) {
    event.preventDefault();
    const form = event.currentTarget;
    if (!form.reportValidity()) return;
    const id = document.getElementById('sectionId').value;
    const course = courses.find((item) => item.id === document.getElementById('courseId').value);
    const semesterId = document.getElementById('semesterId').value;
    const code = document.getElementById('sectionCode').value.trim();
    const maxCapacity = Number(document.getElementById('maxCapacity').value);
    const current = sections.find((item) => item.id === id);
    if (!course) {
        notify('Vui lòng chọn môn học.', 'warning');
        return;
    }
    if (!Number.isInteger(maxCapacity) || maxCapacity < 1) {
        notify('Sĩ số tối đa phải là số nguyên lớn hơn 0.', 'warning');
        return;
    }
    if (current && maxCapacity < current.currentEnrolled) {
        notify(`Sĩ số tối đa không thể thấp hơn ${current.currentEnrolled} sinh viên đang ghi danh.`, 'warning');
        return;
    }

    const payload = {
        courseId: course.id,
        semesterId,
        code,
        maxCapacity,
        status: current?.status || 1
    };
    const endpoint = current
        ? `${API_ENDPOINTS.ECOURSE_SECTIONS}/${encodeURIComponent(id)}`
        : API_ENDPOINTS.ECOURSE_SECTIONS;
    await apiRequest(endpoint, {
        method: current ? 'PUT' : 'POST',
        body: JSON.stringify(payload)
    });
    sectionModal.hide();
    currentPage = 0;
    await loadSections();
    notify(current ? 'Đã cập nhật lớp học phần.' : 'Đã tạo lớp học phần.');
}

async function changeStatus(section, status) {
    const labels = { 1: 'mở', 2: 'hủy', 3: 'khóa' };
    if (status === 2 && !window.confirm(`Hủy lớp ${section.code}? Các đăng ký đang hiệu lực sẽ được chuyển sang trạng thái hủy.`)) {
        return;
    }
    await apiRequest(`${API_ENDPOINTS.ECOURSE_SECTIONS}/${encodeURIComponent(section.id)}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status })
    });
    await loadSections();
    notify(`Đã chuyển lớp ${section.code} sang trạng thái ${labels[status]}.`);
}

async function showRegistrations(section) {
    const activeRegistrations = await apiRequest(
        `${API_ENDPOINTS.REGISTRATIONS}/section/${encodeURIComponent(section.id)}`
    );
    document.getElementById('registrationsDrawerTitle').textContent = `Sinh viên ghi danh · ${section.code}`;
    document.getElementById('registrationsDrawerSubtitle').textContent =
        `${section.courseName} · ${semesterName(section.semesterId)}`;
    document.getElementById('drawerSummary').innerHTML = `
        <div><span>Sĩ số hiện tại</span><strong>${section.currentEnrolled} / ${section.maxCapacity}</strong></div>
        <div><span>Đã thanh toán</span><strong>${activeRegistrations.filter((item) => item.isPaid).length}</strong></div>
        <div><span>Chưa thanh toán</span><strong>${activeRegistrations.filter((item) => !item.isPaid).length}</strong></div>`;

    const body = document.getElementById('registrationsTableBody');
    if (activeRegistrations.length === 0) {
        body.innerHTML = `<tr><td colspan="4" class="empty-state">
            <i class="bi bi-people"></i><strong>Chưa có sinh viên ghi danh</strong>
            <span>Danh sách sinh viên của lớp sẽ xuất hiện tại đây.</span></td></tr>`;
    } else {
        body.innerHTML = activeRegistrations.map((student) => `<tr>
            <td><strong class="student-code">${escapeHtml(student.studentCode)}</strong><small>${escapeHtml(student.studentName)}</small></td>
            <td>${formatDateTime(student.registeredAt)}</td>
            <td>${registrationType(student.registrationType)}</td>
            <td><span class="payment-badge ${student.isPaid ? 'payment-paid' : 'payment-unpaid'}">
                <i class="bi ${student.isPaid ? 'bi-check-circle-fill' : 'bi-clock-fill'}"></i>
                ${student.isPaid ? 'Đã nộp' : 'Chưa nộp'}</span></td>
        </tr>`).join('');
    }
    registrationsDrawer.show();
}

async function exportRegistrations() {
    const periodId = document.querySelector('.report-period-select')?.value;
    const sectionId = sections.length === 1 ? sections[0].id : null;
    if (!periodId && !sectionId) {
        notify('Chọn một đợt đăng ký trong màn hình Báo cáo trước khi xuất.', 'warning');
        return;
    }
    const query = new URLSearchParams();
    if (periodId) query.set('periodId', periodId);
    if (sectionId) query.set('sectionId', sectionId);
    const response = await fetch(`${API_ENDPOINTS.REPORTS}/registrations/export?${query}`);
    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || `Không thể xuất báo cáo (HTTP ${response.status}).`);
    }
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `danh-sach-ghi-danh-${new Date().toISOString().slice(0, 10)}.xlsx`;
    document.body.append(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
    notify('Đã xuất tệp Excel từ API.');
}

function registrationType(type) {
    return `<span class="registration-type type-${type}">${registrationTypeText(type)}</span>`;
}

function registrationTypeText(type) {
    if (type === 2) return 'Học lại';
    if (type === 3) return 'Cải thiện';
    return 'Học mới';
}

function semesterName(id) {
    const semester = semesters.find((item) => item.id === id);
    return semester ? semester.name : 'Chưa xác định học kỳ';
}

function formatDateTime(value) {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? escapeHtml(value) : new Intl.DateTimeFormat('vi-VN').format(date);
}

function notify(message, type = 'success') {
    let toast = document.getElementById('demoToast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'demoToast';
        toast.className = 'demo-toast alert';
        toast.setAttribute('role', 'status');
        document.body.append(toast);
    }
    toast.className = `demo-toast alert alert-${type}`;
    toast.textContent = message;
    toast.classList.add('show');
    window.clearTimeout(toastTimer);
    toastTimer = window.setTimeout(() => toast.classList.remove('show'), 3600);
}

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
