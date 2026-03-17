import React from 'react';
import { Container, Row, Col, Card } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { FaSyringe, FaPhone, FaEnvelope, FaMapMarkerAlt, FaClock, FaShieldAlt, FaUserShield, FaHeartbeat } from 'react-icons/fa';

export default function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="footer bg-gradient">
      <Container fluid className="px-4">
        {/* Main Footer Content */}
        <Row className="py-5">
          {/* Brand Section */}
          <Col lg={4} md={6} className="mb-4 mb-lg-0">
            <div className="d-flex align-items-center mb-3">
              <div className="footer-logo me-2">
                <FaSyringe size={28} />
              </div>
              <div>
                <h5 className="mb-0 fw-bold">VaxZone</h5>
                <small className="text-muted">Smart Vaccination Platform</small>
              </div>
            </div>
            <p className="text-muted small mb-3">
              Your trusted platform for vaccination bookings, health updates, and certificate management. 
              Stay protected, stay healthy.
            </p>
            <div className="d-flex gap-3">
              <span className="footer-badge bg-success-light text-success">
                <FaShieldAlt className="me-1" /> Secure
              </span>
              <span className="footer-badge bg-primary-light text-primary">
                <FaHeartbeat className="me-1" /> Trusted
              </span>
            </div>
          </Col>

          {/* Quick Links */}
          <Col lg={2} md={6} className="mb-4 mb-lg-0">
            <h6 className="footer-heading">Quick Links</h6>
            <ul className="footer-links list-unstyled">
              <li><Link to="/drives"><FaSyringe className="me-2" />Vaccination Drives</Link></li>
              <li><Link to="/centers"><FaMapMarkerAlt className="me-2" />Vaccination Centers</Link></li>
              <li><Link to="/news"><FaClock className="me-2" />Health News</Link></li>
              <li><Link to="/about"><FaUserShield className="me-2" />About Us</Link></li>
              <li><Link to="/verify/certificate"><FaShieldAlt className="me-2" />Verify Certificate</Link></li>
            </ul>
          </Col>

          {/* User Services */}
          <Col lg={2} md={6} className="mb-4 mb-lg-0">
            <h6 className="footer-heading">My Services</h6>
            <ul className="footer-links list-unstyled">
              <li><Link to="/user/bookings">My Bookings</Link></li>
              <li><Link to="/certificates">My Certificates</Link></li>
              <li><Link to="/my-feedback">My Feedback</Link></li>
              <li><Link to="/my-inquiries">My Inquiries</Link></li>
              <li><Link to="/profile">My Profile</Link></li>
            </ul>
          </Col>

          {/* Admin Section */}
          <Col lg={2} md={6} className="mb-4 mb-lg-0">
            <h6 className="footer-heading">Administration</h6>
            <ul className="footer-links list-unstyled">
              <li><Link to="/admin/dashboard">Dashboard</Link></li>
              <li><Link to="/admin/dashboard">Manage Centers</Link></li>
              <li><Link to="/admin/dashboard">Manage Drives</Link></li>
              <li><Link to="/admin/dashboard">Manage Users</Link></li>
            </ul>
          </Col>

          {/* Contact Info */}
          <Col lg={2} md={6} className="mb-4 mb-lg-0">
            <h6 className="footer-heading">Contact</h6>
            <ul className="footer-contact list-unstyled">
              <li><FaPhone className="me-2" />9631376436</li>
              <li><FaEnvelope className="me-2" />vaxzone.vaccine@gmail.com</li>
              <li><FaMapMarkerAlt className="me-2" />India</li>
              <li><FaClock className="me-2" />Mon-Sat: 9AM-6PM</li>
            </ul>
          </Col>
        </Row>

        {/* Divider */}
        <hr className="footer-divider" />

        {/* Bottom Footer */}
        <Row className="py-3">
          <Col md={6} className="mb-3 mb-md-0">
            <div className="d-flex align-items-center">
              <small className="text-muted">
                © {currentYear} VaxZone. All rights reserved.
              </small>
            </div>
          </Col>
          <Col md={6} className="text-md-end">
            <div className="footer-legal">
              <Link to="/privacy-policy" className="text-muted me-3">Privacy Policy</Link>
              <Link to="/terms-conditions" className="text-muted me-3">Terms of Service</Link>
              <Link to="/contact" className="text-muted">Contact Us</Link>
            </div>
          </Col>
        </Row>
      </Container>

      {/* Custom Styles */}
      <style>{`
        .footer {
          background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
          color: #fff;
        }
        .footer-logo {
          width: 50px;
          height: 50px;
          background: rgba(255, 255, 255, 0.1);
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #4ecdc4;
        }
        .footer-heading {
          color: #4ecdc4;
          font-weight: 600;
          text-transform: uppercase;
          font-size: 0.85rem;
          letter-spacing: 0.5px;
          margin-bottom: 1rem;
        }
        .footer-links li {
          margin-bottom: 0.6rem;
        }
        .footer-links a {
          color: #b8b8b8;
          text-decoration: none;
          transition: all 0.2s ease;
          font-size: 0.9rem;
        }
        .footer-links a:hover {
          color: #4ecdc4;
          padding-left: 5px;
        }
        .footer-contact li {
          color: #b8b8b8;
          margin-bottom: 0.6rem;
          font-size: 0.9rem;
        }
        .footer-badge {
          padding: 4px 10px;
          border-radius: 20px;
          font-size: 0.75rem;
          font-weight: 500;
        }
        .bg-success-light { background-color: rgba(40, 167, 69, 0.2); }
        .bg-primary-light { background-color: rgba(0, 123, 255, 0.2); }
        .footer-divider {
          border-color: rgba(255, 255, 255, 0.1);
        }
        .footer-legal a {
          font-size: 0.85rem;
        }
        .footer-legal a:hover {
          color: #4ecdc4 !important;
        }
      `}</style>
    </footer>
  );
}
