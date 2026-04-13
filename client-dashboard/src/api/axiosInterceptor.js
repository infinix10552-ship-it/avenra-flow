import axios from 'axios';

// 1. Initialize the core engine pointing to your Spring Boot server
const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    headers: {
        'Content-Type': 'application/json'
    }
});

// 2. THE REQUEST SHIELD: Run this before ANY request leaves the browser
api.interceptors.request.use(
    (config) => {
        // Pull the cryptographic keys from the browser's local vault
        const token = localStorage.getItem('avenra_token');
        const orgId = localStorage.getItem('avenra_org_id');

        // If we have a token, attach it to the Authorization header
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        // If we have an Org ID, attach it so Java's TenantSecurityManager grants access
        if (orgId) {
            config.headers['X-Organization-Id'] = orgId;
        }

        return config;
    },
    (error) => Promise.reject(error)
);

// 3. THE RESPONSE CATCHER: Handle backend rejections globally
api.interceptors.response.use(
    (response) => response,
    (error) => {
        // If Java says 401 Unauthorized (token expired/tampered), clear credentials.
        if (error.response && error.response.status === 401) {
            console.warn('[AUTH] Vault access denied. Token expired or invalid.');

            // CRITICAL FIX for Login Loop:
            // Instead of calling window.location.href directly (which hard-reloads
            // the page and bypasses React state, causing a redirect loop), we dispatch
            // a custom event. AuthContext listens for this event and performs a clean
            // React state update followed by window.location.replace('/login').
            // This ensures React has a chance to tear down state correctly before navigating.
            //
            // Guard: Only dispatch if we actually had credentials stored (prevents
            // firing on public endpoints that legitimately return 401).
            const hadToken = localStorage.getItem('avenra_token');
            if (hadToken) {
                localStorage.removeItem('avenra_token');
                localStorage.removeItem('avenra_org_id');
                window.dispatchEvent(new Event('auth:logout'));
            }
        }
        return Promise.reject(error);
    }
);

export default api;