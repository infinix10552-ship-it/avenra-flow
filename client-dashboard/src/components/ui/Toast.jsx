import { CheckCircle2, X } from "lucide-react";

export function Toast({ message, isVisible, onClose }) {
  if (!isVisible) return null;

  return (
    <div className="fixed bottom-6 right-6 z-50 animate-in slide-in-from-bottom-5 fade-in duration-300">
      <div className="bg-white border border-slate-200 shadow-premium rounded-lg p-4 flex items-start space-x-3 w-80">
        <CheckCircle2 className="w-5 h-5 text-emerald-500 flex-shrink-0 mt-0.5" />
        <div className="flex-1">
          <h4 className="text-sm font-semibold text-slate-900">Update Received</h4>
          <p className="text-sm text-slate-500 mt-1">{message}</p>
        </div>
        <button 
          onClick={onClose}
          className="text-slate-400 hover:text-slate-600 transition-colors"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}