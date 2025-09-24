import React, { useEffect, useState } from 'react';
import { useHistory, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import authService from '../services/authService';

const OAuth2Callback = () => {
  const [status, setStatus] = useState('processing');
  const [message, setMessage] = useState('Đang đăng nhập...');
  const history = useHistory();
  const location = useLocation();
  const { checkAuthStatus } = useAuth();

  useEffect(() => {
    handleCallback();
  }, []);

  const handleCallback = async () => {
    try {
      setMessage('Vui lòng đợi đăng nhập hoàn tất...');
      
      // Đợi một chút để Spring Security hoàn tất xử lý
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      // Kiểm tra trạng thái authentication từ backend
      const userData = await authService.handleHostedUICallback();
      
      if (userData) {
        setStatus('success');
        setMessage('Đăng nhập thành công! Đang chuyển hướng...');
        
        // Cập nhật auth context
        await checkAuthStatus();
        
        // Chuyển hướng đến dashboard sau 1 giây
        setTimeout(() => {
          history.push('/dashboard');
        }, 1000);
      } else {
        // Thử kiểm tra trực tiếp với backend
        try {
          await checkAuthStatus();
          setStatus('success');
          setMessage('Đăng nhập thành công! Đang chuyển hướng...');
          
          setTimeout(() => {
            history.push('/dashboard');
          }, 1000);
        } catch (checkError) {
          // Nếu vẫn thất bại, chuyển về login
          setStatus('error');
          setMessage('Không thể xác thực. Vui lòng đăng nhập lại.');
          
          setTimeout(() => {
            history.push('/login');
          }, 3000);
        }
      }
      
    } catch (error) {
      setStatus('error');
      setMessage('Đăng nhập thất bại. Vui lòng thử lại.');
      
      // Chuyển hướng về login sau 3 giây
      setTimeout(() => {
        history.push('/login');
      }, 3000);
    }
  };

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <div style={styles.spinner}>
          {status === 'processing' && <div style={styles.loader}></div>}
          {status === 'success' && <div style={styles.success}>✓</div>}
          {status === 'error' && <div style={styles.error}>✗</div>}
        </div>
        <h2 style={styles.title}>
          {status === 'processing' && 'Đang xử lý...'}
          {status === 'success' && 'Thành công!'}
          {status === 'error' && 'Lỗi!'}
        </h2>
        <p style={styles.message}>{message}</p>
      </div>
    </div>
  );
};

const styles = {
  container: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    minHeight: '100vh',
    backgroundColor: '#f5f5f5'
  },
  card: {
    backgroundColor: 'white',
    padding: '2rem',
    borderRadius: '8px',
    boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
    textAlign: 'center',
    maxWidth: '400px',
    width: '90%'
  },
  spinner: {
    marginBottom: '1rem'
  },
  loader: {
    border: '4px solid #f3f3f3',
    borderTop: '4px solid #3498db',
    borderRadius: '50%',
    width: '40px',
    height: '40px',
    animation: 'spin 1s linear infinite',
    margin: '0 auto'
  },
  success: {
    fontSize: '2rem',
    color: '#27ae60',
    fontWeight: 'bold'
  },
  error: {
    fontSize: '2rem',
    color: '#e74c3c',
    fontWeight: 'bold'
  },
  title: {
    color: '#333',
    marginBottom: '1rem'
  },
  message: {
    color: '#666',
    lineHeight: 1.5
  }
};

// CSS cho animation
const styleSheet = document.createElement("style");
styleSheet.type = "text/css";
styleSheet.innerText = `
  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
`;
document.head.appendChild(styleSheet);

export default OAuth2Callback;