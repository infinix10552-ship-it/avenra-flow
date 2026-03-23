import { Routes, Route, Navigate } from "react-router-dom";
import { useAuth } from "./context/useAuth";
import LandingPage from "./pages/LandingPage"; // <-- Import the new page
import Login from "./pages/Login";
import Register from "./pages/Register";
import OAuth2RedirectHandler from "./pages/OAuth2RedirectHandler";
import DashboardLayout from "./components/layout/DashboardLayout";
import UploadHub from "./pages/UploadHub";
import Dashboard from "./pages/Dashboard";
import InvoiceDetails from "./pages/InvoiceDetails";
import Settings from "./pages/Settings";
import AllInvoices from "./pages/AllInvoices";
import ForgotPassword from "./pages/ForgotPassword";
import ResetPassword from "./pages/ResetPassword";

// The Route Protector
const ProtectedRoute = ({ children }) => {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return children;
};

function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} /> 
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route path="/oauth2-redirect" element={<OAuth2RedirectHandler />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      
      {/* --- PROTECTED DASHBOARD ROUTES --- */}
      <Route element={
        <ProtectedRoute>
          <DashboardLayout />
        </ProtectedRoute>
      }>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/upload" element={<UploadHub />} />
        <Route path="/invoices" element={<AllInvoices />} />
        <Route path="/invoices/:id" element={<InvoiceDetails />} />
        <Route path="/settings" element={<Settings />} />
      </Route>

      {/* Fallback - Send unknown URLs back to the landing page */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;