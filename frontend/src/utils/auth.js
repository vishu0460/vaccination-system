export function setAuth(data) {
  localStorage.setItem("accessToken", data.accessToken);
  localStorage.setItem("refreshToken", data.refreshToken);
  localStorage.setItem("role", data.role);
  localStorage.setItem("email", data.email);
  if (data.name) {
    localStorage.setItem("name", data.name);
  }
}

export function clearAuth() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("role");
  localStorage.removeItem("email");
  localStorage.removeItem("name");
}

export function getAccessToken() {
  return localStorage.getItem("accessToken");
}

export function getRole() {
  return localStorage.getItem("role");
}

export function getName() {
  return localStorage.getItem("name");
}

export function getEmail() {
  return localStorage.getItem("email");
}

export function isAuthenticated() {
  return !!getAccessToken();
}

// React hook for auth (for use in functional components)
export function useAuth() {
  return {
    accessToken: getAccessToken(),
    role: getRole(),
    name: getName(),
    email: getEmail(),
    isAuthenticated: isAuthenticated(),
    setAuth,
    clearAuth
  };
}
