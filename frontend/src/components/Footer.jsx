import React from "react";
import { Container, Row, Col } from "react-bootstrap";
import { Link } from "react-router-dom";
import { FaEnvelope, FaFacebookF, FaInstagram, FaLinkedinIn, FaMapMarkerAlt, FaPhone } from "react-icons/fa";

const companyLinks = [
  { to: "/drives", label: "Vaccination Drives" },
  { to: "/centers", label: "Vaccination Centers" },
  { to: "/news", label: "Health News" },
  { to: "/about", label: "About Us" },
  { to: "/verify/certificate", label: "Verify Certificate" }
];

const serviceLinks = [
  { to: "/user/bookings", label: "My Bookings" },
  { to: "/certificates", label: "My Certificates" },
  { to: "/my-feedback", label: "My Feedback" },
  { to: "/my-inquiries", label: "My Inquiries" },
  { to: "/profile", label: "My Profile" }
];

const supportLinks = [
  { to: "/admin/dashboard", label: "Dashboard" },
  { to: "/admin/dashboard", label: "Manage Centers" },
  { to: "/admin/dashboard", label: "Manage Drives" },
  { to: "/admin/dashboard", label: "Manage Users" },
  { to: "/contact", label: "Contact Support" }
];

const socialLinks = [
  { href: "https://www.linkedin.com", label: "LinkedIn", icon: <FaLinkedinIn /> },
  { href: "https://www.instagram.com", label: "Instagram", icon: <FaInstagram /> },
  { href: "https://www.facebook.com", label: "Facebook", icon: <FaFacebookF /> }
];

export default function Footer() {
  return (
    <footer className="site-footer">
      <Container>
        <div className="site-footer__top">
          <Row className="g-4 g-xl-5">
            <Col xl={3} lg={4} md={6}>
              <div className="site-footer__brand">
                <span className="site-footer__eyebrow">Healthcare Platform</span>
                <h5 className="site-footer__title">VaxZone</h5>
                <p className="site-footer__copy">
                  Book appointments, discover trusted vaccination centers, and manage digital records through one secure healthcare experience.
                </p>
              </div>
            </Col>

            <Col xl={2} lg={4} md={6}>
              <h6 className="site-footer__heading">Quick Links</h6>
              <ul className="site-footer__list list-unstyled">
                {companyLinks.map((item) => (
                  <li key={item.to}>
                    <Link to={item.to}>{item.label}</Link>
                  </li>
                ))}
              </ul>
            </Col>

            <Col xl={2} lg={4} md={6}>
              <h6 className="site-footer__heading">Services</h6>
              <ul className="site-footer__list list-unstyled">
                {serviceLinks.map((item) => (
                  <li key={item.to}>
                    <Link to={item.to}>{item.label}</Link>
                  </li>
                ))}
              </ul>
            </Col>

            <Col xl={2} lg={4} md={6}>
              <h6 className="site-footer__heading">Support</h6>
              <ul className="site-footer__list list-unstyled">
                {supportLinks.map((item) => (
                  <li key={`${item.to}-${item.label}`}>
                    <Link to={item.to}>{item.label}</Link>
                  </li>
                ))}
              </ul>
            </Col>

            <Col xl={3} lg={8} md={12}>
              <h6 className="site-footer__heading">Contact & Social</h6>
              <ul className="site-footer__contact list-unstyled">
                <li><FaPhone /> <a href="tel:+919631376436">+91 96313 76436</a></li>
                <li><FaEnvelope /> <a href="mailto:vaxzone.vaccine@gmail.com">vaxzone.vaccine@gmail.com</a></li>
                <li><FaMapMarkerAlt /> <span>India</span></li>
              </ul>
              <div className="site-footer__social">
                {socialLinks.map((item) => (
                  <a
                    key={item.label}
                    href={item.href}
                    target="_blank"
                    rel="noreferrer"
                    aria-label={item.label}
                    className="site-footer__social-link"
                  >
                    {item.icon}
                  </a>
                ))}
              </div>
            </Col>
          </Row>
        </div>

        <div className="site-footer__bottom">
          <p className="site-footer__copyright">{"\u00A9"} 2026 VaxZone. All rights reserved.</p>
          <div className="site-footer__legal">
            <Link to="/privacy-policy">Privacy Policy</Link>
            <Link to="/terms-conditions">Terms</Link>
            <Link to="/contact">Contact</Link>
          </div>
        </div>
      </Container>
    </footer>
  );
}
