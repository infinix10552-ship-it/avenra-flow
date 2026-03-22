import { Menu, Bell, Search, User } from "lucide-react";
import { Button } from "../ui/Button";
import { useAuth } from "../../context/useAuth";
import { useState } from "react";

export default function Topbar({ onMenuClick }) {
  const { logout } = useAuth();
  const [showNotifications, setShowNotifications] = useState(false);

  return (
    <header className="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-4 sm:px-6 sticky top-0 z-30">
      
      {/* Mobile Menu & Search */}
      <div className="flex items-center flex-1">
        <button 
          onClick={onMenuClick}
          className="mr-4 md:hidden p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-md transition-colors cursor-pointer"
        >
          <Menu className="w-6 h-6" />
        </button>
        
        <div className="hidden sm:flex items-center text-slate-400 bg-slate-50 border border-slate-200 rounded-lg px-8 py-2 w-full max-w-md focus-within:border-avenra-500 focus-within:ring-1 focus-within:ring-avenra-500 transition-all">
          <Search className="w-4 h-4 mr-2" />
          <input 
            type="text" 
            placeholder="Search invoices, vendors, or amounts..." 
            className="bg-transparent border-none outline-none text-sm w-full text-slate-900 placeholder:text-slate-400"
          />
        </div>
      </div>

      {/* Right side actions */}
      <div className="flex items-center space-x-2 sm:space-x-4">
        {/* --- NEW: Working Notification Bell --- */}
        <div className="relative">
          <button 
            onClick={() => setShowNotifications(!showNotifications)}
            className="relative p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-full transition-colors cursor-pointer"
          >
            <Bell className="w-5 h-5" />
            <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-avenra-500 rounded-full border-2 border-white"></span>
          </button>

          {showNotifications && (
            <div className="absolute right-0 mt-2 w-72 bg-white rounded-xl shadow-lg border border-slate-200 z-50 animate-in fade-in slide-in-from-top-2">
              <div className="p-3 border-b border-slate-100 font-semibold text-slate-800 flex justify-between items-center">
                Notifications <span className="text-xs bg-avenra-100 text-avenra-700 px-2 py-0.5 rounded-full">1 New</span>
              </div>
              <div className="p-4 text-sm text-slate-600">
                <div className="flex items-start space-x-3 mb-3">
                  <div className="w-2 h-2 mt-1.5 rounded-full bg-avenra-500 shrink-0"></div>
                  <div>
                    <p className="font-medium text-slate-900">Vault Connection Secured</p>
                    <p className="text-xs text-slate-500">Real-time WebSocket active.</p>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
        {/* --------------------------------------- */}
        
        <div className="h-8 w-px bg-slate-200 mx-2"></div>
        
        <div className="flex items-center space-x-3">
          <div className="hidden md:block text-right">
            <p className="text-sm font-medium text-slate-900 leading-none">Admin User</p>
            <p className="text-xs text-slate-500 mt-1">Workspace Owner</p>
          </div>
          <button 
            onClick={logout}
            className="w-9 h-9 rounded-full bg-avenra-100 text-avenra-700 flex items-center justify-center font-bold border border-avenra-200 hover:bg-avenra-200 transition-colors cursor-pointer"
            title="Sign out"
          >
            <User className="w-5 h-5" />
          </button>
        </div>
      </div>
    </header>
  );
}