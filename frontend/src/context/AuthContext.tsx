
import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { useQueryClient } from "@tanstack/react-query";

import { getMyProfile } from "../api/profileApi";
import { AUTH_SESSION_EXPIRED_EVENT } from "../utils/authSession";
import { AuthContext } from "./createAuthContext";

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const userPublicIdRef = useRef<string | null>(null);
  const userTokenRef = useRef<string | null>(null);
  const [user, setUser] = useState<Awaited<ReturnType<typeof getMyProfile>> | null>(null);
  const [loading, setLoading] = useState(
    () => Boolean(localStorage.getItem("token")),
  );

  const clearPrivateData = useCallback(() => {
    queryClient.clear();
    userPublicIdRef.current = null;
    userTokenRef.current = null;
    setUser(null);
  }, [queryClient]);

  const setAuthenticatedUser = useCallback(
    (profile: Awaited<ReturnType<typeof getMyProfile>>, token: string) => {
      if (
        (userPublicIdRef.current &&
          userPublicIdRef.current !== profile.publicId) ||
        (userTokenRef.current && userTokenRef.current !== token)
      ) {
        queryClient.clear();
      }
      userPublicIdRef.current = profile.publicId;
      userTokenRef.current = token;
      setUser(profile);
    },
    [queryClient],
  );

  const refreshUser = useCallback(async () => {
    const token = localStorage.getItem("token");

    if (!token) {
      clearPrivateData();
      setLoading(false);
      return;
    }
    if (userTokenRef.current && userTokenRef.current !== token) {
      clearPrivateData();
    }

    try {
      const profile = await getMyProfile();
      if (localStorage.getItem("token") === token) {
        setAuthenticatedUser(profile, token);
      }
    } catch {
      if (localStorage.getItem("token") === token) {
        localStorage.removeItem("token");
        clearPrivateData();
      }
    } finally {
      setLoading(false);
    }
  }, [clearPrivateData, setAuthenticatedUser]);

  const logout = useCallback(() => {
    localStorage.removeItem("token");
    clearPrivateData();
    setLoading(false);
  }, [clearPrivateData]);

  useEffect(() => {
    const handleSessionExpired = () => {
      clearPrivateData();
      setLoading(false);
    };

    window.addEventListener(AUTH_SESSION_EXPIRED_EVENT, handleSessionExpired);
    return () => {
      window.removeEventListener(
        AUTH_SESSION_EXPIRED_EVENT,
        handleSessionExpired,
      );
    };
  }, [clearPrivateData]);

  useEffect(() => {
    const handleStorageChange = (event: StorageEvent) => {
      if (event.key !== "token") {
        return;
      }

      if (event.newValue === null) {
        // Token was removed in another tab — clear auth state
        clearPrivateData();
        setLoading(false);
      } else if (
        event.newValue !== null &&
        event.newValue !== userTokenRef.current
      ) {
        // Token was changed in another tab (e.g., new login or token rotation).
        // Clear stale data and refresh so this tab picks up the new session.
        clearPrivateData();
        setLoading(true);

        getMyProfile()
          .then((profile) => {
            if (localStorage.getItem("token") === event.newValue) {
              setAuthenticatedUser(profile, event.newValue!);
            }
          })
          .catch(() => {
            localStorage.removeItem("token");
            clearPrivateData();
          })
          .finally(() => {
            setLoading(false);
          });
      }
    };

    window.addEventListener("storage", handleStorageChange);
    return () => {
      window.removeEventListener("storage", handleStorageChange);
    };
  }, [clearPrivateData, setAuthenticatedUser]);

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) {
      return;
    }

    let cancelled = false;

    getMyProfile()
      .then((profile) => {
        if (!cancelled && localStorage.getItem("token") === token) {
          setAuthenticatedUser(profile, token);
        }
      })
      .catch(() => {
        if (!cancelled && localStorage.getItem("token") === token) {
          localStorage.removeItem("token");
          clearPrivateData();
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [clearPrivateData, setAuthenticatedUser]);

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        isAuthenticated: !!user,
        refreshUser,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
