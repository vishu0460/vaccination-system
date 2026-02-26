import axios from 'axios';

const API = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

API.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

API.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const authAPI = {
  login: (data) => API.post('/auth/login', data),
  register: (data) => API.post('/auth/register', data),
  getCurrentUser: () => API.get('/auth/me'),
  refreshToken: (data) => API.post('/auth/refresh', data),
  forgotPassword: (data) => API.post('/auth/forgot-password', data),
  resetPassword: (data) => API.post('/auth/reset-password', data),
  validateResetToken: (token) => API.get('/auth/validate-reset-token', { params: { token } }),
};

export const publicAPI = {
  getDrives: () => API.get('/public/drives'),
  getDriveById: (id) => API.get(`/public/drives/${id}`),
  getSlots: (driveId) => API.get(`/public/drives/${driveId}/slots`),
  getCenters: () => API.get('/public/centers'),
  getCenterById: (id) => API.get(`/public/centers/${id}`),
  getCentersByCity: (city) => API.get(`/public/centers/city/${city}`),
};

export const bookingAPI = {
  createBooking: (data) => API.post('/bookings', data),
  getMyBookings: () => API.get('/bookings/my'),
  getAllBookings: () => API.get('/bookings'),
  getPendingBookings: () => API.get('/bookings/pending'),
  approveBooking: (id) => API.put(`/bookings/${id}/approve`),
  rejectBooking: (id, reason) => API.put(`/bookings/${id}/reject?reason=${reason}`),
  cancelBooking: (id) => API.put(`/bookings/${id}/cancel`),
  completeBooking: (id) => API.put(`/bookings/${id}/complete`),
};

export const adminAPI = {
  getDashboard: () => API.get('/admin/dashboard'),
  getAllUsers: () => API.get('/admin/users'),
  getUserById: (id) => API.get(`/admin/users/${id}`),
  updateUser: (id, data) => API.put(`/admin/users/${id}`, data),
  deleteUser: (id) => API.delete(`/admin/users/${id}`),
  createCenter: (data) => API.post('/admin/centers', data),
  updateCenter: (id, data) => API.put(`/admin/centers/${id}`, data),
  deleteCenter: (id) => API.delete(`/admin/centers/${id}`),
  createDrive: (data) => API.post('/admin/drives', data),
  updateDrive: (id, data) => API.put(`/admin/drives/${id}`, data),
  deleteDrive: (id) => API.delete(`/admin/drives/${id}`),
  getAllBookings: () => API.get('/admin/bookings'),
  getPendingBookings: () => API.get('/admin/bookings/pending'),
};

export default API;
