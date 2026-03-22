import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import api from "../api/axiosInterceptor";
import { Input } from "../components/ui/Input";
import { Button } from "../components/ui/Button";
import logo from "../assets/avenra-logo.png";

export default function Register() {
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [organizationName, setOrganizationName] = useState("");

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const navigate = useNavigate();

  const handleRegister = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError("");
    setSuccessMessage("");

    try {
      await api.post("/auth/register", { 
        firstName, 
        lastName, 
        email, 
        password, 
        organizationName 
      });
      
      setSuccessMessage("Registration successful! Redirecting to login...");
      setTimeout(() => {
        navigate("/login");
      }, 2000);

    } catch (err) {
      setError(err.response?.data?.message || "Registration failed. Please try again.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleGoogleLogin = () => {
    window.location.href = import.meta.env.VITE_OAUTH_URL;
  };

  return (
    // FULL SCREEN GRADIENT BACKGROUND
    <div className="min-h-screen w-full flex items-center justify-center relative overflow-hidden bg-avenra-950">

      {/* 1. The Glowing Ambient Mesh Layer */}
      <div className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] rounded-full bg-avenra-600/20 blur-[120px] pointer-events-none"></div>
      <div className="absolute bottom-[-10%] right-[-5%] w-[40%] h-[40%] rounded-full bg-avenra-400/10 blur-[100px] pointer-events-none"></div>

      <div className="w-full max-w-6xl flex flex-col md:flex-row items-center justify-center md:justify-between px-6 py-12 z-10">

        {/* LEFT SIDE: Brand Identity (Floats over the gradient) */}
        <div className="flex flex-col items-center md:items-start text-center md:text-left mb-12 md:mb-0 max-w-lg">
          <div className="flex items-center space-x-3 text-white mb-8">
            <img src={logo} alt="Avenra" className="w-10 h-10 rounded shadow-sm" />
            <span className="text-3xl font-bold tracking-wide">AVENRA <span className="text-avenra-400">FLOW</span></span>
          </div>
          <h1 className="text-4xl md:text-5xl lg:text-6xl font-extrabold text-white leading-tight mb-6 tracking-tight">
            Built to Simplify <br />
            <span className="text-avenra-400">
              Complexity
            </span>
          </h1>
          <p className="text-slate-300 text-lg md:text-xl font-light">
            The enterprise-grade invoice automation engine. Cognitive extraction and real-time analytics.
          </p>
        </div>

        {/* RIGHT SIDE: The Premium Floating Card */}
        <div className="w-full max-w-md bg-white rounded-2xl shadow-2xl border border-slate-100 p-8 sm:p-10 transform transition-all hover:shadow-[0_20px_40px_-15px_rgba(30,64,175,0.3)]">

          <div className="text-center md:text-left mb-8">
            <h2 className="text-2xl font-bold text-slate-900 tracking-tight">Create an account</h2>
            <p className="text-slate-500 mt-1 text-sm">Join Avenra and set up your workspace.</p>
          </div>

          {error && (
            <div className="mb-6 p-3 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg">
              {error}
            </div>
          )}

          {successMessage && (
            <div className="mb-6 p-3 text-sm text-green-700 bg-green-50 border border-green-200 rounded-lg">
              {successMessage}
            </div>
          )}

          <form onSubmit={handleRegister} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1">
                <label className="block text-sm font-medium text-slate-700">First Name</label>
                <Input
                  type="text"
                  placeholder="John"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-1">
                <label className="block text-sm font-medium text-slate-700">Last Name</label>
                <Input
                  type="text"
                  placeholder="Doe"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="space-y-1">
              <label className="block text-sm font-medium text-slate-700">Organization Name</label>
              <Input
                type="text"
                placeholder="Acme Corp"
                value={organizationName}
                onChange={(e) => setOrganizationName(e.target.value)}
                required
              />
            </div>

            <div className="space-y-1">
              <label className="block text-sm font-medium text-slate-700">Email address</label>
              <Input
                type="email"
                placeholder="name@company.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <div className="space-y-1">
              <label className="block text-sm font-medium text-slate-700">Password</label>
              <Input
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            <Button type="submit" className="w-full mt-2" isLoading={isLoading}>
              Create Workspace
            </Button>
            
            <p className="text-center text-sm text-slate-600 mt-4">
              Already have an account?{" "}
              <Link to="/login" className="font-semibold text-avenra-600 hover:text-avenra-500">
                Sign in
              </Link>
            </p>
          </form>

          <div className="relative my-7">
            <div className="absolute inset-0 flex items-center">
              <span className="w-full border-t border-slate-200" />
            </div>
            <div className="relative flex justify-center text-xs uppercase font-semibold">
              <span className="bg-white px-3 text-slate-400">Or continue with</span>
            </div>
          </div>

          <Button variant="outline" className="w-full" onClick={handleGoogleLogin}>
            <svg className="mr-2 h-4 w-4" viewBox="0 0 24 24">
              <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4" />
              <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
              <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
              <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
            </svg>
            Sign in with Google
          </Button>

        </div>
      </div>
    </div>
  );
}
