import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Helmet } from "react-helmet-async";
import { authAPI } from "../api/client";
import { setAuth } from "../utils/auth";

export default function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ 
    email: "", 
    fullName: "", 
    password: "", 
    confirmPassword: "",
    age: 18,
    phoneNumber: ""
  });
  const [msg, setMsg] = useState("");
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState({});

  const validate = () => {
    const newErrors = {};
    if (!form.fullName.trim()) newErrors.fullName = "Full name is required";
    if (!form.email.trim()) newErrors.email = "Email is required";
    else if (!/\S+@\S+\.\S+/.test(form.email)) newErrors.email = "Invalid email format";
    if (!form.phoneNumber.trim()) newErrors.phoneNumber = "Phone number is required";
    else if (!/^[+]?[0-9]{10,15}$/.test(form.phoneNumber.replace(/\s/g, ""))) {
      newErrors.phoneNumber = "Invalid phone number format";
    }
    if (!form.password) newErrors.password = "Password is required";
    else if (form.password.length < 8) newErrors.password = "Password must be at least 8 characters";
    if (form.password !== form.confirmPassword) newErrors.confirmPassword = "Passwords do not match";
    if (!form.age || form.age < 1) newErrors.age = "Valid age is required";
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

    const submit = async (e) => {
    e.preventDefault();
    if (!validate()) return;
    
    setLoading(true);
    setMsg("");
    try {
      const { data } = await authAPI.register({
        email: form.email, 
        fullName: form.fullName, 
        password: form.password, 
        age: Number(form.age),
        phoneNumber: form.phoneNumber.replace(/\s/g, "")
      });

      if (data?.accessToken) {
        setAuth(data);
        navigate(data.role === "USER" ? "/user/bookings" : "/admin/dashboard", { replace: true });
        return;
      }

      setSuccess(true);
      setMsg("Registration successful! Please check your email to verify your account.");
    } catch (err) {
      console.error("Registration error:", err);
      console.error("Response data:", err.response?.data);
      const errorMessage = err.response?.data?.message 
        || err.response?.data?.error 
        || (typeof err.response?.data === 'string' ? err.response.data : "Registration failed. Please try again.");
      setMsg(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm({ ...form, [name]: value });
    if (errors[name]) setErrors({ ...errors, [name]: "" });
  };

  if (success) {
    return (
      <div className="auth-container">
        <div className="auth-card scale-in">
          <div className="card-header bg-success">
            <i className="bi bi-check-circle display-4 d-block mb-2"></i>
            <h4 className="mb-0 fw-bold">Registration Successful!</h4>
          </div>
          <div className="card-body text-center py-4">
            <div className="text-success mb-4">
              <i className="bi bi-check-circle display-1"></i>
            </div>
            <p className="mb-4">{msg}</p>
            <div className="d-grid gap-2">
              <Link to="/login" className="btn btn-primary btn-lg">
                <i className="bi bi-box-arrow-in-right me-2"></i>Go to Login
              </Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-container">
      <div className="auth-card scale-in">
        <div className="card-header">
          <i className="bi bi-person-plus display-4 d-block mb-2"></i>
          <h4 className="mb-0 fw-bold">Create Account</h4>
          <p className="mb-0 opacity-75">Join VaxZone to book your vaccination</p>
        </div>
        <div className="card-body">
          {msg && (
            <div className={`alert ${success ? "alert-success" : "alert-danger"}`}>
              {msg}
            </div>
          )}

          <form onSubmit={submit}>
            <div className="mb-3">
              <label className="form-label">Full Name</label>
              <div className="input-group">
                <span className="input-group-text bg-light border-end-0">
                  <i className="bi bi-person text-muted"></i>
                </span>
                <input
                  className={`form-control border-start-0 ${errors.fullName ? "is-invalid" : ""}`}
                  name="fullName"
                  placeholder="Enter your full name"
                  value={form.fullName}
                  onChange={handleChange}
                />
              </div>
              {errors.fullName && <div className="text-danger small mt-1">{errors.fullName}</div>}
            </div>

            <div className="mb-3">
              <label className="form-label">Email Address</label>
              <div className="input-group">
                <span className="input-group-text bg-light border-end-0">
                  <i className="bi bi-envelope text-muted"></i>
                </span>
                <input
                  className={`form-control border-start-0 ${errors.email ? "is-invalid" : ""}`}
                  name="email"
                  type="email"
                  placeholder="Enter your email"
                  value={form.email}
                  onChange={handleChange}
                />
              </div>
              {errors.email && <div className="text-danger small mt-1">{errors.email}</div>}
            </div>

            <div className="mb-3">
              <label className="form-label">Phone Number</label>
              <div className="input-group">
                <span className="input-group-text bg-light border-end-0">
                  <i className="bi bi-phone text-muted"></i>
                </span>
                <input
                  className={`form-control border-start-0 ${errors.phoneNumber ? "is-invalid" : ""}`}
                  name="phoneNumber"
                  type="tel"
                  placeholder="Enter your phone number (e.g., +1234567890)"
                  value={form.phoneNumber}
                  onChange={handleChange}
                />
              </div>
              {errors.phoneNumber && <div className="text-danger small mt-1">{errors.phoneNumber}</div>}
              <small className="text-muted">Enter with country code (e.g., +1)</small>
            </div>

            <div className="mb-3">
              <label className="form-label">Age</label>
              <div className="input-group">
                <span className="input-group-text bg-light border-end-0">
                  <i className="bi bi-calendar text-muted"></i>
                </span>
                <input
                  className={`form-control border-start-0 ${errors.age ? "is-invalid" : ""}`}
                  name="age"
                  type="number"
                  min="1"
                  max="120"
                  placeholder="Enter your age"
                  value={form.age}
                  onChange={handleChange}
                />
              </div>
              {errors.age && <div className="text-danger small mt-1">{errors.age}</div>}
            </div>

            <div className="mb-3">
              <label className="form-label">Password</label>
              <div className="input-group">
                <span className="input-group-text bg-light border-end-0">
                  <i className="bi bi-lock text-muted"></i>
                </span>
                <input
                  className={`form-control border-start-0 border-end-0 ${errors.password ? "is-invalid" : ""}`}
                  name="password"
                  type={showPassword ? "text" : "password"}
                  placeholder="Create a password"
                  value={form.password}
                  onChange={handleChange}
                />
                <button
                  className="btn btn-outline-secondary border-start-0"
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  <i className={`bi bi-eye${showPassword ? "-slash" : ""}`}></i>
                </button>
              </div>
              {errors.password && <div className="text-danger small mt-1">{errors.password}</div>}
              <small className="text-muted">At least 8 characters</small>
            </div>

            <div className="mb-4">
              <label className="form-label">Confirm Password</label>
              <div className="input-group">
                <span className="input-group-text bg-light border-end-0">
                  <i className="bi bi-lock-fill text-muted"></i>
                </span>
                <input
                  className={`form-control border-start-0 ${errors.confirmPassword ? "is-invalid" : ""}`}
                  name="confirmPassword"
                  type={showPassword ? "text" : "password"}
                  placeholder="Confirm your password"
                  value={form.confirmPassword}
                  onChange={handleChange}
                />
              </div>
              {errors.confirmPassword && <div className="text-danger small mt-1">{errors.confirmPassword}</div>}
            </div>

            <button type="submit" className="btn btn-primary w-100 btn-lg" disabled={loading}>
              {loading ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                  Creating Account...
                </>
              ) : (
                <>
                  <i className="bi bi-person-plus me-2"></i> Register
                </>
              )}
            </button>
          </form>

          <div className="text-center mt-4">
            <p className="text-muted mb-0">
              Already have an account?{" "}
              <Link to="/login" className="text-decoration-none fw-bold text-primary">
                Login
              </Link>
            </p>
          </div>

          <div className="text-center mt-3">
            <small className="text-muted">
              By registering, you agree to our{" "}
              <Link to="/terms-conditions" className="text-primary">Terms</Link>
              {" "}and{" "}
              <Link to="/privacy-policy" className="text-primary">Privacy Policy</Link>
            </small>
          </div>
        </div>
      </div>
    </div>
  );
}
