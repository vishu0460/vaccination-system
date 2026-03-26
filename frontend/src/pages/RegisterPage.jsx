import React, { useEffect, useMemo, useState } from "react";
import { Helmet } from "react-helmet-async";
import { Link, useNavigate } from "react-router-dom";
import { authAPI, getErrorMessage, getFieldErrors, unwrapApiMessage } from "../api/client";
import FormContainer from "../components/auth/FormContainer";
import InputField from "../components/auth/InputField";
import PasswordField from "../components/auth/PasswordField";
import Button from "../components/auth/Button";
import {
  validateAge,
  getPasswordStrength,
  validateConfirmPassword,
  validateEmail,
  validateFullName,
  validatePassword,
  validatePhone
} from "../utils/authValidation";

const initialForm = {
  fullName: "",
  email: "",
  phoneNumber: "",
  age: "",
  password: "",
  confirmPassword: "",
  acceptedTerms: false
};

export default function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState(initialForm);
  const [errors, setErrors] = useState({});
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [serverMessage, setServerMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const passwordStrength = useMemo(() => getPasswordStrength(form.password), [form.password]);
  const validationErrors = useMemo(() => {
    const nextErrors = {
      fullName: validateFullName(form.fullName),
      email: validateEmail(form.email),
      phoneNumber: validatePhone(form.phoneNumber),
      age: validateAge(form.age),
      password: validatePassword(form.password),
      confirmPassword: validateConfirmPassword(form.password, form.confirmPassword)
    };

    if (!form.acceptedTerms) {
      nextErrors.acceptedTerms = "You must accept the terms and conditions";
    }

    return nextErrors;
  }, [form]);

  const hasValidationIssues = Object.values(validationErrors).some(Boolean);

  useEffect(() => {
    if (!successMessage) {
      return undefined;
    }

    const timer = window.setTimeout(() => {
      if (successMessage.toLowerCase().includes("log in now")) {
        navigate("/login", { replace: true });
        return;
      }

      navigate(`/verify-email?email=${encodeURIComponent(form.email.trim())}`, {
        replace: true,
        state: {
          registrationMessage: successMessage
        }
      });
    }, 1800);

    return () => window.clearTimeout(timer);
  }, [form.email, navigate, successMessage]);

  const updateField = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: "" }));
    setServerMessage("");
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (hasValidationIssues) {
      setErrors(validationErrors);
      return;
    }

    setLoading(true);
    setServerMessage("");

    try {
      const response = await authAPI.register({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        phoneNumber: form.phoneNumber.replace(/\s+/g, ""),
        password: form.password,
        age: Number.parseInt(form.age, 10)
      });

      const registerPayload = response.data || {};
      setSuccessMessage(
        unwrapApiMessage(registerPayload, "Registration successful. Please verify the 7-digit OTP sent to your email.")
      );
      setErrors({});
    } catch (error) {
      const backendFieldErrors = getFieldErrors(error);
      if (Object.keys(backendFieldErrors).length > 0) {
        setErrors((current) => ({ ...current, ...backendFieldErrors }));
      }
      setServerMessage(getErrorMessage(error, "Registration failed. Please review your details."));
    } finally {
      setLoading(false);
    }
  };

  const strengthToneClass = passwordStrength.score >= 90
    ? "is-excellent"
    : passwordStrength.score >= 70
      ? "is-strong"
      : passwordStrength.score >= 50
        ? "is-fair"
        : "is-weak";

  return (
    <>
      <Helmet>
        <title>Create Account - VaxZone</title>
        <meta
          name="description"
          content="Create your VaxZone account to book vaccination slots, monitor appointments, and access certificates."
        />
      </Helmet>

      <FormContainer
        eyebrow="New Account"
        title="Create a trusted account in minutes."
        description="Set up your VaxZone profile with strong credentials, instant validation, and a polished onboarding flow built for real users."
        helper={(
          <>
            <span className="auth-form-kicker">New workspace identity</span>
            <h2 className="auth-form-title">Create your account</h2>
            <p className="auth-form-subtitle">
              Enter your details to unlock bookings, certificates, and notifications in one secure place.
            </p>
          </>
        )}
        footer={(
          <p className="mb-0">
            Already have an account? <Link to="/login">Sign in</Link>
          </p>
        )}
      >
        {serverMessage ? (
          <div className="auth-alert is-error">
            <i className="bi bi-exclamation-octagon"></i>
            <span>{serverMessage}</span>
          </div>
        ) : null}

        {successMessage ? (
          <div className="auth-alert is-success">
            <i className="bi bi-check-circle"></i>
            <span>{successMessage}</span>
          </div>
        ) : null}

        <form className="auth-form-grid" onSubmit={handleSubmit} noValidate>
          <div className="auth-context-bar">
            <div className="auth-context-pill">
              <i className="bi bi-person-badge"></i>
              <span>Professional onboarding</span>
            </div>
            <div className="auth-context-pill">
              <i className="bi bi-patch-check"></i>
              <span>Live validation</span>
            </div>
          </div>

          <InputField
            id="register-full-name"
            label="Full name"
            icon="bi bi-person"
            name="fullName"
            autoComplete="name"
            placeholder="Aarav Sharma"
            value={form.fullName}
            error={errors.fullName || (form.fullName ? validationErrors.fullName : "")}
            onChange={(event) => updateField("fullName", event.target.value)}
          />

          <InputField
            id="register-email"
            label="Email address"
            icon="bi bi-envelope"
            name="email"
            type="email"
            autoComplete="email"
            placeholder="name@company.com"
            value={form.email}
            error={errors.email || (form.email ? validationErrors.email : "")}
            onChange={(event) => updateField("email", event.target.value)}
          />

          <InputField
            id="register-phone"
            label="Phone number"
            icon="bi bi-phone"
            name="phoneNumber"
            type="tel"
            autoComplete="tel"
            placeholder="+91 9876543210"
            value={form.phoneNumber}
            error={errors.phoneNumber || (form.phoneNumber ? validationErrors.phoneNumber : "")}
            hint="Include country code for the most reliable verification flow."
            onChange={(event) => updateField("phoneNumber", event.target.value)}
          />

          <InputField
            id="register-age"
            label="Age"
            icon="bi bi-123"
            name="age"
            type="number"
            min="1"
            max="120"
            inputMode="numeric"
            placeholder="25"
            value={form.age}
            error={errors.age || (form.age ? validationErrors.age : "")}
            hint="Your age helps us filter eligible vaccination drives."
            onChange={(event) => updateField("age", event.target.value)}
          />

          <PasswordField
            id="register-password"
            label="Password"
            name="password"
            autoComplete="new-password"
            placeholder="Create a strong password"
            value={form.password}
            error={errors.password || (form.password ? validationErrors.password : "")}
            showPassword={showPassword}
            onToggle={() => setShowPassword((current) => !current)}
            onChange={(event) => updateField("password", event.target.value)}
          />

          <div className="auth-strength">
            <div className="auth-strength__meta">
              <span>Password strength</span>
              <strong>{passwordStrength.label}</strong>
            </div>
            <div className={`auth-strength__bar ${strengthToneClass}`}>
              <span style={{ width: `${Math.max(passwordStrength.score, 8)}%` }}></span>
            </div>
            <div className="auth-strength__checks">
              <span className={passwordStrength.checks.length ? "is-passed" : ""}>8+ chars</span>
              <span className={passwordStrength.checks.uppercase ? "is-passed" : ""}>Uppercase</span>
              <span className={passwordStrength.checks.lowercase ? "is-passed" : ""}>Lowercase</span>
              <span className={passwordStrength.checks.number ? "is-passed" : ""}>Number</span>
              <span className={passwordStrength.checks.special ? "is-passed" : ""}>Special</span>
            </div>
          </div>

          <PasswordField
            id="register-confirm-password"
            label="Confirm password"
            name="confirmPassword"
            autoComplete="new-password"
            placeholder="Re-enter your password"
            value={form.confirmPassword}
            error={errors.confirmPassword || (form.confirmPassword ? validationErrors.confirmPassword : "")}
            showPassword={showConfirmPassword}
            onToggle={() => setShowConfirmPassword((current) => !current)}
            onChange={(event) => updateField("confirmPassword", event.target.value)}
          />

          <label className={`auth-checkbox auth-checkbox--stacked ${errors.acceptedTerms ? "is-invalid" : ""}`}>
            <input
              type="checkbox"
              checked={form.acceptedTerms}
              onChange={(event) => updateField("acceptedTerms", event.target.checked)}
            />
            <span>
              I agree to the <Link to="/terms-conditions">Terms & Conditions</Link> and <Link to="/privacy-policy">Privacy Policy</Link>.
            </span>
          </label>
          {errors.acceptedTerms ? <p className="auth-field-error">{errors.acceptedTerms}</p> : null}

          <Button type="submit" loading={loading} loadingLabel="Creating account..." disabled={loading || hasValidationIssues}>
            <i className="bi bi-person-plus"></i>
            <span>Create account</span>
          </Button>
        </form>
      </FormContainer>
    </>
  );
}
