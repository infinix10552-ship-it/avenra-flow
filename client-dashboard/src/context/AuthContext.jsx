import { useState, useEffect } from 'react';
import { AuthContext } from './useAuth';

export const AuthProvider = ({ children }) => {
    // Boot Sequence: Check localStorage synchronously on first render
    const [isAuthenticated, setIsAuthenticated] = useState(() => {
        const token = localStorage.getItem('avenra_token');
        const orgId = localStorage.getItem('avenra_org_id');
        return !!(token && orgId);
    });

    // The Login function — called from Login component and OAuth2 redirect
    const login = (token, orgId) => {
        localStorage.setItem('avenra_token', token);
        localStorage.setItem('avenra_org_id', orgId);
        setIsAuthenticated(true);
    };

    // The Logout function
    const logout = () => {
        localStorage.removeItem('avenra_token');
        localStorage.removeItem('avenra_org_id');
        setIsAuthenticated(false);
        // Use replace to prevent users from hitting the browser back button into a broken state
        window.location.replace('/login');
    };

    // CRITICAL FIX for Login Loop: Listen for the custom 'auth:logout' event
    // dispatched by the Axios interceptor when a 401 is received.
    // This avoids the interceptor doing a hard window.location redirect which
    // bypasses React state, causing a redirect loop — instead we let React
    // cleanly clear auth state first, then redirect.
    useEffect(() => {
        const handleAuthFailure = () => {
            console.warn('[AUTH] Session expired. Clearing vault and redirecting to login.');
            localStorage.removeItem('avenra_token');
            localStorage.removeItem('avenra_org_id');
            setIsAuthenticated(false);
            // Use replace so the broken page isn't in browser history
            window.location.replace('/login');
        };

        window.addEventListener('auth:logout', handleAuthFailure);
        return () => window.removeEventListener('auth:logout', handleAuthFailure);
    }, []);

    return (
        <AuthContext.Provider value={{ isAuthenticated, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
};
