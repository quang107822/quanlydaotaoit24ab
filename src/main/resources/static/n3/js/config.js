// Cấu hình URL linh hoạt cho backend (ưu tiên cùng origin, dự phòng http://localhost:8081/api)
const BASE_URL = (typeof window !== 'undefined' && window.location && window.location.origin && window.location.origin.startsWith('http'))
    ? `${window.location.origin}/api`
    : 'http://localhost:8081/api';

export const API_ENDPOINTS = {
    STUDENTS: `${BASE_URL}/students`,
    EMPLOYEES: `${BASE_URL}/employees`,
    DEPARTMENTS: `${BASE_URL}/departments`,
    POSITIONS: `${BASE_URL}/positions`,
    EMPLOYEE_POSITIONS: `${BASE_URL}/employee-positions`,
};