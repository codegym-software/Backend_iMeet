import React from 'react'
import {Link} from 'react-router-dom'

// Import và export các components
export { default as Login } from './Login';
export { default as Signup } from './Signup';
export { default as Dashboard } from './Dashboard';
export { default as Profile } from './Profile';
export { default as OAuth2Callback } from './OAuth2Callback';

export default function HomePage () {
    return (
	<div>
		<Link to='/Signup'><div>Signup</div></Link>
		<Link to='/Login'><div>Login</div></Link>
	</div>
	)
}