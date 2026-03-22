import { useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { Loader2 } from "lucide-react";

export default function OAuth2RedirectHandler() {
  const location = useLocation();
  const navigate = useNavigate();
  const { login } = useAuth();
  // A simple browser-safe JWT decoder
  const decodeJwt = (token) => {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(window.atob(base64));
    } catch {
      console.error("Failed to decode JWT.");
      return null;
    }
  };

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const token = params.get("token");

    if (token) {
      console.log("[OAUTH] Caught secure token from Google redirect.");
      
      // --- NEW: Dynamic Organization Resolution ---
      const decodedPayload = decodeJwt(token);
      const actualOrgId = decodedPayload?.orgId;

      if (actualOrgId) {
        login(token, actualOrgId);
        navigate("/dashboard", { replace: true });
      } else {
        console.error("[OAUTH] Token missing orgId claim.");
        navigate("/login", { replace: true });
      }
      // ------------------------------------------

    } else {
      console.error("[OAUTH] No token found in URL.");
      navigate("/login", { replace: true });
    }
  }, [location, login, navigate]);

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50">
      <Loader2 className="h-8 w-8 text-avenra-500 animate-spin mb-4" />
      <p className="text-slate-600 font-medium text-sm tracking-wide">Securing connection to Avenra Vault...</p>
    </div>
  );
}