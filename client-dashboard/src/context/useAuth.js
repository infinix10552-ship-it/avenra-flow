import { createContext, useContext } from 'react';

export const AuthContext = createContext();

// Custom hook for ultimate developer ergonomics
export const useAuth = () => useContext(AuthContext);
