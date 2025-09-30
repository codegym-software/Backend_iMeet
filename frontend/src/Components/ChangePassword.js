import React, { useState, useRef, useEffect } from 'react';
import { useHistory } from 'react-router-dom';
import { FaRegCalendarAlt, FaCog } from 'react-icons/fa';
import authService from '../services/authService';
import './ChangePassword.css';
import './common/PasswordToggleStyles.css';

export default function ChangePassword({ onBack, onSave, onCancel }) {
  const history = useHistory();
  // const { user } = useAuth(); // Remove this if useAuth is not available
  const isMountedRef = useRef(true);
  
  // State compatible with new UI but keeping backend logic
  const [form, setForm] = useState({
    currentPassword: '',
    newPassword: '',
    rePassword: ''
  });
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  // Password visibility states for new UI
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showRe, setShowRe] = useState(false);
  
  // Keep original backend states but map from new UI
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [passwordMessage, setPasswordMessage] = useState('');
  
  // State cho hiện/ẩn mật khẩu (for backend compatibility)
  const [showPasswords, setShowPasswords] = useState({
    currentPassword: false,
    newPassword: false,
    confirmPassword: false
  });
  
  // Cleanup effect
  useEffect(() => {
    return () => {
      isMountedRef.current = false;
    };
  }, []);
  
  // Password strength state
  const [passwordStrength, setPasswordStrength] = useState({
    hasUpper: false,
    hasLower: false,
    hasNumber: false,
    hasSpecial: false,
    hasLength: false,
    level: 'weak',
    score: 0,
    text: 'Yếu'
  });
  
  // Lấy thông tin user
  const getUserData = () => {
    try {
      const userData = localStorage.getItem('user');
      if (userData) return JSON.parse(userData);
      
      const oauth2Data = localStorage.getItem('oauth2User');
      if (oauth2Data) return JSON.parse(oauth2Data);
      
      return null;
    } catch (error) {
      console.error('Error getting user data:', error);
      return null;
    }
  };
  
  // Check password strength
  const checkPasswordStrength = (password) => {
    const checks = {
      hasUpper: /[A-Z]/.test(password),
      hasLower: /[a-z]/.test(password),
      hasNumber: /[0-9]/.test(password),
      hasSpecial: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password),
      hasLength: password.length >= 8
    };
    
    const score = Object.values(checks).filter(Boolean).length;
    let level = 'weak';
    let text = 'Yếu';
    let scorePercentage = (score / 5) * 100;
    
    if (score >= 5) {
      level = 'strong';
      text = 'Mạnh';
    } else if (score >= 3) {
      level = 'medium';
      text = 'Trung bình';
    }
    
    return {
      ...checks,
      level,
      score: scorePercentage,
      text
    };
  };

  // New UI handler but sync with backend state
  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm({ ...form, [name]: value });
    setError(''); // Clear error message
    
    // Sync with backend state
    const backendName = name === 'rePassword' ? 'confirmPassword' : name;
    setPasswordData(prev => ({
      ...prev,
      [backendName]: value
    }));
    setPasswordMessage(''); // Clear backend message when user types
    
    // Real-time password strength check
    if (name === 'newPassword') {
      setPasswordStrength(checkPasswordStrength(value));
    }
  };
  
  // Password validation function for new UI
  function isPasswordValid(pw) {
    return /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/.test(pw);
  }
  
  const isMatch = form.newPassword === form.rePassword;
  const canSave = isPasswordValid(form.newPassword) && isMatch;
  
  const togglePasswordVisibility = (field) => {
    setShowPasswords(prev => ({
      ...prev,
      [field]: !prev[field]
    }));
  };
  
  const handleBack = () => {
    history.goBack();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');
    setPasswordMessage('');
    
    // Use passwordData as primary source (since form inputs are bound to it)
    const currentPassword = passwordData.currentPassword;
    const newPassword = passwordData.newPassword;
    const confirmPassword = passwordData.confirmPassword;
    
    // Basic validation
    if (!currentPassword.trim()) {
      setPasswordMessage('Vui lòng nhập mật khẩu hiện tại');
      setError('Vui lòng nhập mật khẩu hiện tại');
      setIsLoading(false);
      return;
    }
    
    if (!newPassword.trim()) {
      setPasswordMessage('Vui lòng nhập mật khẩu mới');
      setError('Vui lòng nhập mật khẩu mới');
      setIsLoading(false);
      return;
    }
    
    // Password strength validation
    const regex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;
    if (!regex.test(newPassword)) {
      const errorMsg = 'Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.';
      setError(errorMsg);
      setPasswordMessage(errorMsg);
      setIsLoading(false);
      return;
    }
    
    if (!confirmPassword.trim()) {
      setPasswordMessage('Vui lòng xác nhận mật khẩu mới');
      setError('Vui lòng xác nhận mật khẩu mới');
      setIsLoading(false);
      return;
    }
    
    if (newPassword !== confirmPassword) {
      const errorMsg = 'Mật khẩu xác nhận không khớp';
      setError(errorMsg);
      setPasswordMessage(errorMsg);
      setIsLoading(false);
      return;
    }
    
    // Call onSave if provided (for UI compatibility)
    if (onSave) {
      onSave({
        currentPassword,
        newPassword,
        rePassword: confirmPassword
      });
    }
    
    // Kiểm tra user có đăng nhập qua traditional login không
    const userData = getUserData();
    if (userData && userData.authType === 'cognito-oauth2-server') {
      setPasswordMessage('Tài khoản OAuth2 không thể đổi mật khẩu tại đây');
      setIsLoading(false);
      return;
    }

    try {
      setPasswordMessage('Đang đổi mật khẩu...');
      const response = await authService.changePassword(
        currentPassword,
        newPassword,
        confirmPassword
      );
      
      if (!isMountedRef.current) return;
      
      if (response.success) {
        setPasswordMessage('Đổi mật khẩu thành công!');
        setError('');
        
        // Reset both form states
        setPasswordData({
          currentPassword: '',
          newPassword: '',
          confirmPassword: ''
        });
        
        setForm({
          currentPassword: '',
          newPassword: '',
          rePassword: ''
        });
        
        // Reset password strength
        setPasswordStrength({
          hasLength: false,
          hasUpper: false,
          hasLower: false,
          hasNumber: false,
          hasSpecial: false,
          score: 0,
          level: 'weak',
          text: 'Yếu'
        });
        
        // Handle UI callbacks
        if (onBack) {
          setTimeout(() => {
            if (isMountedRef.current) {
              onBack(); // Go back to profile
            }
          }, 2000);
        } else if (onCancel) {
          setTimeout(() => {
            if (isMountedRef.current) {
              onCancel(); // Close modal/component
            }
          }, 2000);
        } else {
          setTimeout(() => {
            if (isMountedRef.current) {
              history.push('/profile');
            }
          }, 2000);
        }
      } else {
        const errorMsg = response.message || 'Đổi mật khẩu thất bại';
        setError(errorMsg);
        setPasswordMessage(errorMsg);
      }
    } catch (error) {
      if (isMountedRef.current) {
        const errorMsg = error.message || 'Đổi mật khẩu thất bại';
        setError(errorMsg);
        setPasswordMessage(errorMsg);
      }
    } finally {
      if (isMountedRef.current) {
        setIsLoading(false);
      }
    }
  };

  return (
    <div className="change-password-main">
      {/* Header */}
      <div className="change-password-header">
        {/* Left side with back button, logo and title */}
        <div className="change-password-header-left">
          {/* Back button */}
          <div className="change-password-back-button" onClick={onBack || handleBack}>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M19 12H5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              <path d="M12 19L5 12L12 5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </div>
          
          {/* Logo and title */}
          <div className="change-password-header-brand">
            <div className="change-password-header-logo-wrapper">
              <img 
                src="/logo.jpg" 
                alt="iMeet Logo" 
                className="change-password-header-logo" 
              />
            </div>
            <span className="change-password-header-title">iMeet</span>
          </div>
        </div>
        
        {/* Right side with avatar and settings */}
        <div className="change-password-header-right">
          {/* Avatar */}
          <div className="change-password-avatar-container">
            <img 
              className="change-password-avatar" 
              src="https://placehold.co/180x180" 
              alt="User Avatar" 
            />
          </div>
          
          {/* Settings button */}
          <div className="change-password-settings-button">
            <FaCog className="change-password-settings-icon" />
          </div>
        </div>
      </div>
      
      <div className="change-password-container">
        <div className="change-password-card">
          <h2 className="change-password-title">Change Password</h2>
          <form className="change-password-form" onSubmit={handleSubmit}>
            <div className="input-group">
              <label className="input-label" htmlFor="current-password">Current Password</label>
              <div className="password-input-container">
                <input
                  id="current-password"
                  type={showCurrent ? "text" : "password"}
                  name="currentPassword"
                  className="input-field"
                  placeholder="Enter current password"
                  value={form.currentPassword}
                  onChange={handleChange}
                  required
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowCurrent(v => !v)}
                >
                  {showCurrent ? "Ẩn" : "Hiện"}
                </button>
              </div>
            </div>
            
            <div className="input-group">
              <label className="input-label" htmlFor="new-password">New Password</label>
              <div className="password-input-container">
                <input
                  id="new-password"
                  type={showNew ? "text" : "password"}
                  name="newPassword"
                  className="input-field"
                  placeholder="Enter new password"
                  value={form.newPassword}
                  onChange={handleChange}
                  required
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowNew(v => !v)}
                >
                  {showNew ? "Ẩn" : "Hiện"}
                </button>
              </div>
              {error && <div className="password-error">{error}</div>}
              <div className="password-requirements">
                <p>Mật khẩu phải có:</p>
                  <ul>
                    <li className={form.newPassword.length >= 8 ? "valid" : "invalid"}>Ít nhất 8 ký tự</li>
                    <li className={/[A-Z]/.test(form.newPassword) ? "valid" : "invalid"}>Ít nhất 1 ký tự viết hoa</li>
                    <li className={/[a-z]/.test(form.newPassword) ? "valid" : "invalid"}>Ít nhất 1 ký tự viết thường</li>
                    <li className={/[0-9]/.test(form.newPassword) ? "valid" : "invalid"}>Ít nhất 1 số</li>
                    <li className={/[!@#$%^&*()_+\-=\[\]{};':"\|,.<>\/?]/.test(form.newPassword) ? "valid" : "invalid"}>Ít nhất 1 ký tự đặc biệt</li>
                  </ul>
              </div>
            </div>
            
            <div className="input-group">
              <label className="input-label" htmlFor="rePassword">Re-enter Password</label>
              <div className="password-input-container">
                <input
                  id="rePassword"
                  type={showRe ? "text" : "password"}
                  name="rePassword"
                  className="input-field"
                  placeholder="Re-enter new password"
                  value={form.rePassword}
                  onChange={handleChange}
                  required
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowRe(v => !v)}
                >
                  {showRe ? "Ẩn" : "Hiện"}
                </button>
              </div>
            </div>
            
            {/* Status Message */}
            {(error || passwordMessage) && (
              <div className={`password-error ${passwordMessage && passwordMessage.includes('thành công') ? 'success' : ''}`}>
                {error || passwordMessage}
              </div>
            )}
            
            <div className="change-password-btn-row">
              <button 
                className="change-password-save-btn" 
                type="submit" 
                disabled={!canSave || isLoading}
              >
                {isLoading ? 'Đang xử lý...' : 'Save Password'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}