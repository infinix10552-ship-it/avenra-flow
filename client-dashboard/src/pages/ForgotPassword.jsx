import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import api from "../api/axiosInterceptor";
import { Input } from "../components/ui/Input";
import { Button } from "../components/ui/Button";
import { ArrowLeft, MailCheck, ShieldAlert } from "lucide-react";
import logo from "../assets/avenra-logo.png";

export default function ForgotPassword() {
    const [isLoading, setIsLoading] = useState(false);
    const [isSubmitted, setIsSubmitted] = useState(false);
    const [error, setError] = useState("");
    const location = useLocation();
    const [email, setEmail] = useState(location.state?.email || "");

    const handleResetRequest = async (e) => {
        e.preventDefault();
        if (!email) return;

        setIsLoading(true);
        setError("");

        try {
            await api.post("/auth/forgot-password", { email });
        } catch {
            console.log("Password reset network request completed or caught.");
        } finally {
            setIsLoading(false);
            setIsSubmitted(true);
        }
    };

    return (
        <div className="min-h-[100dvh] w-full flex items-center justify-center relative overflow-hidden bg-gradient-to-br from-slate-950 via-avenra-950 to-slate-900">

            <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAiIGhlaWdodD0iMjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iMSIgY3k9IjEiIHI9IjEiIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC4wNykiLz48L3N2Zz4=')] [mask-image:linear-gradient(to_bottom,white,transparent)] pointer-events-none"></div>
            <div className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] rounded-full bg-avenra-600/30 blur-[120px] pointer-events-none animate-pulse" style={{ animationDuration: '6s' }}></div>
            <div className="absolute bottom-[-10%] right-[-5%] w-[40%] h-[40%] rounded-full bg-avenra-400/20 blur-[100px] pointer-events-none"></div>

            <div className="w-full max-w-md bg-white/95 backdrop-blur-md rounded-3xl shadow-[0_8px_30px_rgb(0,0,0,0.12)] border border-white/20 p-6 sm:p-10 z-10 mx-4 animate-in fade-in zoom-in-95 duration-500">

                <div className="flex justify-center mb-8">
                    <img src={logo} alt="Avenra Logo" className="w-12 h-12 object-contain rounded-md shadow-sm" />
                </div>

                {!isSubmitted ? (
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

                <div className="mt-8 text-center">
                    <Link to="/login" className="inline-flex items-center text-sm font-semibold text-slate-500 hover:text-avenra-600 transition-colors">
                        <ArrowLeft className="w-4 h-4 mr-2" /> Back to Login
                    </Link>
                </div>
            </div>
        </div>
    );
}