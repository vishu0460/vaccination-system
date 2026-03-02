import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Form, Button, Card, Alert, Spinner } from 'react-bootstrap';
import { authAPI } from '../api/axios';
import { useToast } from '../components/Toast';

const ForgotPassword = () => {
  const navigate = useNavigate();
  const toast = useToast();
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [step, setStep] = useState(1); // 1: email input, 2: reset form
  const [token, setToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const handleEmailSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await authAPI.forgotPassword({ email });
      setSuccess('Password reset link has been sent to your email');
      toast.success('Password reset link sent! Check your email.');
      setStep(2);
    } catch (err) {
      const errorMsg = err.response?.data?.message || 'Failed to send reset link';
      setError(errorMsg);
      toast.error(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const handlePasswordReset = async (e) => {
    e.preventDefault();
    setError('');
    
    if (newPassword !== confirmPassword) {
      const msg = 'Passwords do not match';
      setError(msg);
      toast.error(msg);
      return;
    }
    
    if (newPassword.length < 6) {
      const msg = 'Password must be at least 6 characters';
      setError(msg);
      toast.error(msg);
      return;
    }
    
    setLoading(true);
    try {
      await authAPI.resetPassword({ token, newPassword });
      setSuccess('Password reset successful! Redirecting to login...');
      toast.success('Password reset successful!');
      setTimeout(() => navigate('/login'), 2000);
    } catch (err) {
      const errorMsg = err.response?.data?.message || 'Failed to reset password';
      setError(errorMsg);
      toast.error(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="d-flex justify-content-center align-items-center min-vh-75 py-4 fade-in">
      <Card className="forgot-password-card shadow-lg" style={{ width: '450px', maxWidth: '100%' }}>
        <Card.Body className="p-4">
          <div className="text-center mb-4">
            <span style={{ fontSize: '3rem' }} role="img" aria-label="forgot-password">🔐</span>
            <h2 className="mt-3 mb-1">
              {step === 1 ? 'Forgot Password?' : 'Reset Password'}
            </h2>
            <p className="text-muted">
              {step === 1 
                ? 'Enter your email to receive a reset link' 
                : 'Enter the token from your email and new password'}
            </p>
          </div>
          
          {error && (
            <Alert variant="danger" className="d-flex align-items-center">
              <span className="me-2">⚠️</span>
              {error}
            </Alert>
          )}
          
          {success && (
            <Alert variant="success" className="d-flex align-items-center">
              <span className="me-2">✅</span>
              {success}
            </Alert>
          )}
          
          {step === 1 ? (
            <Form onSubmit={handleEmailSubmit}>
              <Form.Group className="mb-4">
                <Form.Label>Email Address</Form.Label>
                <div className="input-group">
                  <span className="input-group-text">📧</span>
                  <Form.Control
                    type="email"
                    placeholder="Enter your registered email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    aria-required="true"
                  />
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
                    Sending...
                  </>
                ) : (
                  'Send Reset Link'
                )}
              </Button>
            </Form>
          ) : (
            <Form onSubmit={handlePasswordReset}>
              <Form.Group className="mb-3">
                <Form.Label>Reset Token</Form.Label>
                <div className="input-group">
                  <span className="input-group-text">🔑</span>
                  <Form.Control
                    type="text"
                    placeholder="Enter the token from your email"
                    value={token}
                    onChange={(e) => setToken(e.target.value)}
                    required
                    aria-required="true"
                  />
                </div>
              </Form.Group>
              
              <Form.Group className="mb-3">
                <Form.Label>New Password</Form.Label>
                <div className="input-group">
                  <span className="input-group-text">🔒</span>
                  <Form.Control
                    type={showPassword ? 'text' : 'password'}
                    placeholder="Enter new password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    required
                    aria-required="true"
                  />
                  <Button 
                    variant="outline-secondary"
                    onClick={() => setShowPassword(!showPassword)}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    {showPassword ? '🙈' : '👁️'}
                  </Button>
                </div>
                <Form.Text className="text-muted">
                  Must be at least 6 characters
                </Form.Text>
              </Form.Group>
              
              <Form.Group className="mb-4">
                <Form.Label>Confirm Password</Form.Label>
                <div className="input-group">
                  <span className="input-group-text">🔒</span>
                  <Form.Control
                    type={showPassword ? 'text' : 'password'}
                    placeholder="Confirm new password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                    aria-required="true"
                    isInvalid={confirmPassword && newPassword !== confirmPassword}
                  />
                  <Form.Control.Feedback type="invalid">
                    Passwords do not match
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
                    Resetting...
                  </>
                ) : (
                  'Reset Password'
                )}
              </Button>
            </Form>
          )}
          
          <div className="text-center mt-4">
            <p className="mb-0">
              Remember your password?{' '}
              <Link to="/login" className="text-decoration-none fw-bold">
                Sign in
              </Link>
            </p>
          </div>
        </Card.Body>
      </Card>
    </div>
  );
};

export default ForgotPassword;
