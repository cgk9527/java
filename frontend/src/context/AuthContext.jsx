import React, { createContext, useContext, useState, useEffect } from 'react';
import { login as loginApi, register as registerApi, getMe } from '../api/index.js';
import { message } from 'antd';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('user') || 'null');
    } catch {
      return null;
    }
  });

  const applyUser = (u) => {
    setUser(u);
    if (u) {
      localStorage.setItem('user', JSON.stringify(u));
    } else {
      localStorage.removeItem('user');
    }
  };

  useEffect(() => {
    if (localStorage.getItem('token')) {
      getMe()
        .then(applyUser)
        .catch(() => {});
    }
  }, []);

  const login = async (values) => {
    const data = await loginApi(values);
    localStorage.setItem('token', data.token);
    applyUser(data.user);
    return data;
  };

  const register = async (values) => {
    await registerApi(values);
  };

  const logout = () => {
    localStorage.removeItem('token');
    applyUser(null);
    message.success('已退出登录');
    window.location.href = '/';
  };

  return (
    <AuthContext.Provider value={{ user, login, register, logout, setUser: applyUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}