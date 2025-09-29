import React, { useState } from 'react';
import { Link, useHistory } from 'react-router-dom'; // Thêm useHistory
import { useAuth } from '../contexts/AuthContext'; // Thêm useAuth
import './Signup.css';
import ImgAsset from '../public';

export default function Signup() {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    fullName: '',
    password: '',
    confirmPassword: ''
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [passwordError, setPasswordError] = useState('');
  const [signupStatus, setSignupStatus] = useState({ message: '', type: '' });
  const [isLoading, setIsLoading] = useState(false);

  const { signup } = useAuth(); // Sử dụng useAuth
  const history = useHistory(); // Sử dụng useHistory

  const validatePassword = (password) => {
    const minLength = password.length >= 8;
    const hasUpperCase = /[A-Z]/.test(password);
    const hasLowerCase = /[a-z]/.test(password);
    const hasNumber = /[0-9]/.test(password);
    const hasSpecialChar = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password);

    if (!minLength) return 'Mật khẩu phải có ít nhất 8 ký tự';
    if (!hasUpperCase) return 'Mật khẩu phải có ít nhất 1 ký tự viết hoa';
    if (!hasLowerCase) return 'Mật khẩu phải có ít nhất 1 ký tự viết thường';
    if (!hasNumber) return 'Mật khẩu phải có ít nhất 1 số';
    if (!hasSpecialChar) return 'Mật khẩu phải có ít nhất 1 ký tự đặc biệt';
    return '';
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
    
    if (name === 'password') {
      setPasswordError(validatePassword(value));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Kiểm tra mật khẩu mạnh
    const passwordValidationError = validatePassword(formData.password);
    if (passwordValidationError) {
      setPasswordError(passwordValidationError);
      return;
    }
    
    if (formData.password !== formData.confirmPassword) {
      setSignupStatus({ message: "Mật khẩu không khớp!", type: 'error' });
      return;
    }
    
    setIsLoading(true);
    setSignupStatus({ message: '', type: '' });

    try {
      const result = await signup(formData.username, formData.email, formData.password, formData.fullName);
      
      if (result.success) {
        setSignupStatus({ message: result.message, type: 'success' });
        // Chuyển hướng đến trang login sau khi đăng ký thành công
        setTimeout(() => {
          history.push('/login');
        }, 2000);
      } else {
        setSignupStatus({ message: result.message, type: 'error' });
      }
    } catch (error) {
      setSignupStatus({ message: 'Có lỗi xảy ra khi đăng ký!', type: 'error' });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="signup">
      {/* Left side */}
      <div className="signup-container">
        <div className="signup-box">
          <div className="signup-header">
            <h2 className="signup-title">Sign Up</h2>
            <p className="signup-subtext">Create your account</p>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="input-group">
              <label className="input-label" htmlFor="username">Username</label>
              <input
                id="username"
                type="text"
                name="username"
                className="input-field"
                placeholder="Enter username"
                value={formData.username}
                onChange={handleChange}
                disabled={isLoading}
                required
              />
            </div>

            <div className="input-group">
              <label className="input-label" htmlFor="email">Email</label>
              <input
                id="email"
                type="email"
                name="email"
                className="input-field"
                placeholder="Enter email"
                value={formData.email}
                onChange={handleChange}
                required
              />
            </div>

            <div className="input-group">
              <label className="input-label" htmlFor="fullName">Full Name</label>
              <input
                id="fullName"
                type="text"
                name="fullName"
                className="input-field"
                placeholder="Enter your full name"
                value={formData.fullName}
                onChange={handleChange}
                required
              />
            </div>

            <div className="input-group">
              <label className="input-label" htmlFor="password">Password</label>
              <div className="password-input-container">
                <input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  name="password"
                  className="input-field"
                  placeholder="Enter password"
                  value={formData.password}
                  onChange={handleChange}
                  required
                />
                <button 
                  type="button" 
                  className="password-toggle" 
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? "Ẩn" : "Hiện"}
                </button>
              </div>
              {passwordError && <div className="password-error">{passwordError}</div>}
              <div className="password-requirements">
                <p>Mật khẩu phải có:</p>
                <ul>
                  <li className={formData.password.length >= 8 ? "valid" : "invalid"}>Ít nhất 8 ký tự</li>
                  <li className={/[A-Z]/.test(formData.password) ? "valid" : "invalid"}>Ít nhất 1 ký tự viết hoa</li>
                  <li className={/[a-z]/.test(formData.password) ? "valid" : "invalid"}>Ít nhất 1 ký tự viết thường</li>
                  <li className={/[0-9]/.test(formData.password) ? "valid" : "invalid"}>Ít nhất 1 số</li>
                  <li className={/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(formData.password) ? "valid" : "invalid"}>Ít nhất 1 ký tự đặc biệt</li>
                </ul>
              </div>
            </div>

            <div className="input-group">
              <label className="input-label" htmlFor="confirmPassword">Confirm Password</label>
              <div className="password-input-container">
                <input
                  id="confirmPassword"
                  type={showConfirmPassword ? "text" : "password"}
                  name="confirmPassword"
                  className="input-field"
                  placeholder="Re-enter password"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  required
                />
                <button 
                  type="button" 
                  className="password-toggle" 
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                >
                  {showConfirmPassword ? "Ẩn" : "Hiện"}
                </button>
              </div>
            </div>
            
            {signupStatus.message && (
              <div className={`status-message ${signupStatus.type}`}>
                {signupStatus.message}
              </div>
            )}

            {/* Text dưới confirm password */}
            <p className="signup-text">
              Have an account? <Link to="/login" className="signup-link">Log in</Link>
            </p>

            <div className="signup-button">
              <button 
                type="submit" 
                className="input-field"
                disabled={isLoading}
              >
                {isLoading ? 'Đang đăng ký...' : 'Sign Up'}
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Right side */}
      <img
        className="signup-image"
        src={ImgAsset.Signup_chrislee70l1tDAI6rMunsplash1}
        alt="Signup background"
      />
    </div>
  );
}
