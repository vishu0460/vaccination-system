import axios from "axios";
import { clearAuth, getAccessToken, setAuth } from "../utils/auth";

const normalizeApiBaseUrl = (rawValue) => {
  const value = typeof rawValue === "string" ? rawValue.trim() : "";

  if (!value) {
    return "http://localhost:8080/api";
  }

  const withoutTrailingSlash = value.replace(/\/+$/, "");
  if (withoutTrailingSlash.endsWith("/api")) {
    return withoutTrailingSlash;
  }

  if (withoutTrailingSlash.startsWith("http://") || withoutTrailingSlash.startsWith("https://")) {
    return `${withoutTrailingSlash}/api`;
  }

  return withoutTrailingSlash.startsWith("/api") ? withoutTrailingSlash : `/api${withoutTrailingSlash}`;
};

const apiBaseUrl = normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL);

export const unwrapApiData = (responseOrPayload) => {
  const payload = responseOrPayload?.data ?? responseOrPayload;
  return payload && typeof payload === "object" && "data" in payload ? payload.data : payload;
};

export const unwrapApiMessage = (responseOrPayload, fallback = "") => {
  const payload = responseOrPayload?.data ?? responseOrPayload;
  return payload?.message || fallback;
};

const apiClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: 15000
});

const publicEndpoints = [
  "/auth/",
  "/public/",
  "/health",
  "/news",
  "/contact",
  "/reviews/center/",
  "/certificates/verify/"
];

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken();
  const urlPath = config.url?.split("?")[0] || "";
  const isPublic = publicEndpoints.some((endpoint) => urlPath.startsWith(endpoint));

  if (token && !isPublic) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (!originalRequest) {
      return Promise.reject(error);
    }

    const isRefreshRequest = originalRequest.url?.includes("/auth/refresh");
    if (error.response?.status === 401 && !originalRequest._retry && !isRefreshRequest) {
      originalRequest._retry = true;
      const refreshToken = localStorage.getItem("refreshToken");

      if (refreshToken) {
        try {
          const refreshResponse = await axios.post(`${apiBaseUrl}/auth/refresh`, { refreshToken });
          const refreshedAuth = refreshResponse.data;
          setAuth(refreshedAuth);
          originalRequest.headers = originalRequest.headers || {};
          originalRequest.headers.Authorization = `Bearer ${refreshedAuth.accessToken}`;
          return apiClient(originalRequest);
        } catch (refreshError) {
          clearAuth();
          if (typeof window !== "undefined" && window.location.pathname !== "/login") {
            window.location.href = "/login";
          }
          return Promise.reject(refreshError);
        }
      }
    }

    return Promise.reject(error);
  }
);

export const authAPI = {
  login: (data) => apiClient.post("/auth/login", data),
  register: (data) => apiClient.post("/auth/register", data),
  refresh: (refreshToken) => apiClient.post("/auth/refresh", { refreshToken }),
  forgotPassword: (email) => apiClient.post("/auth/forgot-password", { email }),
  resetPassword: (data) => apiClient.post("/auth/reset-password", data),
  verifyEmail: (token) => apiClient.get(`/auth/verify-email?token=${encodeURIComponent(token)}`),
  resendVerification: (email) => apiClient.post("/auth/resend-verification", { email }),
  verifyTwoFactor: (data) => apiClient.post("/auth/2fa/verify", data)
};

export const publicAPI = {
  getDrives: (params) => apiClient.get("/public/drives", { params }),
  getCenters: (city, page = 0, size = 50) => apiClient.get("/public/centers", { params: { city, page, size } }),
  getCenterDetail: (id) => apiClient.get(`/public/centers/${id}`),
  getDriveSlots: (driveId) => apiClient.get(`/public/drives/${driveId}/slots`),
  getSummary: () => apiClient.get("/public/summary"),
  getStats: () => apiClient.get("/public/summary")
};

export const userAPI = {
  getProfile: () => apiClient.get("/profile"),
  updateProfile: (data) => apiClient.put("/profile", data),
  changePassword: (data) => apiClient.post("/profile/change-password", data),
  getAccount: () => apiClient.get("/user/account"),
  getBookings: () => apiClient.get("/user/bookings"),
  bookSlot: (data) => apiClient.post("/user/bookings", data),
  cancelBooking: (bookingId) => apiClient.patch(`/user/bookings/${bookingId}/cancel`),
  rescheduleBooking: (bookingId, data) => apiClient.patch(`/user/bookings/${bookingId}/reschedule`, data),
  getNotifications: () => apiClient.get("/user/notifications"),
  markNotificationsRead: () => apiClient.patch("/user/notifications/read-all"),
  getSlotRecommendations: (params) => apiClient.get("/user/recommendations/slots", { params })
};

export const adminAPI = {
  getDashboardStats: () => apiClient.get("/admin/dashboard/stats"),
  getAllBookings: () => apiClient.get("/admin/bookings"),
  getAllCenters: () => apiClient.get("/admin/centers"),
  getAllDrives: () => apiClient.get("/admin/drives"),
  getAllUsers: () => apiClient.get("/admin/users"),
  getAllSlots: (params = {}) => apiClient.get("/admin/slots", { params }),
  getAllSlotsList: (params = {}) => apiClient.get("/admin/slots/all", { params }),
  getDriveSlots: (driveId) => apiClient.get(`/admin/drives/${driveId}/slots`),
  createCenter: (data) => apiClient.post("/admin/centers", data),
  updateCenter: (centerId, data) => apiClient.put(`/admin/centers/${centerId}`, data),
  createDrive: (data) => apiClient.post("/admin/drives", data),
  updateDrive: (driveId, data) => apiClient.put(`/admin/drives/${driveId}`, data),
  createSlot: (data) => apiClient.post("/admin/slots", data),
  updateSlot: (slotId, data) => apiClient.put(`/admin/slots/${slotId}`, data),
  deleteSlot: (slotId) => apiClient.delete(`/admin/slots/${slotId}`),
  deleteCenter: (centerId) => apiClient.delete(`/admin/centers/${centerId}`),
  deleteDrive: (driveId) => apiClient.delete(`/admin/drives/${driveId}`),
  enableUser: (userId) => apiClient.patch(`/admin/users/${userId}/enable`),
  disableUser: (userId) => apiClient.patch(`/admin/users/${userId}/disable`),
  updateBookingStatus: (bookingId, status) => {
    const actionMap = {
      approve: "approve",
      reject: "reject",
      cancelled: "cancel",
      canceled: "cancel",
      cancel: "cancel",
      completed: "complete",
      complete: "complete",
      confirmed: "confirm",
      confirm: "confirm"
    };
    const action = actionMap[String(status || "").toLowerCase()];

    if (!action) {
      return Promise.reject(new Error("Invalid booking status action"));
    }

    return apiClient.patch(`/admin/bookings/${bookingId}/${action}`);
  },
  completeBooking: (bookingId) => apiClient.put(`/admin/booking/${bookingId}/complete`),
  deleteBooking: (bookingId) => apiClient.delete(`/admin/booking/${bookingId}`),
  exportBookings: () => apiClient.get("/admin/bookings/export", { responseType: "blob" }),
  getAuditLogs: () => apiClient.get("/admin/audit-logs"),
  getAllFeedback: (page = 0, size = 10) => apiClient.get("/admin/feedback", { params: { page, size } }),
  respondToFeedback: (id, replyMessage) => apiClient.put(`/admin/feedback/${id}/reply`, { replyMessage }),
  getAllContacts: () => apiClient.get("/admin/contacts"),
  respondToContact: (id, replyMessage) => apiClient.put(`/admin/contact/${id}/reply`, { replyMessage }),
  deleteContact: (id) => apiClient.delete(`/admin/contacts/${id}`),
  getAllCertificates: () => apiClient.get("/certificates")
};

export const superAdminAPI = {
  updateUser: (userId, data) => apiClient.put(`/super-admin/users/${userId}`, data),
  deleteUser: (userId) => apiClient.delete(`/super-admin/users/${userId}`),
  updateCenter: (centerId, data) => apiClient.put(`/super-admin/centers/${centerId}`, data),
  deleteCenter: (centerId) => apiClient.delete(`/super-admin/centers/${centerId}`),
  updateDrive: (driveId, data) => apiClient.put(`/super-admin/drives/${driveId}`, data),
  deleteDrive: (driveId) => apiClient.delete(`/super-admin/drives/${driveId}`),
  updateSlot: (slotId, data) => apiClient.put(`/super-admin/slots/${slotId}`, data),
  deleteSlot: (slotId) => apiClient.delete(`/super-admin/slots/${slotId}`)
};

export const feedbackAPI = {
  submitFeedback: (data) => apiClient.post("/feedback", data),
  getMyFeedback: () => apiClient.get("/feedback/my-feedback"),
  getFeedbackById: (id) => apiClient.get(`/feedback/${id}`)
};

export const contactAPI = {
  submitContact: (data) => apiClient.post("/contact", data),
  getMyInquiries: () => apiClient.get("/contact/my-inquiries"),
  getAllContacts: () => apiClient.get("/contact"),
  getContactById: (id) => apiClient.get(`/contact/${id}`),
  respondToContact: (id, response) => apiClient.patch(`/contact/${id}/respond`, { response }),
  deleteContact: (id) => apiClient.delete(`/contact/${id}`)
};

export const newsAPI = {
  getAllNews: (page = 0, size = 10) => apiClient.get("/news", { params: { page, size } }),
  getNewsById: (id) => apiClient.get(`/news/${id}`),
  createNews: (data) => apiClient.post("/news", data),
  updateNews: (id, data) => apiClient.put(`/news/${id}`, data),
  deleteNews: (id) => apiClient.delete(`/news/${id}`)
};

export const certificateAPI = {
  getMyCertificates: () => apiClient.get("/certificates/my-certificates"),
  getCertificateById: (id) => apiClient.get(`/certificates/${id}`),
  verifyCertificate: (certNumber) => apiClient.get(`/certificates/verify/${certNumber}`),
  generateCertificate: (data) => apiClient.post("/certificates", data),
  getAllCertificates: () => apiClient.get("/certificates")
};

export const reviewAPI = {
  getCenterReviews: (centerId) => apiClient.get(`/reviews/center/${centerId}`),
  getCenterReviewsPaged: (centerId, page = 0, size = 10) => apiClient.get(`/reviews/center/${centerId}/paged`, { params: { page, size } }),
  getCenterRating: (centerId) => apiClient.get(`/reviews/center/${centerId}/rating`),
  submitReview: (data) => apiClient.post("/reviews", data),
  approveReview: (id) => apiClient.patch(`/reviews/${id}/approve`),
  deleteReview: (id) => apiClient.delete(`/reviews/${id}`),
  getAllReviews: () => apiClient.get("/reviews")
};

export const notificationAPI = {
  subscribeToSlot: (driveId) => apiClient.post(`/notifications/slots/subscribe/${driveId}`),
  unsubscribeFromSlot: (driveId) => apiClient.post(`/notifications/slots/unsubscribe/${driveId}`),
  getSubscriptions: () => apiClient.get("/notifications/slots/subscriptions")
};

export const healthAPI = {
  check: () => apiClient.get("/health"),
  ping: () => apiClient.get("/health/ping")
};

export { apiClient };
export default apiClient;
