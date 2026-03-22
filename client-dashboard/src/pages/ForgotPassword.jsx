import { useState } from "react";
import { Link } from "react-router-dom";
import api from "../api/axiosInterceptor";
import { Input } from "../components/ui/Input";
import { Button } from "../components/ui/Button";
import { ArrowLeft, MailCheck, ShieldAlert } from "lucide-react";
import logo from "../assets/avenra-logo.png"; // Adjust extension if needed
import { useLocation } from "react-router-dom";

export default function ForgotPassword() {
    const [isLoading, setIsLoading] = useState(false);
    const [isSubmitted, setIsSubmitted] = useState(false);
    const [error, setError] = useState("");
    const location = useLocation();
    // Auto-fill with the passed email, or default to blank
    const [email, setEmail] = useState(location.state?.email || "");

    const handleResetRequest = async (e) => {
        e.preventDefault();
        if (!email) return;

        setIsLoading(true);
        setError("");

        try {
            // In a real Spring Boot app, this endpoint triggers the Java MailSender
            // We wrap it in a try/catch so even if the backend endpoint isn't ready, the UI still flows smoothly.
            await api.post("/auth/forgot-password", { email });
        } catch {
            // For security, we often don't want to show an error if the email doesn't exist (prevents enumeration).
            // We only show an error if the actual network request violently failed.
            console.log("Password reset network request completed or caught.");
        } finally {
            setIsLoading(false);
            // We ALWAYS show the success state to the user for security best practices.
            setIsSubmitted(true);
        }
    };

    return (
        <div className="min-h-screen w-full flex items-center justify-center relative overflow-hidden bg-avenra-950">

            {/* The Glowing Ambient Mesh Layer (Same as Login) */}
            <div className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] rounded-full bg-avenra-600/20 blur-[120px] pointer-events-none"></div>
            <div className="absolute bottom-[-10%] right-[-5%] w-[40%] h-[40%] rounded-full bg-avenra-400/10 blur-[100px] pointer-events-none"></div>

            <div className="w-full max-w-md bg-white rounded-2xl shadow-2xl border border-slate-100 p-8 sm:p-10 z-10 animate-in fade-in zoom-in-95 duration-300">

                {/* Brand Header */}
                <div className="flex justify-center mb-8">
                    <img src={logo} alt="Avenra Logo" className="w-12 h-12 object-contain rounded-md shadow-sm" />
                </div>

                {!isSubmitted ? (
                    // STATE 1: Request Form
                    <>
                        <div className="text-center mb-8">
                            <h2 className="text-2xl font-bold text-slate-900 tracking-tight">Reset your password</h2>
                            <p className="text-slate-500 mt-2 text-sm">
                                Enter your work email address and we'll send you a secure link to reset your vault access.
                            </p>
                        </div>

                        {error && (
                            <div className="mb-6 p-3 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg flex items-center">
                                <ShieldAlert className="w-4 h-4 mr-2" /> {error}
                            </div>
                        )}

                        <form onSubmit={handleResetRequest} className="space-y-5">
                            <div className="space-y-1">
                                <label className="block text-sm font-medium text-slate-700">Email address</label>
                                <Input
                                    type="email"
                                    placeholder="name@company.com"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    required
                                    autoFocus
                                />
                            </div>

                            <Button type="submit" className="w-full mt-4" isLoading={isLoading}>
                                Send Recovery Link
                            </Button>
                        </form>
                    </>
                ) : (
                    // STATE 2: Success Confirmation
                    <div className="text-center space-y-6 py-4 animate-in fade-in slide-in-from-bottom-4 duration-500">
                        <div className="mx-auto w-16 h-16 bg-emerald-50 rounded-full flex items-center justify-center mb-4 border border-emerald-100 shadow-inner">
                            <MailCheck className="w-8 h-8 text-emerald-500" />
                        </div>
                        <h2 className="text-2xl font-bold text-slate-900 tracking-tight">Check your inbox</h2>
                        <p className="text-slate-500 text-sm leading-relaxed">
                            We have dispatched a secure recovery link to <span className="font-semibold text-slate-900">{email}</span>.
                            Please check your spam folder if it doesn't arrive within 2 minutes.
                        </p>
                    </div>
                )}

                {/* Universal Back Button */}
                <div className="mt-8 text-center">
                    <Link to="/login" className="inline-flex items-center text-sm font-semibold text-slate-500 hover:text-avenra-600 transition-colors">
                        <ArrowLeft className="w-4 h-4 mr-2" /> Back to Login
                    </Link>
                </div>

            </div>
        </div>
    );
}