import { useState } from "react";
import { motion as Motion, AnimatePresence } from "framer-motion/react";
import { Menu, Bell, Search, User } from "lucide-react";
import { useAuth } from "../../context/useAuth";

export default function Topbar({ onMenuClick }) {
  const { logout } = useAuth();
  const [showNotifications, setShowNotifications] = useState(false);

  return (
    <header className="h-16 bg-white/80 backdrop-blur-lg border-b border-slate-200 flex items-center justify-between px-4 sm:px-6 sticky top-0 z-30 shadow-sm">
      
      {/* Mobile Menu & Search */}
      <div className="flex items-center flex-1">
        <button 
          onClick={onMenuClick}
          className="mr-4 md:hidden p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-md transition-colors cursor-pointer"
        >
          <Menu className="w-6 h-6" />
        </button>
        
        <Motion.div 
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.4 }}
          className="hidden sm:flex items-center text-slate-400 bg-slate-50/50 border border-slate-200 rounded-lg px-4 py-2 w-full max-w-md focus-within:bg-white focus-within:border-avenra-400 focus-within:ring-2 focus-within:ring-avenra-100 transition-all shadow-inner"
        >
          <Search className="w-4 h-4 mr-2" />
          <input 
            type="text" 
            placeholder="Search invoices, vendors, or amounts..." 
            className="bg-transparent border-none outline-none text-sm w-full text-slate-900 placeholder:text-slate-400"
          />
        </Motion.div>
      </div>

      {/* Right side actions */}
      <div className="flex items-center space-x-2 sm:space-x-4">
        
        <div className="relative">
          <Motion.button 
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={() => setShowNotifications(!showNotifications)}
            className="relative p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-full transition-colors cursor-pointer"
          >
            <Bell className="w-5 h-5" />
            <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-avenra-500 rounded-full border-2 border-white"></span>
          </Motion.button>

          <AnimatePresence>
            {showNotifications && (
              <Motion.div 
                initial={{ opacity: 0, y: 10, scale: 0.95 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: 10, scale: 0.95 }}
                transition={{ duration: 0.2 }}
                className="absolute right-0 mt-2 w-72 bg-white/95 backdrop-blur-xl rounded-xl shadow-[0_10px_40px_-10px_rgba(0,0,0,0.15)] border border-slate-200 z-50 overflow-hidden"
              >
                <div className="p-3 border-b border-slate-100 font-semibold text-slate-800 flex justify-between items-center bg-slate-50/50">
                  Notifications <span className="text-xs bg-avenra-100 text-avenra-700 px-2 py-0.5 rounded-full font-bold">1 New</span>
                </div>
                <div className="p-4 text-sm text-slate-600">
                  <div className="flex items-start space-x-3 mb-1 hover:bg-slate-50 p-2 -mx-2 rounded-lg transition-colors cursor-pointer">
                    <div className="w-2 h-2 mt-1.5 rounded-full bg-avenra-500 shrink-0 shadow-[0_0_6px_rgba(48,91,163,0.5)]"></div>
                    <div>
                      <p className="font-medium text-slate-900">Vault Connection Secured</p>
                      <p className="text-xs text-slate-500 mt-0.5">Real-time WebSocket active.</p>
                    </div>
                  </div>
                </div>
              </Motion.div>
            )}
          </AnimatePresence>
        </div>
        
        <div className="h-6 w-px bg-slate-200 mx-1"></div>
        
        <div className="flex items-center space-x-3">
          <div className="hidden md:block text-right">
            <p className="text-sm font-medium text-slate-900 leading-none">Admin User</p>
            <p className="text-xs text-slate-500 mt-1">Workspace Owner</p>
          </div>
          <Motion.button 
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={logout}
            className="w-9 h-9 rounded-full bg-gradient-to-br from-avenra-100 to-avenra-50 text-avenra-700 flex items-center justify-center border border-avenra-200 shadow-sm cursor-pointer"
            title="Sign out"
          >
            <User className="w-4 h-4" />
          </Motion.button>
        </div>
      </div>
    </header>
  );
}