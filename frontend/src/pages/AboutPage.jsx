import React from "react";
import { Link } from "react-router-dom";
import { Helmet } from "react-helmet-async";

export default function AboutPage() {
  return (
    <>
      <Helmet>
        <title>About Us - VaxZone</title>
        <meta name="description" content="Learn about VaxZone mission to make vaccination accessible to everyone through technology." />
        <meta property="og:title" content="About Us - VaxZone" />
        <meta property="og:description" content="Learn about our mission to make vaccination accessible to everyone." />
      </Helmet>

      <section className="page-header">
        <div className="container">
          <div className="row align-items-center">
            <div className="col-lg-8">
              <h1 className="mb-2">About Our Mission</h1>
              <p className="mb-0 opacity-75">
                Dedicated to making vaccination accessible, efficient, and stress-free for everyone
              </p>
            </div>
            <div className="col-lg-4 text-center text-lg-end mt-3 mt-lg-0">
              <i className="bi bi-heart-pulse display-1" style={{opacity: 0.3}}></i>
            </div>
          </div>
        </div>
      </section>

      <section className="py-5">
        <div className="container">
          <div className="row g-4">
            <div className="col-md-6">
              <div className="card h-100 border-0 shadow-sm p-4">
                <div className="icon-wrapper mb-3" style={{width: '60px', height: '60px'}}>
                  <i className="bi bi-bullseye"></i>
                </div>
                <h3 className="fw-bold mb-3">Our Mission</h3>
                <p className="text-muted">
                  To streamline the vaccination process by providing a secure, user-friendly platform 
                  that enables efficient scheduling, reduces wait times, and ensures equitable access 
                  to vaccines for all communities.
                </p>
              </div>
            </div>
            <div className="col-md-6">
              <div className="card h-100 border-0 shadow-sm p-4">
                <div className="icon-wrapper mb-3" style={{width: '60px', height: '60px'}}>
                  <i className="bi bi-eye"></i>
                </div>
                <h3 className="fw-bold mb-3">Our Vision</h3>
                <p className="text-muted">
                  To become the leading digital health platform for vaccination management, 
                  making it effortless for every person to get vaccinated on time.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="py-5 bg-light">
        <div className="container">
          <div className="text-center mb-5">
            <h2 className="fw-bold">Our Core Values</h2>
            <p className="text-muted">The principles that guide everything we do</p>
          </div>
          <div className="row g-4">
            <div className="col-md-3 text-center">
              <div className="card border-0 bg-transparent h-100">
                <div className="icon-wrapper mx-auto mb-3" style={{width: '70px', height: '70px'}}>
                  <i className="bi bi-shield-check"></i>
                </div>
                <h5 className="fw-bold">Trust & Security</h5>
                <p className="text-muted small">We prioritize the security and privacy of your health data</p>
              </div>
            </div>
            <div className="col-md-3 text-center">
              <div className="card border-0 bg-transparent h-100">
                <div className="icon-wrapper mx-auto mb-3" style={{width: '70px', height: '70px'}}>
                  <i className="bi bi-people"></i>
                </div>
                <h5 className="fw-bold">Accessibility</h5>
                <p className="text-muted small">We ensure everyone has equal access to vaccination services</p>
              </div>
            </div>
            <div className="col-md-3 text-center">
              <div className="card border-0 bg-transparent h-100">
                <div className="icon-wrapper mx-auto mb-3" style={{width: '70px', height: '70px'}}>
                  <i className="bi bi-lightning-charge"></i>
                </div>
                <h5 className="fw-bold">Efficiency</h5>
                <p className="text-muted small">We optimize every step of the vaccination process</p>
              </div>
            </div>
            <div className="col-md-3 text-center">
              <div className="card border-0 bg-transparent h-100">
                <div className="icon-wrapper mx-auto mb-3" style={{width: '70px', height: '70px'}}>
                  <i className="bi bi-heart"></i>
                </div>
                <h5 className="fw-bold">Compassion</h5>
                <p className="text-muted small">We put people first and design solutions with empathy</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="py-5">
        <div className="container">
          <div className="text-center mb-5">
            <h2 className="fw-bold">Meet Our Team</h2>
            <p className="text-muted">Dedicated professionals working towards a healthier future</p>
          </div>
          <div className="row g-4 justify-content-center">
            <div className="col-md-4 col-lg-3">
              <div className="card border-0 shadow-sm text-center p-4 h-100">
                <div className="icon-wrapper mx-auto mb-3" style={{width: '80px', height: '80px'}}>
                  <i className="bi bi-person display-5"></i>
                </div>
                <h5 className="fw-bold">Vishwajeet Kumar</h5>
                <p className="text-muted small mb-2">Project Lead & Developer</p>
                <div className="d-flex justify-content-center gap-3">
                  <a href="https://www.linkedin.com/in/vishwajeet-kumar-755b0a271/" target="_blank" rel="noopener noreferrer" className="text-primary"><i className="bi bi-linkedin fs-5"></i></a>
                  <a href="https://github.com" target="_blank" rel="noopener noreferrer" className="text-dark"><i className="bi bi-github fs-5"></i></a>
                </div>
              </div>
            </div>
            <div className="col-md-4 col-lg-3">
              <div className="card border-0 shadow-sm text-center p-4 h-100">
                <div className="icon-wrapper mx-auto mb-3 bg-success" style={{width: '80px', height: '80px'}}>
                  <i className="bi bi-person display-5"></i>
                </div>
                <h5 className="fw-bold">Healthcare Experts</h5>
                <p className="text-muted small mb-2">Medical Consultants</p>
                <span className="badge bg-success"><i className="bi bi-check-circle me-1"></i> Verified</span>
              </div>
            </div>
            <div className="col-md-4 col-lg-3">
              <div className="card border-0 shadow-sm text-center p-4 h-100">
                <div className="icon-wrapper mx-auto mb-3 bg-info" style={{width: '80px', height: '80px'}}>
                  <i className="bi bi-person display-5"></i>
                </div>
                <h5 className="fw-bold">Support Team</h5>
                <p className="text-muted small mb-2">Customer Care</p>
                <span className="badge bg-info"><i className="bi bi-clock me-1"></i> Always Available</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="py-5 bg-primary text-white">
        <div className="container">
          <div className="row g-4 text-center">
            <div className="col-6 col-md-3">
              <i className="bi bi-people display-4 mb-2 d-block"></i>
              <h3 className="fw-bold">50,000+</h3>
              <p className="mb-0">Users Registered</p>
            </div>
            <div className="col-6 col-md-3">
              <i className="bi bi-calendar-check display-4 mb-2 d-block"></i>
              <h3 className="fw-bold">100,000+</h3>
              <p className="mb-0">Bookings Completed</p>
            </div>
            <div className="col-6 col-md-3">
              <i className="bi bi-building display-4 mb-2 d-block"></i>
              <h3 className="fw-bold">500+</h3>
              <p className="mb-0">Vaccination Centers</p>
            </div>
            <div className="col-6 col-md-3">
              <i className="bi bi-geo-alt display-4 mb-2 d-block"></i>
              <h3 className="fw-bold">50+</h3>
              <p className="mb-0">Cities Covered</p>
            </div>
          </div>
        </div>
      </section>

      <section className="py-5">
        <div className="container text-center">
          <h3 className="fw-bold mb-3">Join Us in Our Mission</h3>
          <p className="text-muted mb-4" style={{maxWidth: '600px', margin: '0 auto 1.5rem'}}>
            Whether you're a healthcare provider or an individual seeking to book your vaccination slot, we've got you covered.
          </p>
          <div className="d-flex gap-3 justify-content-center flex-wrap">
            <Link to="/register" className="btn btn-primary btn-lg">
              <i className="bi bi-person-plus me-2"></i>Register Now
            </Link>
            <Link to="/contact" className="btn btn-outline-primary btn-lg">
              <i className="bi bi-envelope me-2"></i>Contact Us
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}

