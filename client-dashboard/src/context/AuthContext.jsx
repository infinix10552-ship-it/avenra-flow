import { useState } from 'react';
import { AuthContext } from './useAuth';

export const AuthProvider = ({ children }) => {
    // Boot Sequence: Check if we have keys in storage when the app opens
    const [isAuthenticated, setIsAuthenticated] = useState(() => {
        const token = localStorage.getItem('avenra_token');
        const orgId = localStorage.getItem('avenra_org_id');
        return !!(token && orgId);
    });
    const [isLoading] = useState(false);

    // The Login function (We will call this from the Login Component and OAuth2 Redirect)
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
        window.location.href = '/login';
    };

    if (isLoading) {
        // A premium, mobile-responsive loading screen
        return (
            <div className="min-h-screen bg-avenra-900 flex items-center justify-center p-4">
                <div className="flex flex-col items-center space-y-4">
                    <div className="w-8 h-8 border-4 border-avenra-500 border-t-transparent rounded-full animate-spin"></div>
                    <p className="text-avenra-400 font-medium tracking-wide text-sm sm:text-base text-center">
                        Initializing Avenra FLOW...
                    </p>
                </div>
            </div>
        );
    }

    return (
        <AuthContext.Provider value={{ isAuthenticated, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
};