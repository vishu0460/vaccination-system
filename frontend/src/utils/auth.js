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
  const { remember = true } = options;
  const storage = remember ? window.localStorage : window.sessionStorage;

  removeStoredAuth();

  storage.setItem("accessToken", data.accessToken);
  storage.setItem("refreshToken", data.refreshToken);
  storage.setItem("role", data.role);
  storage.setItem("email", data.email);
  if (data.name) {
    storage.setItem("name", data.name);
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
