import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { motion as Motion, useScroll, useTransform } from "framer-motion";
import { ArrowRight, Box, Shield, Zap, Terminal, Activity, Layers, ChevronRight, Github } from "lucide-react";
import logo from "../assets/avenra-logo.png";
import { Button } from "../components/ui/Button";

const HeroBackground = () => (
  <div className="absolute inset-0 overflow-hidden pointer-events-none">
    <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iMSIgY3k9IjEiIHI9IjEiIGZpbGw9InJnYmEoMjU1LDI1NSwyNTUsMC4wMykiLz48L3N2Zz4=')] [mask-image:linear-gradient(to_bottom,white,transparent_80%)]"></div>
    <Motion.div animate={{ scale: [1, 1.1, 1], opacity: [0.15, 0.25, 0.15] }} transition={{ duration: 8, repeat: Infinity, ease: "easeInOut" }} className="absolute top-[-10%] right-[10%] w-[600px] h-[600px] rounded-full bg-indigo-600/20 blur-[120px] mix-blend-screen" />
    <Motion.div animate={{ scale: [1, 1.2, 1], opacity: [0.1, 0.2, 0.1] }} transition={{ duration: 10, repeat: Infinity, ease: "easeInOut" }} className="absolute bottom-[20%] left-[-10%] w-[500px] h-[500px] rounded-full bg-blue-600/20 blur-[120px] mix-blend-screen" />
    
    {/* Animated Data Ribbons */}
    <div className="absolute inset-0 flex items-center justify-center opacity-40">
      <svg className="w-[150%] min-w-[1000px] h-[150%] max-w-none" viewBox="0 0 1000 1000" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="landing-grad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#305ba3" stopOpacity="0" />
            <stop offset="30%" stopColor="#4a7acb" stopOpacity="0.8" />
            <stop offset="70%" stopColor="#8b5cf6" stopOpacity="0.6" />
            <stop offset="100%" stopColor="#060b14" stopOpacity="0" />
          </linearGradient>
        </defs>
        <g stroke="url(#landing-grad)" fill="none" strokeWidth="2">
          {[...Array(5)].map((_, i) => (
            <Motion.path key={i} initial={{ pathLength: 0, opacity: 0 }} animate={{ pathLength: 1, opacity: 1 - (i * 0.15), d: [ `M-200,${300 + i * 40} C200,${400 - i * 30} 600,${200 + i * 50} 1200,${400 + i * 20}`, `M-200,${320 + i * 40} C250,${380 - i * 30} 550,${220 + i * 50} 1200,${380 + i * 20}`, `M-200,${300 + i * 40} C200,${400 - i * 30} 600,${200 + i * 50} 1200,${400 + i * 20}` ] }} transition={{ pathLength: { duration: 2, ease: "easeOut", delay: i * 0.2 }, opacity: { duration: 1, delay: i * 0.2 }, d: { duration: 15 + i * 2, repeat: Infinity, ease: "easeInOut" } }} />
          ))}
        </g>
      </svg>
    </div>
  </div>
);

const Navbar = () => {
  const [scrolled, setScrolled] = useState(false);
  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 50);
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <Motion.nav 
      initial={{ y: -100 }} animate={{ y: 0 }} transition={{ duration: 0.6, ease: "easeOut" }}
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${scrolled ? "bg-[#060b14]/80 backdrop-blur-lg border-b border-white/10 py-3" : "bg-transparent py-5"}`}
    >
      <div className="max-w-7xl mx-auto px-6 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <img src={logo} alt="Avenra" className="w-8 h-8 rounded" />
          <span className="text-xl font-bold tracking-wide text-white">AVENRA <span className="text-avenra-400">FLOW</span></span>
        </div>
        <div className="hidden md:flex items-center space-x-8 text-sm font-medium text-slate-300">
          <a href="#features" className="hover:text-white transition-colors">Features</a>
          <a href="#architecture" className="hover:text-white transition-colors">Architecture</a>
          <a href="#security" className="hover:text-white transition-colors">Security</a>
        </div>
        <div className="flex items-center space-x-4">
          <Link to="/login" className="text-sm font-medium text-slate-300 hover:text-white transition-colors hidden sm:block">Sign In</Link>
          <Link to="/register">
            <Button className="bg-white text-slate-900 hover:bg-slate-100 rounded-full px-5">Get Started</Button>
          </Link>
        </div>
      </div>
    </Motion.nav>
  );
};

export default function LandingPage() {
  const { scrollYProgress } = useScroll();
  const y1 = useTransform(scrollYProgress, [0, 1], [0, 200]);
  const opacity1 = useTransform(scrollYProgress, [0, 0.2], [1, 0]);

  return (
    <div className="min-h-screen bg-[#060b14] text-slate-50 font-sans selection:bg-avenra-500 selection:text-white overflow-x-hidden">
      <Navbar />

      {/* --- HERO SECTION --- */}
      <section className="relative pt-32 pb-20 md:pt-48 md:pb-32 px-6 flex flex-col items-center text-center">
        <HeroBackground />
        
        <Motion.div style={{ y: y1, opacity: opacity1 }} className="max-w-4xl mx-auto z-10 relative">
          <Motion.div initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.5 }} className="inline-flex items-center space-x-2 bg-white/5 border border-white/10 rounded-full px-3 py-1 mb-8 backdrop-blur-md">
            <span className="flex h-2 w-2 rounded-full bg-emerald-500 animate-pulse"></span>
            <span className="text-xs font-medium text-slate-300">v2.0 Architecture Live</span>
          </Motion.div>
          
          <Motion.h1 initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.7, delay: 0.1 }} className="text-5xl md:text-7xl lg:text-8xl font-extrabold tracking-tight mb-6 leading-tight">
            Financial Data, <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-indigo-400 to-purple-400">
              Automated at Scale.
            </span>
          </Motion.h1>
          
          <Motion.p initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.7, delay: 0.2 }} className="text-lg md:text-xl text-slate-400 mb-10 max-w-2xl mx-auto font-light leading-relaxed">
            Avenra FLOW is the enterprise-grade ingestion engine. We combine cognitive Python AI workers with a secure Java Spring Boot vault to extract, process, and route your invoices in real-time.
          </Motion.p>
          
          <Motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.7, delay: 0.3 }} className="flex flex-col sm:flex-row items-center justify-center space-y-4 sm:space-y-0 sm:space-x-4">
            <Link to="/register">
              <Button size="lg" className="w-full sm:w-auto text-base rounded-full px-8 h-12 bg-avenra-600 hover:bg-avenra-500 border-none shadow-[0_0_20px_rgba(37,70,130,0.5)]">
                Create Workspace <ArrowRight className="w-4 h-4 ml-2" />
              </Button>
            </Link>
            <a href="#features" className="w-full sm:w-auto">
              <Button size="lg" variant="outline" className="w-full sm:w-auto text-base rounded-full px-8 h-12 border-white/20 text-white hover:bg-white/10 backdrop-blur-md">
                Explore Architecture
              </Button>
            </a>
          </Motion.div>
        </Motion.div>

        {/* Floating Dashboard Mockup */}
        <Motion.div 
          initial={{ opacity: 0, y: 100 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 1, delay: 0.5, type: "spring" }}
          className="mt-20 w-full max-w-5xl aspect-[16/9] md:aspect-[21/9] bg-[#0c1527] rounded-xl border border-white/10 shadow-[0_0_50px_rgba(0,0,0,0.5)] relative overflow-hidden z-10"
        >
          {/* Mock Header */}
          <div className="h-12 border-b border-white/5 flex items-center px-4 space-x-2 bg-white/5">
            <div className="w-3 h-3 rounded-full bg-red-500/50"></div>
            <div className="w-3 h-3 rounded-full bg-amber-500/50"></div>
            <div className="w-3 h-3 rounded-full bg-emerald-500/50"></div>
            <div className="ml-4 text-xs font-mono text-slate-500">avenra-ai-worker.onrender.com</div>
          </div>
          {/* Mock Terminal Body */}
          <div className="p-6 font-mono text-sm text-slate-400 flex flex-col space-y-3">
            <Motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 1.5 }} className="flex items-center text-emerald-400"><ChevronRight className="w-4 h-4 mr-1"/> [✅] Successfully connected to RabbitMQ. Awaiting invoices...</Motion.div>
            <Motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 2.5 }} className="flex items-center text-blue-400"><ChevronRight className="w-4 h-4 mr-1"/> [🚀] NEW JOB RECEIVED! Invoice ID: 70305ae7...</Motion.div>
            <Motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 3.5 }} className="flex items-center"><ChevronRight className="w-4 h-4 mr-1"/> [*] PDF detected. Rasterizing at 300 DPI with Tesseract...</Motion.div>
            <Motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 5.0 }} className="flex items-center text-purple-400"><ChevronRight className="w-4 h-4 mr-1"/> [*] Handing off to Groq LLM for cognitive extraction...</Motion.div>
            <Motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 6.5 }} className="flex items-center text-emerald-400"><ChevronRight className="w-4 h-4 mr-1"/> [✅] Webhook fired to Java Vault. Status: 200 OK.</Motion.div>
          </div>
        </Motion.div>
      </section>

      {/* --- BENTO GRID FEATURES --- */}
      <section id="features" className="py-24 px-6 max-w-7xl mx-auto relative z-10">
        <div className="mb-16">
          <h2 className="text-3xl md:text-4xl font-bold tracking-tight mb-4">Enterprise Infrastructure.</h2>
          <p className="text-slate-400 text-lg max-w-2xl">Not just a wrapper. A deeply integrated microservice architecture designed to handle massive financial throughput securely.</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Card 1 */}
          <Motion.div whileHover={{ y: -5 }} className="md:col-span-2 bg-gradient-to-br from-white/5 to-white/0 border border-white/10 rounded-3xl p-8 backdrop-blur-sm relative overflow-hidden group">
            <div className="absolute top-0 right-0 w-64 h-64 bg-blue-500/10 rounded-full blur-[80px] -mr-32 -mt-32 transition-opacity group-hover:bg-blue-500/20"></div>
            <Zap className="w-10 h-10 text-blue-400 mb-6" />
            <h3 className="text-2xl font-bold mb-3">Asynchronous AI Workers</h3>
            <p className="text-slate-400 leading-relaxed max-w-md">
              Heavy PDF rasterization and OCR processing is offloaded to a dedicated Dockerized Python environment. Orchestrated by RabbitMQ, your Java vault never blocks during peak loads.
            </p>
          </Motion.div>

          {/* Card 2 */}
          <Motion.div whileHover={{ y: -5 }} className="bg-gradient-to-br from-white/5 to-white/0 border border-white/10 rounded-3xl p-8 backdrop-blur-sm relative overflow-hidden group">
            <Activity className="w-10 h-10 text-emerald-400 mb-6" />
            <h3 className="text-xl font-bold mb-3">Real-time Telemetry</h3>
            <p className="text-slate-400 leading-relaxed text-sm">
              Live STOMP WebSockets instantly push extraction results from the cloud to your React dashboard the millisecond processing completes.
            </p>
          </Motion.div>

          {/* Card 3 */}
          <Motion.div whileHover={{ y: -5 }} className="bg-gradient-to-br from-white/5 to-white/0 border border-white/10 rounded-3xl p-8 backdrop-blur-sm relative overflow-hidden group">
            <Shield className="w-10 h-10 text-indigo-400 mb-6" />
            <h3 className="text-xl font-bold mb-3">Cloudflare R2 Vault</h3>
            <p className="text-slate-400 leading-relaxed text-sm">
              Original documents are instantly routed and vaulted in highly-available edge buckets via the AWS SDK, fully decoupled from database limits.
            </p>
          </Motion.div>

          {/* Card 4 */}
          <Motion.div whileHover={{ y: -5 }} className="md:col-span-2 bg-gradient-to-br from-white/5 to-white/0 border border-white/10 rounded-3xl p-8 backdrop-blur-sm relative overflow-hidden group">
            <div className="absolute bottom-0 right-0 w-64 h-64 bg-purple-500/10 rounded-full blur-[80px] -mr-32 -mb-32 transition-opacity group-hover:bg-purple-500/20"></div>
            <Layers className="w-10 h-10 text-purple-400 mb-6" />
            <h3 className="text-2xl font-bold mb-3">Groq Cognitive Extraction</h3>
            <p className="text-slate-400 leading-relaxed max-w-md">
              We bypass brittle Regex parsing. Raw OCR data is streamed through Groq's lightning-fast inference API, allowing the LLM to contextually identify Vendors, Dates, and Taxes regardless of format.
            </p>
          </Motion.div>
        </div>
      </section>

      {/* --- FOOTER CTA --- */}
      <section className="py-24 relative overflow-hidden border-t border-white/5">
        <div className="absolute inset-0 bg-blue-600/5 blur-[100px]"></div>
        <div className="max-w-4xl mx-auto px-6 text-center relative z-10">
          <h2 className="text-4xl md:text-5xl font-bold mb-6">Ready to automate your ledger?</h2>
          <p className="text-slate-400 text-lg mb-10">Join the next generation of financial operations.</p>
          <Link to="/register">
            <Button size="lg" className="rounded-full px-10 h-14 bg-white text-slate-900 hover:bg-slate-100 text-lg font-semibold shadow-[0_0_30px_rgba(255,255,255,0.2)]">
              Start Building Now
            </Button>
          </Link>
        </div>
      </section>

      {/* FOOTER */}
      <footer className="border-t border-white/10 bg-[#060b14] py-12 px-6 relative z-10">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row justify-between items-center text-sm text-slate-500">
          <div className="flex items-center space-x-2 mb-4 md:mb-0">
            <img src={logo} alt="Avenra" className="w-6 h-6 rounded opacity-50" />
            <span>&copy; {new Date().getFullYear()} Avenra FLOW. Engineered for scale.</span>
          </div>
          <div className="flex space-x-6">
            <a href="#" className="hover:text-slate-300 transition-colors">Privacy Policy</a>
            <a href="#" className="hover:text-slate-300 transition-colors">Terms of Service</a>
            <a href="#" className="hover:text-slate-300 transition-colors flex items-center"><Github className="w-4 h-4 mr-2"/> Repository</a>
          </div>
        </div>
      </footer>
    </div>
  );
}