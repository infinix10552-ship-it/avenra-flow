import { useState } from "react";
import { Outlet } from "react-router-dom";
import Sidebar from "../shared/Sidebar";
import Topbar from "../shared/Topbar";
import { useWebSocket } from "../../hooks/useWebSocket";
import toast from "react-hot-toast";

export default function DashboardLayout() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  useWebSocket((notification) => {
    if (notification.type === "INVOICE_STATUS_UPDATE") {
      toast.success(`Invoice Processing Complete!`, {
        position: "bottom-right",
        duration: 5000,
      });
      window.dispatchEvent(new CustomEvent('invoice-updated', { detail: notification }));
    }
  });

  return (
    <div className="flex h-screen w-full bg-slate-50 overflow-hidden">
      {/* The Navigation Panel */}
      <Sidebar 
        isOpen={isSidebarOpen} 
        onClose={() => setIsSidebarOpen(false)} 
      />

      {/* The Main Content Column */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <Topbar onMenuClick={() => setIsSidebarOpen(true)} />
        
        {/* The actual page content is injected here */}
        <main className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
