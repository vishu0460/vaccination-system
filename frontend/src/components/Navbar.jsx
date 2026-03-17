import React, { useState, useEffect } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { isAuthenticated, getRole, clearAuth } from "../utils/auth";
import ThemeToggle from "./ThemeToggle";

export default function Navbar() {
  const navigate = useNavigate();
  const location = useLocation();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [role, setRole] = useState(null);
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    const checkAuth = () => {
      setIsLoggedIn(isAuthenticated());
      setRole(getRole());
    };
    checkAuth();

    const handleScroll = () => {
      setScrolled(window.scrollY > 10);
    };

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, [location]);

  const handleLogout = () => {
    clearAuth();
    setIsLoggedIn(false);
    setRole(null);
    navigate("/");
  };

  const isActive = (path) => location.pathname === path;

  return (
    <nav className={`navbar navbar-expand-lg navbar-light bg-white ${scrolled ? "shadow-sm" : ""}`}>
      <div className="container">
        <Link className="navbar-brand d-flex align-items-center" to="/">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="32"
            height="32"
            viewBox="0 0 40 40"
            fill="none"
            className="me-2"
          >
            <defs>
              <linearGradient id="navShieldGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style={{ stopColor: "#1E88E5" }} />
                <stop offset="100%" style={{ stopColor: "#1565C0" }} />
              </linearGradient>
            </defs>

            <path
              d="M20 4 L36 12 L36 24 C36 34 28 40 20 42 C12 40 4 34 4 24 L4 12 Z"
              fill="url(#navShieldGrad)"
            />

            <path
              d="M20 10 L30 15 L30 22 C30 29 25 33 20 35 C15 33 10 29 10 22 L10 15 Z"
              fill="#fff"
              opacity="0.2"
            />

            <path
              d="M16 20 L14 28 M20 18 L24 26 M24 20 L26 28"
              stroke="#43A047"
              strokeWidth="2"
              strokeLinecap="round"
            />

            <path
              d="M20 8 L20 14 M16 10 L20 14 L24 10"
              stroke="#fff"
              strokeWidth="1.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>

          <span className="fw-bold" style={{ color: "#1E88E5", fontSize: "1.4rem" }}>
            VaxZone
          </span>
        </Link>

        <div className="d-flex align-items-center gap-2">
          <ThemeToggle />

          <button
            className="navbar-toggler"
            type="button"
            onClick={() => setMenuOpen(!menuOpen)}
            aria-controls="navbarNav"
            aria-expanded={menuOpen}
            aria-label="Toggle navigation"
          >
            <span className="navbar-toggler-icon"></span>
          </button>
        </div>

        <div className={`collapse navbar-collapse ${menuOpen ? "show" : ""}`} id="navbarNav">
          <ul className="navbar-nav ms-auto align-items-lg-center">
            <li className="nav-item">
              <Link className={`nav-link ${isActive("/") ? "active" : ""}`} to="/" onClick={() => setMenuOpen(false)}>
                Home
              </Link>
            </li>

            <li className="nav-item">
              <Link className={`nav-link ${isActive("/drives") ? "active" : ""}`} to="/drives" onClick={() => setMenuOpen(false)}>
                Drives
              </Link>
            </li>

            <li className="nav-item">
              <Link className={`nav-link ${isActive("/centers") ? "active" : ""}`} to="/centers" onClick={() => setMenuOpen(false)}>
                Centers
              </Link>
            </li>

            <li className="nav-item">
              <Link className={`nav-link ${isActive("/news") ? "active" : ""}`} to="/news" onClick={() => setMenuOpen(false)}>
                News
              </Link>
            </li>

            <li className="nav-item dropdown">
              <a className="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown">
                More
              </a>

              <ul className="dropdown-menu">
                <li><Link className="dropdown-item" to="/about" onClick={() => setMenuOpen(false)}>About</Link></li>
                <li><Link className="dropdown-item" to="/contact" onClick={() => setMenuOpen(false)}>Contact</Link></li>
                <li><Link className="dropdown-item" to="/feedback" onClick={() => setMenuOpen(false)}>Feedback</Link></li>
                <li><hr className="dropdown-divider" /></li>
                <li><Link className="dropdown-item" to="/privacy-policy" onClick={() => setMenuOpen(false)}>Privacy Policy</Link></li>
                <li><Link className="dropdown-item" to="/terms-conditions" onClick={() => setMenuOpen(false)}>Terms</Link></li>
              </ul>
            </li>

            {isLoggedIn ? (
              <li className="nav-item dropdown ms-lg-3">
                <a className="nav-link dropdown-toggle btn btn-primary text-white px-3" href="#" role="button" data-bs-toggle="dropdown">
                  My Account
                </a>

                <ul className="dropdown-menu dropdown-menu-end">
                  {role === "ADMIN" || role === "SUPER_ADMIN" ? (
                    <li>
                      <Link className="dropdown-item" to="/admin/dashboard" onClick={() => setMenuOpen(false)}>
                        Dashboard
                      </Link>
                    </li>
                  ) : (
                    <>
                      <li>
                        <Link className="dropdown-item" to="/user/bookings" onClick={() => setMenuOpen(false)}>
                          My Bookings
                        </Link>
                      </li>

                      <li>
                        <Link className="dropdown-item" to="/certificates" onClick={() => setMenuOpen(false)}>
                          Certificates
                        </Link>
                      </li>

                      <li>
                        <Link className="dropdown-item" to="/profile" onClick={() => setMenuOpen(false)}>
                          Profile
                        </Link>
                      </li>
                    </>
                  )}

                  <li><hr className="dropdown-divider" /></li>

                  <li>
                    <button className="dropdown-item text-danger" onClick={handleLogout}>
                      Logout
                    </button>
                  </li>
                </ul>
              </li>
            ) : (
              <li className="nav-item ms-lg-3">
                <div className="d-flex gap-2">
                  <Link className="btn btn-outline-primary btn-sm" to="/login" onClick={() => setMenuOpen(false)}>
                    Login
                  </Link>

                  <Link className="btn btn-primary btn-sm" to="/register" onClick={() => setMenuOpen(false)}>
                    Register
                  </Link>
                </div>
              </li>
            )}
          </ul>
        </div>
      </div>
    </nav>
  );
}