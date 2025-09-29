import React, { useState, useEffect, useRef } from 'react';
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

  // State cho đổi avatar
  const [showChangeAvatar, setShowChangeAvatar] = useState(false);
  const [avatarFile, setAvatarFile] = useState(null);
  const [avatarPreview, setAvatarPreview] = useState(null);
  const [avatarMessage, setAvatarMessage] = useState('');
  const [isUploading, setIsUploading] = useState(false);

  // State cho đổi tên
  const [showChangeName, setShowChangeName] = useState(false);
  const [nameData, setNameData] = useState({
    name: ''
  });
  const [nameMessage, setNameMessage] = useState('');
  const [isUpdatingName, setIsUpdatingName] = useState(false);
  
  // Ref để kiểm tra component có còn mount không
  const isMountedRef = useRef(true);

  // Cleanup effect
  useEffect(() => {
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  // Lấy thông tin user từ context hoặc localStorage
  const getUserData = () => {
    // Ưu tiên user từ context
    if (user) return user;
    
    // Fallback: lấy từ localStorage
    const userData = localStorage.getItem('user');
    if (userData) return JSON.parse(userData);
    
    const oauth2Data = localStorage.getItem('oauth2User');
    if (oauth2Data) return JSON.parse(oauth2Data);
    
    return null;
  };

  // Lấy thông tin OAuth2 user từ server
  const getOAuth2UserData = async () => {
    try {
      const response = await authService.checkOAuth2Status();
      if (response.authenticated) {
        return {
          id: response.sub,
          username: response.username,
          email: response.email,
          fullName: response.fullName || response.name,
          picture: response.picture,
          authType: 'cognito-oauth2-server',
          attributes: response.attributes
        };
      }
    } catch (error) {
      console.error('Error getting OAuth2 user data:', error);
    }
    return null;
  };

  const userData = getUserData();
  const avatarUrl = userData?.avatarUrl;
  const isGooglePicture = avatarUrl && avatarUrl.startsWith('https://');
  const isBase64Data = avatarUrl && avatarUrl.startsWith('data:');
  
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

  // Render avatar - ưu tiên avatar upload, sau đó Google picture, cuối cùng là initials
  const renderAvatar = () => {
    if (avatarUrl) {
      let imageSrc = avatarUrl;
      
      if (isGooglePicture) {
        // Google picture URL
        imageSrc = avatarUrl;
      } else if (isBase64Data) {
        // Base64 data URL từ database
        imageSrc = avatarUrl;
      } else {
        // Fallback cho uploaded file (nếu có)
        imageSrc = `http://localhost:8081${avatarUrl}`;
      }
      
      return (
        <img 
          src={imageSrc} 
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
      
      if (!isMountedRef.current) return;
      
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
      if (isMountedRef.current) {
        setPasswordMessage(error.message || 'Đổi mật khẩu thất bại');
      }
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

  // Xử lý đổi avatar
  const handleAvatarChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      // Kiểm tra loại file
      if (!file.type.startsWith('image/')) {
        setAvatarMessage('Vui lòng chọn file ảnh hợp lệ');
        return;
      }
      
      // Kiểm tra kích thước file (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        setAvatarMessage('Kích thước ảnh không được vượt quá 5MB');
        return;
      }
      
      setAvatarFile(file);
      setAvatarMessage('');
      
      // Tạo preview
      const reader = new FileReader();
      reader.onload = (e) => {
        setAvatarPreview(e.target.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSubmitAvatarChange = async (e) => {
    e.preventDefault();
    
    // Kiểm tra loại tài khoản
    if (userData && userData.authType === 'cognito-oauth2-server') {
      setAvatarMessage('Tài khoản Google không thể đổi avatar tại đây');
      return;
    }
    
    if (!avatarFile) {
      setAvatarMessage('Vui lòng chọn ảnh để upload');
      return;
    }

    if (!isMountedRef.current) return;
    setIsUploading(true);
    setAvatarMessage('Đang upload avatar...');
    
    try {
      const formData = new FormData();
      formData.append('avatar', avatarFile);
      
      const response = await authService.uploadAvatar(formData);
      
      if (!isMountedRef.current) return;
      
      if (response.success) {
        setAvatarMessage('Cập nhật avatar thành công!');
        
        // Cập nhật user data với avatar mới
        const updatedUserData = {
          ...userData,
          avatarUrl: response.avatarUrl
        };
        
        // Lưu vào localStorage
        if (userData) {
          localStorage.setItem('user', JSON.stringify(updatedUserData));
        }
        
        // Reset form
        setAvatarFile(null);
        setAvatarPreview(null);
        setShowChangeAvatar(false);
        
        // Reload trang sau khi upload avatar thành công
        window.location.reload();
      } else {
        setAvatarMessage(response.message || 'Upload avatar thất bại');
      }
    } catch (error) {
      if (isMountedRef.current) {
        const errorMessage = error.response?.data?.message || error.message || 'Upload avatar thất bại';
        setAvatarMessage(errorMessage);
      }
    } finally {
      if (isMountedRef.current) {
        setIsUploading(false);
      }
    }
  };

  const toggleChangeAvatar = () => {
    setShowChangeAvatar(!showChangeAvatar);
    setAvatarMessage('');
    setAvatarFile(null);
    setAvatarPreview(null);
  };

  const removeAvatar = async () => {
    // Kiểm tra loại tài khoản
    if (userData && userData.authType === 'cognito-oauth2-server') {
      setAvatarMessage('Tài khoản Google không thể xóa avatar tại đây');
      return;
    }
    
    if (!confirm('Bạn có chắc chắn muốn xóa avatar hiện tại?')) {
      return;
    }

    if (!isMountedRef.current) return;
    setIsUploading(true);
    setAvatarMessage('Đang xóa avatar...');
    
    try {
      const response = await authService.removeAvatar();
      
      if (!isMountedRef.current) return;
      
      if (response.success) {
        setAvatarMessage('Xóa avatar thành công!');
        
        // Cập nhật user data
        const updatedUserData = {
          ...userData,
          avatarUrl: null
        };
        
        // Lưu vào localStorage
        if (userData) {
          localStorage.setItem('user', JSON.stringify(updatedUserData));
        }
        
        setShowChangeAvatar(false);
        
        // Reload trang sau khi xóa avatar thành công
        window.location.reload();
      } else {
        setAvatarMessage(response.message || 'Xóa avatar thất bại');
      }
    } catch (error) {
      if (isMountedRef.current) {
        setAvatarMessage(error.message || 'Xóa avatar thất bại');
      }
    } finally {
      if (isMountedRef.current) {
        setIsUploading(false);
      }
    }
  };

  // Xử lý đổi tên
  const handleNameChange = (e) => {
    const { name, value } = e.target;
    setNameData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const toggleChangeName = () => {
    setShowChangeName(!showChangeName);
    if (!showChangeName) {
      // Khi mở form, set tên hiện tại
      setNameData({
        name: userData?.fullName || ''
      });
    }
    setNameMessage('');
  };

  const handleSubmitNameChange = async (e) => {
    e.preventDefault();
    
    if (!nameData.name.trim()) {
      setNameMessage('Vui lòng nhập tên mới');
      return;
    }

    if (nameData.name.trim() === userData?.fullName) {
      setNameMessage('Tên mới phải khác tên hiện tại');
      return;
    }

    try {
      setIsUpdatingName(true);
      setNameMessage('');

      const response = await authService.updateProfile({
        name: nameData.name.trim()
      });

      if (response.success) {
        setNameMessage('Đổi tên thành công!');
        
        // Cập nhật user data trong localStorage
        if (userData) {
          const updatedUserData = {
            ...userData,
            fullName: nameData.name.trim()
          };
          localStorage.setItem('user', JSON.stringify(updatedUserData));
        }
        
        setShowChangeName(false);
        
        // Reload trang sau khi đổi tên thành công
        window.location.reload();
      } else {
        setNameMessage(response.message || 'Đổi tên thất bại');
      }
    } catch (error) {
      if (isMountedRef.current) {
        setNameMessage(error.message || 'Đổi tên thất bại');
      }
    } finally {
      if (isMountedRef.current) {
        setIsUpdatingName(false);
      }
    }
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
          {/* Chỉ hiển thị nút đổi avatar cho tài khoản đăng ký tại web */}
          {userData && userData.authType !== 'cognito-oauth2-server' && (
            <button 
              className="change-avatar-btn"
              onClick={toggleChangeAvatar}
            >
              {showChangeAvatar ? 'Hủy đổi avatar' : 'Đổi avatar'}
            </button>
          )}
          
        </div>
        
        <div className="profile-fields">
          <div className="profile-field-row">
            <label className="profile-label">Họ và tên:</label>
            <div className="profile-field-with-button">
              <input 
                className="profile-input" 
                type="text" 
                value={userData.fullName || ''} 
                readOnly 
              />
              {/* Chỉ hiển thị nút đổi tên cho tài khoản đăng ký tại web */}
              {userData && userData.authType !== 'cognito-oauth2-server' && (
                <button 
                  className="change-name-btn"
                  onClick={toggleChangeName}
                >
                  {showChangeName ? 'Hủy' : 'Đổi tên'}
                </button>
              )}
            </div>
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

          {/* Form đổi tên */}
          {showChangeName && (
            <div className="change-name-section">
              <h3 className="change-name-title">Đổi tên</h3>
              
              <form onSubmit={handleSubmitNameChange} className="change-name-form">
                <div className="profile-field-row">
                  <label className="profile-label">Tên mới:</label>
                  <input 
                    className="profile-input" 
                    type="text" 
                    name="name"
                    value={nameData.name}
                    onChange={handleNameChange}
                    placeholder="Nhập tên mới"
                    required
                    disabled={isUpdatingName}
                  />
                </div>
                
                {nameMessage && (
                  <div className={`name-message ${nameMessage.includes('thành công') ? 'success' : 'error'}`}>
                    {nameMessage}
                  </div>
                )}
                
                <div className="profile-field-row">
                  <button 
                    type="submit" 
                    className="submit-name-btn"
                    disabled={isUpdatingName}
                  >
                    {isUpdatingName ? 'Đang cập nhật...' : 'Đổi tên'}
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* Form đổi avatar */}
          {showChangeAvatar && (
            <div className="change-avatar-section">
              <h3 className="change-avatar-title">Đổi avatar</h3>
              
              <form onSubmit={handleSubmitAvatarChange} className="change-avatar-form">
                <div className="avatar-preview-container">
                  <div className="avatar-preview">
                    {avatarPreview ? (
                      <img 
                        src={avatarPreview} 
                        alt="Preview" 
                        className="avatar-preview-img"
                      />
                    ) : (
                      <div className="avatar-preview-placeholder">
                        {renderAvatar()}
                      </div>
                    )}
                  </div>
                  <p className="avatar-preview-text">
                    {avatarPreview ? 'Ảnh preview' : 'Avatar hiện tại'}
                  </p>
                </div>
                
                <div className="profile-field-row">
                  <label className="profile-label">Chọn ảnh mới:</label>
                  <input 
                    type="file"
                    accept="image/*"
                    onChange={handleAvatarChange}
                    className="avatar-file-input"
                    disabled={isUploading}
                  />
                  <p className="avatar-help-text">
                    Định dạng: JPG, PNG, GIF. Kích thước tối đa: 5MB
                  </p>
                </div>
                
                {avatarMessage && (
                  <div className={`avatar-message ${avatarMessage.includes('thành công') ? 'success' : 'error'}`}>
                    {avatarMessage}
                  </div>
                )}
                
                <div className="avatar-actions">
                  <button 
                    type="submit" 
                    className="submit-avatar-btn"
                    disabled={!avatarFile || isUploading}
                  >
                    {isUploading ? 'Đang xử lý...' : 'Cập nhật avatar'}
                  </button>
                  
                  {avatarUrl && userData && userData.authType !== 'cognito-oauth2-server' && !isGooglePicture && !isBase64Data && (
                    <button 
                      type="button"
                      className="remove-avatar-btn"
                      onClick={removeAvatar}
                      disabled={isUploading}
                    >
                      Xóa avatar
                    </button>
                  )}
                </div>
              </form>
            </div>
          )}
          
        </div>
      </div>
    </div>
  );
}
