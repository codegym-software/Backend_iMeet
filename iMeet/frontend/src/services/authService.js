import axios from 'axios';

const API_BASE_URL = 'http://localhost:8081';

// Cấu hình axios với credentials
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true, // Quan trọng để gửi cookies session
  headers: {
    'Content-Type': 'application/json',
  }
});

class AuthService {
  // Đăng nhập truyền thống bằng email/password
  async login(email, password) {
    try {
      const response = await apiClient.post('/api/auth/login', {
        email,
        password
      });
      
      // Lưu token vào localStorage nếu login thành công
      if (response.data.success && response.data.token) {
        localStorage.setItem('token', response.data.token);
      }
      
      return response.data;
    } catch (error) {
      throw error.response?.data || error.message;
    }
  }

  // Đăng ký người dùng mới
  async signup(username, email, password, fullName) {
    try {
      const response = await apiClient.post('/api/auth/signup', {
        username,
        email,
        password,
        fullName
      });
      return response.data;
    } catch (error) {
      throw error.response?.data || error.message;
    }
  }

  // Bắt đầu quá trình đăng nhập Cognito (Server-side flow)
  initiateCognitoLogin() {
    // Chuyển hướng trực tiếp đến endpoint OAuth2 của backend
    window.location.href = `${API_BASE_URL}/oauth2/authorization/cognito`;
  }

  // Bắt đầu quá trình đăng nhập Cognito (Server-side OAuth2 flow)
  async initiateCognitoHostedUILogin() {
    // Luôn lấy login-url-force để hiện chọn tài khoản
    try {
      const res = await apiClient.get('/api/oauth2/hosted-ui/login-url-force');
      const loginUrl = res.data.loginUrl;
      window.location.href = loginUrl;
    } catch (error) {
      // Fallback: vẫn chuyển hướng như cũ nếu lỗi
      window.location.href = `${API_BASE_URL}/oauth2/authorization/cognito`;
    }
  }

  // Xử lý callback từ server-side OAuth2 flow
  async handleHostedUICallback() {
    try {
      // Clear any existing user data first
      this.clearAllUserData();
      
      // Đợi một chút để Spring Security hoàn tất xử lý
      await new Promise(resolve => setTimeout(resolve, 1500));
      
      // Kiểm tra trạng thái OAuth2 từ backend với retry
      const oauth2Status = await this.checkAuthStatusWithRetry(5);
      
      if (oauth2Status.authenticated) {
        // Lưu thông tin user từ server-side session
        const userData = {
          id: oauth2Status.sub,
          username: oauth2Status.username,
          email: oauth2Status.email,
          fullName: oauth2Status.fullName || oauth2Status.name,
          picture: oauth2Status.picture,
          authType: 'cognito-oauth2-server',
          attributes: oauth2Status.attributes
        };
        
        // Lưu vào localStorage để maintain state
        localStorage.setItem('oauth2User', JSON.stringify(userData));
        return userData;
      }
      
      return null;
    } catch (error) {
      return null;
    }
  }

  // Kiểm tra token OAuth2 server-side
  getOAuth2User() {
    const oauth2User = localStorage.getItem('oauth2User');
    if (oauth2User) {
      return JSON.parse(oauth2User);
    }
    return null;
  }

  // Đăng xuất OAuth2 server-side
  async logoutOAuth2() {
    try {
      // Xóa local storage trước
      localStorage.removeItem('oauth2User');
      localStorage.removeItem('user');
      localStorage.removeItem('token');
      localStorage.removeItem('userId');
      localStorage.removeItem('username');
      localStorage.removeItem('fullName');

      // Gọi backend logout endpoint (nếu có)
      try {
        await apiClient.post('/api/auth/logout');
      } catch (error) {
        console.warn('Backend logout failed:', error);
      }

      // Lấy Cognito logout URL từ backend
      const res = await apiClient.get('/api/oauth2/hosted-ui/logout-url');
      const logoutUrl = res.data.logoutUrl;

      // Redirect tới Cognito logout (xóa session Cognito)
      window.location.href = logoutUrl;
    } catch (error) {
      console.error('Error during OAuth2 logout:', error);
      // Fallback: xóa local storage và redirect
      localStorage.removeItem('oauth2User');
      localStorage.removeItem('user');
      localStorage.removeItem('token');
      localStorage.removeItem('userId');
      localStorage.removeItem('username');
      localStorage.removeItem('fullName');
      window.location.href = '/login';
    }
  }

  // Kiểm tra trạng thái đăng nhập OAuth2
  async checkOAuth2Status() {
    try {
      const response = await apiClient.get('/api/oauth2/user');
      return response.data;
    } catch (error) {
      // Throw error để checkAuthStatusWithRetry có thể xử lý
      throw error;
    }
  }
  
  // Debug endpoint để kiểm tra trạng thái authentication
  async checkAuthStatusDebug() {
    try {
      const response = await apiClient.get('/api/oauth2/status');
      return response.data;
    } catch (error) {
      console.error('Error checking auth status debug:', error);
      return { authenticated: false };
    }
  }
  
  // Refresh session
  async refreshSession() {
    try {
      const response = await apiClient.post('/api/oauth2/refresh');
      return response.data;
    } catch (error) {
      console.error('Error refreshing session:', error);
      return { success: false, authenticated: false };
    }
  }
  
  // Clear all user data
  clearAllUserData() {
    localStorage.removeItem('oauth2User');
    localStorage.removeItem('user');
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    localStorage.removeItem('fullName');
  }
  
  // Kiểm tra trạng thái authentication với retry
  async checkAuthStatusWithRetry(maxRetries = 3) {
    for (let i = 0; i < maxRetries; i++) {
      try {
        const status = await this.checkOAuth2Status();
        if (status.authenticated) {
          return status;
        }
        // Đợi 1s trước khi retry
        await new Promise(resolve => setTimeout(resolve, 1000));
      } catch (error) {
        // Nếu là lỗi 403, đợi lâu hơn
        if (error.code === 403 || error.httpStatus === 403) {
          await new Promise(resolve => setTimeout(resolve, 2000));
        } else {
          await new Promise(resolve => setTimeout(resolve, 1000));
        }
        
        if (i === maxRetries - 1) {
          return { authenticated: false };
        }
      }
    }
    return { authenticated: false };
  }

  // Lấy URL đăng nhập (không cần thiết nếu dùng trực tiếp)
  async getLoginUrl() {
    try {
      const response = await apiClient.get('/api/oauth2/login-url');
      return response.data;
    } catch (error) {
      throw error.response?.data || error.message;
    }
  }

  // Đăng xuất
  async logout() {
    try {
      // Kiểm tra loại authentication hiện tại
      const authStatus = await this.isAuthenticated();
      
      if (authStatus.authenticated && authStatus.type === 'oauth2-server') {
        // Logout OAuth2 server-side
        await this.logoutOAuth2();
        return true;
      } else {
        // Logout traditional
        await apiClient.post('/logout');
        // Xóa bất kỳ token local nào
        localStorage.removeItem('user');
        localStorage.removeItem('token');
        localStorage.removeItem('oauth2User');
        return true;
      }
    } catch (error) {
      console.error('Logout error:', error);
      // Vẫn xóa local storage dù có lỗi
      localStorage.removeItem('user');
      localStorage.removeItem('token');
      localStorage.removeItem('oauth2User');
      return false;
    }
  }

  // Lưu thông tin user vào localStorage (cho traditional login)
  saveUserToStorage(userData) {
    localStorage.setItem('user', JSON.stringify(userData));
  }

  // Lấy thông tin user từ localStorage
  getUserFromStorage() {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  }

  // Kiểm tra xem user đã đăng nhập chưa (hybrid check)
  async isAuthenticated() {
    // Kiểm tra OAuth2 server-side trước
    const oauth2User = this.getOAuth2User();
    
    if (oauth2User) {
      // Double-check với server với retry
      try {
        const serverStatus = await this.checkAuthStatusWithRetry(2);
        
        if (serverStatus.authenticated) {
          // Kiểm tra xem user ID có khớp không (tránh conflict khi switch account)
          if (serverStatus.sub === oauth2User.id) {
            return { authenticated: true, user: oauth2User, type: 'oauth2-server' };
          } else {
            // User đã đổi account, cập nhật với user mới
            const newUserData = {
              id: serverStatus.sub,
              username: serverStatus.username,
              email: serverStatus.email,
              fullName: serverStatus.fullName || serverStatus.name,
              picture: serverStatus.picture,
              authType: 'cognito-oauth2-server',
              attributes: serverStatus.attributes
            };
            localStorage.setItem('oauth2User', JSON.stringify(newUserData));
            return { authenticated: true, user: newUserData, type: 'oauth2-server' };
          }
        } else {
          // Session hết hạn, xóa local storage
          localStorage.removeItem('oauth2User');
        }
      } catch (error) {
        console.error('Error checking OAuth2 status:', error);
        // Nếu có lỗi, vẫn giữ user trong localStorage để retry sau
        return { authenticated: true, user: oauth2User, type: 'oauth2-server' };
      }
    }

    // Kiểm tra traditional login
    const localUser = this.getUserFromStorage();
    if (localUser) {
      return { authenticated: true, user: localUser, type: 'traditional' };
    }

    // Kiểm tra OAuth2 server-side session trực tiếp với retry
    try {
      const oauth2Status = await this.checkAuthStatusWithRetry(2);
      if (oauth2Status.authenticated) {
        // Lưu user data nếu tìm thấy session
        const userData = {
          id: oauth2Status.sub,
          username: oauth2Status.username,
          email: oauth2Status.email,
          fullName: oauth2Status.fullName || oauth2Status.name,
          picture: oauth2Status.picture,
          authType: 'cognito-oauth2-server',
          attributes: oauth2Status.attributes
        };
        localStorage.setItem('oauth2User', JSON.stringify(userData));
        return { authenticated: true, user: userData, type: 'oauth2-server' };
      }
    } catch (error) {
      console.error('Error checking OAuth2 status directly:', error);
    }
    
    // Thử refresh session nếu không tìm thấy authentication
    try {
      const refreshResult = await this.refreshSession();
      if (refreshResult.success && refreshResult.authenticated) {
        // Lưu user data từ refresh result
        const userData = {
          id: refreshResult.sub,
          username: refreshResult.username,
          email: refreshResult.email,
          fullName: refreshResult.fullName || refreshResult.name,
          picture: refreshResult.picture,
          authType: 'cognito-oauth2-server',
          attributes: refreshResult.attributes
        };
        localStorage.setItem('oauth2User', JSON.stringify(userData));
        return { authenticated: true, user: userData, type: 'oauth2-server' };
      }
    } catch (error) {
      console.error('Error refreshing session:', error);
    }

    return { authenticated: false };
  }

  // Đổi mật khẩu
  async changePassword(currentPassword, newPassword, confirmPassword) {
    try {
      // Lấy token từ localStorage
      const token = localStorage.getItem('token');
      
      if (!token) {
        throw new Error('Không có token. Vui lòng đăng nhập lại.');
      }
      
      const response = await apiClient.post('/api/auth/change-password', {
        currentPassword,
        newPassword,
        confirmPassword
      }, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      return response.data;
    } catch (error) {
      throw error.response?.data || error.message;
    }
  }

  // Kiểm tra token hợp lệ
  async validateToken() {
    try {
      const token = localStorage.getItem('token');
      
      if (!token) {
        return { valid: false, message: 'Không có token' };
      }
      
      const response = await apiClient.get('/api/auth/check-auth', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      return { valid: true, data: response.data };
    } catch (error) {
      return { valid: false, message: 'Token không hợp lệ' };
    }
  }
}

export default new AuthService();