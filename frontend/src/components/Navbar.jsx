import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Navbar as BSNavbar, Nav, Container, Button } from 'react-bootstrap';
import { useTheme } from '../context/ThemeContext';

const Navbar = () => {
  const navigate = useNavigate();
  const { isDarkMode, toggleTheme } = useTheme();
  const token = localStorage.getItem('token');
  const user = JSON.parse(localStorage.getItem('user') || '{}');

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  const isAdmin = user.roles?.some(r => r.name === 'ADMIN' || r.name === 'SUPER_ADMIN');

  return (
    <BSNavbar bg={isDarkMode ? 'dark' : 'primary'} variant={isDarkMode ? 'dark' : 'dark'} expand="lg" className="mb-4 theme-navbar">
      <Container>
        <BSNavbar.Brand as={Link} to="/" className="d-flex align-items-center">
          <span className="me-2">💉</span>
          <span className="brand-text">Vaccination System</span>
        </BSNavbar.Brand>
        <BSNavbar.Toggle aria-controls="basic-navbar-nav" />
        <BSNavbar.Collapse id="basic-navbar-nav">
          <Nav className="me-auto">
            <Nav.Link as={Link} to="/" className="nav-link-hover">
              <span className="me-1">🏠</span>Home
            </Nav.Link>
            <Nav.Link as={Link} to="/drives" className="nav-link-hover">
              <span className="me-1">📅</span>Drives
            </Nav.Link>
            <Nav.Link as={Link} to="/centers" className="nav-link-hover">
              <span className="me-1">🏥</span>Centers
            </Nav.Link>
            {token && (
              <>
                <Nav.Link as={Link} to="/dashboard" className="nav-link-hover">
                  <span className="me-1">📊</span>Dashboard
                </Nav.Link>
                <Nav.Link as={Link} to="/my-bookings" className="nav-link-hover">
                  <span className="me-1">📋</span>My Bookings
                </Nav.Link>
                {isAdmin && (
                  <Nav.Link as={Link} to="/admin" className="nav-link-hover">
                    <span className="me-1">⚙️</span>Admin
                  </Nav.Link>
                )}
              </>
            )}
          </Nav>
          <Nav className="align-items-center">
            <Button 
              variant={isDarkMode ? 'outline-light' : 'outline-light'} 
              size="sm"
              onClick={toggleTheme}
              className="me-3 theme-toggle"
              title={isDarkMode ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
            >
              {isDarkMode ? '☀️' : '🌙'}
            </Button>
            {token ? (
              <Button variant="outline-light" onClick={handleLogout} className="btn-hover">
                <span className="me-1">🚪</span>Logout
              </Button>
            ) : (
              <>
                <Nav.Link as={Link} to="/login" className="nav-link-hover">
                  <span className="me-1">🔑</span>Login
                </Nav.Link>
                <Nav.Link as={Link} to="/register" className="nav-link-hover">
                  <span className="me-1">📝</span>Register
                </Nav.Link>
              </>
            )}
          </Nav>
        </BSNavbar.Collapse>
      </Container>
    </BSNavbar>
  );
};

export default Navbar;
