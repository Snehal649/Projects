import { createContext, useContext, useState } from 'react';

const Ctx = createContext(null);
export const useAuth = () => useContext(Ctx);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => JSON.parse(localStorage.getItem('user') || 'null'));
  const [privacy, setPrivacy] = useState(() => localStorage.getItem('privacy') === '1');

  const login = (data) => {
    localStorage.setItem('token', data.token);
    const u = { name: data.name, email: data.email };
    localStorage.setItem('user', JSON.stringify(u));
    setUser(u);
  };
  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  };
  const togglePrivacy = () => {
    setPrivacy((p) => {
      localStorage.setItem('privacy', p ? '0' : '1');
      return !p;
    });
  };

  return <Ctx.Provider value={{ user, login, logout, privacy, togglePrivacy }}>{children}</Ctx.Provider>;
}
