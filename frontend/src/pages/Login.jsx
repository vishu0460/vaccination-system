import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Form, Button, Card, Alert, Spinner } from 'react-bootstrap';
import { authAPI } from '../api/axios';
import { useToast } from '../components/Toast';

const Login = () => {
  const navigate = useNavigate();
  const toast = useToast();
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  // Validate form fields
  const validateForm = () => {
    const newErrors = {};
    
    if (!formData.email) {
      newErrors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      newErrors.email = 'Please enter a valid email address';
    }
    
    if (!formData.password) {
      newErrors.password = 'Password is required';
    } else if (formData.password.length < 6) {
      newErrors.password = 'Password must be at least 6 characters';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
    // Clear error when user starts typing
    if (errors[name]) {
      setErrors({ ...errors, [name]: '' });
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }
    
    setLoading(true);
    try {
      const { data } = await authAPI.login(formData);
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify(data.user));
      toast.success('Welcome back! Login successful.');
      navigate('/dashboard');
    } catch (err) {
      const errorMsg = err.response?.data?.message || 'Login failed. Please check your credentials.';
      setErrors({ form: errorMsg });
      toast.error(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="d-flex justify-content-center align-items-center min-vh-75 fade-in">
      <Card className="login-card shadow-lg" style={{ width: '450px', maxWidth: '100%' }}>
        <Card.Body className="p-4">
          <div className="text-center mb-4">
            <span style={{ fontSize: '3rem' }} role="img" aria-label="lock">🔐</span>
            <h2 className="mt-3 mb-1">Welcome Back!</h2>
            <p className="text-muted">Sign in to your account</p>
          </div>
          
          {errors.form && (
            <Alert variant="danger" className="d-flex align-items-center">
              <span className="me-2">⚠️</span>
              {errors.form}
            </Alert>
          )}
          
          <Form onSubmit={handleSubmit} noValidate>
            <Form.Group className="mb-3">
              <Form.Label>Email Address</Form.Label>
              <div className="input-group">
                <span className="input-group-text">📧</span>
                <Form.Control
                  type="email"
                  name="email"
                  placeholder="Enter your email"
                  value={formData.email}
                  onChange={handleChange}
                  isInvalid={!!errors.email}
                  required
                  autoComplete="email"
                  aria-describedby="emailHelp"
                />
                <Form.Control.Feedback type="invalid">
                  {errors.email}
                </Form.Control.Feedback>
              </div>
              <Form.Text id="emailHelp" className="text-muted" muted>
                We'll never share your email with anyone else.
              </Form.Text>
            </Form.Group>
            
            <Form.Group className="mb-4">
              <Form.Label>Password</Form.Label>
              <div className="input-group">
                <span className="input-group-text">🔑</span>
                <Form.Control
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  placeholder="Enter your password"
                  value={formData.password}
                  onChange={handleChange}
                  isInvalid={!!errors.password}
                  required
                  autoComplete="current-password"
                />
                <Button 
                  variant="outline-secondary"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? '🙈' : '👁️'}
                </Button>
                <Form.Control.Feedback type="invalid">
                  {errors.password}
                </Form.Control.Feedback>
              </div>
            </Form.Group>
            
            <Button 
              variant="primary" 
              type="submit" 
              className="w-100 py-2"
              disabled={loading}
            >
              {loading ? (
                <>
                  <Spinner animation="border" size="sm" className="me-2" />
                  Signing in...
                </>
              ) : (
                'Sign In'
              )}
            </Button>
          </Form>
          
          <div className="text-center mt-4">
            <p className="mb-0">
              Don't have an account?{' '}
              <Link to="/register" className="text-decoration-none fw-bold">
                Register here
              </Link>
            </p>
            <p className="mt-2">
              <Link to="/forgot-password" className="text-decoration-none">
                Forgot Password?
              </Link>
            </p>
          </div>
          
          <hr className="my-4" />
          
          <div className="text-center text-muted">
            <small>
              <strong>Demo credentials:</strong><br/>
              Admin: admin@vaccine.com / admin123<br/>
              User: user@vaccine.com / user123
            </small>
          </div>
        </Card.Body>
      </Card>
    </div>
  );
};

export default Login;
