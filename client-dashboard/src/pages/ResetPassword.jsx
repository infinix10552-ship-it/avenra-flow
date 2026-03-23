import { useState } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { motion as Motion } from "framer-motion";
import api from "../api/axiosInterceptor";
import { Input } from "../components/ui/Input";
import { Button } from "../components/ui/Button";
import logo from "../assets/avenra-logo.png";

const StripeBackground = () => (
  <div className="absolute inset-0 overflow-hidden pointer-events-none bg-[#060b14]">
    <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iMSIgY3k9IjEiIHI9IjEiIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC4wNCkiLz48L3N2Zz4=')] [mask-image:linear-gradient(to_bottom,white,transparent)]"></div>
    <Motion.div animate={{ scale: [1, 1.1, 1], opacity: [0.15, 0.25, 0.15] }} transition={{ duration: 8, repeat: Infinity, ease: "easeInOut" }} className="absolute top-[-10%] right-[-5%] w-[500px] h-[500px] rounded-full bg-indigo-600/30 blur-[120px] mix-blend-screen" />
    <Motion.div animate={{ scale: [1, 1.2, 1], opacity: [0.1, 0.2, 0.1] }} transition={{ duration: 10, repeat: Infinity, ease: "easeInOut" }} className="absolute bottom-[-10%] left-[-10%] w-[600px] h-[600px] rounded-full bg-blue-600/20 blur-[120px] mix-blend-screen" />
    <div className="absolute inset-0 flex items-center justify-center opacity-60">
      <svg className="w-[150%] min-w-[1000px] h-[150%] max-w-none" viewBox="0 0 1000 1000" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="ribbon-grad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#305ba3" stopOpacity="0" />
            <stop offset="20%" stopColor="#4a7acb" stopOpacity="0.6" />
            <stop offset="50%" stopColor="#8b5cf6" stopOpacity="0.8" />
            <stop offset="80%" stopColor="#254682" stopOpacity="0.4" />
            <stop offset="100%" stopColor="#060b14" stopOpacity="0" />
          </linearGradient>
        </defs>
        <g stroke="url(#ribbon-grad)" fill="none" strokeWidth="1.5">
          {[...Array(6)].map((_, i) => (
            <Motion.path key={i} initial={{ pathLength: 0, opacity: 0 }} animate={{ pathLength: 1, opacity: 1 - (i * 0.15), d: [ `M-200,${400 + i * 30} C100,${500 - i * 20} 500,${300 + i * 40} 1200,${500 + i * 20}`, `M-200,${420 + i * 30} C150,${480 - i * 20} 450,${320 + i * 40} 1200,${480 + i * 20}`, `M-200,${400 + i * 30} C100,${500 - i * 20} 500,${300 + i * 40} 1200,${500 + i * 20}` ] }} transition={{ pathLength: { duration: 2, ease: "easeOut", delay: i * 0.2 }, opacity: { duration: 1, delay: i * 0.2 }, d: { duration: 12 + i * 2, repeat: Infinity, ease: "easeInOut" } }} />
          ))}
        </g>
      </svg>
    </div>
  </div>
);

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
        <Motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="p-10 text-center text-red-500 bg-white rounded-xl shadow-xl">
          Invalid or missing reset token.
        </Motion.div>
      </div>
    );
  }

  return (
    <div className="min-h-[100dvh] w-full flex items-center justify-center relative bg-[#060b14]">
      
      <StripeBackground />

      <Motion.div 
        initial={{ opacity: 0, y: 30 }} 
        animate={{ opacity: 1, y: 0 }} 
        transition={{ duration: 0.8, ease: "easeOut" }}
        className="w-full max-w-md bg-white/95 backdrop-blur-xl rounded-3xl shadow-[0_8px_30px_rgb(0,0,0,0.12)] border border-white/20 p-6 sm:p-10 z-10 mx-4"
      >
        <div className="flex justify-center mb-8">
            <img src={logo} alt="Avenra Logo" className="w-12 h-12 object-contain rounded-md shadow-sm" />
        </div>

        <div className="text-center mb-8">
          <h2 className="text-2xl font-bold text-slate-900 tracking-tight">Set New Password</h2>
          <p className="text-slate-500 mt-2 text-sm">Please enter your new password below to secure your vault.</p>
        </div>

        {error && <Motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="mb-4 p-3 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg">{error}</Motion.div>}
        {message && <Motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="mb-4 p-3 text-sm text-green-700 bg-green-50 border border-green-200 rounded-lg">{message}</Motion.div>}

        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="space-y-1">
            <label className="block text-sm font-medium text-slate-700">New Password</label>
            <Input type="password" placeholder="••••••••" value={password} onChange={(e) => setPassword(e.target.value)} required className="bg-white/50 focus:bg-white" />
          </div>
          <div className="space-y-1">
            <label className="block text-sm font-medium text-slate-700">Confirm Password</label>
            <Input type="password" placeholder="••••••••" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required className="bg-white/50 focus:bg-white" />
          </div>
          <Button type="submit" className="w-full mt-4" isLoading={isLoading}>Reset Password</Button>
        </form>
      </Motion.div>
    </div>
  );
}
