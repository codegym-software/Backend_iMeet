import React, { useState } from 'react'
import './Login.css'
import './common/PasswordToggleStyles.css'
import ImgAsset from '../public'
import { Link, useHistory } from 'react-router-dom' 
import { useAuth } from '../contexts/AuthContext'

export default function Login() {
	const [email, setEmail] = useState('');
	const [password, setPassword] = useState('');
	const [showPassword, setShowPassword] = useState(false);
	const [loginStatus, setLoginStatus] = useState({ message: '', type: '' });
	const [isLoading, setIsLoading] = useState(false);
	
	const { login, loginWithCognito, loginWithCognitoHostedUI } = useAuth();
	const history = useHistory();

	const handleLogin = async (e) => {
		e.preventDefault();
		if (!email || !password) {
			setLoginStatus({ message: 'Vui lòng nhập đầy đủ thông tin!', type: 'error' });
			return;
		}
		
		setIsLoading(true);
		setLoginStatus({ message: '', type: '' });

		try {
			const result = await login(email, password);
			
			if (result.success) {
				setLoginStatus({ message: result.message, type: 'success' });
				// Chuyển hướng đến dashboard sau khi đăng nhập thành công
				setTimeout(() => {
					history.push('/dashboard');
				}, 1000);
			} else {
				setLoginStatus({ message: result.message, type: 'error' });
			}
		} catch (error) {
			setLoginStatus({ message: 'Có lỗi xảy ra khi đăng nhập!', type: 'error' });
		} finally {
			setIsLoading(false);
		}
	};

	// Xử lý đăng nhập Cognito Server-side
	const handleCognitoLogin = () => {
		setLoginStatus({ message: 'Đang chuyển hướng đến Google...', type: 'info' });
		loginWithCognitoHostedUI(); // Sử dụng server-side flow
	};

	return (
		<div className='login'>
			<div className='login-container'>
				<div className='login-box'>
					<div className='logo-section'>
						<div className='logo'>
							<img src='/logo.jpg' alt='iMeet' className='logo-img' />
							<span className='logo-text'>iMeet</span>
						</div>
						<p className='logo-subtitle'>Hệ thống đặt lịch họp trực tuyến</p>
					</div>
					
					<div className='login-header'>
						<span className='welcome-text'>Welcome back!</span>
					</div>
					<span className='sub-text'>Enter your credentials to access your account</span>

					<form onSubmit={handleLogin}>
						{/* Username/Email */}
						<div className='input-group'>
							<label className='input-label' htmlFor="email">Username or Email</label>
							<input
								id="email"
								type="text"
								placeholder="Enter your username or email"
								className='input-field input-username'
								value={email}
								onChange={(e) => setEmail(e.target.value)}
								required
								disabled={isLoading}
							/>
						</div>

						{/* Password */}
						<div className='input-group'>
							<label className='input-label' htmlFor="password">Password</label>
							<div className="password-input-container">
								<input
									id="password"
									type={showPassword ? "text" : "password"}
									placeholder="Enter your password"
									className='input-field input-password'
									value={password}
									onChange={(e) => setPassword(e.target.value)}
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

						{loginStatus.message && (
							<div className={`status-message ${loginStatus.type}`}>
								{loginStatus.message}
							</div>
						)}

						<div className='signup-link'>
							<span>
								Don't have an account? <Link to="/signup" className="signup-link-text">Sign Up</Link>
							</span>
						</div>

						{/* Login button */}
						<button 
							type="submit" 
							className='login-button'
							disabled={isLoading}
						>
							{isLoading ? 'Đang đăng nhập...' : 'Login'}
						</button>
					</form>

					{/* Divider */}
					<div className='divider'>
						<div className='line'></div>
						<div className='divider-text'>
							<span>Or</span>
						</div>
						<div className='line'></div>
					</div>

					{/* Social login */}
					<div className='social-login'>
						{/* Nút đăng nhập Cognito (server-side flow) */}
						<div className='cognito-login' onClick={handleCognitoLogin}>
							<svg className='cognito-icon' width="20" height="20" viewBox="0 0 24 24">
								<path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
								<path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
								<path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
								<path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
							</svg>
							<span>Sign in with Google</span>
						</div>
					</div>

					{/* Forgot password */}
					<span className='forgot-password'>
						<Link to="/forgot-password" className="forgot-password-link">Forgot password?</Link>
					</span>
				</div>
			</div>

			<img className='login-image' src={ImgAsset.Signup_chrislee70l1tDAI6rMunsplash1} alt="Login background" />
		</div>
	)
}