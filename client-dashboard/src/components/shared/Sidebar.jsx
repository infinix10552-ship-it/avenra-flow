import { NavLink } from "react-router-dom";
import { LayoutDashboard, UploadCloud, FileText, Settings, X } from "lucide-react";
import { cn } from "../../lib/utils";
import logo from "../../assets/avenra-logo.png"; 

export default function Sidebar({ isOpen, onClose }) {
  
  const navItems = [
    { name: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
    { name: "Upload Hub", href: "/upload", icon: UploadCloud },
    { name: "All Invoices", href: "/invoices", icon: FileText },
    { name: "Settings", href: "/settings", icon: Settings },
  ];

  return (
    <>
      {/* Mobile Backdrop Overlay */}
      {isOpen && (
        <div 
          className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-40 md:hidden transition-opacity"
          onClick={onClose}
        />
      )}

      {/* Sidebar Container */}
      <aside className={cn(
        "fixed inset-y-0 left-0 z-50 w-64 bg-avenra-950 border-r border-avenra-800 text-slate-300 transform transition-transform duration-300 ease-in-out md:translate-x-0 md:static md:flex-shrink-0 flex flex-col",
        isOpen ? "translate-x-0" : "-translate-x-full"
      )}>
        
        {/* Brand Header */}
        <div className="h-16 flex items-center justify-between px-6 bg-avenra-950/50 border-b border-avenra-800/50">
          <div className="flex items-center space-x-3">
            <img src={logo} alt="Avenra" className="w-8 h-8 rounded shadow-sm" />
            <span className="text-xl font-bold text-white tracking-wide">FLOW</span>
          </div>
          <button onClick={onClose} className="md:hidden text-slate-400 hover:text-white cursor-pointer">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Navigation Links */}
        <nav className="flex-1 px-4 py-6 space-y-1 overflow-y-auto">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.name}
                to={item.href}
                onClick={() => onClose()} // Close mobile menu on click
                className={({ isActive }) => cn(
                  "flex items-center space-x-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors cursor-pointer",
                  isActive 
                    ? "bg-avenra-600/20 text-avenra-400 border border-avenra-500/30" 
                    : "text-slate-400 hover:bg-avenra-800/50 hover:text-slate-200"
                )}
              >
                <Icon className="w-5 h-5" />
                <span>{item.name}</span>
              </NavLink>
            );
          })}
        </nav>

        {/* Bottom Status Block */}
        <div className="p-4 m-4 rounded-xl bg-gradient-to-br from-avenra-800/50 to-avenra-900 border border-avenra-700/50">
          <div className="flex items-center space-x-2 text-white mb-2">
            <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></div>
            <span className="text-xs font-semibold uppercase tracking-wider">System Online</span>
          </div>
          <p className="text-xs text-slate-400">Secure Vault Connected.</p>
        </div>

      </aside>
    </>
  );
}