import React, { useState, useEffect } from 'react';
import { Toast as BSToast, ToastContainer } from 'react-bootstrap';

// Toast context for global toast access
export const ToastContext = React.createContext();

export const useToast = () => {
  const context = React.useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
};

export const ToastProvider = ({ children }) => {
  const [toasts, setToasts] = useState([]);

  const addToast = (message, type = 'info', duration = 4000) => {
    const id = Date.now() + Math.random();
    setToasts((prev) => [...prev, { id, message, type, duration }]);
  };

  const removeToast = (id) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id));
  };

  const success = (message, duration) => addToast(message, 'success', duration);
  const error = (message, duration) => addToast(message, 'danger', duration);
  const warning = (message, duration) => addToast(message, 'warning', duration);
  const info = (message, duration) => addToast(message, 'info', duration);

  return (
    <ToastContext.Provider value={{ addToast, removeToast, success, error, warning, info }}>
      {children}
      <ToastContainer position="top-end" className="p-3" style={{ zIndex: 9999 }}>
        {toasts.map((toast) => (
          <BSToast
            key={toast.id}
            onClose={() => removeToast(toast.id)}
            delay={toast.duration}
            autohide
            bg={toast.type}
          >
            <BSToast.Header>
              <strong className="me-auto">
                {toast.type === 'success' && '✅ '}
                {toast.type === 'danger' && '❌ '}
                {toast.type === 'warning' && '⚠️ '}
                {toast.type === 'info' && 'ℹ️ '}
                {toast.type.charAt(0).toUpperCase() + toast.type.slice(1)}
              </strong>
            </BSToast.Header>
            <BSToast.Body className={toast.type === 'danger' ? 'text-white' : ''}>
              {toast.message}
            </BSToast.Body>
          </BSToast>
        ))}
      </ToastContainer>
    </ToastContext.Provider>
  );
};

// Individual toast component with animation
const Toast = ({ message, type, onClose }) => {
  const [show, setShow] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setShow(false);
      setTimeout(onClose, 300);
    }, 4000);

    return () => clearTimeout(timer);
  }, [onClose]);

  const bgClass = {
    success: 'bg-success',
    danger: 'bg-danger',
    warning: 'bg-warning',
    info: 'bg-info'
  }[type] || 'bg-info';

  return (
    <div 
      className={`toast-item ${show ? 'show' : 'hide'} ${bgClass}`}
      role="alert"
      aria-live="assertive"
      aria-atomic="true"
    >
      <div className="toast-content">
        <span className="toast-icon">
          {type === 'success' && '✓'}
          {type === 'danger' && '✕'}
          {type === 'warning' && '⚠'}
          {type === 'info' && 'ℹ'}
        </span>
        <span className="toast-message">{message}</span>
        <button 
          type="button" 
          className="toast-close" 
          onClick={() => {
            setShow(false);
            setTimeout(onClose, 300);
          }}
          aria-label="Close"
        >
          ×
        </button>
      </div>
    </div>
  );
};

export default Toast;
