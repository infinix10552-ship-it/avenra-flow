import { Routes, Route, Navigate } from "react-router-dom";
import { useAuth } from "./context/useAuth";
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

// A temporary component just to prove the layout works
const PlaceholderPage = ({ title }) => (
  <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-8 h-full flex flex-col items-center justify-center text-slate-400">
    <h2 className="text-2xl font-bold text-slate-800 mb-2">{title}</h2>
    <p>This page is ready for development.</p>
  </div>
);

function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route path="/oauth2-redirect" element={<OAuth2RedirectHandler />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      
      {/* EVERYTHING inside this block gets the Sidebar and Topbar! */}
      <Route element={
        <ProtectedRoute>
          <DashboardLayout />
        </ProtectedRoute>
      }>
        {/* Child Routes - The <Outlet /> renders these */}
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/upload" element={<UploadHub />} />
        <Route path="/invoices" element={<AllInvoices />} />
        <Route path="/invoices/:id" element={<InvoiceDetails />} />
        <Route path="/settings" element={<Settings />} />
      </Route>

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

export default App;