import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { motion as Motion } from "framer-motion";
import api from "../api/axiosInterceptor";
import { Input } from "../components/ui/Input";
import { Button } from "../components/ui/Button";
import { ArrowLeft, MailCheck, ShieldAlert } from "lucide-react";
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

                {!isSubmitted ? (
                    <Motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.5 }}>
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
                                    className="bg-white/50 focus:bg-white"
                                />
                            </div>

                            <Button type="submit" className="w-full mt-4" isLoading={isLoading}>
                                Send Recovery Link
                            </Button>
                        </form>
                    </Motion.div>
                ) : (
                    <Motion.div 
                      initial={{ opacity: 0, scale: 0.9 }} 
                      animate={{ opacity: 1, scale: 1 }} 
                      transition={{ duration: 0.5, type: "spring" }}
                      className="text-center space-y-6 py-4"
                    >
                        <div className="mx-auto w-16 h-16 bg-emerald-50 rounded-full flex items-center justify-center mb-4 border border-emerald-100 shadow-inner">
                            <MailCheck className="w-8 h-8 text-emerald-500" />
                        </div>
                        <h2 className="text-2xl font-bold text-slate-900 tracking-tight">Check your inbox</h2>
                        <p className="text-slate-500 text-sm leading-relaxed">
                            We have dispatched a secure recovery link to <span className="font-semibold text-slate-900">{email}</span>.
                            Please check your spam folder if it doesn't arrive within 2 minutes.
                        </p>
                    </Motion.div>
                )}

                <div className="mt-8 text-center">
                    <Link to="/login" className="inline-flex items-center text-sm font-semibold text-slate-500 hover:text-avenra-600 transition-colors">
                        <ArrowLeft className="w-4 h-4 mr-2" /> Back to Login
                    </Link>
                </div>
            </Motion.div>
        </div>
    );
}
