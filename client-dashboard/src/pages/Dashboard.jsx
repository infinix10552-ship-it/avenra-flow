import { useState, useEffect, useCallback } from "react";
import { motion as Motion } from "framer-motion";
import { useNavigate } from "react-router-dom";
import api from "../api/axiosInterceptor";
import { Toast } from "../components/ui/Toast";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/Card";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import WhatsAppModal from "../components/shared/WhatsAppModal";
import FilterBar from "../components/shared/FilterBar";
import { DollarSign, Clock, CheckCircle2, Download, Share2 } from "lucide-react";

// Framer Motion Variants
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
  const [isLoading, setIsLoading] = useState(true);
  const [toastConfig, setToastConfig] = useState({ isVisible: false, message: "" });
  const [selectedInvoice, setSelectedInvoice] = useState(null);
  const navigate = useNavigate();

  const fetchInvoices = useCallback(async (filters = {}) => {
    if (invoices.length === 0) setIsLoading(true); 
    try {
      const params = Object.fromEntries(Object.entries(filters).filter(([, v]) => v !== ""));
      const response = await api.get("/invoices/search", { params });
      setInvoices(response.data);
    } catch (error) {
      console.error("Failed to fetch ledger data:", error);
    } finally {
      setIsLoading(false);
    }
  }, [invoices.length]);

  useEffect(() => {
    fetchInvoices();
    const handleUpdate = () => fetchInvoices();
    window.addEventListener('invoice-updated', handleUpdate);
    return () => window.removeEventListener('invoice-updated', handleUpdate);
  }, [fetchInvoices]);

  const totalVolume = invoices.reduce((sum, inv) => sum + (inv.totalAmount || 0), 0);
  const pendingCount = invoices.filter(i => i.status === "PENDING" || i.status === "PROCESSING").length;
  const completedCount = invoices.filter(i => i.status === "COMPLETED").length;

  const formatCurrency = (amount) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount || 0);

  const getStatusBadge = (status) => {
    switch (status) {
      case "COMPLETED": return <Badge variant="success">Completed</Badge>;
      case "PROCESSING": return <Badge variant="warning">AI Processing</Badge>;
      case "PENDING": return <Badge variant="default">Pending</Badge>;
      case "FAILED": return <Badge variant="error">Failed</Badge>;
      default: return <Badge variant="default">{status}</Badge>;
    }
  };

  const handleExportCSV = () => {
    if (invoices.length === 0) return;
    const headers = ["Invoice ID", "Vendor Name", "Category", "Date", "Status", "Amount", "Currency"];
    const csvRows = invoices.map(inv => [
      inv.id, `"${inv.vendorName || inv.originalFileName || "Unknown"}"`, inv.category || "Uncategorized",
      inv.invoiceDate || "N/A", inv.status, inv.totalAmount || 0, inv.currency || "INR"
    ].join(","));
    const csvContent = [headers.join(","), ...csvRows].join("\n");
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", `avenra_export_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="max-w-7xl mx-auto space-y-8 relative">
      
      {/* HEADER ZONE */}
      <Motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Analytics Dashboard</h1>
          <p className="text-slate-500 mt-1 text-sm">Real-time overview of your financial document processing.</p>
        </div>
        <div className="flex items-center space-x-2">
          <Button variant="outline" onClick={handleExportCSV}>
            <Download className="w-4 h-4 mr-2"/> Export CSV
          </Button>
          <Button onClick={() => fetchInvoices()}>Refresh Grid</Button>
        </div>
      </Motion.div>

      {/* ZONE 1: THE TELEMETRY DECK */}
      <Motion.div variants={containerVariants} initial="hidden" animate="show" className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        
        <Motion.div variants={itemVariants}>
          <Card className="hover:shadow-[0_8px_30px_rgb(0,0,0,0.06)] transition-shadow duration-300 border-slate-200/60">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-slate-500">Total Processed Volume</CardTitle>
              <div className="w-8 h-8 rounded-full bg-blue-50 flex items-center justify-center">
                <DollarSign className="h-4 w-4 text-avenra-500" />
              </div>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-slate-900">{formatCurrency(totalVolume)}</div>
              <p className="text-xs text-emerald-600 mt-1 flex items-center font-medium">
                +12% from last month
              </p>
            </CardContent>
          </Card>
        </Motion.div>

        <Motion.div variants={itemVariants}>
          <Card className="hover:shadow-[0_8px_30px_rgb(0,0,0,0.06)] transition-shadow duration-300 border-slate-200/60">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-slate-500">Pending Extraction</CardTitle>
              <div className="w-8 h-8 rounded-full bg-amber-50 flex items-center justify-center">
                <Clock className="h-4 w-4 text-amber-500" />
              </div>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-slate-900">{pendingCount} <span className="text-sm font-normal text-slate-500">docs</span></div>
              <p className="text-xs text-slate-500 mt-1">Awaiting AI worker availability</p>
            </CardContent>
          </Card>
        </Motion.div>

        <Motion.div variants={itemVariants} className="sm:col-span-2 lg:col-span-1">
          <Card className="hover:shadow-[0_8px_30px_rgb(0,0,0,0.06)] transition-shadow duration-300 border-slate-200/60">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-slate-500">Completed Successfully</CardTitle>
              <div className="w-8 h-8 rounded-full bg-emerald-50 flex items-center justify-center">
                <CheckCircle2 className="h-4 w-4 text-emerald-500" />
              </div>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-slate-900">{completedCount} <span className="text-sm font-normal text-slate-500">docs</span></div>
              <p className="text-xs text-slate-500 mt-1">Ready for ERP integration</p>
            </CardContent>
          </Card>
        </Motion.div>

      </Motion.div>

      {/* ZONE 2 & 3: FILTER BAR & DATA GRID */}
      <Motion.div 
        initial={{ opacity: 0, y: 20 }} 
        animate={{ opacity: 1, y: 0 }} 
        transition={{ duration: 0.5, delay: 0.3 }}
      >
        <Card className="overflow-hidden border-slate-200 shadow-sm">
          <FilterBar onFilterChange={fetchInvoices} />
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="text-xs text-slate-500 uppercase bg-slate-50/80 border-b border-slate-200 backdrop-blur-sm">
                <tr>
                  <th className="px-6 py-4 font-semibold">Vendor Name</th>
                  <th className="px-6 py-4 font-semibold">Category</th>
                  <th className="px-6 py-4 font-semibold">Date</th>
                  <th className="px-6 py-4 font-semibold">Status</th>
                  <th className="px-6 py-4 font-semibold text-right">Amount</th>
                  <th className="px-6 py-4 font-semibold text-center">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr>
                    <td colSpan="6" className="px-6 py-12 text-center text-slate-500">
                      <div className="flex justify-center mb-2">
                        <div className="w-6 h-6 border-2 border-avenra-500 border-t-transparent rounded-full animate-spin"></div>
                      </div>
                      Syncing with Avenra Vault...
                    </td>
                  </tr>
                ) : invoices.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="px-6 py-12 text-center text-slate-500">
                      No invoices found. Navigate to the Upload Hub to begin ingestion.
                    </td>
                  </tr>
                ) : (
                  invoices.map((invoice, index) => (
                    <Motion.tr 
                      key={invoice.id} 
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ duration: 0.3, delay: index * 0.05 }}
                      className="bg-white hover:bg-slate-50 transition-colors"
                    >
                      <td className="px-6 py-4 font-medium text-slate-900 whitespace-nowrap">
                        {invoice.vendorName || invoice.originalFileName}
                      </td>
                      <td className="px-6 py-4 text-slate-600">{invoice.category || "---"}</td>
                      <td className="px-6 py-4 text-slate-600">{invoice.invoiceDate || "---"}</td>
                      <td className="px-6 py-4">{getStatusBadge(invoice.status)}</td>
                      <td className="px-6 py-4 font-semibold text-slate-900 text-right">
                        {invoice.totalAmount ? formatCurrency(invoice.totalAmount) : "---"}
                      </td>
                      <td className="px-6 py-4 text-center flex items-center justify-center space-x-2">
                        {invoice.status === "COMPLETED" && (
                          <button 
                            onClick={() => setSelectedInvoice(invoice)}
                            className="text-emerald-600 hover:text-emerald-700 transition-colors cursor-pointer p-1.5 rounded-md hover:bg-emerald-50 border border-transparent hover:border-emerald-200"
                            title="Share via WhatsApp"
                          >
                            <Share2 className="w-4 h-4" />
                          </button>
                        )}
                        <button 
                          onClick={() => navigate(`/invoices/${invoice.id}`)}
                          className="text-slate-500 hover:text-avenra-600 transition-colors cursor-pointer p-1.5 rounded-md hover:bg-avenra-50 border border-transparent hover:border-avenra-200 font-medium text-xs flex items-center shadow-sm bg-white"
                          title="View Details"
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

      <Toast isVisible={toastConfig.isVisible} message={toastConfig.message} onClose={() => setToastConfig({ isVisible: false, message: "" })}/>
      <WhatsAppModal isOpen={!!selectedInvoice} invoice={selectedInvoice} onClose={() => setSelectedInvoice(null)} />
    </div>
  );
}
