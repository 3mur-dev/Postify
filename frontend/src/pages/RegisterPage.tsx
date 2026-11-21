import React, { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { register } from "../api/authApi";
import { getApiErrorMessage } from "../utils/apiError";
import { toast } from "sonner";

function RegisterPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();
  const location = useLocation();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await register({ username, email, password, confirmPassword });
      navigate("/login", { state: location.state });
    } catch (err: unknown) {
      toast.error(getApiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

return (
      <div className="flex min-h-screen items-center justify-center bg-slate-950 px-4 py-6 sm:px-6 lg:px-8">
        <div className="w-full max-w-md rounded-2xl border border-white/10 bg-slate-900/80 p-6 shadow-2xl shadow-black/40 backdrop-blur sm:p-8">
          <h1 className="text-2xl font-semibold text-white">Register</h1>
          <form className="mt-6" onSubmit={handleSubmit}>
            <div>
              <label className="block text-sm font-medium text-slate-300">Username</label>
              <input
                required
                type="text"
                className="mt-1 block w-full rounded-lg border border-white/10 bg-slate-800 px-3 py-2 text-white placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-fuchsia-500"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
              />
            </div>  
            <div className="mt-4">
              <label className="block text-sm font-medium text-slate-300">Email</label>
              <input
                required
                type="email"
                className="mt-1 block w-full rounded-lg border border-white/10 bg-slate-800 px-3 py-2 text-white placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-fuchsia-500"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>          
            <div className="mt-4">
              <label className="block text-sm font-medium text-slate-300">Password</label>
              <input
                required
                type="password"
                className="mt-1 block w-full rounded-lg border border-white/10 bg-slate-800 px-3 py-2 text-white placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-fuchsia-500"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>

            <div className="mt-4">
              <label className="block text-sm font-medium text-slate-300">Confirm Password</label>
              <input
                required
                type="password"
                className="mt-1 block w-full rounded-lg border border-white/10 bg-slate-800 px-3 py-2 text-white placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-fuchsia-500"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
            </div>
            
            <div className="mt-6">
        
              <button
                type="submit"
                disabled={loading}
                className="w-full rounded-lg bg-fuchsia-600 py-2 font-medium text-white transition hover:bg-fuchsia-500 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {loading ? 'Registering...' : 'Register'}
              </button>
            </div>
            <div className="mt-6 text-center text-sm text-slate-400">
  Already have an account?{" "}
  <button
    type="button"
    onClick={() => navigate("/login", { state: location.state })}
    className="font-medium text-fuchsia-400 transition hover:text-fuchsia-300"
  >
    Sign in
  </button>
</div>
          </form>
        </div>
      </div>
    )
  }

export default RegisterPage