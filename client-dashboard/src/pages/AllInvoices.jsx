import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { motion as Motion } from "framer-motion";
import api from "../api/axiosInterceptor";
import { Card } from "../components/ui/Card";
import { Badge } from "../components/ui/Badge";
import FilterBar from "../components/shared/FilterBar";
import { Eye, Trash2, X, FileText } from "lucide-react";

export default function AllInvoices() {
  const [invoices, setInvoices] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [previewPdfUrl, setPreviewPdfUrl] = useState(null);
  const navigate = useNavigate();

  const fetchInvoices = useCallback(async (filters = {}) => {
    setIsLoading(true); 
    try {
      const params = Object.fromEntries(Object.entries(filters).filter(([, v]) => v !== ""));
      const response = await api.get("/invoices/search", { params });
      setInvoices(response.data);
    } catch (error) {
      console.error("Failed to fetch ledger data:", error);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => { fetchInvoices(); }, [fetchInvoices]);

  const handleDelete = async (invoiceId) => {
    const isConfirmed = window.confirm("Are you sure you want to permanently delete this invoice from the secure vault?");
    if (!isConfirmed) return;
    try {
      await api.delete(`/invoices/${invoiceId}`);
      setInvoices(invoices.filter(inv => inv.id !== invoiceId));
    } catch (err) {
      alert("Failed to delete invoice: Access denied or record not found.");
    }
  };

  const formatCurrency = (amount) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount || 0);

  const getStatusBadge = (status) => {
    switch (status) {
      case "COMPLETED": return <Badge variant="success">Completed</Badge>;
      case "PROCESSING": return <Badge variant="warning">Processing</Badge>;
      case "PENDING": return <Badge variant="default">Pending</Badge>;
      case "FAILED": return <Badge variant="error">Failed</Badge>;
      default: return <Badge variant="default">{status}</Badge>;
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      <Motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }}>
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Invoice Master Ledger</h1>
        <p className="text-slate-500 mt-1 text-sm">Complete history of all processed documents across your workspace.</p>
      </Motion.div>

      <Motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.2 }}>
        <Card className="overflow-hidden border-slate-200/60 shadow-sm">
          <FilterBar onFilterChange={fetchInvoices} />
          <div className="overflow-x-auto min-h-[500px]">
            <table className="w-full text-sm text-left">
              <thead className="text-xs text-slate-500 uppercase bg-slate-50/80 border-b border-slate-200 backdrop-blur-sm">
                <tr>
                  <th className="px-6 py-4 font-semibold">Supplier Name</th>
                  <th className="px-6 py-4 font-semibold">Ledger Account</th>
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
                      <div className="flex justify-center mb-3">
                        <div className="w-6 h-6 border-2 border-avenra-500 border-t-transparent rounded-full animate-spin"></div>
                      </div>
                      Loading secure vault records...
                    </td>
                  </tr>
                ) : invoices.length === 0 ? (
                  <tr><td colSpan="6" className="px-6 py-12 text-center text-slate-500">No records found matching your criteria.</td></tr>
                ) : (
                  invoices.map((invoice, index) => (
                    <Motion.tr 
                      key={invoice.id} 
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ duration: 0.3, delay: index * 0.05 }}
                      className="bg-white hover:bg-slate-50 transition-colors"
                    >
                      <td className="px-6 py-4 font-medium text-slate-900 whitespace-nowrap">{invoice.supplierName || invoice.originalFileName}</td>
                      <td className="px-6 py-4 text-slate-600">{invoice.ledgerAccountName || "---"}</td>
                      <td className="px-6 py-4 text-slate-600">{invoice.invoiceDate || "---"}</td>
                      <td className="px-6 py-4">{getStatusBadge(invoice.status)}</td>
                      <td className="px-6 py-4 font-semibold text-right text-slate-900">{invoice.totalAmount ? formatCurrency(invoice.totalAmount) : "---"}</td>
                  <td className="px-6 py-4 text-center">
                        <div className="flex items-center justify-center space-x-2">
                          <button 
                            title="View Data"
                            onClick={() => navigate(`/invoices/${invoice.id}`)}
                            className="text-slate-500 hover:text-avenra-600 transition-colors p-1.5 rounded-md hover:bg-avenra-50 border border-transparent hover:border-avenra-200 font-medium text-xs flex items-center justify-center shadow-sm bg-white"
                          >
                            View
                          </button>
                          {invoice.s3FileUrl && (
                            <button 
                              title="Preview Document"
                              onClick={() => setPreviewPdfUrl(invoice.s3FileUrl)}
                              className="text-slate-500 hover:text-blue-600 transition-colors p-1.5 rounded-md hover:bg-blue-50 border border-transparent hover:border-blue-200 shadow-sm bg-white"
                            >
                              <Eye className="w-4 h-4" />
                            </button>
                          )}
                          <button 
                            title="Delete Document"
                            onClick={() => handleDelete(invoice.id)}
                            className="text-slate-500 hover:text-red-600 transition-colors p-1.5 rounded-md hover:bg-red-50 border border-transparent hover:border-red-200 shadow-sm bg-white"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </Motion.tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </Card>
      </Motion.div>

      {/* PDF PREVIEW MODAL */}
      {previewPdfUrl && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4 sm:p-8">
          <Motion.div 
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-white w-full max-w-5xl h-[85vh] rounded-2xl shadow-2xl flex flex-col overflow-hidden"
          >
            <div className="flex items-center justify-between p-4 border-b border-slate-200 bg-slate-50">
              <h3 className="font-bold text-slate-800 flex items-center">
                <FileText className="w-5 h-5 mr-2 text-blue-500" />
                Document Preview
              </h3>
              <button onClick={() => setPreviewPdfUrl(null)} className="p-1.5 hover:bg-slate-200 rounded-md text-slate-500 transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="flex-1 bg-slate-300/50 relative">
              <iframe src={`${previewPdfUrl}#toolbar=0`} className="absolute inset-0 w-full h-full border-0" title="PDF Preview" />
            </div>
          </Motion.div>
        </div>
      )}
    </div>
  );
}
