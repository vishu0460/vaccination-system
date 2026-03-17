import React, { useState, useEffect } from "react";
import { useSearchParams, Link } from "react-router-dom";
import { Helmet } from "react-helmet-async";
import { authAPI } from "../api/client";

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");
  const [status, setStatus] = useState("loading");
  const [message, setMessage] = useState("");

  useEffect(() => {
    const verifyEmail = async () => {
      if (!token) {
        setStatus("error");
        setMessage("Verification token is missing");
        return;
      }

      try {
        const response = await authAPI.verifyEmail(token);
        setStatus("success");
        setMessage(response.data.message || "Email verified successfully!");
      } catch (error) {
        setStatus("error");
        setMessage(
          error.response?.data?.message || 
          error.response?.data?.error || 
          "Email verification failed. The token may be invalid or expired."
        );
      }
    };

    verifyEmail();
  }, [token]);

  return (
    <>
      <Helmet>
        <title>Verify Email - VaxZone</title>
        <meta name="description" content="Verify your email address" />
      </Helmet>

      <div className="min-vh-100 d-flex align-items-center justify-content-center bg-light">
        <div className="container">
          <div className="row justify-content-center">
            <div className="col-md-6 col-lg-5">
              <div className="card shadow-sm border-0">
                <div className="card-body p-5 text-center">
                  {status === "loading" && (
                    <>
                      <div className="mb-4">
                        <div className="spinner-border text-primary" role="status">
                          <span className="visually-hidden">Loading...</span>
                        </div>
                      </div>
                      <h4 className="mb-3">Verifying Email...</h4>
                      <p className="text-muted">Please wait while we verify your email address.</p>
                    </>
                  )}

                  {status === "success" && (
                    <>
                      <div className="mb-4">
                        <div className="verification-icon success">
                          <i className="bi bi-check-circle-fill"></i>
                        </div>
                      </div>
                      <h4 className="mb-3">Email Verified!</h4>
                      <p className="text-muted mb-4">{message}</p>
                      <Link to="/login" className="btn btn-primary btn-lg w-100">
                        <i className="bi bi-box-arrow-in-right me-2"></i>Login to Account
                      </Link>
                    </>
                  )}

                  {status === "error" && (
                    <>
                      <div className="mb-4">
                        <div className="verification-icon error">
                          <i className="bi bi-x-circle-fill"></i>
                        </div>
                      </div>
                      <h4 className="mb-3">Verification Failed</h4>
                      <p className="text-muted mb-4">{message}</p>
                      <div className="d-flex gap-3 justify-content-center">
                        <Link to="/login" className="btn btn-outline-primary">
                          Go to Login
                        </Link>
                        <Link to="/register" className="btn btn-primary">
                          Register New Account
                        </Link>
                      </div>
                    </>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <style>{`
        .verification-icon {
          width: 80px;
          height: 80px;
          border-radius: 50%;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          font-size: 40px;
        }
        .verification-icon.success {
          background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
          color: #059669;
        }
        .verification-icon.error {
          background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
          color: #dc2626;
        }
      `}</style>
    </>
  );
}
