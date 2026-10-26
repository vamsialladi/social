import React, { createContext, useContext, useState, useEffect } from 'react';
import { api } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check if token exists in session
    const token = localStorage.getItem('truehunt_token');
    const storedUser = localStorage.getItem('truehunt_user');
    if (token && storedUser) {
      try {
        setUser(JSON.parse(storedUser));
      } catch (e) {
        localStorage.removeItem('truehunt_user');
      }
    }
    setLoading(false);
  }, []);

  const login = async (email, password) => {
    const res = await api.auth.login(email, password);
    if (res.data && res.data.token) {
      localStorage.setItem('truehunt_token', res.data.token);
      const userData = res.data.user || {
        id: 'u-1',
        name: email.split('@')[0],
        email,
        role: 'TALENT'
      };
      localStorage.setItem('truehunt_user', JSON.stringify(userData));
      setUser(userData);
      return { success: true, user: userData };
    }
    return { success: false, message: 'Invalid credentials' };
  };

  const register = async (formData) => {
    const res = await api.auth.register(formData);
    if (res.data && res.data.token) {
      localStorage.setItem('truehunt_token', res.data.token);
      const userData = res.data.user || {
        id: `u-${Date.now()}`,
        name: formData.fullName || formData.name,
        email: formData.email,
        role: formData.role || 'TALENT'
      };
      localStorage.setItem('truehunt_user', JSON.stringify(userData));
      setUser(userData);
      return { success: true, user: userData };
    }
    return { success: false, message: 'Registration failed' };
  };

  const logout = () => {
    localStorage.removeItem('truehunt_token');
    localStorage.removeItem('truehunt_user');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, isAuthenticated: !!user }}>
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
