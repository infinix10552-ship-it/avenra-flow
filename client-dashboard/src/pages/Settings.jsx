import { useState } from "react";
import { motion as Motion } from "framer-motion";
import { useAuth } from "../context/useAuth";
import { Card, CardHeader, CardTitle, CardContent } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { Input } from "../components/ui/Input";
import { User, Building, Shield, LogOut, Copy, CheckCircle2 } from "lucide-react";

// Framer Motion Variants
const containerVariants = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.1 } }
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0, transition: { type: "spring", stiffness: 300, damping: 24 } }
};

export default function Settings() {
  const { logout } = useAuth();
  const orgId = localStorage.getItem("avenra_org_id") || "No Organization Linked";
  
  const token = localStorage.getItem("avenra_token");
  let userEmail = "Loading...";
  if (token) {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(window.atob(base64));
      userEmail = payload.sub || "user@avenra.com"; 
    } catch  {
      console.error("Failed to decode token");
    }
  }
  
  const [copiedOrg, setCopiedOrg] = useState(false);
  const [copiedApi, setCopiedApi] = useState(false);

  const handleCopy = (text, setCopiedState) => {
    navigator.clipboard.writeText(text);
    setCopiedState(true);
    setTimeout(() => setCopiedState(false), 2000);
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8 pb-12">
      
      <Motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }}>
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Workspace Settings</h1>
        <p className="text-slate-500 mt-1 text-sm">Manage your Avenra FLOW environment, API keys, and security.</p>
      </Motion.div>

      <Motion.div variants={containerVariants} initial="hidden" animate="show" className="grid gap-6">
        
        {/* === CARD 1: User Profile === */}
        <Motion.div variants={itemVariants}>
          <Card className="border-slate-200/60 shadow-[0_8px_30px_rgb(0,0,0,0.04)] hover:shadow-md transition-shadow duration-300 overflow-hidden">
            <CardHeader className="bg-slate-50/50 border-b border-slate-100">
              <CardTitle className="flex items-center text-slate-800 text-lg">
                <div className="p-2 bg-white rounded-lg shadow-sm border border-slate-100 mr-3">
                  <User className="w-5 h-5 text-avenra-500" />
                </div>
                Personal Profile
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-5 pt-6">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">Full Name</label>
                  <Input type="text" defaultValue="Admin User" disabled className="bg-slate-50/50 text-slate-500 border-slate-200 cursor-not-allowed shadow-inner" />
                </div>
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">Email Address</label>
                  <Input type="email" value={userEmail} disabled className="bg-slate-50/50 text-slate-600 font-medium border-slate-200 cursor-not-allowed shadow-inner" />
                </div>
              </div>
              <div className="flex items-center p-3 bg-avenra-50 rounded-xl border border-avenra-100">
                <Shield className="w-4 h-4 text-avenra-600 mr-2 shrink-0" /> 
                <p className="text-xs text-avenra-800 font-medium">Profile data is securely synced and protected via your OAuth Identity Provider.</p>
              </div>
            </CardContent>
          </Card>
        </Motion.div>

        {/* === CARD 2: Workspace & Integrations === */}
        <Motion.div variants={itemVariants}>
          <Card className="border-slate-200/60 shadow-[0_8px_30px_rgb(0,0,0,0.04)] hover:shadow-md transition-shadow duration-300 overflow-hidden">
            <CardHeader className="bg-slate-50/50 border-b border-slate-100">
              <CardTitle className="flex items-center text-slate-800 text-lg">
                <div className="p-2 bg-white rounded-lg shadow-sm border border-slate-100 mr-3">
                  <Building className="w-5 h-5 text-indigo-500" />
                </div>
                Workspace Credentials
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-8 pt-6">
              
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Organization ID (UUID)</label>
                <div className="flex space-x-3">
                  <Input 
                    type="text" 
                    value={orgId} 
                    readOnly 
                    className="font-mono text-sm bg-slate-50/50 text-slate-600 border-slate-200 shadow-inner flex-1" 
                  />
                  <Motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.95 }}>
                    <Button 
                      variant="outline" 
                      onClick={() => handleCopy(orgId, setCopiedOrg)}
                      className={`w-28 shrink-0 transition-colors ${copiedOrg ? 'bg-emerald-50 text-emerald-600 border-emerald-200 hover:bg-emerald-100' : 'bg-white'}`}
                    >
                      {copiedOrg ? <><CheckCircle2 className="w-4 h-4 mr-2" /> Copied</> : <><Copy className="w-4 h-4 mr-2" /> Copy ID</>}
                    </Button>
                  </Motion.div>
                </div>
                <p className="text-xs text-slate-500 mt-2">This ID rigidly isolates and routes all financial telemetry to your specific tenant database.</p>
              </div>

              <div className="border-t border-slate-100"></div>

              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">ERP Integration API Key</label>
                <div className="flex space-x-3">
                  <Input 
                    type="password" 
                    defaultValue="sk_live_avenra_9876543210abcdef" 
                    readOnly 
                    className="font-mono text-sm bg-slate-50/50 text-slate-600 border-slate-200 shadow-inner flex-1 tracking-widest" 
                  />
                  <Motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.95 }}>
                    <Button 
                      variant="outline" 
                      onClick={() => handleCopy("sk_live_avenra_9876543210abcdef", setCopiedApi)}
                      className={`w-28 shrink-0 transition-colors ${copiedApi ? 'bg-emerald-50 text-emerald-600 border-emerald-200 hover:bg-emerald-100' : 'bg-white'}`}
                    >
                      {copiedApi ? <><CheckCircle2 className="w-4 h-4 mr-2" /> Copied</> : <><Copy className="w-4 h-4 mr-2" /> Copy Key</>}
                    </Button>
                  </Motion.div>
                </div>
                <p className="text-xs text-slate-500 mt-2">Required for authenticating cross-origin requests from your internal ERP mainframes.</p>
              </div>

            </CardContent>
          </Card>
        </Motion.div>

        {/* === CARD 3: Danger Zone === */}
        <Motion.div variants={itemVariants}>
          <Card className="border-red-100/60 shadow-sm hover:shadow-md transition-shadow overflow-hidden bg-gradient-to-br from-white to-red-50/30">
            <CardHeader className="border-b border-red-50">
              <CardTitle className="text-red-600 flex items-center text-lg font-bold">
                Danger Zone
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <p className="text-sm text-slate-600 flex-1 pr-4">
                Executing a manual logout will immediately purge local encryption keys and sever the secure WebSocket connection.
              </p>
              <Motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.95 }} className="shrink-0">
                <Button 
                  variant="outline" 
                  className="w-full sm:w-auto text-red-600 border-red-200 bg-white hover:bg-red-50 hover:text-red-700 shadow-sm"
                  onClick={logout}
                >
                  <LogOut className="w-4 h-4 mr-2" />
                  Terminate Session
                </Button>
              </Motion.div>
            </CardContent>
          </Card>
        </Motion.div>

      </Motion.div>
    </div>
  );
}
