import { useState } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import api from "../api/axiosInterceptor";
import { Input } from "../components/ui/Input";
import { Button } from "../components/ui/Button";
import logo from "../assets/avenra-logo.png";

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");
  const navigate = useNavigate();

  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setIsLoading(true);
    setError("");

    try {
      await api.post("/auth/reset-password", { token, newPassword: password });
      setMessage("Password successfully reset! Redirecting to login...");
      setTimeout(() => navigate("/login"), 3000);
    } catch (err) {
      setError(err.response?.data?.error || "Failed to reset password. The link may have expired.");
    } finally {
      setIsLoading(false);
    }
  };

  if (!token) {
    return (
      <div className="min-h-[100dvh] flex items-center justify-center bg-slate-950">
        <div className="p-10 text-center text-red-500 bg-white rounded-xl">Invalid or missing reset token.</div>
      </div>
    );
  }

  return (
    <div className="min-h-[100dvh] w-full flex items-center justify-center relative overflow-hidden bg-gradient-to-br from-slate-950 via-avenra-950 to-slate-900">
      
      {/* Pattern and Orbs */}
      <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAiIGhlaWdodD0iMjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iMSIgY3k9IjEiIHI9IjEiIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC4wNykiLz48L3N2Zz4=')] [mask-image:linear-gradient(to_bottom,white,transparent)] pointer-events-none"></div>
      <div className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] rounded-full bg-avenra-600/30 blur-[120px] pointer-events-none animate-pulse" style={{ animationDuration: '6s' }}></div>
      <div className="absolute bottom-[-10%] right-[-5%] w-[40%] h-[40%] rounded-full bg-avenra-400/20 blur-[100px] pointer-events-none"></div>

      <div className="w-full max-w-md bg-white/95 backdrop-blur-md rounded-3xl shadow-[0_8px_30px_rgb(0,0,0,0.12)] border border-white/20 p-6 sm:p-10 z-10 mx-4 animate-in fade-in zoom-in-95 duration-500">
        
        <div className="flex justify-center mb-8">
            <img src={logo} alt="Avenra Logo" className="w-12 h-12 object-contain rounded-md shadow-sm" />
        </div>

        <div className="text-center mb-8">
          <h2 className="text-2xl font-bold text-slate-900 tracking-tight">Set New Password</h2>
          <p className="text-slate-500 mt-2 text-sm">Please enter your new password below to secure your vault.</p>
        </div>

        {error && <div className="mb-4 p-3 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg">{error}</div>}
        {message && <div className="mb-4 p-3 text-sm text-green-700 bg-green-50 border border-green-200 rounded-lg">{message}</div>}

        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="space-y-1">
            <label className="block text-sm font-medium text-slate-700">New Password</label>
            <Input type="password" placeholder="••••••••" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </div>
          <div className="space-y-1">
            <label className="block text-sm font-medium text-slate-700">Confirm Password</label>
            <Input type="password" placeholder="••••••••" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required />
          </div>
          <Button type="submit" className="w-full mt-4" isLoading={isLoading}>Reset Password</Button>
        </form>
      </div>
    </div>
  );
}