import React from "react";
import { Link } from "react-router-dom";

function BrandMark() {
  return (
    <div className="auth-brand-mark" aria-hidden="true">
      <div className="auth-brand-mark__pulse"></div>
      <div className="auth-brand-mark__ring"></div>
      <div className="auth-brand-mark__shield">
        <i className="bi bi-shield-check"></i>
      </div>
    </div>
  );
}

export default function FormContainer({
  eyebrow,
  title,
  description,
  helper,
  footer,
  children
}) {
  return (
    <section className="auth-page">
      <div className="auth-backdrop auth-backdrop--one"></div>
      <div className="auth-backdrop auth-backdrop--two"></div>
      <div className="container auth-page__container">
        <div className="auth-shell fade-in">
          <aside className="auth-shell__brand">
            <div className="auth-brand-orb auth-brand-orb--one" aria-hidden="true"></div>
            <div className="auth-brand-orb auth-brand-orb--two" aria-hidden="true"></div>

            <Link to="/" className="auth-brand-link">
              <BrandMark />
              <div>
                <div className="auth-brand-name">VaxZone</div>
                <div className="auth-brand-tag">Vaccination Operations Platform</div>
              </div>
            </Link>

            <div className="auth-brand-copy">
              <span className="auth-eyebrow">{eyebrow}</span>
              <h1>{title}</h1>
              <p>{description}</p>
            </div>

            <div className="auth-brand-metrics">
              <div className="auth-brand-metric">
                <strong>Fast</strong>
                <span>Instant feedback</span>
              </div>
              <div className="auth-brand-metric">
                <strong>Secure</strong>
                <span>Protected sessions</span>
              </div>
              <div className="auth-brand-metric">
                <strong>Clear</strong>
                <span>Low-friction flow</span>
              </div>
            </div>

            <div className="auth-brand-panels">
              <div className="auth-brand-panel">
                <span className="auth-brand-panel__icon"><i className="bi bi-stars"></i></span>
                <div>
                  <strong>Premium experience</strong>
                  <p>Fast validation, smooth interactions, and a focused dashboard-first journey.</p>
                </div>
              </div>
              <div className="auth-brand-panel">
                <span className="auth-brand-panel__icon"><i className="bi bi-shield-lock"></i></span>
                <div>
                  <strong>Secure by default</strong>
                  <p>JWT-based access, BCrypt hashing, and strict backend checks for every submission.</p>
                </div>
              </div>
            </div>
          </aside>

          <div className="auth-shell__form">
            <div className="auth-form-card">
              <div className="auth-form-card__glow" aria-hidden="true"></div>
              {helper ? <div className="auth-helper">{helper}</div> : null}
              {children}
              {footer ? <div className="auth-form-footer">{footer}</div> : null}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
