const AUTH_STORAGE_KEYS = ["accessToken", "refreshToken", "role", "email", "name"];

const getStorageValue = (key) =>
  window.localStorage.getItem(key) ?? window.sessionStorage.getItem(key);

const removeStoredAuth = () => {
  AUTH_STORAGE_KEYS.forEach((key) => {
    window.localStorage.removeItem(key);
    window.sessionStorage.removeItem(key);
  });
};

const notifyAuthChanged = () => {
  window.dispatchEvent(new Event("vaxzone:auth-changed"));
};

export function setAuth(data, options = {}) {
  const normalizedData = data && typeof data === "object" && "data" in data ? data.data : data;
  const normalizedName = normalizedData?.name || normalizedData?.fullName || normalizedData?.userName || "";

  removeStoredAuth();

  if (!normalizedData?.accessToken || !normalizedData?.refreshToken) {
    return;
  }

  window.localStorage.setItem("accessToken", normalizedData.accessToken);
  window.localStorage.setItem("refreshToken", normalizedData.refreshToken);
  window.localStorage.setItem("role", normalizedData.role || "");
  window.localStorage.setItem("email", normalizedData.email || "");
  if (normalizedName) {
    window.localStorage.setItem("name", normalizedName);
  }

  notifyAuthChanged();
}

export function clearAuth() {
  removeStoredAuth();
  notifyAuthChanged();
}

export function getAccessToken() {
  return getStorageValue("accessToken");
}

export function getRefreshToken() {
  return getStorageValue("refreshToken");
}

export function getRole() {
  return getStorageValue("role");
}

export function getName() {
  return getStorageValue("name");
}

export function getEmail() {
  return getStorageValue("email");
}

export function isAuthenticated() {
  return Boolean(getAccessToken());
}

export function useAuth() {
  return {
    accessToken: getAccessToken(),
    refreshToken: getRefreshToken(),
    role: getRole(),
    name: getName(),
    email: getEmail(),
    isAuthenticated: isAuthenticated(),
    setAuth,
    clearAuth
  };
}
