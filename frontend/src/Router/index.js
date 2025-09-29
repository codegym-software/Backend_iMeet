import React from 'react';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import { AuthProvider } from '../contexts/AuthContext';
import ProtectedRoute from '../Components/ProtectedRoute';
import Signup from '../Components/Signup';
import Login from '../Components/Login';
import Dashboard from '../Components/Dashboard';
import Profile from '../Components/Profile';
import { OAuth2Callback } from '../Components';

const RouterDOM = () => {
	return (
		<AuthProvider>
			<Router>
				<Switch>
					<Route exact path="/">
						<Login />
					</Route>
					<Route exact path="/signup"><Signup /></Route>
					<Route exact path="/login"><Login /></Route>
					
					{/* Protected Routes - cần đăng nhập */}
					<ProtectedRoute exact path="/dashboard">
						<Dashboard />
					</ProtectedRoute>
					<ProtectedRoute exact path="/profile">
						<Profile />
					</ProtectedRoute>
					
					{/* Route xử lý callback từ Cognito */}
					<Route exact path="/oauth2/callback"><OAuth2Callback /></Route>
				</Switch>
			</Router>
		</AuthProvider>
	);
}


export default RouterDOM;