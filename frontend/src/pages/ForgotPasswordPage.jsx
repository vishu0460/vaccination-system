import React, { useState } from "react";
import { Link } from "react-router-dom";
import { Helmet } from "react-helmet-async";
import api from "../api/client";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [msg, setMsg] = useState("");
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const { data } = await api.post("/auth/forgot-password", { email });
      setMsg(data.message || "Password reset link sent to your email!");
    } catch (err) {
      setMsg(err.response?.data?.message || "Failed to send reset link");
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Helmet>
        <title>Forgot Password - VaxZone</title>
      </Helmet>

      <div className="auth-container">
        <div className="auth-card scale-in">
          <div className="card-header">
            <i className="bi bi-key display-4 d-block mb-2"></i>
            <h4 className="mb-0 fw-bold">Forgot Password?</h4>
            <p className="mb-0 opacity-75">Enter your email to reset your password</p>
          </div>
          <div className="card-body">
            {msg && (
              <div className={`alert ${msg.toLowerCase().includes("failed") || msg.toLowerCase().includes("error") ? "alert-danger" : "alert-success"}`}>
                {msg}
              </div>
            )}

            <form onSubmit={submit}>
              <div className="mb-4">
                <label className="form-label">Email Address</label>
                <div className="input-group">
                  <span className="input-group-text bg-light border-end-0">
                    <i className="bi bi-envelope text-muted"></i>
                  </span>
                  <input 
                    className="form-control border-start-0" 
                    type="email" 
                    required 
                    placeholder="Enter your registered email"
                    value={email} 
                    onChange={(e) => setEmail(e.target.value)} 
                  />
                </div>
              </div>

              <button type="submit" className="btn btn-primary w-100 btn-lg" disabled={loading}>
                {loading ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                    Sending...
                  </>
                ) : (
                  <>
                    <i className="bi bi-send me-2"></i> Send Reset Link
                  </>
                )}
              </button>
            </form>

            <div className="text-center mt-4">
              <p className="text-muted mb-0">
                Remember your password?{" "}
                <Link to="/login" className="text-decoration-none fw-bold text-primary">
                  Login here
                </Link>
              </p>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
