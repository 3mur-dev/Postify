import React, { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { setToken } from "../utils/token";
import { login } from "../api/authApi";
import { useAuth } from "../context/useAuth";
import { toast } from "sonner";
import { getApiErrorMessage } from "../utils/apiError";

function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();
  const location = useLocation();
  const { refreshUser } = useAuth();

  const getRedirectPath = () => {
    const state = location.state;
    if (!state || typeof state !== "object" || !("from" in state)) {
      return "/";
    }

    const from = state.from;
    if (!from || typeof from !== "object" || !("pathname" in from)) {
      return "/";
    }

    const { pathname, search, hash } = from;
    if (
      typeof pathname !== "string" ||
      !pathname.startsWith("/") ||
      pathname.startsWith("//") ||
      pathname === "/login" ||
      pathname === "/register"
    ) {
      return "/";
    }

    return `${pathname}${typeof search === "string" ? search : ""}${
      typeof hash === "string" ? hash : ""
    }`;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const { token } = await login({ username, password });

      setToken(token);
      await refreshUser();
      navigate(getRedirectPath(), { replace: true });
    } catch (err: unknown) {
      toast.error(getApiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-950 px-4 py-6 sm:px-6 lg:px-8">
      <div className="w-full max-w-md rounded-2xl border border-white/10 bg-slate-900/80 p-6 shadow-2xl shadow-black/40 backdrop-blur sm:p-8">
        <h1 className="text-2xl font-semibold text-white">
          Sign in
        </h1>
        <p className="mt-1 mb-6 text-sm text-slate-400">
          Welcome back. Enter your credentials to continue.
        </p>
        <form onSubmit={handleSubmit} className="space-y-4">
          
          <div>
            <label className="text-sm text-slate-300">Username</label>
            <input
              className="mt-1 w-full rounded-lg border border-white/10 bg-slate-800 px-3 py-2 text-white placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-fuchsia-500"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Enter username"
            />
          </div>

          <div>
            <label className="text-sm text-slate-300">Password</label>
            <input
              type="password"
              className="mt-1 w-full rounded-lg border border-white/10 bg-slate-800 px-3 py-2 text-white placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-fuchsia-500"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter password"
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-lg bg-fuchsia-600 py-2 font-medium text-white transition hover:bg-fuchsia-500 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {loading ? 'Signing in...' : 'Sign in'}
          </button>
           <div className="mt-6 text-center text-sm text-slate-400">
            Don't have an account?{" "}
           <button
            type="button"
            onClick={() => navigate("/register", { state: location.state })}
            className="font-medium text-fuchsia-400 transition hover:text-fuchsia-300"
           >
            Create one
           </button>
         </div>
        </form>
      </div>
    </div>
  )
}

export default LoginPage