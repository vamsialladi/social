import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
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
      } catch (e) {
        localStorage.removeItem('truehunt_user');
      }
    }
    setLoading(false);
  }, []);

  const login = async (email, password, role) => {
    const res = await api.auth.login(email, password, role);
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
    localStorage.removeItem('truehunt_token');
    localStorage.removeItem('truehunt_user');
    setUser(null);
    navigate('/');
  }, [navigate]);

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
