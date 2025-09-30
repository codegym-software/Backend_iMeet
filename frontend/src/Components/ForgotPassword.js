import React, { useState, useRef, useEffect } from 'react';
import { Link } from 'react-router-dom';
import authService from '../services/authService';
import './ForgotPassword.css';

export default function ForgotPassword() {
  const [step, setStep] = useState(1); // 1: Email, 2: Verify Code, 3: Reset Password
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [message, setMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isCodeValid, setIsCodeValid] = useState(false);
  
  const isMountedRef = useRef(true);
  const timeoutRef = useRef(null);

  useEffect(() => {
    // Reset loading state khi component mount
    setIsLoading(false);
    setIsCodeValid(false);
    
    return () => {
      isMountedRef.current = false;
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, []);

  // Reset loading state khi chuyển step
  useEffect(() => {
    setIsLoading(false);
  }, [step]);

  const clearMessage = () => {
    if (isMountedRef.current) {
      setMessage('');
    }
  };

  const showMessage = (msg, isError = false) => {
    if (isMountedRef.current) {
      setMessage(msg);
      timeoutRef.current = setTimeout(clearMessage, 5000);
    }
  };

  const handleSendCode = async (e) => {
    e.preventDefault();
    if (!email.trim()) {
      showMessage('Vui lòng nhập email', true);
      return;
    }

    setIsLoading(true);
    try {
      const response = await authService.sendForgotPasswordCode(email);
      if (response.success) {
        showMessage('Mã xác minh đã được gửi đến email của bạn');
        setStep(2);
      } else {
        showMessage(response.message || 'Có lỗi xảy ra', true);
      }
    } catch (error) {
      showMessage(error.message || 'Có lỗi xảy ra khi gửi mã xác minh', true);
    } finally {
      if (isMountedRef.current) {
        setIsLoading(false);
      }
    }
  };

  const handleVerifyCode = async (e) => {
    e.preventDefault();
    if (!code.trim()) {
      showMessage('Vui lòng nhập mã xác minh', true);
      return;
    }

    setIsLoading(true);
    try {
      const response = await authService.verifyForgotPasswordCode(email, code);
      if (response.success) {
        setIsCodeValid(true);
        showMessage('Mã xác minh hợp lệ');
        // Chuyển sang bước tiếp theo sau 0.5 giây để hiển thị màu xanh
        setTimeout(() => {
          if (isMountedRef.current) {
            setStep(3);
          }
        }, 500);
      } else {
        setIsCodeValid(false);
        showMessage(response.message || 'Mã xác minh không hợp lệ', true);
      }
    } catch (error) {
      setIsCodeValid(false);
      showMessage(error.message || 'Có lỗi xảy ra khi xác minh mã', true);
    } finally {
      if (isMountedRef.current) {
        setIsLoading(false);
      }
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    if (!newPassword.trim()) {
      showMessage('Vui lòng nhập mật khẩu mới', true);
      return;
    }
    if (newPassword.length < 6) {
      showMessage('Mật khẩu phải có ít nhất 6 ký tự', true);
      return;
    }
    if (newPassword !== confirmPassword) {
      showMessage('Mật khẩu xác nhận không khớp', true);
      return;
    }

    setIsLoading(true);
    try {
      const response = await authService.resetPassword(email, code, newPassword, confirmPassword);
      if (response.success) {
        showMessage('Đặt lại mật khẩu thành công! Bạn có thể đăng nhập với mật khẩu mới.');
        setTimeout(() => {
          window.location.href = '/login';
        }, 2000);
      } else {
        showMessage(response.message || 'Có lỗi xảy ra khi đặt lại mật khẩu', true);
      }
    } catch (error) {
      showMessage(error.message || 'Có lỗi xảy ra khi đặt lại mật khẩu', true);
    } finally {
      if (isMountedRef.current) {
        setIsLoading(false);
      }
    }
  };

  const handleResendCode = async () => {
    setIsLoading(true);
    try {
      const response = await authService.sendForgotPasswordCode(email);
      if (response.success) {
        showMessage('Mã xác minh mới đã được gửi');
        // Reset loading và code để user có thể nhập mã mới
        setIsLoading(false);
        setCode('');
        setIsCodeValid(false);
      } else {
        showMessage(response.message || 'Có lỗi xảy ra', true);
      }
    } catch (error) {
      showMessage(error.message || 'Có lỗi xảy ra khi gửi lại mã', true);
    } finally {
      if (isMountedRef.current) {
        setIsLoading(false);
      }
    }
  };

  return (
    <div className="forgot-password-container">
      <div className="forgot-password-box">
        <div className="forgot-password-header">
          <h1>Quên mật khẩu</h1>
          <p>Nhập email của bạn để nhận mã xác minh</p>
        </div>

        {message && (
          <div className={`forgot-password-message ${message.includes('thành công') ? 'success' : 'error'}`}>
            {message}
          </div>
        )}

        {step === 1 && (
          <form onSubmit={handleSendCode} className="forgot-password-form">
            <div className="input-group">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                type="email"
                placeholder="Nhập email của bạn"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                disabled={isLoading}
              />
            </div>
            <button type="submit" className="forgot-password-btn" disabled={isLoading}>
              {isLoading ? 'Đang gửi...' : 'Gửi mã xác minh'}
            </button>
          </form>
        )}

        {step === 2 && (
          <form onSubmit={handleVerifyCode} className="forgot-password-form">
            <div className="input-group">
              <label htmlFor="code">Mã xác minh</label>
              <input
                id="code"
                type="text"
                placeholder="Nhập mã 6 chữ số"
                value={code}
                  onChange={(e) => {
                  setCode(e.target.value);
                  if (isCodeValid) {
                    setIsCodeValid(false);
                  }
                  if (isLoading) {
                    setIsLoading(false);
                  }
                }}
                maxLength="6"
                required
                disabled={isLoading}
                className={isCodeValid ? 'code-valid' : ''}
              />
            </div>
            <button type="submit" className="forgot-password-btn" disabled={isLoading || !code.trim()}>
              {isLoading ? (
                <>
                  <span className="loading-spinner"></span>
                  Đang xác minh...
                </>
              ) : (
                'Xác minh mã'
              )}
            </button>
            <button type="button" className="resend-btn" onClick={handleResendCode} disabled={isLoading}>
              Gửi lại mã
            </button>
          </form>
        )}

        {step === 3 && (
          <form onSubmit={handleResetPassword} className="forgot-password-form">
            <div className="input-group">
              <label htmlFor="newPassword">Mật khẩu mới</label>
              <div className="password-input-container">
                <input
                  id="newPassword"
                  type={showPassword ? "text" : "password"}
                  placeholder="Nhập mật khẩu mới"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                  disabled={isLoading}
                />
                <button 
                  type="button" 
                  className="password-toggle" 
                  onClick={() => setShowPassword(!showPassword)}
                  disabled={isLoading}
                >
                  {showPassword ? "Ẩn" : "Hiện"}
                </button>
              </div>
            </div>
            <div className="input-group">
              <label htmlFor="confirmPassword">Xác nhận mật khẩu</label>
              <div className="password-input-container">
                <input
                  id="confirmPassword"
                  type={showConfirmPassword ? "text" : "password"}
                  placeholder="Nhập lại mật khẩu mới"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                  disabled={isLoading}
                />
                <button 
                  type="button" 
                  className="password-toggle" 
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  disabled={isLoading}
                >
                  {showConfirmPassword ? "Ẩn" : "Hiện"}
                </button>
              </div>
            </div>
            <button type="submit" className="forgot-password-btn" disabled={isLoading}>
              {isLoading ? 'Đang đặt lại...' : 'Đặt lại mật khẩu'}
            </button>
          </form>
        )}

        <div className="forgot-password-footer">
          <Link to="/login" className="back-to-login">← Quay lại đăng nhập</Link>
        </div>
      </div>
    </div>
  );
}