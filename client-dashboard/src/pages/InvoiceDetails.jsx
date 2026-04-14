import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { motion as Motion } from "framer-motion";
import api from "../api/axiosInterceptor";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/Card";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { ArrowLeft, Download, FileText, Building2, Calendar, DollarSign, Activity, Edit2, Save, X, CheckCircle2 } from "lucide-react";
import { Input } from "../components/ui/Input";

const staggerContainer = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.15 } }
};

const staggerItem = {
  hidden: { opacity: 0, x: 20 },
  show: { opacity: 1, x: 0, transition: { type: "spring", stiffness: 300, damping: 24 } }
};

export default function InvoiceDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  
  // CRITICAL FIX: These useState declarations were missing, causing a complete white screen crash
  const [invoice, setInvoice] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState({});
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    const fetchInvoiceDetails = async () => {
      try {
        setIsLoading(true);
        setError(null);
        const response = await api.get(`/invoices/${id}`);
        setInvoice(response.data);
        setEditForm(response.data);
      } catch (err) {
        console.error("[InvoiceDetails] Fetch failed:", err);
        setError("Failed to locate this invoice in the secure vault.");
      } finally {
        setIsLoading(false);
      }
    };
    fetchInvoiceDetails();
  }, [id]);

  const formatAmount = (amount, currency = 'INR') => {
    return new Intl.NumberFormat('en-IN', { 
      style: 'currency', 
      currency: currency || 'INR',
      minimumFractionDigits: 2
    }).format(amount || 0);
  };

  const handleSave = async () => {
    try {
      setIsSaving(true);
      const response = await api.put(`/invoices/${id}/correct`, editForm);
      setInvoice(response.data);
      setIsEditing(false);
    } catch (err) {
      alert("Failed to save corrections: " + (err.response?.data?.error || err.message));
    } finally {
      setIsSaving(false);
    }
  };

  const handleApprove = async () => {
    try {
      setIsSaving(true);
      const response = await api.post(`/invoices/${id}/approve`);
      setInvoice(response.data);
    } catch (err) {
      alert("Approval failed: " + (err.response?.data?.error || err.message));
    } finally {
      setIsSaving(false);
    }
  };

  const getStatusBadge = (status, failureReason) => {
    switch (status) {
      case "COMPLETED": return <Badge variant="success" className="bg-emerald-50 text-emerald-700 border-emerald-200"><CheckCircle2 className="w-3 h-3 mr-1"/> Verified</Badge>;
      case "PROCESSING": return <Badge variant="warning">AI Processing</Badge>;
      case "PENDING": return <Badge variant="default">Pending</Badge>;
      case "REQUIRES_MANUAL_REVIEW": return <Badge variant="destructive" className="animate-pulse">Action Required</Badge>;
      case "FAILED": return (
        <div className="flex flex-col items-start gap-1">
          <Badge variant="destructive">Process Failure</Badge>
          {failureReason && <span className="text-[10px] text-red-500 font-medium whitespace-nowrap">({failureReason})</span>}
        </div>
      );
      default: return <Badge variant="default">{status}</Badge>;
    }
  };

  if (isLoading) {
    return (
      <Motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex flex-col items-center justify-center h-[70vh] text-slate-500">
        <div className="w-8 h-8 border-4 border-avenra-500 border-t-transparent rounded-full animate-spin mb-4 shadow-[0_0_15px_rgba(48,91,163,0.3)]"></div>
        <p className="animate-pulse">Decrypting vault record...</p>
      </Motion.div>
    );
  }

  if (error || !invoice) {
    return (
      <Motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="text-center py-12">
        <p className="text-red-500 font-medium mb-4 bg-red-50 p-4 rounded-xl inline-block border border-red-100 shadow-sm">{error}</p>
        <br />
        <Button onClick={() => navigate("/dashboard")} variant="outline">Return to Dashboard</Button>
      </Motion.div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto space-y-6 pb-12">
      
      {/* THE CONTROL HEADER */}
      <Motion.div 
        initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }}
        className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white/80 backdrop-blur-md p-4 rounded-2xl border border-slate-200/60 shadow-[0_8px_30px_rgb(0,0,0,0.04)] sticky top-0 z-20"
      >
        <div className="flex items-center space-x-4">
          <Button variant="ghost" onClick={() => navigate(-1)} className="px-2 text-slate-500 hover:text-slate-900 hover:bg-slate-100 transition-colors">
            <ArrowLeft className="w-5 h-5 mr-2" /> Back
          </Button>
          <div className="h-6 w-px bg-slate-200"></div>
          <div>
            <h1 className="text-lg font-bold text-slate-900 flex items-center">
              Invoice Record <span className="text-slate-400 font-mono ml-2 text-sm bg-slate-100 px-2 py-0.5 rounded-md border border-slate-200">#{invoice.id?.substring(0,8)}</span>
            </h1>
          </div>
        </div>
        <div className="flex items-center space-x-3">
          {invoice.status === "REQUIRES_MANUAL_REVIEW" && !isEditing && (
            <Button onClick={handleApprove} disabled={isSaving} className="bg-emerald-600 hover:bg-emerald-700 text-white shadow-lg shadow-emerald-200/50">
              Approve Record
            </Button>
          )}
          
          {isEditing ? (
            <>
              <Button variant="outline" onClick={() => { setIsEditing(false); setEditForm(invoice); }} disabled={isSaving}>
                <X className="w-4 h-4 mr-2" /> Cancel
              </Button>
              <Button onClick={handleSave} disabled={isSaving} className="bg-avenra-600 hover:bg-avenra-700 text-white">
                <Save className="w-4 h-4 mr-2" /> {isSaving ? "Saving..." : "Save Corrections"}
              </Button>
            </>
          ) : (
            <Button variant="outline" onClick={() => setIsEditing(true)} className="border-slate-200 hover:bg-slate-50">
              <Edit2 className="w-4 h-4 mr-2 text-slate-500" /> Edit Metadata
            </Button>
          )}
          
          {invoice.s3FileUrl && (
            <Button variant="outline" onClick={() => window.open(invoice.s3FileUrl, '_blank')} className="shadow-sm hover:shadow-md transition-shadow">
              <Download className="w-4 h-4 mr-2" /> Raw PDF
            </Button>
          )}
          {getStatusBadge(invoice.status, invoice.failureReason)}
        </div>
      </Motion.div>

      {/* THE SPLIT PANE ENGINE */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 h-auto lg:h-[calc(100vh-12rem)] lg:min-h-[600px]">
        
        {/* LEFT PANE: Document Viewer */}
        <Motion.div initial={{ opacity: 0, scale: 0.98 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.5, delay: 0.1 }} className="lg:col-span-2 h-[500px] lg:h-full">
          <Card className="flex flex-col overflow-hidden h-full border-slate-200/60 shadow-[0_8px_30px_rgb(0,0,0,0.06)] hover:shadow-[0_8px_30px_rgb(0,0,0,0.1)] transition-shadow duration-500">
            <CardHeader className="bg-slate-50/80 backdrop-blur-sm border-b border-slate-200 py-3">
              <CardTitle className="text-sm flex items-center text-slate-600 font-medium">
                <FileText className="w-4 h-4 mr-2 text-avenra-500" /> Document Source
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0 flex-1 bg-slate-200/50 relative">
              {invoice.s3FileUrl ? (
                <iframe src={`${invoice.s3FileUrl}#toolbar=0`} className="absolute inset-0 w-full h-full border-0" title="Invoice PDF" />
              ) : (
                <div className="flex flex-col items-center justify-center h-full text-slate-400 bg-white">
                  <FileText className="w-16 h-16 mb-4 opacity-20" />
                  <p>No document source available for rendering.</p>
                </div>
              )}
            </CardContent>
          </Card>
        </Motion.div>

        {/* RIGHT PANE: Cognitive Data */}
        <Motion.div variants={staggerContainer} initial="hidden" animate="show" className="space-y-6 overflow-y-auto lg:pr-2 pb-4">
          
          <Motion.div variants={staggerItem}>
            <Card className="border-slate-200/60 shadow-sm hover:shadow-md transition-shadow">
              <CardHeader className="py-4 border-b border-slate-100 bg-slate-50/50">
                <CardTitle className="text-xs text-slate-500 uppercase tracking-wider font-semibold">Financial Summary</CardTitle>
              </CardHeader>
              <CardContent className="pt-6 space-y-6">
                <div className="flex items-start justify-between">
                  <div className="flex items-start space-x-4">
                    <div className="p-3 bg-avenra-50 rounded-xl text-avenra-600 shadow-inner border border-avenra-100"><DollarSign className="w-6 h-6" /></div>
                    <div>
                      <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1">Total Amount {isEditing && "(Original)"}</p>
                      {isEditing ? (
                        <div className="flex items-center space-x-2">
                          <select 
                            value={editForm.originalCurrency || 'INR'} 
                            onChange={(e) => setEditForm({...editForm, originalCurrency: e.target.value})}
                            className="bg-slate-50 border border-slate-200 rounded px-2 py-1 text-sm font-bold"
                          >
                            {['INR', 'USD', 'EUR', 'GBP', 'AED', 'SGD'].map(c => <option key={c} value={c}>{c}</option>)}
                          </select>
                          <Input 
                            type="number" 
                            value={editForm.totalAmount} 
                            onChange={(e) => setEditForm({...editForm, totalAmount: Number(e.target.value)})}
                            className="w-32 font-bold text-lg"
                          />
                        </div>
                      ) : (
                        <p className="text-4xl font-extrabold text-slate-900 tracking-tight">{formatAmount(invoice.totalAmount, invoice.originalCurrency)}</p>
                      )}
                    </div>
                  </div>
                </div>

                {invoice.originalCurrency && invoice.originalCurrency !== 'INR' && (
                  <div className="bg-slate-50 rounded-xl p-4 border border-slate-100 space-y-2">
                    <div className="flex justify-between text-xs font-semibold text-slate-400 uppercase tracking-wider">
                      <span>Historical INR Conversion (Invoice Date)</span>
                      <span>Rate: {invoice.exchangeRate}</span>
                    </div>
                    <p className="text-2xl font-bold text-emerald-600">{formatAmount(invoice.convertedAmountInr, 'INR')}</p>
                    {isEditing && (
                      <div className="pt-2 flex items-center space-x-2">
                        <span className="text-[10px] text-slate-400">Manual Rate:</span>
                        <Input 
                          type="number" 
                          value={editForm.exchangeRate} 
                          onChange={(e) => setEditForm({...editForm, exchangeRate: Number(e.target.value), convertedAmountInr: (editForm.totalAmount * Number(e.target.value))})}
                          className="h-6 w-20 text-[10px] px-1"
                        />
                      </div>
                    )}
                  </div>
                )}
                <div className="grid grid-cols-2 gap-4 pt-4 border-t border-slate-100">
                  <div className="bg-slate-50 rounded-lg p-3 border border-slate-100">
                    <p className="text-xs font-semibold text-slate-400 uppercase mb-1">Tax Amount</p>
                    <p className="text-base font-bold text-slate-800">
                      {formatAmount(
                        (Number(isEditing ? editForm.cgst : invoice.cgst) || 0) + 
                        (Number(isEditing ? editForm.sgst : invoice.sgst) || 0) + 
                        (Number(isEditing ? editForm.igst : invoice.igst) || 0),
                        isEditing ? editForm.originalCurrency : invoice.originalCurrency
                      )}
                    </p>
                  </div>
                  <div className="bg-slate-50 rounded-lg p-3 border border-slate-100">
                    <p className="text-xs font-semibold text-slate-400 uppercase mb-1">Tax Code</p>
                    <p className="text-base font-bold text-slate-800">{invoice.hsnSac || "GSTR-2A"}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </Motion.div>

          <Motion.div variants={staggerItem}>
            <Card className="border-slate-200/60 shadow-sm hover:shadow-md transition-shadow">
              <CardHeader className="py-4 border-b border-slate-100 bg-slate-50/50">
                <CardTitle className="text-xs text-slate-500 uppercase tracking-wider font-semibold">Extracted Metadata</CardTitle>
              </CardHeader>
              <CardContent className="pt-6 space-y-5">
                <div className="flex items-center space-x-4 p-3 rounded-xl hover:bg-slate-50 transition-colors border border-transparent hover:border-slate-100">
                  <div className="p-2.5 bg-white shadow-sm border border-slate-100 rounded-lg text-indigo-500"><Building2 className="w-5 h-5" /></div>
                  <div className="flex-1">
                    <p className="text-xs text-slate-500 font-medium">Supplier Name</p>
                    {isEditing ? (
                      <Input 
                        value={editForm.supplierName || ''} 
                        onChange={(e) => setEditForm({...editForm, supplierName: e.target.value})}
                        className="h-8 mt-1 text-sm font-bold"
                      />
                    ) : (
                      <p className="text-sm font-bold text-slate-900">{invoice.supplierName || "Unidentified Supplier"}</p>
                    )}
                  </div>
                </div>
                <div className="flex items-center space-x-4 p-3 rounded-xl hover:bg-slate-50 transition-colors border border-transparent hover:border-slate-100">
                  <div className="p-2.5 bg-white shadow-sm border border-slate-100 rounded-lg text-emerald-500"><Calendar className="w-5 h-5" /></div>
                  <div className="flex-1">
                    <p className="text-xs text-slate-500 font-medium">Invoice Date</p>
                    {isEditing ? (
                      <Input 
                        type="date"
                        value={editForm.invoiceDate || ''} 
                        onChange={(e) => setEditForm({...editForm, invoiceDate: e.target.value})}
                        className="h-8 mt-1 text-sm font-bold"
                      />
                    ) : (
                      <p className="text-sm font-bold text-slate-900">{invoice.invoiceDate || "---"}</p>
                    )}
                  </div>
                </div>
                <div className="flex items-center space-x-4 p-3 rounded-xl hover:bg-slate-50 transition-colors border border-transparent hover:border-slate-100">
                  <div className="p-2.5 bg-white shadow-sm border border-slate-100 rounded-lg text-amber-500"><Activity className="w-5 h-5" /></div>
                  <div className="flex-1">
                    <p className="text-xs text-slate-500 font-medium">Ledger Account Mapping</p>
                    {isEditing ? (
                      <Input 
                        value={editForm.ledgerAccountName || ''} 
                        onChange={(e) => setEditForm({...editForm, ledgerAccountName: e.target.value})}
                        placeholder="Type to map ledger..."
                        className="h-8 mt-1 text-sm font-bold"
                      />
                    ) : (
                      <p className="text-sm font-bold text-slate-900">{invoice.ledgerAccountName || "Unmapped Ledger"}</p>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>
          </Motion.div>

        </Motion.div>
      </div>
    </div>
  );
}
