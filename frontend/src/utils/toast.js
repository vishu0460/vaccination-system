import toast from "react-hot-toast";

const baseOptions = {
  position: "top-right",
  duration: 3500,
  style: {
    borderRadius: "16px",
    padding: "14px 16px",
    background: "#ffffff",
    color: "#0f172a",
    boxShadow: "0 18px 45px rgba(15, 23, 42, 0.14)",
    border: "1px solid rgba(226, 232, 240, 0.9)",
    fontWeight: 600
  }
};

export function successToast(message) {
  return toast.success(message, {
    ...baseOptions,
    iconTheme: {
      primary: "#10b981",
      secondary: "#ffffff"
    }
  });
}

export function errorToast(message) {
  return toast.error(message, {
    ...baseOptions,
    duration: 4000,
    iconTheme: {
      primary: "#ef4444",
      secondary: "#ffffff"
    }
  });
}

export function infoToast(message) {
  return toast(message, {
    ...baseOptions,
    icon: "ℹ️"
  });
}
