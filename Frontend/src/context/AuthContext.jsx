import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    // Check if token exists in session
    const token = localStorage.getItem('truehunt_token');
    const storedUser = localStorage.getItem('truehunt_user');
    if (token && storedUser) {
      try {
        setUser(JSON.parse(storedUser));
      } catch (error) {
        console.warn('Could not parse the cached user during sign-in initialization:', error);
        localStorage.removeItem('truehunt_user');
      }
    }
    setLoading(false);
  }, []);

  const login = async (identifier, password) => {
    const res = await api.auth.login(identifier, password);
    if (!res.data?.token || !res.data?.user) {
      throw new Error('The authentication service returned an invalid sign-in response.');
    }

    localStorage.setItem('truehunt_token', res.data.token);
    localStorage.setItem('truehunt_user', JSON.stringify(res.data.user));
    setUser(res.data.user);
    return { success: true, user: res.data.user };
  };

  const register = async (formData) => {
    const res = await api.auth.register(formData);
    if (!res.data?.token || !res.data?.user) {
      throw new Error('The authentication service returned an invalid registration response.');
    }

    localStorage.setItem('truehunt_token', res.data.token);
    localStorage.setItem('truehunt_user', JSON.stringify(res.data.user));
    setUser(res.data.user);
    return { success: true, user: res.data.user };
  };

  const logout = useCallback(() => {
    const role = String(user?.role || '').trim().toUpperCase();
    const signInRole = role === 'STUDIO' || role === 'PRODUCER' ? 'STUDIO' : 'TALENT';
    localStorage.removeItem('truehunt_token');
    localStorage.removeItem('truehunt_user');
    const userKeys = [user?.userId, user?.id, user?.email].filter(Boolean);
    userKeys.forEach((key) => {
      localStorage.removeItem(`truehunt-profile:${encodeURIComponent(String(key).trim().toLowerCase())}`);
    });
    localStorage.removeItem('truehunt-profile:me');
    setUser(null);
    navigate(`/sign-in?role=${signInRole}`);
  }, [navigate, user]);

  const updateUser = useCallback((updates) => {
    if (!user) return;
    const nextUser = { ...user, ...updates };
    localStorage.setItem('truehunt_user', JSON.stringify(nextUser));
    setUser(nextUser);
  }, [user]);

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, updateUser, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
