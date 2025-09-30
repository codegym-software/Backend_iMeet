import React, { useState } from 'react';
import PasswordToggleButton from './PasswordToggleButton';
import './PasswordInput.css';

const PasswordInput = ({
  name,
  placeholder = "Nhập mật khẩu",
  value,
  onChange,
  className = '',
  required = false,
  disabled = false,
  error = false,
  ...props
}) => {
  const [showPassword, setShowPassword] = useState(false);

  const togglePassword = () => {
    setShowPassword(!showPassword);
  };

  return (
    <div className="password-input-wrapper">
      <input
        type={showPassword ? "text" : "password"}
        name={name}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        className={`password-input ${error ? 'error' : ''} ${className}`}
        required={required}
        disabled={disabled}
        {...props}
      />
      <PasswordToggleButton
        showPassword={showPassword}
        onClick={togglePassword}
        disabled={disabled}
      />
    </div>
  );
};

export default PasswordInput;