import React, { useMemo, useState } from "react";
import { Helmet } from "react-helmet-async";
import { contactAPI, getErrorMessage } from "../api/client";

const INITIAL_FORM = {
  name: "",
  email: "",
  message: ""
};

const notifyDataUpdated = () => {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent("vaxzone:data-updated"));
  }
};

export default function ContactPage() {
  const [formData, setFormData] = useState(INITIAL_FORM);
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const contactHighlights = useMemo(() => ([
    {
      icon: "bi-envelope-paper",
      title: "Fast Support",
      copy: "Questions about bookings, certificates, or scheduling are routed directly to the support queue."
    },
    {
      icon: "bi-clock-history",
      title: "Saved Automatically",
      copy: "Each inquiry is stored in the system so admins and signed-in users can track updates reliably."
    },
    {
      icon: "bi-shield-check",
      title: "Professional Handling",
      copy: "Messages are reviewed securely and answered by the admin team responsible for support."
    }
  ]), []);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((current) => ({ ...current, [name]: value }));
    setError("");
    setSuccessMessage("");
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    setSuccessMessage("");

    if (!formData.name.trim()) {
      setLoading(false);
      setError("Name is required");
      return;
    }

    if (!formData.email.trim()) {
      setLoading(false);
      setError("Email is required");
      return;
    }

    if (!formData.message.trim()) {
      setLoading(false);
      setError("Message is required");
      return;
    }

    try {
      const response = await contactAPI.submitContact({
        ...formData,
        subject: "General inquiry"
      });

      if (response.status === 200 || response.status === 201) {
        setSubmitted(true);
        setSuccessMessage("Your message has been sent successfully. The admin team can now see it immediately.");
        setFormData(INITIAL_FORM);
        notifyDataUpdated();
      }
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Failed to send message. Please try again."));
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Helmet>
        <title>Contact Support - VaxZone</title>
        <meta name="description" content="Reach the VaxZone support team for help with bookings, certificates, and vaccination scheduling." />
        <meta property="og:title" content="Contact Support - VaxZone" />
        <meta property="og:description" content="Send a support inquiry to the VaxZone team." />
      </Helmet>

      <section className="page-header">
        <div className="container">
          <div className="row align-items-center">
            <div className="col-lg-7">
              <h1 className="mb-2">Contact Support</h1>
              <p className="mb-0 opacity-75">Send a message to the VaxZone team and get help with bookings, certificates, or general platform questions.</p>
            </div>
            <div className="col-lg-5 text-center text-lg-end mt-3 mt-lg-0">
              <i className="bi bi-headset display-1" style={{ opacity: 0.3 }}></i>
            </div>
          </div>
        </div>
      </section>

      <div className="container py-5">
        <div className="row justify-content-center g-4">
          <div className="col-xl-10">
            <div className="row g-4">
              <div className="col-lg-5">
                <div className="card border-0 shadow-sm h-100 p-4">
                  <span className="badge bg-primary-subtle text-primary px-3 py-2 mb-3">Support Desk</span>
                  <h3 className="fw-bold mb-2">We are here to help</h3>
                  <p className="text-muted mb-4">Use this form to reach the admin support queue. Your inquiry is saved in the database and reflected across the dashboard automatically.</p>

                  <div className="d-grid gap-3">
                    {contactHighlights.map((item) => (
                      <div key={item.title} className="d-flex align-items-start gap-3 border rounded-4 p-3 bg-light-subtle">
                        <div className="icon-wrapper flex-shrink-0" style={{ width: "3rem", height: "3rem" }}>
                          <i className={`bi ${item.icon}`}></i>
                        </div>
                        <div>
                          <h6 className="fw-bold mb-1">{item.title}</h6>
                          <p className="text-muted small mb-0">{item.copy}</p>
                        </div>
                      </div>
                    ))}
                  </div>

                  <div className="border rounded-4 p-3 mt-4 bg-white">
                    <h6 className="fw-bold mb-2">Support Details</h6>
                    <p className="text-muted small mb-1">Email: vaxzone.vaccine@gmail.com</p>
                    <p className="text-muted small mb-1">Hours: Monday to Saturday, 9:00 AM to 6:00 PM</p>
                    <p className="text-muted small mb-0">For emergencies, contact your local medical helpline directly.</p>
                  </div>
                </div>
              </div>

              <div className="col-lg-7">
                <div className="card border-0 shadow-sm h-100 p-4 p-lg-5">
                  <div className="d-flex justify-content-between align-items-start mb-4">
                    <div>
                      <h3 className="fw-bold mb-1">Send a Message</h3>
                      <p className="text-muted mb-0">A clear message helps us respond faster.</p>
                    </div>
                    <span className="badge rounded-pill text-bg-light">Secure Form</span>
                  </div>

                  {successMessage ? (
                    <div className="alert alert-success border-0">
                      <i className="bi bi-check-circle me-2"></i>{successMessage}
                    </div>
                  ) : null}

                  {error ? (
                    <div className="alert alert-danger border-0">
                      <i className="bi bi-exclamation-circle me-2"></i>{error}
                    </div>
                  ) : null}

                  {submitted ? (
                    <div className="text-center py-4">
                      <div className="text-success mb-3">
                        <i className="bi bi-check-circle display-4"></i>
                      </div>
                      <h4 className="fw-bold">Message Sent</h4>
                      <p className="text-muted">Your inquiry is stored and visible to the admin team now.</p>
                      <button
                        className="btn btn-primary px-4"
                        onClick={() => {
                          setSubmitted(false);
                          setSuccessMessage("");
                        }}
                      >
                        Send Another Message
                      </button>
                    </div>
                  ) : (
                    <form onSubmit={handleSubmit} noValidate>
                      <div className="row g-3">
                        <div className="col-md-6">
                          <label htmlFor="name" className="form-label fw-semibold">Name</label>
                          <input
                            id="name"
                            name="name"
                            type="text"
                            className="form-control form-control-lg"
                            value={formData.name}
                            onChange={handleChange}
                            placeholder="Enter your full name"
                            required
                          />
                        </div>
                        <div className="col-md-6">
                          <label htmlFor="email" className="form-label fw-semibold">Email</label>
                          <input
                            id="email"
                            name="email"
                            type="email"
                            className="form-control form-control-lg"
                            value={formData.email}
                            onChange={handleChange}
                            placeholder="Enter your email address"
                            required
                          />
                        </div>
                        <div className="col-12">
                          <label htmlFor="message" className="form-label fw-semibold">Message</label>
                          <textarea
                            id="message"
                            name="message"
                            className="form-control"
                            rows="7"
                            value={formData.message}
                            onChange={handleChange}
                            placeholder="Describe your question or issue"
                            required
                          ></textarea>
                        </div>
                        <div className="col-12 d-grid">
                          <button type="submit" className="btn btn-primary btn-lg" disabled={loading}>
                            {loading ? (
                              <><span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Sending message...</>
                            ) : (
                              <><i className="bi bi-send me-2"></i>Submit Inquiry</>
                            )}
                          </button>
                        </div>
                      </div>
                    </form>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="mt-5">
          <h3 className="fw-bold mb-4 text-center">Frequently Asked Questions</h3>
          <div className="row g-4">
            <div className="col-md-6">
              <div className="card border-0 shadow-sm">
                <div className="card-body">
                  <h6 className="fw-bold"><i className="bi bi-question-circle text-primary me-2"></i>How do I book a vaccination slot?</h6>
                  <p className="text-muted small mb-0">Register an account, open the Drives page, choose your preferred drive, and confirm an available slot.</p>
                </div>
              </div>
            </div>
            <div className="col-md-6">
              <div className="card border-0 shadow-sm">
                <div className="card-body">
                  <h6 className="fw-bold"><i className="bi bi-question-circle text-primary me-2"></i>Can I cancel or reschedule my booking?</h6>
                  <p className="text-muted small mb-0">Yes. Visit My Bookings to cancel or reschedule an appointment, subject to the active booking rules.</p>
                </div>
              </div>
            </div>
            <div className="col-md-6">
              <div className="card border-0 shadow-sm">
                <div className="card-body">
                  <h6 className="fw-bold"><i className="bi bi-question-circle text-primary me-2"></i>What documents do I need for vaccination?</h6>
                  <p className="text-muted small mb-0">Bring a valid photo ID and your booking confirmation details when you visit the vaccination center.</p>
                </div>
              </div>
            </div>
            <div className="col-md-6">
              <div className="card border-0 shadow-sm">
                <div className="card-body">
                  <h6 className="fw-bold"><i className="bi bi-question-circle text-primary me-2"></i>Will my support message be saved?</h6>
                  <p className="text-muted small mb-0">Yes. Contact inquiries are stored in the database and remain visible after restarts and page refreshes.</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
