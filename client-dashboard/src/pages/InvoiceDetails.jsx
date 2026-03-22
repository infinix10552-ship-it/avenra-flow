import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import api from "../api/axiosInterceptor";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/Card";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { ArrowLeft, Download, FileText, Building2, Calendar, DollarSign, Activity } from "lucide-react";

export default function InvoiceDetails() {
  const { id } = useParams(); // Extracts the ID from the URL (/invoices/123)
  const navigate = useNavigate();
  
  const [invoice, setInvoice] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchInvoiceDetails = async () => {
      try {
        setIsLoading(true);
        // Hit your Spring Boot endpoint to get the specific invoice
        const response = await api.get(`/invoices/${id}`);
        setInvoice(response.data);
      } catch {
        setError("Failed to locate this invoice in the secure vault.");
      } finally {
        setIsLoading(false);
      }
    };

    fetchInvoiceDetails();
  }, [id]);

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount || 0);
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case "COMPLETED": return <Badge variant="success">Completed</Badge>;
      case "PROCESSING": return <Badge variant="warning">AI Processing</Badge>;
      case "PENDING": return <Badge variant="default">Pending</Badge>;
      case "FAILED": return <Badge variant="error">Failed</Badge>;
      default: return <Badge variant="default">{status}</Badge>;
    }
  };

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center h-[70vh] text-slate-500">
        <div className="w-8 h-8 border-4 border-avenra-500 border-t-transparent rounded-full animate-spin mb-4"></div>
        <p>Decrypting vault record...</p>
      </div>
    );
  }

  if (error || !invoice) {
    return (
      <div className="text-center py-12">
        <p className="text-red-500 font-medium">{error}</p>
        <Button onClick={() => navigate("/dashboard")} className="mt-4">Return to Dashboard</Button>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      
      {/* THE CONTROL HEADER */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
        <div className="flex items-center space-x-4">
          <Button variant="ghost" onClick={() => navigate("/dashboard")} className="px-2 text-slate-500">
            <ArrowLeft className="w-5 h-5 mr-2" /> Back
          </Button>
          <div className="h-6 w-px bg-slate-200"></div>
          <div>
            <h1 className="text-lg font-bold text-slate-900 flex items-center">
              Invoice Record <span className="text-slate-400 font-normal ml-2 text-sm">#{invoice.id.substring(0,8)}</span>
            </h1>
          </div>
        </div>
        <div className="flex items-center space-x-3">
          {getStatusBadge(invoice.status)}
          {/* If you have a PDF URL, allow them to download it directly */}
          {invoice.s3Url && (
            <Button variant="outline" onClick={() => window.open(invoice.s3Url, '_blank')}>
              <Download className="w-4 h-4 mr-2" /> Original File
            </Button>
          )}
        </div>
      </div>

      {/* THE SPLIT PANE ENGINE */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 h-[calc(100vh-12rem)] min-h-[600px]">
        
        {/* LEFT PANE: The Document Viewer (2/3 width) */}
        <Card className="lg:col-span-2 flex flex-col overflow-hidden h-full">
          <CardHeader className="bg-slate-50 border-b border-slate-200 py-3">
            <CardTitle className="text-sm flex items-center text-slate-600">
              <FileText className="w-4 h-4 mr-2" /> Document Source
            </CardTitle>
          </CardHeader>
          <CardContent className="p-0 flex-1 bg-slate-200/50">
            {invoice.s3Url ? (
              <iframe 
                src={`${invoice.s3Url}#toolbar=0`} 
                className="w-full h-full border-0"
                title="Invoice PDF"
              />
            ) : (
              <div className="flex flex-col items-center justify-center h-full text-slate-400">
                <FileText className="w-16 h-16 mb-4 opacity-20" />
                <p>No document source available for rendering.</p>
              </div>
            )}
          </CardContent>
        </Card>

        {/* RIGHT PANE: Cognitive Extraction Data (1/3 width) */}
        <div className="space-y-6 overflow-y-auto pr-2">
          
          <Card>
            <CardHeader className="py-4 border-b border-slate-100">
              <CardTitle className="text-sm text-slate-500 uppercase tracking-wider">Financial Summary</CardTitle>
            </CardHeader>
            <CardContent className="pt-6 space-y-6">
              
              <div className="flex items-start space-x-3">
                <div className="p-2 bg-avenra-50 rounded-lg text-avenra-600"><DollarSign className="w-5 h-5" /></div>
                <div>
                  <p className="text-xs font-medium text-slate-500 uppercase tracking-wider mb-1">Total Amount</p>
                  <p className="text-3xl font-bold text-slate-900">{formatCurrency(invoice.totalAmount)}</p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4 pt-4 border-t border-slate-100">
                <div>
                  <p className="text-xs font-medium text-slate-500 uppercase mb-1">Tax Amount</p>
                  <p className="text-sm font-semibold text-slate-900">{invoice.taxAmount ? formatCurrency(invoice.taxAmount) : "---"}</p>
                </div>
                <div>
                  <p className="text-xs font-medium text-slate-500 uppercase mb-1">Currency</p>
                  <p className="text-sm font-semibold text-slate-900">{invoice.currency || "INR"}</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="py-4 border-b border-slate-100">
              <CardTitle className="text-sm text-slate-500 uppercase tracking-wider">Extracted Details</CardTitle>
            </CardHeader>
            <CardContent className="pt-6 space-y-5">
              
              <div className="flex items-center space-x-3">
                <div className="p-2 bg-slate-50 rounded-lg text-slate-400"><Building2 className="w-4 h-4" /></div>
                <div>
                  <p className="text-xs text-slate-500">Vendor Name</p>
                  <p className="text-sm font-semibold text-slate-900">{invoice.vendorName || "Unidentified Vendor"}</p>
                </div>
              </div>

              <div className="flex items-center space-x-3">
                <div className="p-2 bg-slate-50 rounded-lg text-slate-400"><Calendar className="w-4 h-4" /></div>
                <div>
                  <p className="text-xs text-slate-500">Invoice Date</p>
                  <p className="text-sm font-semibold text-slate-900">{invoice.invoiceDate || "---"}</p>
                </div>
              </div>

              <div className="flex items-center space-x-3">
                <div className="p-2 bg-slate-50 rounded-lg text-slate-400"><Activity className="w-4 h-4" /></div>
                <div>
                  <p className="text-xs text-slate-500">Assigned Category</p>
                  <p className="text-sm font-semibold text-slate-900">{invoice.category || "Uncategorized"}</p>
                </div>
              </div>

            </CardContent>
          </Card>

        </div>
      </div>
    </div>
  );
}