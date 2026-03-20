import React, { Suspense, lazy } from "react";
import { Route, Routes } from "react-router-dom";
import ProtectedRoute from "../components/ProtectedRoute";
import PageErrorBoundary from "../components/PageErrorBoundary";

const HomePage = lazy(() => import("../pages/HomePage"));
const DrivesPage = lazy(() => import("../pages/DrivesPage"));
const CentersPage = lazy(() => import("../pages/CentersPage"));
const AboutPage = lazy(() => import("../pages/AboutPage"));
const ContactPage = lazy(() => import("../pages/ContactPage"));
const LoginPage = lazy(() => import("../pages/LoginPage"));
const RegisterPage = lazy(() => import("../pages/RegisterPage"));
const ForgotPasswordPage = lazy(() => import("../pages/ForgotPasswordPage"));
const ResetPasswordPage = lazy(() => import("../pages/ResetPasswordPage"));
const AdminDashboardPage = lazy(() => import("../pages/AdminDashboardPage"));
const UserBookingsPage = lazy(() => import("../pages/UserBookingsPage"));
const NotFoundPage = lazy(() => import("../pages/NotFoundPage"));
const PrivacyPolicyPage = lazy(() => import("../pages/PrivacyPolicyPage"));
const TermsConditionsPage = lazy(() => import("../pages/TermsConditionsPage"));
const CopyrightPage = lazy(() => import("../pages/CopyrightPage"));
const ContactLegalPage = lazy(() => import("../pages/ContactLegalPage"));
const ProfilePage = lazy(() => import("../pages/ProfilePage"));
const FeedbackPage = lazy(() => import("../pages/FeedbackPage"));
const NewsPage = lazy(() => import("../pages/NewsPage"));
const CertificatePage = lazy(() => import("../pages/CertificatePage"));
const CenterDetailPage = lazy(() => import("../pages/CenterDetailPage"));
const VerifyEmailPage = lazy(() => import("../pages/VerifyEmailPage"));
const CookiePolicyPage = lazy(() => import("../pages/CookiePolicyPage"));
const DisclaimerPage = lazy(() => import("../pages/DisclaimerPage"));
const MyFeedbackPage = lazy(() => import("../pages/MyFeedbackPage"));
const MyContactPage = lazy(() => import("../pages/MyContactPage"));
const VerifyCertificatePage = lazy(() => import("../pages/VerifyCertificatePage"));

function RouteLoader() {
  return (
    <div className="d-flex justify-content-center align-items-center" style={{ minHeight: "40vh" }}>
      <div className="spinner-border text-primary" role="status">
        <span className="visually-hidden">Loading...</span>
      </div>
    </div>
  );
}

export default function AppRoutes() {
  return (
    <Suspense fallback={<RouteLoader />}>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/drives" element={<DrivesPage />} />
        <Route path="/centers" element={<CentersPage />} />
        <Route path="/centers/:id" element={<CenterDetailPage />} />
        <Route path="/news" element={<NewsPage />} />
        <Route path="/about" element={<AboutPage />} />
        <Route path="/contact" element={<ContactPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/verify/certificate" element={<VerifyCertificatePage />} />
        <Route path="/verify/certificate/:certNumber" element={<VerifyCertificatePage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/feedback" element={<FeedbackPage />} />

        <Route path="/privacy-policy" element={<PrivacyPolicyPage />} />
        <Route path="/terms-conditions" element={<TermsConditionsPage />} />
        <Route path="/copyright" element={<CopyrightPage />} />
        <Route path="/contact-legal" element={<ContactLegalPage />} />
        <Route path="/cookie-policy" element={<CookiePolicyPage />} />
        <Route path="/disclaimer" element={<DisclaimerPage />} />

        <Route
          path="/user/bookings"
          element={(
            <ProtectedRoute roles={["USER", "ADMIN", "SUPER_ADMIN"]}>
              <UserBookingsPage />
            </ProtectedRoute>
          )}
        />
        <Route
          path="/profile"
          element={(
            <ProtectedRoute roles={["USER", "ADMIN", "SUPER_ADMIN"]}>
              <ProfilePage />
            </ProtectedRoute>
          )}
        />
        <Route
          path="/certificates"
          element={(
            <ProtectedRoute roles={["USER", "ADMIN", "SUPER_ADMIN"]}>
              <CertificatePage />
            </ProtectedRoute>
          )}
        />
        <Route
          path="/my-feedback"
          element={(
            <ProtectedRoute roles={["USER", "ADMIN", "SUPER_ADMIN"]}>
              <MyFeedbackPage />
            </ProtectedRoute>
          )}
        />
        <Route
          path="/my-inquiries"
          element={(
            <ProtectedRoute roles={["USER", "ADMIN", "SUPER_ADMIN"]}>
              <MyContactPage />
            </ProtectedRoute>
          )}
        />

        <Route
          path="/admin/dashboard"
          element={(
            <ProtectedRoute roles={["ADMIN", "SUPER_ADMIN"]}>
              <PageErrorBoundary resetKey="/admin/dashboard">
                <AdminDashboardPage />
              </PageErrorBoundary>
            </ProtectedRoute>
          )}
        />

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
}
