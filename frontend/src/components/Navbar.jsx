import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Navbar as BSNavbar, Nav, Container, Button, Dropdown } from 'react-bootstrap';
import { useTheme } from '../context/ThemeContext';

const Navbar = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { isDarkMode, toggleTheme } = useTheme();
  const token = localStorage.getItem('token');
  const user = JSON.parse(localStorage.getItem('user') || '{}');

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  const isAdmin = user.roles?.some(r => r.name === 'ADMIN' || r.name === 'SUPER_ADMIN');

  // Get user initials for avatar
  const getUserInitials = () => {
    if (user.firstName && user.lastName) {
      return `${user.firstName[0]}${user.lastName[0]}`.toUpperCase();
    }
    if (user.email) {
      return user.email[0].toUpperCase();
    }
    return 'U';
  };

  // Check if current path is active
  const isActive = (path) => location.pathname === path;

  return (
    <BSNavbar bg={isDarkMode ? 'dark' : 'primary'} variant={isDarkMode ? 'dark' : 'dark'} expand="lg" className="mb-0 theme-navbar">
      <Container>
        <BSNavbar.Brand as={Link} to="/" className="d-flex align-items-center">
          <span className="me-2" role="img" aria-label="vaccine">💉</span>
          <span className="brand-text">Vaccination System</span>
        </BSNavbar.Brand>
        <BSNavbar.Toggle aria-controls="basic-navbar-nav" />
        <BSNavbar.Collapse id="basic-navbar-nav">
          <Nav className="me-auto">
            <Nav.Link 
              as={Link} 
              to="/" 
              className={`nav-link-hover ${isActive('/') ? 'active' : ''}`}
            >
              <span className="me-1">🏠</span>Home
            </Nav.Link>
            <Nav.Link 
              as={Link} 
              to="/drives" 
              className={`nav-link-hover ${isActive('/drives') ? 'active' : ''}`}
            >
              <span className="me-1">📅</span>Drives
            </Nav.Link>
            <Nav.Link 
              as={Link} 
              to="/centers" 
              className={`nav-link-hover ${isActive('/centers') ? 'active' : ''}`}
            >
              <span className="me-1">🏥</span>Centers
            </Nav.Link>
            {token && (
              <>
                <Nav.Link 
                  as={Link} 
                  to="/dashboard" 
                  className={`nav-link-hover ${isActive('/dashboard') ? 'active' : ''}`}
                >
                  <span className="me-1">📊</span>Dashboard
                </Nav.Link>
                <Nav.Link 
                  as={Link} 
                  to="/my-bookings" 
                  className={`nav-link-hover ${isActive('/my-bookings') ? 'active' : ''}`}
                >
                  <span className="me-1">📋</span>My Bookings
                </Nav.Link>
                {isAdmin && (
                  <Nav.Link 
                    as={Link} 
                    to="/admin" 
                    className={`nav-link-hover ${isActive('/admin') ? 'active' : ''}`}
                  >
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
              aria-label={isDarkMode ? 'Switch to light mode' : 'Switch to dark mode'}
            >
              {isDarkMode ? '☀️' : '🌙'}
            </Button>
            {token ? (
              <Dropdown align="end">
                <Dropdown.Toggle 
                  variant="outline-light" 
                  id="user-dropdown"
                  className="d-flex align-items-center p-1"
                  style={{ borderRadius: '50px' }}
                >
                  <div className="user-avatar me-2">
                    {getUserInitials()}
                  </div>
                  <span className="d-none d-md-inline">
                    {user.firstName || 'User'}
                  </span>
                </Dropdown.Toggle>
                <Dropdown.Menu>
                  <Dropdown.Header>
                    <strong>{user.firstName} {user.lastName}</strong>
                    <br />
                    <small className="text-muted">{user.email}</small>
                  </Dropdown.Header>
                  <Dropdown.Divider />
                  <Dropdown.Item as={Link} to="/dashboard">
                    <span className="me-2">📊</span>Dashboard
                  </Dropdown.Item>
                  <Dropdown.Item as={Link} to="/my-bookings">
                    <span className="me-2">📋</span>My Bookings
                  </Dropdown.Item>
                  <Dropdown.Divider />
                  <Dropdown.Item onClick={handleLogout} className="text-danger">
                    <span className="me-2">🚪</span>Logout
                  </Dropdown.Item>
                </Dropdown.Menu>
              </Dropdown>
            ) : (
              <>
                <Nav.Link 
                  as={Link} 
                  to="/login" 
                  className={`nav-link-hover ${isActive('/login') ? 'active' : ''}`}
                >
                  <span className="me-1">🔑</span>Login
                </Nav.Link>
                <Nav.Link 
                  as={Link} 
                  to="/register" 
                  className={`nav-link-hover ${isActive('/register') ? 'active' : ''}`}
                >
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
