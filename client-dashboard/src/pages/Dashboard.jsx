import { useState, useEffect, useCallback } from "react";
import { motion as Motion } from "framer-motion";
import { useNavigate } from "react-router-dom";
import api from "../api/axiosInterceptor";
import { Toast } from "../components/ui/Toast";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/Card";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import FilterBar from "../components/shared/FilterBar";
import { DollarSign, Clock, CheckCircle2, Download, FileText, ClipboardCheck, Trash2, History } from "lucide-react";

const containerVariants = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.1 } }
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0, transition: { type: "spring", stiffness: 300, damping: 24 } }
};

export default function Dashboard() {
  const [invoices, setInvoices] = useState([]);
  const [deletedInvoices, setDeletedInvoices] = useState([]);
  const [stats, setStats] = useState({
    totalInvoices: 0,
    completed: 0,
    pending: 0,
    needsReview: 0,
    failed: 0,
    totalBilling: 0,
    totalTax: 0
  });
  const [isLoading, setIsLoading] = useState(true);
  const [toastConfig, setToastConfig] = useState({ isVisible: false, message: "" });
  const [exportLoading, setExportLoading] = useState(false);
  const navigate = useNavigate();

  const fetchInvoices = useCallback(async (filters = {}) => {
    try {
      const params = Object.fromEntries(Object.entries(filters).filter(([, v]) => v !== ""));
      const response = await api.get("/invoices/search", { params });
      setInvoices(response.data);
    } catch (error) {
      console.error("Failed to fetch ledger data:", error);
    }
  }, []);

  const fetchAnalytics = useCallback(async () => {
    try {
      const response = await api.get("/invoices/analytics");
      setStats(response.data);
    } catch (error) {
      console.error("Failed to fetch analytics:", error);
    }
  }, []);

  const fetchDeletedHistory = useCallback(async () => {
    try {
      const response = await api.get("/invoices/deleted-history");
      setDeletedInvoices(response.data);
    } catch (error) {
      console.error("Failed to fetch deletion audit log:", error);
    }
  }, []);

  useEffect(() => {
    setIsLoading(true);
    Promise.all([fetchInvoices(), fetchAnalytics(), fetchDeletedHistory()]).finally(() => setIsLoading(false));
    
    const handleUpdate = () => {
      fetchInvoices();
      fetchAnalytics();
      fetchDeletedHistory();
    };
    window.addEventListener('invoice-updated', handleUpdate);
    return () => window.removeEventListener('invoice-updated', handleUpdate);
  }, [fetchInvoices, fetchAnalytics, fetchDeletedHistory]);

  const formatCurrency = (amount) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount || 0);

  const getStatusBadge = (status) => {
    switch (status) {
      case "COMPLETED": return <Badge variant="success">Completed</Badge>;
      case "PROCESSING": return <Badge variant="warning">AI Processing</Badge>;
      case "PENDING": return <Badge variant="default">Pending</Badge>;
      case "REQUIRES_MANUAL_REVIEW": return <Badge variant="warning">Review Required</Badge>;
      case "FAILED": return <Badge variant="error">Failed</Badge>;
      default: return <Badge variant="default">{status}</Badge>;
    }
  };

  const handleExportCSV = async () => {
    try {
      setExportLoading(true);
      const response = await api.get("/invoices/export", { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `avenra_export_${new Date().toISOString().split('T')[0]}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      setToastConfig({ isVisible: true, message: "Ledger exported successfully for GST filing." });
    } catch (error) {
      console.error("Export failed:", error);
      setToastConfig({ isVisible: true, message: "Failed to generate export. Please check permissions." });
    } finally {
      setExportLoading(false);
    }
  };

  const formatDateTime = (dateStr) => {
    if (!dateStr) return "—";
    try {
      const d = new Date(dateStr);
      return d.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
    } catch { return dateStr; }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-8 relative">
      
      {/* HEADER ZONE */}
      <Motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">GST Compliance Dashboard</h1>
          <p className="text-slate-500 mt-1 text-sm">Real-time overview of invoice processing and GST compliance.</p>
        </div>
        <div className="flex items-center space-x-2">
          <Button variant="outline" onClick={handleExportCSV} disabled={exportLoading}>
            <Download className="w-4 h-4 mr-2"/> {exportLoading ? "Generating..." : "Export to Tally"}
          </Button>
          <Button onClick={() => { fetchInvoices(); fetchAnalytics(); fetchDeletedHistory(); }}>Refresh</Button>
        </div>
      </Motion.div>

      {/* TELEMETRY DECK — 4 cards */}
      <Motion.div variants={containerVariants} initial="hidden" animate="show" className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <Motion.div variants={itemVariants}>
          <Card className="hover:shadow-[0_8px_30px_rgb(0,0,0,0.06)] transition-shadow duration-300 border-slate-200/60">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-slate-500">Total Revenue Vault</CardTitle>
              <div className="w-8 h-8 rounded-full bg-blue-50 flex items-center justify-center">
                <DollarSign className="h-4 w-4 text-avenra-500" />
              </div>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-slate-900">{formatCurrency(stats.totalBilling)}</div>
              <p className="text-[10px] text-slate-400 mt-1">Tax Component: {formatCurrency(stats.totalTax)}</p>
            </CardContent>
          </Card>
        </Motion.div>

        <Motion.div variants={itemVariants}>
          <Card className="hover:shadow-[0_8px_30px_rgb(0,0,0,0.06)] transition-shadow duration-300 border-slate-200/60">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-slate-500">Queue Processing</CardTitle>
              <div className="w-8 h-8 rounded-full bg-amber-50 flex items-center justify-center">
                <Clock className="h-4 w-4 text-amber-500" />
              </div>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-slate-900">{stats.pending} <span className="text-sm font-normal text-slate-500">active</span></div>
            </CardContent>
          </Card>
        </Motion.div>

        <Motion.div variants={itemVariants}>
          <Card className="hover:shadow-[0_8px_30px_rgb(0,0,0,0.06)] transition-shadow duration-300 border-slate-200/60">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-slate-500">Completed Records</CardTitle>
              <div className="w-8 h-8 rounded-full bg-emerald-50 flex items-center justify-center">
                <CheckCircle2 className="h-4 w-4 text-emerald-500" />
              </div>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-slate-900">{stats.completed} <span className="text-sm font-normal text-slate-500">safeguarded</span></div>
            </CardContent>
          </Card>
        </Motion.div>

        <Motion.div variants={itemVariants}>
          <Card className={`hover:shadow-[0_8px_30px_rgb(0,0,0,0.06)] transition-shadow duration-300 ${stats.needsReview > 0 ? "border-amber-300 bg-amber-50/30" : "border-slate-200/60"}`}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-slate-500">Manual Interventions</CardTitle>
              <div className="w-8 h-8 rounded-full bg-amber-50 flex items-center justify-center">
                <ClipboardCheck className="h-4 w-4 text-amber-500" />
              </div>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-slate-900">{stats.needsReview} <span className="text-sm font-normal text-slate-500">blocked</span></div>
              {stats.needsReview > 0 && (
                <button onClick={() => navigate("/review-queue")} className="text-xs text-amber-600 hover:text-amber-700 font-medium mt-1 cursor-pointer">
                  Correct errors now →
                </button>
              )}
            </CardContent>
          </Card>
        </Motion.div>
      </Motion.div>

      {/* DATA GRID */}
      <Motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.3 }}>
        <Card className="overflow-hidden border-slate-200 shadow-sm">
          <FilterBar onFilterChange={fetchInvoices} />
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="text-xs text-slate-500 uppercase bg-slate-50/80 border-b border-slate-200 backdrop-blur-sm">
                <tr>
                  <th className="px-5 py-3 font-semibold">Supplier</th>
                  <th className="px-5 py-3 font-semibold">GSTIN</th>
                  <th className="px-5 py-3 font-semibold">Date</th>
                  <th className="px-5 py-3 font-semibold">Status</th>
                  <th className="px-5 py-3 font-semibold text-right">Amount</th>
                  <th className="px-5 py-3 font-semibold text-center">Confidence</th>
                  <th className="px-5 py-3 font-semibold text-center">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr>
                    <td colSpan="7" className="px-6 py-12 text-center text-slate-500">
                      <div className="flex justify-center mb-2">
                        <div className="w-6 h-6 border-2 border-avenra-500 border-t-transparent rounded-full animate-spin"></div>
                      </div>
                      Syncing with Avenra Vault...
                    </td>
                  </tr>
                ) : invoices.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="px-6 py-12 text-center text-slate-500">
                      No invoices found. Navigate to the Upload Hub to begin ingestion.
                    </td>
                  </tr>
                ) : (
                  invoices.map((invoice, index) => (
                    <Motion.tr 
                      key={invoice.id} 
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ duration: 0.3, delay: index * 0.03 }}
                      className="bg-white hover:bg-slate-50 transition-colors"
                    >
                      <td className="px-5 py-3.5 font-medium text-slate-900 whitespace-nowrap max-w-[180px] truncate">
                        {invoice.supplierName || invoice.originalFileName}
                      </td>
                      <td className="px-5 py-3.5 text-slate-600 font-mono text-xs">{invoice.supplierGstin || "—"}</td>
                      <td className="px-5 py-3.5 text-slate-600">{invoice.invoiceDate || "—"}</td>
                      <td className="px-5 py-3.5">{getStatusBadge(invoice.status)}</td>
                      {/* Financial Integrity: Native INR Conversion display */}
                      <td className="px-5 py-3.5 font-semibold text-slate-900 text-right">
                        {invoice.convertedAmountInr ? (
                          <div className="flex flex-col items-end">
                            <span>{formatCurrency(invoice.convertedAmountInr)}</span>
                            {invoice.originalCurrency && invoice.originalCurrency !== 'INR' && (
                              <span className="text-[10px] text-slate-400 font-medium tracking-wide">
                                {invoice.originalCurrency} {invoice.totalAmount}
                              </span>
                            )}
                          </div>
                        ) : invoice.totalAmount ? (
                          formatCurrency(invoice.totalAmount)
                        ) : (
                          "—"
                        )}
                      </td>
                      <td className="px-5 py-3.5 text-center">
                        {invoice.aiConfidenceScore != null ? (
                          <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
                            invoice.aiConfidenceScore >= 75 ? "bg-emerald-100 text-emerald-700" :
                            invoice.aiConfidenceScore >= 50 ? "bg-amber-100 text-amber-700" :
                            "bg-red-100 text-red-700"
                          }`}>
                            {invoice.aiConfidenceScore}%
                          </span>
                        ) : "—"}
                      </td>
                      <td className="px-5 py-3.5 text-center">
                        <button 
                          onClick={() => navigate(`/invoices/${invoice.id}`)}
                          className="text-slate-500 hover:text-avenra-600 transition-colors cursor-pointer p-1.5 rounded-md hover:bg-avenra-50 border border-transparent hover:border-avenra-200 font-medium text-xs"
                        >
                          View
                        </button>
                      </td>
                    </Motion.tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </Card>
      </Motion.div>

      {/* DELETION AUDIT LOG */}
      {deletedInvoices.length > 0 && (
        <Motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.4 }}>
          <Card className="overflow-hidden border-red-200/60 shadow-sm bg-red-50/20">
            <CardHeader className="flex flex-row items-center justify-between py-4 border-b border-red-100 bg-red-50/50">
              <CardTitle className="text-sm font-semibold text-red-800 flex items-center">
                <History className="w-4 h-4 mr-2 text-red-500" />
                Deletion Audit Log
                <span className="ml-2 text-xs font-normal text-red-500 bg-red-100 px-2 py-0.5 rounded-full">{deletedInvoices.length} records</span>
              </CardTitle>
            </CardHeader>
            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead className="text-xs text-red-600 uppercase bg-red-50/80 border-b border-red-100">
                  <tr>
                    <th className="px-5 py-3 font-semibold">Supplier / File</th>
                    <th className="px-5 py-3 font-semibold">Invoice Date</th>
                    <th className="px-5 py-3 font-semibold text-right">Amount</th>
                    <th className="px-5 py-3 font-semibold">Deleted By</th>
                    <th className="px-5 py-3 font-semibold">Deleted At</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-red-100">
                  {deletedInvoices.map((inv) => (
                    <tr key={inv.id} className="bg-white/60 hover:bg-red-50/40 transition-colors">
                      <td className="px-5 py-3 font-medium text-slate-800 whitespace-nowrap max-w-[200px] truncate">
                        <div className="flex items-center space-x-2">
                          <Trash2 className="w-3.5 h-3.5 text-red-400 flex-shrink-0" />
                          <span>{inv.supplierName || inv.originalFileName}</span>
                        </div>
                      </td>
                      <td className="px-5 py-3 text-slate-600">{inv.invoiceDate || "—"}</td>
                      <td className="px-5 py-3 font-semibold text-slate-800 text-right">{inv.totalAmount ? formatCurrency(inv.totalAmount) : "—"}</td>
                      <td className="px-5 py-3">
                        <span className="text-xs font-medium text-red-700 bg-red-100 px-2 py-1 rounded-md">
                          {inv.deletedBy || "SYSTEM"}
                        </span>
                      </td>
                      <td className="px-5 py-3 text-slate-600 text-xs">{formatDateTime(inv.deletedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>
        </Motion.div>
      )}

      <Toast isVisible={toastConfig.isVisible} message={toastConfig.message} onClose={() => setToastConfig({ isVisible: false, message: "" })}/>
    </div>
  );
}
