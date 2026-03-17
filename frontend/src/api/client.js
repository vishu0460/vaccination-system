import axios from "axios";
import { getAccessToken, clearAuth, setAuth } from "../utils/auth";

const normalizeApiBaseUrl = (rawValue) => {
  const value = typeof rawValue === "string" ? rawValue.trim() : "";

  if (!value) {
    return "/api";
  }

  const withoutTrailingSlash = value.replace(/\/+$/, "");
  if (withoutTrailingSlash.endsWith("/api")) {
    return withoutTrailingSlash;
  }

  if (withoutTrailingSlash.startsWith("http://") || withoutTrailingSlash.startsWith("https://")) {
    return `${withoutTrailingSlash}/api`;
  }

  return withoutTrailingSlash;
};

const apiClient = axios.create({
  baseURL: normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL),
  timeout: 15000
});

const publicEndpoints = ['/public/**', '/contact', '/feedback', '/news', '/health'];

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken();
  const isPublic = publicEndpoints.some(endpoint => {
    if (endpoint.includes('**')) {
      const basePath = endpoint.replace('/**', '');
      return config.url?.startsWith(basePath);
    }
    return config.url?.startsWith(endpoint);
  });
  if (token && !isPublic) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    if (!original) {
      return Promise.reject(error);
    }

    const isRefreshRequest = original.url?.includes("/auth/refresh");
    if (error.response?.status === 401 && !original._retry && !isRefreshRequest) {
      original._retry = true;
      const refreshToken = localStorage.getItem("refreshToken");
      if (refreshToken) {
        try {
          const { data } = await axios.post(`${apiClient.defaults.baseURL}/auth/refresh`, { refreshToken });
          setAuth(data);
          original.headers.Authorization = `Bearer ${data.accessToken}`;
          return apiClient(original);
        } catch {
          clearAuth();
          if (typeof window !== "undefined" && window.location.pathname !== "/login") {
            window.location.href = "/login";
          }
        }
      }
    }
    return Promise.reject(error);
  }
);

// Auth APIs
export const authAPI = {
  login: (data) => apiClient.post("/auth/login", data),
  register: (data) => apiClient.post("/auth/register", data),
  refresh: (refreshToken) => apiClient.post("/auth/refresh", { refreshToken }),
  forgotPassword: (email) => apiClient.post("/auth/forgot-password", { email }),
  resetPassword: (data) => apiClient.post("/auth/reset-password", data),
  verifyEmail: (token) => apiClient.get(`/auth/verify-email?token=${token}`)
};

// Public APIs
export const publicAPI = {
  getDrives: (params) => apiClient.get("/public/drives", { params }),
  getCenters: (city) => apiClient.get("/public/centers", { params: { city } }),
  getCenterDetail: (id) => apiClient.get(`/public/centers/${id}`),
  getDriveSlots: (driveId) => apiClient.get(`/public/drives/${driveId}/slots`),
  getSummary: () => apiClient.get("/public/summary"),
  getStats: () => apiClient.get("/public/summary")
};

// User APIs
export const userAPI = {
  getProfile: () => apiClient.get("/user/account"),
  updateProfile: (data) => apiClient.put("/user/account", data),
  changePassword: (data) => apiClient.post("/user/account/change-password", data),
  getBookings: () => apiClient.get("/user/bookings"),
  bookSlot: (data) => apiClient.post("/user/bookings", data),
  cancelBooking: (bookingId) => apiClient.patch(`/user/bookings/${bookingId}/cancel`),
  rescheduleBooking: (bookingId, data) => apiClient.patch(`/user/bookings/${bookingId}/reschedule`, data),
  getNotifications: () => apiClient.get("/user/notifications"),
  getSlotRecommendations: (params) => apiClient.get("/user/recommendations/slots", { params })
};

// Admin APIs
export const adminAPI = {
  getDashboardStats: () => apiClient.get("/admin/dashboard/stats"),
  getAllBookings: () => apiClient.get("/admin/bookings"),
  getAllCenters: () => apiClient.get("/admin/centers"),
  getAllDrives: () => apiClient.get("/admin/drives"),
  getAllUsers: () => apiClient.get("/admin/users"),
  getDriveSlots: (driveId) => apiClient.get(`/admin/drives/${driveId}/slots`),
  createCenter: (data) => apiClient.post("/admin/centers", data),
  createDrive: (data) => apiClient.post("/admin/drives", data),
  createSlot: (data) => apiClient.post("/admin/slots", data),
  deleteCenter: (centerId) => apiClient.delete(`/admin/centers/${centerId}`),
  deleteDrive: (driveId) => apiClient.delete(`/admin/drives/${driveId}`),
  enableUser: (userId) => apiClient.patch(`/admin/users/${userId}/enable`),
  disableUser: (userId) => apiClient.patch(`/admin/users/${userId}/disable`),
  updateBookingStatus: (bookingId, status) => {
    const actionMap = {
      approved: "approve",
      approve: "approve",
      rejected: "reject",
      reject: "reject",
      cancelled: "cancel",
      canceled: "cancel",
      cancel: "cancel",
      completed: "complete",
      complete: "complete"
    };
    const action = actionMap[String(status || "").toLowerCase()];
    if (!action) {
      return Promise.reject(new Error("Invalid booking status action"));
    }
    return apiClient.patch(`/admin/bookings/${bookingId}/${action}`);
  },
  exportBookings: () => apiClient.get("/admin/bookings/export", { responseType: "blob" }),
  getAuditLogs: () => apiClient.get("/admin/audit-logs"),
  getNotifications: () => apiClient.get("/admin/notifications"),
  // Feedback management
  getAllFeedback: (page = 0, size = 10) => apiClient.get("/admin/feedback", { params: { page, size } }),
  getFeedbackById: (id) => apiClient.get(`/admin/feedback/${id}`),
  respondToFeedback: (id, response) => apiClient.patch(`/admin/feedback/${id}/respond`, { response }),
  // Contact management
  getAllContacts: () => apiClient.get("/admin/contacts"),
  getContactById: (id) => apiClient.get(`/admin/contacts/${id}`),
  respondToContact: (id, response) => apiClient.patch(`/admin/contacts/${id}/respond`, { response }),
  deleteContact: (id) => apiClient.delete(`/admin/contacts/${id}`),
  // Certificate generation
  generateCertificate: (data) => apiClient.post("/certificates", data),
  getAllCertificates: () => apiClient.get("/certificates")
};

// Feedback APIs
export const feedbackAPI = {
  submitFeedback: (data) => apiClient.post("/feedback", data),
  getMyFeedback: () => apiClient.get("/feedback/my-feedback"),
  getFeedbackById: (id) => apiClient.get(`/feedback/${id}`)
};

// Contact APIs
export const contactAPI = {
  submitContact: (data) => apiClient.post("/contact", data),
  getMyInquiries: () => apiClient.get("/contact/my-inquiries"),
  getAllContacts: () => apiClient.get("/contact"),
  getContactById: (id) => apiClient.get(`/contact/${id}`),
  respondToContact: (id, response) => apiClient.patch(`/contact/${id}/respond`, { response }),
  deleteContact: (id) => apiClient.delete(`/contact/${id}`)
};

// News APIs
export const newsAPI = {
  getAllNews: (page = 0, size = 10) => apiClient.get("/news", { params: { page, size } }),
  getNewsById: (id) => apiClient.get(`/news/${id}`),
  createNews: (data) => apiClient.post("/news", data),
  updateNews: (id, data) => apiClient.put(`/news/${id}`, data),
  deleteNews: (id) => apiClient.delete(`/news/${id}`)
};

// Certificate APIs
export const certificateAPI = {
  getMyCertificates: () => apiClient.get("/certificates/my-certificates"),
  getCertificateById: (id) => apiClient.get(`/certificates/${id}`),
  verifyCertificate: (certNumber) => apiClient.get(`/certificates/verify/${certNumber}`),
  generateCertificate: (data) => apiClient.post("/certificates", data)
};

// Review APIs
export const reviewAPI = {
  getCenterReviews: (centerId) => apiClient.get(`/reviews/center/${centerId}`),
  getCenterReviewsPaged: (centerId, page = 0, size = 10) => apiClient.get(`/reviews/center/${centerId}/paged`, { params: { page, size } }),
  getCenterRating: (centerId) => apiClient.get(`/reviews/center/${centerId}/rating`),
  submitReview: (centerId, data) => apiClient.post(`/reviews`, data),
  approveReview: (id) => apiClient.patch(`/reviews/${id}/approve`),
  deleteReview: (id) => apiClient.delete(`/reviews/${id}`),
  getAllReviews: () => apiClient.get("/reviews")
};

// Notification APIs
export const notificationAPI = {
  subscribeToSlot: (driveId) => apiClient.post(`/notifications/slots/subscribe/${driveId}`),
  unsubscribeFromSlot: (driveId) => apiClient.post(`/notifications/slots/unsubscribe/${driveId}`),
  getSubscriptions: () => apiClient.get("/notifications/slots/subscriptions")
};

// Health API
export const healthAPI = {
  check: () => apiClient.get("/health"),
  ping: () => apiClient.get("/health/ping")
};

// Named export for apiClient
export { apiClient };
export default apiClient;
