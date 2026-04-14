import { useState, useCallback, Suspense } from "react";
import { Outlet, useLocation } from "react-router-dom";
import { motion as Motion, AnimatePresence } from "framer-motion";
import Sidebar from "../shared/Sidebar";
import Topbar from "../shared/Topbar";
import { useWebSocket } from "../../hooks/useWebSocket";
import toast from "react-hot-toast";

// Shown while a page chunk is being fetched — prevents white flash on navigation
function PageLoader() {
  return (
    <div className="flex-1 flex items-center justify-center min-h-[60vh]">
      <div className="flex flex-col items-center gap-4">
        <div className="w-8 h-8 border-[3px] border-avenra-500 border-t-transparent rounded-full animate-spin" />
        <p className="text-slate-400 text-sm font-medium tracking-wide">
          Loading workspace...
        </p>
      </div>
    </div>
  );
}

// Page transition variants for smooth navigation
const pageVariants = {
  initial: { opacity: 0, y: 12 },
  animate: { opacity: 1, y: 0, transition: { duration: 0.3, ease: [0.22, 1, 0.36, 1] } },
  exit: { opacity: 0, y: -8, transition: { duration: 0.15, ease: "easeIn" } }
};

export default function DashboardLayout() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const location = useLocation();

  // CRITICAL FIX: useCallback guarantees a stable reference.
  // Without this, an inline arrow function would create a new reference on every
  // render — which was the direct cause of the WebSocket reconnect storm that
  // triggered the white screen on every navigation.
  const handleWebSocketMessage = useCallback((notification) => {
    if (notification.type === "INVOICE_STATUS_UPDATE") {
      toast.success(`Invoice Processing Complete!`, {
        position: "bottom-right",
        duration: 5000,
      });
      window.dispatchEvent(new CustomEvent("invoice-updated", { detail: notification }));
    }
  }, []);

  useWebSocket(handleWebSocketMessage);

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

        {/* AnimatePresence + Suspense boundary prevents white screens and adds smooth transitions */}
        <main className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
          <Suspense fallback={<PageLoader />}>
            <AnimatePresence mode="wait">
              <Motion.div
                key={location.pathname}
                variants={pageVariants}
                initial="initial"
                animate="animate"
                exit="exit"
                className="h-full"
              >
                <Outlet />
              </Motion.div>
            </AnimatePresence>
          </Suspense>
        </main>
      </div>
    </div>
  );
}
