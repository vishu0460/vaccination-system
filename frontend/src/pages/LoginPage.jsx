import React, { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Helmet } from "react-helmet-async";
import api from "../api/client";
import { setAuth } from "../utils/auth";

export default function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const redirect = searchParams.get("redirect") || "";
  const [form, setForm] = useState({ email: "", password: "" });
  const [verificationCode, setVerificationCode] = useState("");
  const [show2FA, setShow2FA] = useState(false);
  const [msg, setMsg] = useState("");
  const [loading, setLoading] = useState(false);
  const [resendEmail, setResendEmail] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMsg("");
    try {
      const { data } = await api.post("/auth/login", form);
      
      if (data.requiresTwoFactor) {
        setShow2FA(true);
        setLoading(false);
        return;
      }
      
      setAuth(data);
      const destination = redirect || (data.role === "USER" ? "/user/bookings" : "/admin/dashboard");
      navigate(destination);
    } catch (err) {
      const errorMsg = err.response?.data?.message || "Login failed. Please check your credentials.";
      setMsg(errorMsg);
      
      if (err.response?.data?.requiresVerification) {
        setResendEmail(form.email);
      }
    } finally {
      setLoading(false);
    }
  };

  const submit2FA = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMsg("");
    try {
      const response2FA = await api.post("/auth/2fa/verify", {
        email: form.email,
        code: verificationCode
      });
      const data2FA = response2FA.data;
      setAuth(data2FA);
      const destination = redirect || (data2FA.role === "USER" ? "/user/bookings" : "/admin/dashboard");
      navigate(destination);
    } catch (err) {
      setMsg(err.response?.data?.message || "Invalid verification code");
    } finally {
      setLoading(false);
    }
  };

  const resendVerification = async () => {
    if (!resendEmail) {
      setMsg("Please enter your email address");
      return;
    }
    try {
      const { data } = await api.post("/auth/resend-verification", { email: resendEmail });
      setMsg(data.message || "Verification email sent!");
    } catch (err) {
      setMsg(err.response?.data?.message || "Unable to resend verification email");
    }
  };

  return (
    <>
      <Helmet>
        <title>Login - VaxZone</title>
        <meta name="description" content="Login to your account to book vaccination slots and manage your appointments." />
      </Helmet>

      <div className="auth-container">
        <div className="auth-card scale-in">
          <div className="card-header">
            <i className="bi bi-shield-check display-4 d-block mb-2"></i>
            <h4 className="mb-0 fw-bold">Welcome Back</h4>
            <p className="mb-0 opacity-75">Login to manage your vaccination bookings</p>
          </div>
          <div className="card-body">
            {msg && (
              <div className={`alert ${msg.toLowerCase().includes("unable") || msg.toLowerCase().includes("failed") || msg.toLowerCase().includes("check") || msg.toLowerCase().includes("invalid") ? "alert-danger" : "alert-info"}`}>
                {msg}
              </div>
            )}

            {!show2FA ? (
              <form onSubmit={submit}>
                <div className="mb-3">
                  <label className="form-label">Email Address</label>
                  <div className="input-group">
                    <span className="input-group-text bg-light border-end-0">
                      <i className="bi bi-envelope text-muted"></i>
                    </span>
                    <input
                      className="form-control border-start-0"
                      name="email"
                      placeholder="Enter your email"
                      type="email"
                      required
                      value={form.email}
                      onChange={(e) => setForm({ ...form, email: e.target.value })}
                    />
                  </div>
                </div>

                <div className="mb-3">
                  <label className="form-label">Password</label>
                  <div className="input-group">
                    <span className="input-group-text bg-light border-end-0">
                      <i className="bi bi-lock text-muted"></i>
                    </span>
                    <input
                      className="form-control border-start-0 border-end-0"
                      name="password"
                      placeholder="Enter your password"
                      type={showPassword ? "text" : "password"}
                      required
                      value={form.password}
                      onChange={(e) => setForm({ ...form, password: e.target.value })}
                    />
                    <button
                      className="btn btn-outline-secondary border-start-0"
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                    >
                      <i className={`bi bi-eye${showPassword ? "-slash" : ""}`}></i>
                    </button>
                  </div>
                </div>

                <div className="d-flex justify-content-end mb-4">
                  <Link to="/forgot-password" className="text-decoration-none small text-primary">
                    Forgot Password?
                  </Link>
                </div>

                <button type="submit" className="btn btn-primary w-100 btn-lg" disabled={loading}>
                  {loading ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                      Logging in...
                    </>
                  ) : (
                    <>
                      <i className="bi bi-box-arrow-in-right me-2"></i> Login
                    </>
                  )}
                </button>
              </form>
            ) : (
              <form onSubmit={submit2FA}>
                <div className="mb-3">
                  <label className="form-label">Two-Factor Authentication</label>
                  <div className="input-group">
                    <span className="input-group-text bg-light border-end-0">
                      <i className="bi bi-shield-lock text-muted"></i>
                    </span>
                    <input
                      className="form-control border-start-0"
                      placeholder="Enter 6-digit code"
                      type="text"
                      maxLength="6"
                      required
                      value={verificationCode}
                      onChange={(e) => setVerificationCode(e.target.value.replace(/\D/g, ''))}
                    />
                  </div>
                  <small className="text-muted">Enter the 6-digit code from your authenticator app</small>
                </div>

                <button type="submit" className="btn btn-primary w-100 btn-lg" disabled={loading}>
                  {loading ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                      Verifying...
                    </>
                  ) : (
                    <>
                      <i className="bi bi-check-circle me-2"></i> Verify
                    </>
                  )}
                </button>

                <button 
                  type="button" 
                  className="btn btn-link w-100 mt-2"
                  onClick={() => {
                    setShow2FA(false);
                    setVerificationCode("");
                  }}
                >
                  Back to Login
                </button>
              </form>
            )}

            <div className="text-center mt-4">
              <p className="text-muted mb-0">
                Don't have an account?{" "}
                <Link to="/register" className="text-decoration-none fw-bold text-primary">
                  Register Now
                </Link>
              </p>
            </div>

            {(!show2FA) && (
              <div className="card bg-light border-0 mt-4">
                <div className="card-body py-3">
                  <h6 className="fw-bold mb-2">
                    <i className="bi bi-envelope-exclamation me-2 text-primary"></i>
                    Email Not Verified?
                  </h6>
                  <p className="small text-muted mb-3">
                    Enter your email below to resend verification token.
                  </p>
                  <div className="d-flex gap-2">
                    <input
                      className="form-control form-control-sm"
                      type="email"
                      placeholder="Registered email"
                      value={resendEmail}
                      onChange={(e) => setResendEmail(e.target.value)}
                    />
                    <button type="button" className="btn btn-primary btn-sm" onClick={resendVerification}>
                      Resend
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  );
}

