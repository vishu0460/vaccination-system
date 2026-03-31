import axios from "axios";
import { clearAuth, getAccessToken, getRefreshToken, setAuth } from "../utils/auth";

const DEFAULT_DEV_BACKEND_PORTS = [8080, 8081, 8082, 8083, 8084, 8085, 8086, 8087, 8088, 8089];

const getConfiguredApiBaseEnv = () => {
  const baseUrl = typeof import.meta.env.VITE_API_BASE_URL === "string"
    ? import.meta.env.VITE_API_BASE_URL.trim()
    : "";
  const legacyUrl = typeof import.meta.env.VITE_API_URL === "string"
    ? import.meta.env.VITE_API_URL.trim()
    : "";

  return baseUrl || legacyUrl;
};

const normalizeApiBaseUrl = (rawValue) => {
  const value = typeof rawValue === "string" ? rawValue.trim() : "";

  if (!value || value.toLowerCase() === "auto") {
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

const getDevBackendPorts = () => {
  const configuredPorts = typeof import.meta.env.VITE_API_PORT_CANDIDATES === "string"
    ? import.meta.env.VITE_API_PORT_CANDIDATES
        .split(",")
        .map((port) => Number.parseInt(port.trim(), 10))
        .filter((port) => Number.isInteger(port) && port > 0)
    : [];

  const ports = configuredPorts.length > 0 ? configuredPorts : DEFAULT_DEV_BACKEND_PORTS;
  return [...ports].sort((a, b) => a - b);
};

const shouldAutoDetectApiBaseUrl = () => {
  const value = getConfiguredApiBaseEnv();

  return !value || value.toLowerCase() === "auto";
};

const resolveApiOrigin = (origin, includePort = true) => {
  try {
    const parsed = new URL(origin);
    if (includePort) {
      return parsed.origin;
    }

    return `${parsed.protocol}//${parsed.hostname}`;
  } catch (error) {
    return origin;
  }
};

const buildCandidateApiBaseUrls = () => {
  if (typeof window === "undefined") {
    return [normalizeApiBaseUrl(getConfiguredApiBaseEnv())];
  }

  const currentOrigin = window.location.origin;
  const currentHostOrigin = resolveApiOrigin(currentOrigin, false);
  const candidates = [];

  candidates.push(`${currentOrigin}/api`);

  if (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1") {
    getDevBackendPorts().forEach((port) => {
      candidates.push(`${currentHostOrigin}:${port}/api`);
    });
  }

  return [...new Set(candidates)];
};

const candidateApiBaseUrls = buildCandidateApiBaseUrls();
let resolvedApiBaseUrl = normalizeApiBaseUrl(getConfiguredApiBaseEnv());
let apiBaseUrlPromise = null;

const pingApiBaseUrl = async (baseUrl) => {
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), 1200);

  try {
    const response = await fetch(`${baseUrl}/health`, {
      method: "GET",
      signal: controller.signal
    });

    return response.status < 500 || response.status === 503;
  } catch (error) {
    return false;
  } finally {
    window.clearTimeout(timeoutId);
  }
};

const getApiBaseUrl = async () => {
  if (!shouldAutoDetectApiBaseUrl() || typeof window === "undefined") {
    return resolvedApiBaseUrl;
  }

  if (apiBaseUrlPromise) {
    return apiBaseUrlPromise;
  }

  apiBaseUrlPromise = (async () => {
    for (const baseUrl of candidateApiBaseUrls) {
      if (await pingApiBaseUrl(baseUrl)) {
        resolvedApiBaseUrl = baseUrl;
        return resolvedApiBaseUrl;
      }
    }

    resolvedApiBaseUrl = candidateApiBaseUrls[0] || resolvedApiBaseUrl;
    return resolvedApiBaseUrl;
  })();

  return apiBaseUrlPromise;
};

export const unwrapApiData = (responseOrPayload) => {
  const payload = responseOrPayload?.data ?? responseOrPayload;
  return payload && typeof payload === "object" && "data" in payload ? payload.data : payload;
};

export const unwrapApiMessage = (responseOrPayload, fallback = "") => {
  const payload = responseOrPayload?.data ?? responseOrPayload;
  return payload?.message || fallback;
};

export const getErrorMessage = (error, fallback = "Something went wrong. Please try again.") =>
  error?.response?.data?.message
  || error?.response?.data?.errors?.[0]
  || error?.message
  || fallback;

export const getFieldErrors = (error) => {
  const payload = error?.response?.data;
  if (!payload || typeof payload !== "object") {
    return {};
  }

  const metadataErrors = payload.metadata?.fieldErrors;
  if (metadataErrors && typeof metadataErrors === "object") {
    return metadataErrors;
  }

  const errors = payload.errors;
  if (!Array.isArray(errors)) {
    return {};
  }

  return errors.reduce((accumulator, entry) => {
    if (typeof entry !== "string") {
      return accumulator;
    }

    const separatorIndex = entry.indexOf(":");
    if (separatorIndex <= 0) {
      return accumulator;
    }

    const field = entry.slice(0, separatorIndex).trim();
    const message = entry.slice(separatorIndex + 1).trim();
    if (field && message) {
      accumulator[field] = message;
    }
    return accumulator;
  }, {});
};

export const normalizeSearchValue = (value) => {
  if (typeof value !== "string") {
    return "";
  }

  return value.trim().toLowerCase();
};

const apiClient = axios.create({
  baseURL: resolvedApiBaseUrl,
  timeout: 15000
});

const buildAuthHeaders = () => {
  const token = getAccessToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
};

const isPublicEndpoint = (urlPath, method = "get") => {
  if (urlPath.startsWith("/auth/")) {
    return true;
  }
  if (urlPath.startsWith("/public/")) {
    return true;
  }
  if (urlPath === "/health" || urlPath === "/health/ping") {
    return true;
  }
  if (urlPath.startsWith("/reviews/center/")) {
    return true;
  }
  if (urlPath.startsWith("/certificates/verify/")) {
    return true;
  }
  return false;
};

apiClient.interceptors.request.use(async (config) => {
  config.baseURL = await getApiBaseUrl();

  const token = getAccessToken();
  const urlPath = config.url?.split("?")[0] || "";
  const isPublic = isPublicEndpoint(urlPath, config.method);

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
      const refreshToken = getRefreshToken();

      if (refreshToken) {
        try {
          const refreshBaseUrl = await getApiBaseUrl();
          const refreshResponse = await axios.post(`${refreshBaseUrl}/auth/refresh`, { refreshToken });
          const refreshedAuth = refreshResponse.data;
          setAuth(refreshedAuth, { remember: Boolean(window.localStorage.getItem("refreshToken")) });
          originalRequest.headers = originalRequest.headers || {};
          originalRequest.headers.Authorization = `Bearer ${refreshedAuth.accessToken}`;
          return apiClient(originalRequest);
        } catch (refreshError) {
          clearAuth();
          if (typeof window !== "undefined" && window.location.pathname !== "/login") {
            const redirect = `${window.location.pathname}${window.location.search}`;
            window.location.href = `/login?redirect=${encodeURIComponent(redirect)}`;
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
  sendOtp: (data) => apiClient.post("/auth/resend-otp", { email: data.email }),
  verifyOtp: (data) => apiClient.post("/auth/verify-otp", data),
  forgotPassword: (email) => apiClient.post("/auth/forgot-password", { email }),
  resetPassword: (data) => apiClient.post("/auth/reset-password", data),
  verifyEmail: (token) => apiClient.get(`/auth/verify-email?token=${encodeURIComponent(token)}`),
  resendVerification: (email) => apiClient.post("/auth/resend-otp", { email }),
  verifyTwoFactor: (data) => apiClient.post("/auth/2fa/verify", data)
};

export const publicAPI = {
  getDrives: (params) => apiClient.get("/public/drives", { params }),
  getCenters: (params = {}) => apiClient.get("/public/centers", { params: { page: 0, size: 50, ...params } }),
  getCitySuggestions: (query, limit = 8) => apiClient.get("/public/cities", {
    params: {
      query: normalizeSearchValue(query),
      limit
    }
  }),
  smartSearch: (params = {}) => apiClient.get("/public/search", { params }),
  getNearbyCenters: (params = {}) => apiClient.get("/public/nearby-centers", { params }),
  getCenterDetail: (id) => apiClient.get(`/public/centers/${id}`),
  getDriveSlots: (driveId) => apiClient.get(`/public/drives/${driveId}/slots`),
  getSummary: () => apiClient.get("/public/summary"),
  getStats: () => apiClient.get("/public/summary")
};

export const userAPI = {
  getProfile: () => apiClient.get("/users/me"),
  updateProfile: (data) => apiClient.put("/users/update-profile", data),
  requestPasswordChangeOtp: () => apiClient.post("/users/change-password/request-otp"),
  changePassword: (data) => apiClient.put("/users/change-password", data),
  getAccount: () => apiClient.get("/user/account"),
  getBookings: () => apiClient.get("/user/bookings"),
  bookSlot: (data) => apiClient.post("/user/bookings", data),
  cancelBooking: (bookingId) => apiClient.patch(`/user/bookings/${bookingId}/cancel`),
  rescheduleBooking: (bookingId, data) => apiClient.patch(`/user/bookings/${bookingId}/reschedule`, data),
  getNotifications: () => apiClient.get("/user/notifications"),
  markNotificationsRead: () => apiClient.patch("/user/notifications/read-all"),
  getSlotRecommendations: (params) => apiClient.get("/user/recommendations/slots", { params }),
  joinWaitlist: (slotId) => apiClient.post(`/user/slots/${slotId}/waitlist`),
  getWaitlist: () => apiClient.get("/user/waitlist")
};

const notificationEndpoints = {
  getNotifications: () => apiClient.get("/notifications"),
  getUnreadCount: () => apiClient.get("/notifications/unread-count"),
  markAsRead: (notificationId) => apiClient.patch(`/notifications/${notificationId}/read`),
  markAllRead: () => apiClient.patch("/notifications/read-all"),
  subscribeToSlot: (driveId) => apiClient.post(`/notifications/slots/subscribe/${driveId}`),
  unsubscribeFromSlot: (driveId) => apiClient.post(`/notifications/slots/unsubscribe/${driveId}`),
  getSubscriptions: () => apiClient.get("/notifications/slots/subscriptions")
};

export const adminAPI = {
  getDashboardStats: () => apiClient.get("/admin/dashboard/stats"),
  getDashboardAnalytics: () => apiClient.get("/admin/dashboard/analytics"),
  getSearchAnalytics: () => apiClient.get("/admin/search-analytics"),
  getAllBookings: () => apiClient.get("/admin/bookings"),
  getAllCenters: (params = {}) => apiClient.get("/admin/centers", { params: { page: 0, size: 500, ...params } }),
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
  getSystemLogs: (params = {}) => apiClient.get("/admin/logs", { params }),
  getActivityLogs: (params = {}) => apiClient.get("/admin/logs/activity", { params }),
  getSecurityLogs: (params = {}) => apiClient.get("/admin/logs/security", { params }),
  getAllFeedback: (page = 0, size = 10) => apiClient.get("/admin/feedback", { params: { page, size } }),
  respondToFeedback: (id, replyMessage) => apiClient.put(`/admin/feedback/${id}/reply`, { replyMessage }),
  getAllContacts: () => apiClient.get("/admin/contacts"),
  getContactAnalytics: () => apiClient.get("/contact/analytics"),
  respondToContact: (id, replyMessage) => apiClient.put(`/admin/contact/${id}/reply`, { replyMessage }),
  deleteContact: (id) => apiClient.delete(`/admin/contacts/${id}`),
  getAllCertificates: () => apiClient.get("/certificates")
};

export const superAdminAPI = {
  createAdmin: (data) => apiClient.post("/superadmin/create-admin", data),
  getAdmins: () => apiClient.get("/admins"),
  createManagedAdmin: (data) => apiClient.post("/admins", data),
  updateAdmin: (adminId, data) => apiClient.put(`/admins/${adminId}`, data),
  deleteAdmin: (adminId) => apiClient.delete(`/admins/${adminId}`),
  updateUser: (userId, data) => apiClient.put(`/super-admin/users/${userId}`, data),
  deleteUser: (userId) => apiClient.delete(`/super-admin/users/${userId}`),
  updateCenter: (centerId, data) => apiClient.put(`/super-admin/centers/${centerId}`, data),
  deleteCenter: (centerId) => apiClient.delete(`/super-admin/centers/${centerId}`),
  updateDrive: (driveId, data) => apiClient.put(`/super-admin/drives/${driveId}`, data),
  deleteDrive: (driveId) => apiClient.delete(`/super-admin/drives/${driveId}`),
  getDriveSlots: (driveId) => apiClient.get(`/super-admin/drives/${driveId}/slots`),
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
  getUserHistory: (userId) => apiClient.get(`/contact/user/${userId}`),
  getAllContacts: () => apiClient.get("/contact/all"),
  getContactById: (id) => apiClient.get(`/contact/${id}`),
  respondToContact: (id, response) => apiClient.patch(`/contact/${id}/respond`, { response }),
  deleteContact: (id) => apiClient.delete(`/contact/${id}`)
};

export const newsAPI = {
  getAllNews: (page = 0, size = 10) => apiClient.get("/public/news", {
    params: { page, size, _: Date.now() },
    headers: {
      "Cache-Control": "no-cache",
      Pragma: "no-cache"
    }
  }),
  getAdminNews: (page = 0, size = 100) => apiClient.get("/admin/news", { params: { page, size } }),
  getNewsById: (id) => apiClient.get(`/public/news/${id}`),
  createNews: (data) => apiClient.post("/admin/news", data, { headers: buildAuthHeaders() }),
  updateNews: (id, data) => apiClient.put(`/admin/news/${id}`, data, { headers: buildAuthHeaders() }),
  deleteNews: (id) => apiClient.delete(`/admin/news/${id}`, { headers: buildAuthHeaders() })
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
  ...notificationEndpoints
};

export const healthAPI = {
  check: () => apiClient.get("/health"),
  ping: () => apiClient.get("/health/ping")
};

export { apiClient };
export default apiClient;
