import React, { useMemo, useState } from "react";
import { Helmet } from "react-helmet-async";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { authAPI, getErrorMessage } from "../api/client";
import FormContainer from "../components/auth/FormContainer";
import InputField from "../components/auth/InputField";
import PasswordField from "../components/auth/PasswordField";
import Button from "../components/auth/Button";
import { setAuth } from "../utils/auth";
import { validateEmail } from "../utils/authValidation";

const initialForm = {
  email: "",
  password: "",
  rememberMe: true
};

export default function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const redirect = searchParams.get("redirect") || "";
  const [form, setForm] = useState(initialForm);
  const [errors, setErrors] = useState({});
  const [serverMessage, setServerMessage] = useState("");
  const [resendEmail, setResendEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [show2FA, setShow2FA] = useState(false);
  const [verificationCode, setVerificationCode] = useState("");
  const [twoFactorMessage, setTwoFactorMessage] = useState("");

  const formIsValid = useMemo(() => {
    const nextErrors = {};
    const emailError = validateEmail(form.email);
    if (emailError) {
      nextErrors.email = emailError;
    }
    if (!form.password) {
      nextErrors.password = "Password is required";
    }
    return nextErrors;
  }, [form.email, form.password]);

  const updateField = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: "" }));
    setServerMessage("");
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (Object.keys(formIsValid).length > 0) {
      setErrors(formIsValid);
      return;
    }

    setLoading(true);
    setServerMessage("");

    try {
      const { data } = await authAPI.login({
        email: form.email.trim(),
        password: form.password
      });

      if (data.requiresTwoFactor) {
        setShow2FA(true);
        setTwoFactorMessage("Enter the verification code from your authenticator app.");
        return;
      }

      setAuth(data, { remember: form.rememberMe });
      navigate(redirect || (data.role === "USER" ? "/user/bookings" : "/admin/dashboard"), { replace: true });
    } catch (error) {
      const message = getErrorMessage(error, "Login failed. Please check your credentials.");
      setServerMessage(message);

      if (message.toLowerCase().includes("verify")) {
        setResendEmail(form.email.trim());
      }
    } finally {
      setLoading(false);
    }
  };

  const handleTwoFactorSubmit = async (event) => {
    event.preventDefault();

    if (verificationCode.trim().length !== 6) {
      setTwoFactorMessage("Enter the 6-digit verification code.");
      return;
    }

    setLoading(true);
    setTwoFactorMessage("");

    try {
      const { data } = await authAPI.verifyTwoFactor({
        email: form.email.trim(),
        twoFactorCode: verificationCode.trim()
      });

      setAuth(data, { remember: form.rememberMe });
      navigate(redirect || (data.role === "USER" ? "/user/bookings" : "/admin/dashboard"), { replace: true });
    } catch (error) {
      setTwoFactorMessage(getErrorMessage(error, "Unable to verify that code."));
    } finally {
      setLoading(false);
    }
  };

  const handleResendVerification = async () => {
    if (validateEmail(resendEmail)) {
      setServerMessage("Enter a valid registered email to resend verification.");
      return;
    }

    try {
      const response = await authAPI.resendVerification(resendEmail.trim());
      setServerMessage(response.data?.message || "Verification OTP sent.");
    } catch (error) {
      setServerMessage(getErrorMessage(error, "Unable to resend verification email."));
    }
  };

  return (
    <>
      <Helmet>
        <title>Login - VaxZone</title>
        <meta
          name="description"
          content="Sign in to VaxZone to manage vaccination bookings, notifications, and certificates."
        />
      </Helmet>

      <FormContainer
        eyebrow="Secure Access"
        title="Welcome back to a smoother vaccination workflow."
        description="Sign in to manage bookings, monitor updates, and keep every appointment moving with confidence."
        helper={(
          <>
            <span className="auth-form-kicker">{show2FA ? "Security checkpoint" : "Account access"}</span>
            <h2 className="auth-form-title">{show2FA ? "Two-factor verification" : "Sign in"}</h2>
            <p className="auth-form-subtitle">
              {show2FA
                ? "Add your final verification step before entering the dashboard."
                : "Use your registered email to continue into your account."}
            </p>
          </>
        )}
        footer={(
          <>
            <p className="mb-0">
              New to VaxZone? <Link to="/register">Create an account</Link>
            </p>
          </>
        )}
      >
        {serverMessage ? (
          <div className={`auth-alert ${serverMessage.toLowerCase().includes("sent") ? "is-success" : "is-error"}`}>
            <i className={`bi ${serverMessage.toLowerCase().includes("sent") ? "bi-check-circle" : "bi-exclamation-octagon"}`}></i>
            <span>{serverMessage}</span>
          </div>
        ) : null}

        {!show2FA ? (
          <form className="auth-form-grid" onSubmit={handleSubmit} noValidate>
            <div className="auth-context-bar">
              <div className="auth-context-pill">
                <i className="bi bi-lightning-charge"></i>
                <span>Fast sign-in</span>
              </div>
              <div className="auth-context-pill">
                <i className="bi bi-shield-check"></i>
                <span>Protected session</span>
              </div>
            </div>

            <InputField
              id="login-email"
              label="Email address"
              icon="bi bi-envelope"
              name="email"
              type="email"
              autoComplete="email"
              placeholder="name@company.com"
              value={form.email}
              error={errors.email || (form.email ? formIsValid.email : "")}
              onChange={(event) => updateField("email", event.target.value)}
            />

            <PasswordField
              id="login-password"
              label="Password"
              name="password"
              autoComplete="current-password"
              placeholder="Enter your password"
              value={form.password}
              error={errors.password || (form.password ? formIsValid.password : "")}
              showPassword={showPassword}
              onToggle={() => setShowPassword((current) => !current)}
              onChange={(event) => updateField("password", event.target.value)}
            />

            <div className="auth-row">
              <label className="auth-checkbox">
                <input
                  type="checkbox"
                  checked={form.rememberMe}
                  onChange={(event) => updateField("rememberMe", event.target.checked)}
                />
                <span>Remember me for future sessions</span>
              </label>

              <Link to="/forgot-password" className="auth-link-muted">
                Forgot Password?
              </Link>
            </div>

            <Button type="submit" loading={loading} loadingLabel="Signing you in...">
              <i className="bi bi-box-arrow-in-right"></i>
              <span>Sign in</span>
            </Button>

            <div className="auth-subcard">
              <div className="auth-subcard__header">
                <i className="bi bi-envelope-check"></i>
                <div>
                  <strong>Email not verified?</strong>
                  <p>Resend your verification OTP and complete setup securely.</p>
                </div>
              </div>
              <div className="auth-inline-actions">
                <input
                  className="auth-inline-input"
                  type="email"
                  placeholder="Registered email address"
                  value={resendEmail}
                  onChange={(event) => setResendEmail(event.target.value)}
                />
                <button type="button" className="auth-secondary-button" onClick={handleResendVerification}>
                  Resend
                </button>
              </div>
            </div>
          </form>
        ) : (
          <form className="auth-form-grid" onSubmit={handleTwoFactorSubmit} noValidate>
            <div className="auth-context-bar">
              <div className="auth-context-pill">
                <i className="bi bi-phone"></i>
                <span>Authenticator ready</span>
              </div>
              <div className="auth-context-pill">
                <i className="bi bi-lock"></i>
                <span>Final security step</span>
              </div>
            </div>

            {twoFactorMessage ? (
              <div className={`auth-alert ${twoFactorMessage.toLowerCase().includes("enter") ? "" : "is-error"}`}>
                <i className={`bi ${twoFactorMessage.toLowerCase().includes("enter") ? "bi-info-circle" : "bi-exclamation-octagon"}`}></i>
                <span>{twoFactorMessage}</span>
              </div>
            ) : null}

            <InputField
              id="two-factor-code"
              label="Verification code"
              icon="bi bi-shield-lock"
              inputMode="numeric"
              placeholder="123456"
              maxLength={6}
              value={verificationCode}
              onChange={(event) => {
                setVerificationCode(event.target.value.replace(/\D/g, ""));
                setTwoFactorMessage("");
              }}
              hint="Use the 6-digit code generated by your authenticator app."
            />

            <Button type="submit" loading={loading} loadingLabel="Verifying code...">
              <i className="bi bi-patch-check"></i>
              <span>Verify and continue</span>
            </Button>

            <button
              type="button"
              className="auth-text-button"
              onClick={() => {
                setShow2FA(false);
                setVerificationCode("");
                setTwoFactorMessage("");
              }}
            >
              Back to password login
            </button>
          </form>
        )}
      </FormContainer>
    </>
  );
}
