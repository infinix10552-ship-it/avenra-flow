import { useState } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import api from "../api/axiosInterceptor";
import { Input } from "../components/ui/Input";
import { Button } from "../components/ui/Button";

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
      // Send the token and the new password to your Java backend
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
    return <div className="p-10 text-center text-red-500">Invalid or missing reset token.</div>;
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-avenra-950">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-2xl p-8">
        <h2 className="text-2xl font-bold text-slate-900 mb-2">Set New Password</h2>
        <p className="text-slate-500 text-sm mb-6">Please enter your new password below.</p>

        {error && <div className="mb-4 p-3 text-sm text-red-600 bg-red-50 rounded-lg">{error}</div>}
        {message && <div className="mb-4 p-3 text-sm text-green-600 bg-green-50 rounded-lg">{message}</div>}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700">New Password</label>
            <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700">Confirm Password</label>
            <Input type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required />
          </div>
          <Button type="submit" className="w-full" isLoading={isLoading}>Reset Password</Button>
        </form>
      </div>
    </div>
  );
}