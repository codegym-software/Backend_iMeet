import React, { useState } from 'react'
import './Login.css'
import ImgAsset from '../public/index'
import { Link, useHistory } from 'react-router-dom' 
import { useAuth } from '../contexts/AuthContext'

export default function Login() {
	const [email, setEmail] = useState(''); // Đổi từ username thành email
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
		console.log('Cognito server-side login clicked');
		setLoginStatus({ message: 'Đang chuyển hướng đến Cognito...', type: 'info' });
		loginWithCognitoHostedUI(); // Sử dụng server-side flow
	};

	return (
		<div className='login'>
			<div className='login-container'>
				<div className='login-box'>
					<div className='login-header'>
						<span className='welcome-text'>Welcome back!</span>
					</div>
					<span className='sub-text'>Enter your credentials to access your account</span>

					<form onSubmit={handleLogin}>
						{/* Email */}
						<div className='input-group'>
							<label className='input-label' htmlFor="email">Email</label>
							<input
								id="email"
								type="email"
								placeholder="Enter your email"
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
							<img className='cognito-icon' src={ImgAsset.Login_Vector} alt="Cognito" />
							<span>Sign in with Cognito</span>
						</div>
					</div>

					{/* Forgot password */}
					<span className='forgot-password'>Forgot password?</span>
				</div>
			</div>

			<img className='login-image' src={ImgAsset.Signup_chrislee70l1tDAI6rMunsplash1} alt="Login background" />
		</div>
	)
}
