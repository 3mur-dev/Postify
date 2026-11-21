import { createContext } from "react";
import type { ProfileResponse } from "../api/profileApi";

export type AuthContextType = {
  user: ProfileResponse | null;
  loading: boolean;
  isAuthenticated: boolean;
  refreshUser: () => Promise<void>;
  logout: () => void;
};

export const AuthContext = createContext<AuthContextType | undefined>(
  undefined,
);