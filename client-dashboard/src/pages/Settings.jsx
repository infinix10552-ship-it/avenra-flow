import { useState } from "react";
import { useAuth } from "../context/useAuth"; // Adjust if your import path is slightly different
import { Card, CardHeader, CardTitle, CardContent } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { Input } from "../components/ui/Input";
import { User, Building, Shield, LogOut, Copy, CheckCircle2, Key } from "lucide-react";

export default function Settings() {
  const { logout } = useAuth();
  // Retrieve the credentials we locked in the vault during login
  const orgId = localStorage.getItem("avenra_org_id") || "No Organization Linked";
  
  // --- NEW: Decode the JWT to get the real email ---
  const token = localStorage.getItem("avenra_token");
  let userEmail = "Loading...";
  if (token) {
    try {
      // JWTs are 3 parts separated by dots. The middle part is the Base64 encoded payload.
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(window.atob(base64));
      userEmail = payload.sub || "user@avenra.com"; // Spring Boot usually puts email in 'sub'
    } catch  {
      console.error("Failed to decode token");
    }
  }
  // Retrieve the credentials we locked in the vault during login
  
  // UX State for the copy buttons
  const [copiedOrg, setCopiedOrg] = useState(false);
  const [copiedApi, setCopiedApi] = useState(false);

  const handleCopy = (text, setCopiedState) => {
    navigator.clipboard.writeText(text);
    setCopiedState(true);
    setTimeout(() => setCopiedState(false), 2000);
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      
      <div>
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Workspace Settings</h1>
        <p className="text-slate-500 mt-1 text-sm">Manage your Avenra FLOW environment, API keys, and security.</p>
      </div>

      <div className="grid gap-6">
        
        {/* === CARD 1: User Profile === */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center text-slate-800">
              <User className="w-5 h-5 mr-2 text-avenra-500" /> Personal Profile
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Full Name</label>
                <Input type="text" defaultValue="Admin User" disabled className="bg-slate-50 text-slate-500" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Email Address</label>
                <Input type="email" value={userEmail} disabled className="bg-slate-50 text-slate-500 font-medium" />
              </div>
            </div>
            <p className="text-xs text-slate-500 mt-2">
              <Shield className="w-3 h-3 inline mr-1" /> Profile data is currently synced via your Google/OAuth Provider.
            </p>
          </CardContent>
        </Card>

        {/* === CARD 2: Workspace & Integrations === */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center text-slate-800">
              <Building className="w-5 h-5 mr-2 text-indigo-500" /> Workspace Credentials
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            
            {/* Organization ID */}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Organization ID (UUID)</label>
              <div className="flex space-x-2">
                <Input 
                  type="text" 
                  value={orgId} 
                  readOnly 
                  className="font-mono text-sm bg-slate-50 text-slate-600" 
                />
                <Button 
                  variant="outline" 
                  onClick={() => handleCopy(orgId, setCopiedOrg)}
                  className="w-24 shrink-0"
                >
                  {copiedOrg ? <CheckCircle2 className="w-4 h-4 text-emerald-500" /> : <><Copy className="w-4 h-4 mr-2" /> Copy</>}
                </Button>
              </div>
              <p className="text-xs text-slate-500 mt-1">This ID routes all financial data to your isolated tenant database.</p>
            </div>

            <div className="border-t border-slate-100 pt-6"></div>

            {/* Dummy API Key for ERP Integration */}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">ERP Integration API Key</label>
              <div className="flex space-x-2">
                <Input 
                  type="password" 
                  defaultValue="sk_live_avenra_9876543210abcdef" 
                  readOnly 
                  className="font-mono text-sm bg-slate-50 text-slate-600" 
                />
                <Button 
                  variant="outline" 
                  onClick={() => handleCopy("sk_live_avenra_9876543210abcdef", setCopiedApi)}
                  className="w-24 shrink-0"
                >
                  {copiedApi ? <CheckCircle2 className="w-4 h-4 text-emerald-500" /> : <><Copy className="w-4 h-4 mr-2" /> Copy</>}
                </Button>
              </div>
              <p className="text-xs text-slate-500 mt-1">Use this key to authenticate requests from your internal ERP systems.</p>
            </div>

          </CardContent>
        </Card>

        {/* === CARD 3: Danger Zone === */}
        <Card className="border-red-100 shadow-sm">
          <CardHeader>
            <CardTitle className="text-red-600 flex items-center">
              Danger Zone
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-slate-600 mb-4">
              Logging out will clear your local encryption keys and terminate your secure WebSocket connection.
            </p>
            <Button 
              variant="outline" 
              className="text-red-600 border-red-200 hover:bg-red-50 hover:text-red-700"
              onClick={logout}
            >
              <LogOut className="w-4 h-4 mr-2" />
              Terminate Session
            </Button>
          </CardContent>
        </Card>

      </div>
    </div>
  );
}