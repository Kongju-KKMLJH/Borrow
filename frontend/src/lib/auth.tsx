import AsyncStorage from '@react-native-async-storage/async-storage';
import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';

import { authApi } from '@/lib/api/endpoints';
import { clearCredentials, setCredentials } from '@/lib/api/client';
import type { MeResponse, SignupRequest } from '@/lib/api/types';

const STORAGE_KEY = 'borrow.auth';

export interface AuthContextValue {
  user: MeResponse | null;
  loading: boolean;
  login: (loginId: string, password: string) => Promise<void>;
  signup: (data: SignupRequest) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue>({
  user: null,
  loading: true,
  login: async () => {},
  signup: async () => {},
  logout: async () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<MeResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      const stored = await AsyncStorage.getItem(STORAGE_KEY);
      if (!stored) {
        setLoading(false);
        return;
      }
      try {
        const { loginId, password } = JSON.parse(stored) as { loginId: string; password: string };
        setCredentials(loginId, password);
        const me = await authApi.me();
        setUser(me);
      } catch {
        await AsyncStorage.removeItem(STORAGE_KEY);
        clearCredentials();
      }
      setLoading(false);
    })();
  }, []);

  async function login(loginId: string, password: string) {
    setCredentials(loginId, password);
    const me = await authApi.me();
    await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify({ loginId, password }));
    setUser(me);
  }

  async function signup(data: SignupRequest) {
    const me = await authApi.signup(data);
    setCredentials(data.loginId, data.password);
    await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify({ loginId: data.loginId, password: data.password }));
    setUser(me);
  }

  async function logout() {
    await AsyncStorage.removeItem(STORAGE_KEY);
    clearCredentials();
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, signup, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
