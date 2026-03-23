import { motion as Motion, AnimatePresence } from "framer-motion";
import { CheckCircle2, X } from "lucide-react";

export function Toast({ message, isVisible, onClose }) {
  return (
    <AnimatePresence>
      {isVisible && (
        <Motion.div 
          initial={{ opacity: 0, y: 50, scale: 0.9 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: 20, scale: 0.95 }}
          className="fixed bottom-6 right-6 z-50 shadow-2xl"
        >
          <div className="bg-white/95 backdrop-blur-xl border border-slate-200 shadow-[0_10px_40px_-10px_rgba(0,0,0,0.15)] rounded-2xl p-4 flex items-start space-x-3 w-80">
            <div className="bg-emerald-50 p-1.5 rounded-lg border border-emerald-100/50">
              <CheckCircle2 className="w-5 h-5 text-emerald-500" />
            </div>
            <div className="flex-1">
              <h4 className="text-sm font-bold text-slate-900 tracking-tight">System Update</h4>
              <p className="text-sm text-slate-500 mt-1 leading-relaxed">{message}</p>
            </div>
            <button 
              onClick={onClose}
              className="text-slate-400 hover:text-slate-900 transition-colors p-1 hover:bg-slate-100 rounded-md"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </Motion.div>
      )}
    </AnimatePresence>
  );
}
