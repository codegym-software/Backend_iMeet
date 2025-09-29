import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useHistory } from 'react-router-dom';
import './Dashboard.css';

export default function Dashboard() {
  const { logout, user } = useAuth();
  const history = useHistory();
  
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
  
  const userData = getUserData();
  const displayName = userData?.fullName || userData?.username || 'User';
  const userEmail = userData?.email || '';
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
          alt={displayName}
          className="user-avatar-img"
        />
      );
    } else {
      return getAvatarInitials();
    }
  };

  const handleLogout = async () => {
    await logout();
  };

  const handleAvatarClick = () => {
    history.push('/profile');
  };

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <h1>Dashboard</h1>
        <div className="user-info">
          <div className="user-avatar-container">
            <div 
              className="user-avatar" 
              onClick={handleAvatarClick}
              title={`${displayName} - Click to view profile`}
            >
              {renderAvatar()}
            </div>
            <div className="user-details">
              <span className="user-name">{displayName}</span>
              <span className="user-email">{userEmail}</span>
            </div>
          </div>
          <button onClick={handleLogout} className="logout-btn">
            Logout
          </button>
        </div>
      </div>
      
      <div className="dashboard-content">
        <div className="welcome-message">
          <h2>Chào mừng đến với iMeet!</h2>
          <p>Đây là trang dashboard của bạn. Bạn có thể bắt đầu sử dụng các tính năng từ đây.</p>
        </div>
      </div>
    </div>
  );
}
