import React, { createContext, useContext, useState, useEffect } from 'react';
import authService from '../services/authService';

const AuthContext = createContext();

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [authType, setAuthType] = useState(null); // 'traditional' hoặc 'oauth2'
  const [loading, setLoading] = useState(true);

  // Kiểm tra authentication status khi component mount
  useEffect(() => {
    checkAuthStatus();
  }, []);

  const checkAuthStatus = async () => {
    let isMounted = true;
    
    try {
      if (isMounted) setLoading(true);
      
      const authStatus = await authService.isAuthenticated();
      
      if (isMounted) {
        if (authStatus.authenticated) {
          setUser(authStatus.user);
          setIsAuthenticated(true);
          setAuthType(authStatus.type);
        } else {
          setUser(null);
          setIsAuthenticated(false);
          setAuthType(null);
        }
      }
    } catch (error) {
      console.error('AuthContext: Error checking auth status:', error);
      if (isMounted) {
        setUser(null);
        setIsAuthenticated(false);
        setAuthType(null);
      }
    } finally {
      if (isMounted) {
        setLoading(false);
      }
    }
    
    return () => {
      isMounted = false;
    };
  };

  // Đăng nhập truyền thống
  const login = async (email, password) => {
    try {
      const response = await authService.login(email, password);
      
      if (response.success) {
        const userData = {
          id: response.userId,
          username: response.username,
          fullName: response.fullName,
          email: email,
          token: response.token,
          avatarUrl: response.avatarUrl
        };
        
        authService.saveUserToStorage(userData);
        setUser(userData);
        setIsAuthenticated(true);
        setAuthType('traditional');
        return { success: true, message: response.message };
      } else {
        return { success: false, message: response.message };
      }
    } catch (error) {
      return { 
        success: false, 
        message: error.message || 'Đăng nhập thất bại' 
      };
    }
  };

  // Đăng nhập bằng Cognito Server-side
  const loginWithCognito = () => {
    authService.initiateCognitoLogin();
  };

  // Đăng nhập bằng Cognito Hosted UI
  const loginWithCognitoHostedUI = () => {
    try {
      authService.initiateCognitoHostedUILogin();
    } catch (error) {
      console.error('Error initiating Cognito Hosted UI login:', error);
    }
  };

  // Đăng ký
  const signup = async (username, email, password, fullName) => {
    try {
      const response = await authService.signup(username, email, password, fullName);
      return { 
        success: response.success, 
        message: response.message 
      };
    } catch (error) {
      return { 
        success: false, 
        message: error.message || 'Đăng ký thất bại' 
      };
    }
  };

  // Force refresh authentication status
  const forceRefreshAuth = async () => {
    await checkAuthStatus();
  };

  // Clear all user data and refresh auth
  const clearAndRefreshAuth = async () => {
    authService.clearAllUserData();
    await checkAuthStatus();
  };

  // Đăng xuất
  const logout = async () => {
    try {
      await authService.logout();
      setUser(null);
      setIsAuthenticated(false);
      setAuthType(null);
      // Chuyển hướng về trang login
      window.location.href = '/login';
    } catch (error) {
      console.error('Logout error:', error);
      // Vẫn clear state local dù có lỗi
      setUser(null);
      setIsAuthenticated(false);
      setAuthType(null);
    }
  };

  const value = {
    user,
    setUser,
    isAuthenticated,
    authType,
    loading,
    login,
    loginWithCognito,
    loginWithCognitoHostedUI,
    signup,
    logout,
    checkAuthStatus,
    forceRefreshAuth,
    clearAndRefreshAuth
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};