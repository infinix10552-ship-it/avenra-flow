import { useState } from "react";
import { motion as Motion, AnimatePresence } from "framer-motion/react";
import api from "../../api/axiosInterceptor";
import { Button } from "../ui/Button";
import { Input } from "../ui/Input";
import { X, MessageCircle, Send, CheckCircle2 } from "lucide-react";

export default function WhatsAppModal({ isOpen, onClose, invoice }) {
  const [phoneNumber, setPhoneNumber] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [status, setStatus] = useState({ type: "idle", message: "" });

  const handleSend = async (e) => {
    e.preventDefault();
    if (!phoneNumber) return;

    setIsLoading(true);
    setStatus({ type: "idle", message: "" });

    const formattedNumber = phoneNumber.startsWith("+") ? phoneNumber : `+91${phoneNumber}`;

    try {
      await api.post("/whatsapp/send", {
        invoiceId: invoice.id,
        targetPhoneNumber: formattedNumber,
      });

      setStatus({ type: "success", message: "Invoice securely dispatched via WhatsApp." });
      
      setTimeout(() => {
        handleClose();
      }, 3000);

    } catch (error) {
      setStatus({ 
        type: "error", 
        message: error.response?.data?.error || "Failed to connect to Twilio network." 
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleClose = () => {
    setStatus({ type: "idle", message: "" });
    setPhoneNumber("");
    onClose();
  };

  return (
    <AnimatePresence>
      {isOpen && invoice && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          {/* The Blurred Backdrop */}
          <Motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 bg-slate-900/60 backdrop-blur-md transition-opacity"
            onClick={handleClose}
          />

          {/* The Premium Glassmorphic Card */}
          <Motion.div 
            initial={{ opacity: 0, scale: 0.9, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 10 }}
            transition={{ type: "spring", damping: 25, stiffness: 300 }}
            className="relative bg-white/95 backdrop-blur-2xl rounded-3xl shadow-[0_20px_60px_-15px_rgba(0,0,0,0.3)] w-full max-w-md overflow-hidden z-10 border border-white/20"
          >
            
            {/* Header */}
            <div className="bg-emerald-50/80 backdrop-blur-sm px-6 py-5 border-b border-emerald-100 flex items-center justify-between">
              <div className="flex items-center space-x-3 text-emerald-700">
                <div className="p-2 bg-white rounded-xl shadow-sm border border-emerald-100/50">
                  <MessageCircle className="w-5 h-5" />
                </div>
                <h3 className="font-bold text-lg tracking-tight">Vault Share</h3>
              </div>
              <button 
                onClick={handleClose} 
                className="p-2 text-emerald-600 hover:text-emerald-800 hover:bg-emerald-100/50 rounded-lg transition-colors"
                title="Close"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Content */}
            <div className="p-8">
              <Motion.div 
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 }}
                className="mb-8 p-4 bg-slate-50/80 rounded-2xl border border-slate-200/60 shadow-inner text-sm text-slate-600 space-y-2"
              >
                <div className="flex justify-between">
                  <span className="font-medium">Vendor:</span>
                  <span className="font-bold text-slate-900">{invoice.vendorName || "Unidentified"}</span>
                </div>
                <div className="flex justify-between">
                  <span className="font-medium">Total Amount:</span>
                  <span className="font-bold text-slate-900">₹{invoice.totalAmount?.toLocaleString() || "0.00"}</span>
                </div>
              </Motion.div>

              <form onSubmit={handleSend} className="space-y-6">
                <div className="space-y-2">
                  <label className="block text-sm font-bold text-slate-700 ml-1">
                    Recipient Phone
                  </label>
                  <Input
                    type="tel"
                    placeholder="+91 98765 43210"
                    value={phoneNumber}
                    onChange={(e) => setPhoneNumber(e.target.value)}
                    required
                    className="w-full bg-white/50 focus:bg-white text-lg font-medium p-4 h-12 rounded-xl"
                  />
                  <p className="text-xs text-slate-500 ml-1">E.164 format (e.g., +919876543210)</p>
                </div>

                {/* Status Messages */}
                <AnimatePresence mode="wait">
                  {status.type !== "idle" && (
                    <Motion.div 
                      key={status.type}
                      initial={{ opacity: 0, height: 0 }}
                      animate={{ opacity: 1, height: "auto" }}
                      exit={{ opacity: 0, height: 0 }}
                      className={`text-sm p-3 rounded-xl border transition-all ${
                        status.type === "error" ? "text-red-600 bg-red-50 border-red-100" : "text-emerald-600 bg-emerald-50 border-emerald-100"
                      } flex items-center`}
                    >
                      {status.type === "success" && <CheckCircle2 className="w-4 h-4 mr-2 shrink-0" />}
                      {status.message}
                    </Motion.div>
                  )}
                </AnimatePresence>

                <div className="pt-2 flex flex-col space-y-3">
                  <Button 
                    type="submit" 
                    disabled={isLoading || status.type === "success"}
                    className="w-full bg-gradient-to-r from-emerald-600 to-emerald-500 hover:from-emerald-700 hover:to-emerald-600 text-white h-12 rounded-xl shadow-[0_10px_20px_-10px_rgba(16,185,129,0.5)] font-bold text-base transition-all active:scale-95"
                  >
                    {isLoading ? "Transmitting..." : <div className="flex items-center justify-center"><Send className="w-4 h-4 mr-2" /> Dispatch Secure Link</div>}
                  </Button>
                  <Button 
                    type="button" 
                    variant="ghost" 
                    onClick={handleClose} 
                    disabled={isLoading}
                    className="w-full h-12 text-slate-500 font-semibold rounded-xl hover:bg-slate-100"
                  >
                    Cancel
                  </Button>
                </div>
              </form>
            </div>
          </Motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}