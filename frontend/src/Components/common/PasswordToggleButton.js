import React from 'react';
import './PasswordToggleButton.css';

const PasswordToggleButton = ({ 
  showPassword, 
  onClick, 
  disabled = false,
  className = ''
}) => {
  return (
    <button
      type="button"
      className={`password-toggle-btn ${className}`}
      onClick={onClick}
      disabled={disabled}
      aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
    >
      {showPassword ? 'Ẩn' : 'Hiện'}
    </button>
  );
};

export default PasswordToggleButton;