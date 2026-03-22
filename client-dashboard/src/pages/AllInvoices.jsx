import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/axiosInterceptor";
import { Card } from "../components/ui/Card";
import { Badge } from "../components/ui/Badge";
import FilterBar from "../components/shared/FilterBar";
import { MoreHorizontal } from "lucide-react";

export default function AllInvoices() {
  const [invoices, setInvoices] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
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
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Invoice Master Ledger</h1>
        <p className="text-slate-500 mt-1 text-sm">Complete history of all processed documents.</p>
      </div>

      <Card className="overflow-hidden border-slate-200 shadow-sm">
        <FilterBar onFilterChange={fetchInvoices} />
        <div className="overflow-x-auto min-h-[500px]">
          <table className="w-full text-sm text-left">
            <thead className="text-xs text-slate-500 uppercase bg-slate-50 border-b border-slate-200">
              <tr>
                <th className="px-6 py-4 font-semibold">Vendor Name</th>
                <th className="px-6 py-4 font-semibold">Category</th>
                <th className="px-6 py-4 font-semibold">Date</th>
                <th className="px-6 py-4 font-semibold">Status</th>
                <th className="px-6 py-4 font-semibold text-right">Amount</th>
                <th className="px-6 py-4 font-semibold text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {isLoading ? (
                <tr><td colSpan="6" className="px-6 py-12 text-center text-slate-500">Loading vault records...</td></tr>
              ) : invoices.length === 0 ? (
                <tr><td colSpan="6" className="px-6 py-12 text-center text-slate-500">No records found.</td></tr>
              ) : (
                invoices.map((invoice) => (
                  <tr key={invoice.id} className="bg-white hover:bg-slate-50">
                    <td className="px-6 py-4 font-medium text-slate-900">{invoice.vendorName || invoice.originalFileName}</td>
                    <td className="px-6 py-4 text-slate-600">{invoice.category || "---"}</td>
                    <td className="px-6 py-4 text-slate-600">{invoice.invoiceDate || "---"}</td>
                    <td className="px-6 py-4">{getStatusBadge(invoice.status)}</td>
                    <td className="px-6 py-4 font-semibold text-right">{invoice.totalAmount ? formatCurrency(invoice.totalAmount) : "---"}</td>
                    <td className="px-6 py-4 text-center">
                      <button 
                        onClick={() => navigate(`/invoices/${invoice.id}`)}
                        className="text-slate-400 hover:text-avenra-600 transition-colors p-1.5 rounded-md hover:bg-avenra-50 border border-transparent hover:border-avenra-200 font-medium text-xs"
                      >
                        View
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}