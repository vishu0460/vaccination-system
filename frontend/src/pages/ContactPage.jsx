import React, { useState } from "react";
import { Helmet } from "react-helmet-async";
import { contactAPI } from "../api/client";

export default function ContactPage() {
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    phone: "",
    subject: "",
    message: ""
  });
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    
    if (!formData.name || !formData.name.trim()) {
      setLoading(false);
      setError("Name is required");
      return;
    }
    if (!formData.email || !formData.email.trim()) {
      setLoading(false);
      setError("Email is required");
      return;
    }
    if (!formData.subject) {
      setLoading(false);
      setError("Please select a subject");
      return;
    }
    if (!formData.message || !formData.message.trim()) {
      setLoading(false);
      setError("Message is required");
      return;
    }
    
    try {
      const response = await contactAPI.submitContact(formData);
      if (response.status === 200 || response.status === 201) {
        setLoading(false);
        setSubmitted(true);
        setFormData({ name: "", email: "", phone: "", subject: "", message: "" });
      }
    } catch (err) {
      setLoading(false);
      console.error("Contact submission error:", err);
      if (err.response) {
        const status = err.response.status;
        const data = err.response.data;
        if (status === 400) {
          const errorMsg = typeof data?.message === 'string' ? data.message : 
                           data?.errors ? Object.values(data.errors).flat().join(', ') : "Invalid form data";
          setError(errorMsg || "Invalid form data. Please check your inputs.");
        } else if (status === 401) {
          setError("Please log in to submit a contact form.");
        } else if (status === 500) {
          setError("Server error. Please try again later.");
        } else {
          setError(data?.message || "Failed to send message. Please try again.");
        }
      } else if (err.request) {
        setError("Network error. Please check your internet connection.");
      } else {
        setError("Failed to send message. Please try again.");
      }
    }
  };

  return (
    <>
      <Helmet>
        <title>Contact Us - VaxZone</title>
        <meta name="description" content="Get in touch with VaxZone support team. We're here to help with vaccination booking and scheduling." />
        <meta property="og:title" content="Contact Us - VaxZone" />
        <meta property="og:description" content="Get in touch with our support team." />
      </Helmet>

      <section className="page-header">
        <div className="container">
          <div className="row align-items-center">
            <div className="col-lg-8">
              <h1 className="mb-2">Contact Us</h1>
              <p className="mb-0 opacity-75">Have questions? We're here to help!</p>
            </div>
            <div className="col-lg-4 text-center text-lg-end mt-3 mt-lg-0">
              <i className="bi bi-headset display-1" style={{opacity: 0.3}}></i>
            </div>
          </div>
        </div>
      </section>

      <div className="container py-5">
        <div className="row g-5">
          <div className="col-lg-7">
            <div className="card border-0 shadow-sm p-4">
              <h3 className="fw-bold mb-4">Send us a Message</h3>
              {submitted ? (
                <div className="text-center py-4">
                  <div className="text-success mb-3">
                    <i className="bi bi-check-circle display-1"></i>
                  </div>
                  <h4 className="fw-bold">Thank You!</h4>
                  <p className="text-muted">Your message has been sent successfully. We'll get back to you within 24-48 hours.</p>
                  <button className="btn btn-primary" onClick={() => setSubmitted(false)}>Send Another Message</button>
                </div>
              ) : (
                <form onSubmit={handleSubmit}>
                  <div className="row g-3">
                    <div className="col-md-6">
                      <label htmlFor="name" className="form-label">Your Name *</label>
                      <input type="text" className="form-control" id="name" name="name" value={formData.name} onChange={handleChange} required placeholder="John Doe" />
                    </div>
                    <div className="col-md-6">
                      <label htmlFor="email" className="form-label">Email Address *</label>
                      <input type="email" className="form-control" id="email" name="email" value={formData.email} onChange={handleChange} required placeholder="john@example.com" />
                    </div>
                    <div className="col-md-6">
                      <label htmlFor="phone" className="form-label">Phone Number</label>
                      <input type="tel" className="form-control" id="phone" name="phone" value={formData.phone} onChange={handleChange} placeholder="+91 9631376436" />
                    </div>
                    <div className="col-12">
                      <label htmlFor="subject" className="form-label">Subject *</label>
                      <select className="form-select" id="subject" name="subject" value={formData.subject} onChange={handleChange} required>
                        <option value="">Select a topic</option>
                        <option value="general">General Inquiry</option>
                        <option value="booking">Booking Assistance</option>
                        <option value="technical">Technical Support</option>
                        <option value="feedback">Feedback</option>
                        <option value="partnership">Partnership</option>
                        <option value="other">Other</option>
                      </select>
                    </div>
                    <div className="col-12">
                      <label htmlFor="message" className="form-label">Message *</label>
                      <textarea className="form-control" id="message" name="message" rows="5" value={formData.message} onChange={handleChange} required placeholder="How can we help you?"></textarea>
                    </div>
                    <div className="col-12">
                      {error && <div className="alert alert-danger">{error}</div>}
                      <button type="submit" className="btn btn-primary btn-lg w-100" disabled={loading}>
                        {loading ? (<><span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Sending...</>) : (<><i className="bi bi-send me-2"></i> Send Message</>)}
                      </button>
                    </div>
                  </div>
                </form>
              )}
            </div>
          </div>

          <div className="col-lg-5">
            <div className="card border-0 shadow-sm p-4 mb-4">
              <h3 className="fw-bold mb-4">Get in Touch</h3>
              <div className="d-flex align-items-start mb-4">
                <div className="icon-wrapper me-3" style={{width: '50px', height: '50px'}}><i className="bi bi-envelope"></i></div>
                <div>
                  <h6 className="fw-bold mb-1">Email</h6>
                  <p className="text-muted small mb-0">vaxzone.vaccine@gmail.com</p>
                  <p className="text-muted small mb-0">We reply within 24-48 hours</p>
                </div>
              </div>
              <div className="d-flex align-items-start mb-4">
                <div className="icon-wrapper me-3" style={{width: '50px', height: '50px'}}><i className="bi bi-telephone"></i></div>
                <div>
                  <h6 className="fw-bold mb-1">Phone</h6>
                  <p className="text-muted small mb-0">+91 9631376436</p>
                  <p className="text-muted small mb-0">Mon - Sat, 9AM - 6PM</p>
                </div>
              </div>
              <div className="d-flex align-items-start mb-4">
                <div className="icon-wrapper me-3" style={{width: '50px', height: '50px'}}><i className="bi bi-geo-alt"></i></div>
                <div>
                  <h6 className="fw-bold mb-1">Location</h6>
                  <p className="text-muted small mb-0">India</p>
                </div>
              </div>
              <div className="d-flex align-items-start">
                <div className="icon-wrapper me-3" style={{width: '50px', height: '50px'}}><i className="bi bi-clock"></i></div>
                <div>
                  <h6 className="fw-bold mb-1">Support Hours</h6>
                  <p className="text-muted small mb-0">Monday - Saturday: 9AM - 6PM</p>
                </div>
              </div>
            </div>

            <div className="card border-0 shadow-sm p-4">
              <h3 className="fw-bold mb-3">Follow Us</h3>
              <p className="text-muted small mb-4">Stay connected with VaxZone on social media for updates and health tips.</p>
              <div className="d-flex gap-2">
                <a href="https://www.facebook.com/profile.php?id=100086218892613" target="_blank" rel="noopener noreferrer" className="btn btn-outline-primary"><i className="bi bi-facebook"></i></a>
                <a href="https://x.com/Thevishu133" target="_blank" rel="noopener noreferrer" className="btn btn-outline-primary"><i className="bi bi-twitter-x"></i></a>
                <a href="https://www.instagram.com/the_vishu.7" target="_blank" rel="noopener noreferrer" className="btn btn-outline-primary"><i className="bi bi-instagram"></i></a>
                <a href="https://www.linkedin.com/in/vishwajeet-kumar-755b0a271/" target="_blank" rel="noopener noreferrer" className="btn btn-outline-primary"><i className="bi bi-linkedin"></i></a>
              </div>
            </div>

            <div className="card border-0 shadow-sm p-4 mt-4" style={{background: '#fff3cd'}}>
              <div className="d-flex align-items-start">
                <div className="text-warning me-3"><i className="bi bi-exclamation-triangle display-6"></i></div>
                <div>
                  <h6 className="fw-bold text-warning">Emergency Notice</h6>
                  <p className="small mb-0">For medical emergencies, please call your local emergency number (102/108 in India) immediately. Our platform is for vaccination scheduling only.</p>
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
                  <p className="text-muted small mb-0">Register an account, navigate to the Drives page, select your preferred drive, and book an available slot.</p>
                </div>
              </div>
            </div>
            <div className="col-md-6">
              <div className="card border-0 shadow-sm">
                <div className="card-body">
                  <h6 className="fw-bold"><i className="bi bi-question-circle text-primary me-2"></i>Can I cancel or reschedule my booking?</h6>
                  <p className="text-muted small mb-0">Yes, go to My Bookings section and you can cancel or reschedule your appointment up to 24 hours before.</p>
                </div>
              </div>
            </div>
            <div className="col-md-6">
              <div className="card border-0 shadow-sm">
                <div className="card-body">
                  <h6 className="fw-bold"><i className="bi bi-question-circle text-primary me-2"></i>What documents do I need for vaccination?</h6>
                  <p className="text-muted small mb-0">Bring a valid photo ID and your booking confirmation.</p>
                </div>
              </div>
            </div>
            <div className="col-md-6">
              <div className="card border-0 shadow-sm">
                <div className="card-body">
                  <h6 className="fw-bold"><i className="bi bi-question-circle text-primary me-2"></i>Is the vaccination free?</h6>
                  <p className="text-muted small mb-0">It depends on the drive. Some drives are free while others may have a nominal fee.</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
