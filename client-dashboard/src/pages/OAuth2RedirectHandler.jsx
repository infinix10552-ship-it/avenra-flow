import { useEffect, useRef } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { Loader2 } from "lucide-react";

export default function OAuth2RedirectHandler() {
  const location = useLocation();
  const navigate = useNavigate();
  const { login } = useAuth();
  // Guard: Ensure useEffect only fires once (React 18 strict mode double-fires)
  const hasProcessed = useRef(false);

  // A robust browser-safe JWT decoder with proper Base64URL padding
  const decodeJwt = (token) => {
    try {
      const base64Url = token.split('.')[1];
      if (!base64Url) return null;
      // Convert Base64URL to standard Base64 and fix padding
      let base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      // Pad with '=' to make length a multiple of 4
      const padLength = (4 - (base64.length % 4)) % 4;
      base64 += '='.repeat(padLength);
      return JSON.parse(window.atob(base64));
    } catch (err) {
      console.error("[OAUTH] Failed to decode JWT:", err);
      return null;
    }
  };

  useEffect(() => {
    // Prevent double-processing in React 18 strict mode
    if (hasProcessed.current) return;
    hasProcessed.current = true;

    const params = new URLSearchParams(location.search);
    const token = params.get("token");

    if (token) {
      console.log("[OAUTH] Caught secure token from Google redirect.");
      
      const decodedPayload = decodeJwt(token);
      const actualOrgId = decodedPayload?.orgId;

      if (actualOrgId) {
        login(token, actualOrgId);
        navigate("/dashboard", { replace: true });
      } else {
        console.error("[OAUTH] Token missing orgId claim. Payload:", decodedPayload);
        navigate("/login", { replace: true });
      }

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
