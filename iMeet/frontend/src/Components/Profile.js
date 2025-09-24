import React, { useState } from 'react';
import { useHistory } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import authService from '../services/authService';
import './Profile.css';

export default function Profile() {
  const history = useHistory();
  const { user } = useAuth();
  
  // State cho đổi mật khẩu
  const [showChangePassword, setShowChangePassword] = useState(false);
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [passwordMessage, setPasswordMessage] = useState('');
  
  // State cho hiện/ẩn mật khẩu
  const [showPasswords, setShowPasswords] = useState({
    current: false,
    new: false,
    confirm: false
  });

  // Lấy thông tin user từ localStorage nếu chưa có trong context
  const getUserData = () => {
    if (user) return user;
    
    // Fallback: lấy từ localStorage
    const userData = localStorage.getItem('user');
    if (userData) return JSON.parse(userData);
    
    const oauth2Data = localStorage.getItem('oauth2User');
    if (oauth2Data) return JSON.parse(oauth2Data);
    
    return null;
  };

  const userData = getUserData();
  const pictureUrl = userData?.picture || userData?.pictureUrl;
  
  // Tạo avatar từ tên hoặc email
  const getAvatarInitials = () => {
    if (userData?.fullName) {
      return userData.fullName.split(' ').map(name => name[0]).join('').toUpperCase().slice(0, 2);
    }
    if (userData?.email) {
      return userData.email[0].toUpperCase();
    }
    return 'U';
  };

  // Render avatar - ảnh hoặc initials
  const renderAvatar = () => {
    if (pictureUrl) {
      return (
        <img 
          src={pictureUrl} 
          alt={userData.fullName || userData.username || 'User'}
          className="profile-avatar-img"
        />
      );
    } else {
      return getAvatarInitials();
    }
  };

  const handleBack = () => {
    history.goBack();
  };

  // Xử lý đổi mật khẩu
  const handlePasswordChange = (e) => {
    const { name, value } = e.target;
    setPasswordData(prev => ({
      ...prev,
      [name]: value
    }));
    setPasswordMessage(''); // Clear message khi user typing
  };

  const handleSubmitPasswordChange = async (e) => {
    e.preventDefault();
    
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setPasswordMessage('Mật khẩu mới và xác nhận mật khẩu không khớp');
      return;
    }
    
    if (passwordData.newPassword.length < 6) {
      setPasswordMessage('Mật khẩu mới phải có ít nhất 6 ký tự');
      return;
    }

    // Kiểm tra user có đăng nhập qua traditional login không
    const userData = getUserData();
    if (userData.authType === 'cognito-oauth2-server') {
      setPasswordMessage('Tài khoản OAuth2 không thể đổi mật khẩu tại đây');
      return;
    }

    // Kiểm tra token hợp lệ trước khi đổi mật khẩu
    setPasswordMessage('Đang kiểm tra token...');
    const tokenValidation = await authService.validateToken();
    
    if (!tokenValidation.valid) {
      setPasswordMessage('Token không hợp lệ. Vui lòng đăng nhập lại.');
      return;
    }

    setPasswordMessage('Đang đổi mật khẩu...');
    try {
      const response = await authService.changePassword(
        passwordData.currentPassword,
        passwordData.newPassword,
        passwordData.confirmPassword
      );
      
      if (response.success) {
        setPasswordMessage('Đổi mật khẩu thành công!');
        setPasswordData({
          currentPassword: '',
          newPassword: '',
          confirmPassword: ''
        });
        setShowChangePassword(false);
      } else {
        setPasswordMessage(response.message || 'Đổi mật khẩu thất bại');
      }
    } catch (error) {
      setPasswordMessage(error.message || 'Đổi mật khẩu thất bại');
    }
  };

  const toggleChangePassword = () => {
    setShowChangePassword(!showChangePassword);
    setPasswordMessage('');
    setPasswordData({
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    });
    // Reset show passwords state
    setShowPasswords({
      current: false,
      new: false,
      confirm: false
    });
  };

  const togglePasswordVisibility = (field) => {
    setShowPasswords(prev => ({
      ...prev,
      [field]: !prev[field]
    }));
  };


  if (!userData) {
    return (
      <div className="profile-main-container">
        <div className="profile-error">
          <h2>Không thể tải thông tin người dùng</h2>
          <button onClick={handleBack} className="profile-back-btn">
            Quay lại
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="profile-main-container">
      <button className="profile-back-btn" onClick={handleBack} aria-label="Back">
        <svg width="40" height="40" viewBox="0 0 40 40">
          <path d="M30 20H10M10 20L18 12M10 20L18 28" stroke="#000" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
      </button>
      
      <div className="profile-flex">
        <div className="profile-avatar-section">
          <div className="profile-avatar-large">
            {renderAvatar()}
          </div>
          <h2 className="profile-name">{userData.fullName || userData.username || 'User'}</h2>
        </div>
        
        <div className="profile-fields">
          <div className="profile-field-row">
            <label className="profile-label">Họ và tên:</label>
            <input 
              className="profile-input" 
              type="text" 
              value={userData.fullName || ''} 
              readOnly 
            />
          </div>
          
          <div className="profile-field-row">
            <label className="profile-label">Tên đăng nhập:</label>
            <input 
              className="profile-input" 
              type="text" 
              value={userData.username || ''} 
              readOnly 
            />
          </div>
          
          <div className="profile-field-row">
            <label className="profile-label">Email:</label>
            <input 
              className="profile-input" 
              type="text" 
              value={userData.email || ''} 
              readOnly 
            />
          </div>

          {/* Chỉ hiển thị nút đổi mật khẩu cho tài khoản đăng ký tại web */}
          {userData.authType !== 'cognito-oauth2-server' && (
            <div className="profile-field-row">
              <button 
                className="change-password-btn"
                onClick={toggleChangePassword}
              >
                {showChangePassword ? 'Hủy đổi mật khẩu' : 'Đổi mật khẩu'}
              </button>
            </div>
          )}

          {/* Form đổi mật khẩu */}
          {showChangePassword && (
            <div className="change-password-section">
              <h3 className="change-password-title">Đổi mật khẩu</h3>
              
              <form onSubmit={handleSubmitPasswordChange} className="change-password-form">
                <div className="profile-field-row">
                  <label className="profile-label">Mật khẩu hiện tại:</label>
                  <div className="password-input-container">
                    <input 
                      className="profile-input" 
                      type={showPasswords.current ? "text" : "password"}
                      name="currentPassword"
                      value={passwordData.currentPassword}
                      onChange={handlePasswordChange}
                      required
                    />
                    <button 
                      type="button"
                      className="password-toggle-btn"
                      onClick={() => togglePasswordVisibility('current')}
                      aria-label={showPasswords.current ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                    >
                      {showPasswords.current ? '🙈' : '👁️'}
                    </button>
                  </div>
                </div>
                
                <div className="profile-field-row">
                  <label className="profile-label">Mật khẩu mới:</label>
                  <div className="password-input-container">
                    <input 
                      className="profile-input" 
                      type={showPasswords.new ? "text" : "password"}
                      name="newPassword"
                      value={passwordData.newPassword}
                      onChange={handlePasswordChange}
                      required
                      minLength={6}
                    />
                    <button 
                      type="button"
                      className="password-toggle-btn"
                      onClick={() => togglePasswordVisibility('new')}
                      aria-label={showPasswords.new ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                    >
                      {showPasswords.new ? '🙈' : '👁️'}
                    </button>
                  </div>
                </div>
                
                <div className="profile-field-row">
                  <label className="profile-label">Xác nhận mật khẩu mới:</label>
                  <div className="password-input-container">
                    <input 
                      className="profile-input" 
                      type={showPasswords.confirm ? "text" : "password"}
                      name="confirmPassword"
                      value={passwordData.confirmPassword}
                      onChange={handlePasswordChange}
                      required
                      minLength={6}
                    />
                    <button 
                      type="button"
                      className="password-toggle-btn"
                      onClick={() => togglePasswordVisibility('confirm')}
                      aria-label={showPasswords.confirm ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                    >
                      {showPasswords.confirm ? '🙈' : '👁️'}
                    </button>
                  </div>
                </div>
                
                {passwordMessage && (
                  <div className={`password-message ${passwordMessage.includes('thành công') ? 'success' : 'error'}`}>
                    {passwordMessage}
                  </div>
                )}
                
                <div className="profile-field-row">
                  <button type="submit" className="submit-password-btn">
                    Đổi mật khẩu
                  </button>
                </div>
              </form>
            </div>
          )}
          
        </div>
      </div>
    </div>
  );
}
