import { useState } from "react";
import api from "../../api/axiosInterceptor";
import { Button } from "../ui/Button";
import { Input } from "../ui/Input";
import { X, MessageCircle, Send, CheckCircle2 } from "lucide-react";

export default function WhatsAppModal({ isOpen, onClose, invoice }) {
  const [phoneNumber, setPhoneNumber] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [status, setStatus] = useState({ type: "idle", message: "" });

  // If the modal is closed, don't render it
  if (!isOpen || !invoice) return null;

  const handleSend = async (e) => {
    e.preventDefault();
    if (!phoneNumber) return;

    setIsLoading(true);
    setStatus({ type: "idle", message: "" });

    // Ensure the phone number has a country code (default to India if none provided for testing)
    const formattedNumber = phoneNumber.startsWith("+") ? phoneNumber : `+91${phoneNumber}`;

    try {
      // Hit the Spring Boot WhatsApp Controller
      await api.post("/whatsapp/send", {
        invoiceId: invoice.id,
        targetPhoneNumber: formattedNumber,
      });

      setStatus({ type: "success", message: "Invoice securely dispatched via WhatsApp." });
      
      // Auto-close after a few seconds so the user can keep working
      setTimeout(() => {
        onClose();
        setStatus({ type: "idle", message: "" });
        setPhoneNumber("");
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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* The Blurred Backdrop */}
      <div 
        className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm transition-opacity"
        onClick={handleClose}
      ></div>

      {/* The Premium Glassmorphic Card */}
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        
        {/* Header */}
        <div className="bg-emerald-50 px-6 py-4 border-b border-emerald-100 flex items-center justify-between">
          <div className="flex items-center space-x-2 text-emerald-700">
            <MessageCircle className="w-5 h-5" />
            <h3 className="font-semibold text-lg">Share via WhatsApp</h3>
          </div>
          <button onClick={handleClose} className="text-emerald-600 hover:text-emerald-800 transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6">
          <div className="mb-6 p-3 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-600">
            You are sharing the invoice for <span className="font-semibold text-slate-900">{invoice.vendorName || invoice.originalFileName}</span> 
            <br/>Amount: <span className="font-semibold text-slate-900">₹{invoice.totalAmount || "0.00"}</span>
          </div>

          <form onSubmit={handleSend} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Recipient Phone Number
              </label>
              <Input
                type="tel"
                placeholder="+91 98765 43210"
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                required
                className="w-full"
              />
              <p className="text-xs text-slate-500 mt-1">Include country code (e.g., +1 or +91).</p>
            </div>

            {/* Status Messages */}
            {status.type === "error" && (
              <p className="text-sm text-red-600 bg-red-50 p-2 rounded border border-red-100">{status.message}</p>
            )}
            {status.type === "success" && (
              <p className="text-sm text-emerald-600 bg-emerald-50 p-2 rounded border border-emerald-100 flex items-center">
                <CheckCircle2 className="w-4 h-4 mr-2" /> {status.message}
              </p>
            )}

            <div className="pt-2 flex justify-end space-x-3">
              <Button type="button" variant="ghost" onClick={handleClose} disabled={isLoading}>
                Cancel
              </Button>
              <Button 
                type="submit" 
                disabled={isLoading || status.type === "success"}
                className="bg-emerald-600 hover:bg-emerald-700 text-white"
              >
                {isLoading ? "Transmitting..." : <><Send className="w-4 h-4 mr-2" /> Send Secure Link</>}
              </Button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}