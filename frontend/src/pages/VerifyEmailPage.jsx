import React, { useEffect, useMemo, useState } from "react";
import { Helmet } from "react-helmet-async";
import { Link, useLocation, useSearchParams } from "react-router-dom";
import { authAPI, getErrorMessage, unwrapApiMessage } from "../api/client";
import FormContainer from "../components/auth/FormContainer";
import InputField from "../components/auth/InputField";
import Button from "../components/auth/Button";
import { validateEmail, validateOtp } from "../utils/authValidation";

const RESEND_SECONDS = 30;
const OTP_EXPIRY_SECONDS = 600;

export default function VerifyEmailPage() {
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") || "";
  const initialEmail = searchParams.get("email") || "";
  const registrationMessage = location.state?.registrationMessage || "";

  const [email, setEmail] = useState(initialEmail);
  const [otp, setOtp] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(Boolean(token));
  const [verified, setVerified] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(0);
  const [expiryCountdown, setExpiryCountdown] = useState(initialEmail ? OTP_EXPIRY_SECONDS : 0);

  const emailError = useMemo(() => (email ? validateEmail(email) : ""), [email]);
  const otpError = useMemo(() => (otp ? validateOtp(otp) : ""), [otp]);

  useEffect(() => {
    if (!token) {
      return undefined;
    }

    const verifyToken = async () => {
      try {
        const response = await authAPI.verifyEmail(token);
        setVerified(true);
        setMessage(unwrapApiMessage(response, "Email verified successfully."));
      } catch (err) {
        setError(getErrorMessage(err, "Email verification failed. The link may be invalid or expired."));
      } finally {
        setLoading(false);
      }
    };

    verifyToken();
    return undefined;
  }, [token]);

  useEffect(() => {
    if (resendCooldown <= 0) {
      return undefined;
    }
    const timer = window.setTimeout(() => setResendCooldown((current) => current - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [resendCooldown]);

  useEffect(() => {
    if (expiryCountdown <= 0 || verified) {
      return undefined;
    }
    const timer = window.setTimeout(() => setExpiryCountdown((current) => Math.max(0, current - 1)), 1000);
    return () => window.clearTimeout(timer);
  }, [expiryCountdown, verified]);

  const handleVerifyOtp = async (event) => {
    event.preventDefault();
    if (emailError || otpError) {
      setError(emailError || otpError);
      return;
    }

    setLoading(true);
    setError("");
    setMessage("");

    try {
      const response = await authAPI.verifyOtp({
        email: email.trim(),
        otp: otp.trim(),
        purpose: "EMAIL_VERIFICATION"
      });
      setVerified(true);
      setMessage(unwrapApiMessage(response, "Email verified successfully."));
    } catch (err) {
      setError(getErrorMessage(err, "Unable to verify OTP."));
    } finally {
      setLoading(false);
    }
  };

  const handleResendOtp = async () => {
    if (emailError) {
      setError(emailError);
      return;
    }

    setLoading(true);
    setError("");
    setMessage("");

    try {
      const response = await authAPI.resendVerification(email.trim());
      setMessage(unwrapApiMessage(response, "OTP sent to your email for verification."));
      setResendCooldown(RESEND_SECONDS);
      setExpiryCountdown(OTP_EXPIRY_SECONDS);
    } catch (err) {
      setError(getErrorMessage(err, "Unable to resend OTP."));
    } finally {
      setLoading(false);
    }
  };

  const countdownLabel = `${String(Math.floor(expiryCountdown / 60)).padStart(2, "0")}:${String(expiryCountdown % 60).padStart(2, "0")}`;

  return (
    <>
      <Helmet>
        <title>Verify Email - VaxZone</title>
        <meta name="description" content="Verify your VaxZone account using a secure 7-digit OTP." />
      </Helmet>

      <FormContainer
        eyebrow="Email Verification"
        title="Protect your account before you sign in."
        description="Enter the 7-digit verification code sent to your email to complete registration securely."
        helper={(
          <>
            <span className="auth-form-kicker">Security verification</span>
            <h2 className="auth-form-title">{verified ? "Verification complete" : "Verify your email"}</h2>
            <p className="auth-form-subtitle">
              {verified
                ? "Your VaxZone account is ready."
                : "We use a short-lived OTP to confirm email ownership and protect your account."}
            </p>
          </>
        )}
        footer={(
          <p className="mb-0">
            Need access later? <Link to="/login">Go to login</Link>
          </p>
        )}
      >
        {registrationMessage && !message && !error ? (
          <div className="auth-alert is-success">
            <i className="bi bi-envelope-check"></i>
            <span>{registrationMessage}</span>
          </div>
        ) : null}

        {message ? (
          <div className="auth-alert is-success">
            <i className="bi bi-check-circle"></i>
            <span>{message}</span>
          </div>
        ) : null}

        {error ? (
          <div className="auth-alert is-error">
            <i className="bi bi-exclamation-octagon"></i>
            <span>{error}</span>
          </div>
        ) : null}

        {verified ? (
          <Link to="/login" className="btn btn-primary btn-lg w-100">
            Continue to login
          </Link>
        ) : (
          <form className="auth-form-grid" onSubmit={handleVerifyOtp} noValidate>
            <InputField
              id="verify-email"
              label="Email address"
              icon="bi bi-envelope"
              type="email"
              autoComplete="email"
              placeholder="name@company.com"
              value={email}
              error={emailError}
              onChange={(event) => {
                setEmail(event.target.value);
                setError("");
                setMessage("");
              }}
            />

            <InputField
              id="verify-otp"
              label="7-digit OTP"
              icon="bi bi-shield-lock"
              inputMode="numeric"
              placeholder="1234567"
              maxLength={7}
              value={otp}
              error={otpError}
              onChange={(event) => {
                setOtp(event.target.value.replace(/\D/g, "").slice(0, 7));
                setError("");
              }}
              hint={expiryCountdown > 0 ? `OTP expires in ${countdownLabel}` : "Request a fresh OTP if your previous code expired."}
              autoFocus
            />

            <Button type="submit" loading={loading} loadingLabel="Verifying OTP...">
              <i className="bi bi-patch-check"></i>
              <span>Verify OTP</span>
            </Button>

            <button
              type="button"
              className="auth-secondary-button"
              disabled={loading || resendCooldown > 0}
              onClick={handleResendOtp}
            >
              {resendCooldown > 0 ? `Resend OTP in ${resendCooldown}s` : "Resend OTP"}
            </button>
          </form>
        )}
      </FormContainer>
    </>
  );
}
