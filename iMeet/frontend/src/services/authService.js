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
      
      // Đợi ngắn để Spring Security hoàn tất xử lý
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Lấy thông tin user đơn giản từ server
      const response = await fetch('http://localhost:8081/api/oauth2/user', {
        credentials: 'include'
      });
      
      if (response.ok) {
        const userData = await response.json();
        
        if (userData.authenticated) {
          // Lưu thông tin user từ server-side session
          const oauth2User = {
            id: userData.sub,
            username: userData.username,
            email: userData.email,
            fullName: userData.fullName || userData.name,
            avatarUrl: userData.picture, // Lưu Google picture vào avatarUrl
            authType: 'cognito-oauth2-server',
            attributes: userData.attributes
          };
          
          // Lưu vào localStorage để maintain state
          localStorage.setItem('oauth2User', JSON.stringify(oauth2User));
          return oauth2User;
        }
      }
      
      return null;
    } catch (error) {
      console.error('Error in handleHostedUICallback:', error);
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

      // Clear server session
      try {
        await apiClient.post('/api/oauth2/clear-session');
      } catch (error) {
        console.warn('Clear session failed:', error);
      }

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
      // Thêm timeout 5 giây
      const timeoutPromise = new Promise((_, reject) => 
        setTimeout(() => reject(new Error('Request timeout')), 5000)
      );
      
      const responsePromise = apiClient.get('/api/oauth2/user');
      const response = await Promise.race([responsePromise, timeoutPromise]);
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
  async checkAuthStatusWithRetry(maxRetries = 2) {
    for (let i = 0; i < maxRetries; i++) {
      try {
        const status = await this.checkOAuth2Status();
        if (status.authenticated) {
          return status;
        }
        // Đợi ngắn hơn trước khi retry
        if (i < maxRetries - 1) {
          await new Promise(resolve => setTimeout(resolve, 300));
        }
      } catch (error) {
        // Đợi ngắn hơn cho tất cả lỗi
        if (i < maxRetries - 1) {
          await new Promise(resolve => setTimeout(resolve, 300));
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
      // Đơn giản hóa: chỉ cần có user trong localStorage là đủ
      // Không cần double-check với server để tăng tốc
      return { authenticated: true, user: oauth2User, type: 'oauth2-server' };
    }

    // Kiểm tra traditional login
    const localUser = this.getUserFromStorage();
    if (localUser) {
      // Validate token với server để lấy thông tin user mới nhất
      try {
        const tokenValidation = await this.validateToken();
        if (tokenValidation.valid) {
          // Cập nhật user data với thông tin mới nhất từ server
          const updatedUserData = {
            ...localUser,
            // Ưu tiên avatarUrl từ server, fallback về localStorage
            avatarUrl: tokenValidation.data?.avatarUrl || localUser.avatarUrl
          };
          
          
          localStorage.setItem('user', JSON.stringify(updatedUserData));
          return { authenticated: true, user: updatedUserData, type: 'traditional' };
        } else {
          // Token không hợp lệ, xóa local storage
          this.clearAllUserData();
          return { authenticated: false };
        }
      } catch (error) {
        console.error('Error validating token:', error);
        // Nếu có lỗi, vẫn trả về user từ localStorage
        return { authenticated: true, user: localUser, type: 'traditional' };
      }
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
          avatarUrl: oauth2Status.picture, // Lưu Google picture vào avatarUrl
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
          avatarUrl: refreshResult.picture, // Lưu Google picture vào avatarUrl
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
      
      // Thêm timeout 5 giây
      const timeoutPromise = new Promise((_, reject) => 
        setTimeout(() => reject(new Error('Request timeout')), 5000)
      );
      
      const responsePromise = apiClient.get('/api/auth/check-auth', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      const response = await Promise.race([responsePromise, timeoutPromise]);
      return { valid: true, data: response.data };
    } catch (error) {
      return { valid: false, message: 'Token không hợp lệ' };
    }
  }

  // Upload avatar
  async uploadAvatar(formData) {
    try {
      // Bỏ qua authentication check - để backend xử lý
      // Vì có thể có vấn đề với isAuthenticated() nhưng backend vẫn hoạt động
      
      const token = localStorage.getItem('token');
      if (!token) {
        throw new Error('Không có token. Vui lòng đăng nhập lại.');
      }
      
      const response = await apiClient.post('/api/auth/upload-avatar', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
          'Authorization': `Bearer ${token}`
        }
      });
      
      return response.data;
    } catch (error) {
      throw error.response?.data || error.message;
    }
  }

  // Xóa avatar
  async removeAvatar() {
    try {
      // Bỏ qua authentication check - để backend xử lý
      
      const token = localStorage.getItem('token');
      if (!token) {
        throw new Error('Không có token. Vui lòng đăng nhập lại.');
      }
      
      const response = await apiClient.delete('/api/auth/remove-avatar', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      return response.data;
    } catch (error) {
      throw error.response?.data || error.message;
    }
  }
}

export default new AuthService();