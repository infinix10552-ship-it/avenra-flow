import { useState, useEffect, useCallback } from "react";
import { motion as Motion } from "framer-motion";
import api from "../api/axiosInterceptor";
import { Card, CardHeader, CardTitle, CardContent } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { Input } from "../components/ui/Input";
import { Badge } from "../components/ui/Badge";
import { ClipboardCheck, AlertTriangle, CheckCircle2, XCircle, Eye, Edit3, Save } from "lucide-react";

export default function ReviewQueue() {
  const [invoices, setInvoices] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedInvoice, setSelectedInvoice] = useState(null);
  const [auditLogs, setAuditLogs] = useState([]);
  const [clientLedgers, setClientLedgers] = useState([]);
  const [editMode, setEditMode] = useState(false);
  const [editData, setEditData] = useState({});
  const [actionStatus, setActionStatus] = useState({ type: "idle", message: "" });

  const fetchReviewQueue = useCallback(async () => {
    try {
      const res = await api.get("/invoices/search", { params: { status: "REQUIRES_MANUAL_REVIEW" } });
      setInvoices(res.data);
    } catch (err) {
      console.error("Failed to fetch review queue:", err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => { fetchReviewQueue(); }, [fetchReviewQueue]);

  const handleApprove = async (invoiceId) => {
    setActionStatus({ type: "loading", message: "Approving..." });
    try {
      await api.post(`/invoices/${invoiceId}/approve`);
      setActionStatus({ type: "success", message: "Invoice approved." });
      setSelectedInvoice(null);
      fetchReviewQueue();
    } catch (err) {
      setActionStatus({ type: "error", message: err.response?.data?.error || "Approval failed." });
    }
  };

  const handleCorrectAndApprove = async (invoiceId) => {
    setActionStatus({ type: "loading", message: "Saving corrections..." });
    try {
      await api.put(`/invoices/${invoiceId}/correct`, editData);
      setActionStatus({ type: "success", message: "Invoice corrected and approved." });
      setSelectedInvoice(null);
      setEditMode(false);
      fetchReviewQueue();
    } catch (err) {
      setActionStatus({ type: "error", message: err.response?.data?.error || "Correction failed." });
    }
  };

  const openReview = async (invoice) => {
    setSelectedInvoice(invoice);
    setEditMode(false);
    setEditData({
      invoiceNumber: invoice.invoiceNumber || "",
      invoiceDate: invoice.invoiceDate || "",
      supplierName: invoice.supplierName || "",
      supplierGstin: invoice.supplierGstin || "",
      buyerGstin: invoice.buyerGstin || "",
      hsnSac: invoice.hsnSacCode || "",
      ledgerAccountName: invoice.ledgerAccountName || "",
      baseAmount: invoice.baseTaxableAmount || 0,
      cgst: invoice.cgst || 0,
      sgst: invoice.sgst || 0,
      igst: invoice.igst || 0,
      totalAmount: invoice.totalAmount || 0,
    });
    setActionStatus({ type: "idle", message: "" });
    setAuditLogs([]);
    setClientLedgers([]);

    try {
      const [auditRes, ledgerRes] = await Promise.all([
        api.get(`/invoices/${invoice.id}/audit-log`),
        invoice.clientId ? api.get(`/clients/${invoice.clientId}/ledgers`) : Promise.resolve({ data: { ledgers: [] } })
      ]);
      setAuditLogs(auditRes.data.logs || []);
      setClientLedgers(ledgerRes.data.ledgers || []);
    } catch (err) {
      console.error("Failed to load invoice details:", err);
    }
  };

  const getConfidenceBadge = (score) => {
    if (score == null) return <Badge variant="default">N/A</Badge>;
    if (score >= 85) return <Badge variant="success">{score}%</Badge>;
    if (score >= 50) return <Badge variant="warning">{score}%</Badge>;
    return <Badge variant="error">{score}%</Badge>;
  };

  const formatCurrency = (amount) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount || 0);

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      <Motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }}>
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight flex items-center">
          <ClipboardCheck className="w-6 h-6 mr-2 text-amber-500" /> Review Queue
        </h1>
        <p className="text-slate-500 mt-1 text-sm">Invoices flagged for manual review due to low confidence or validation failures.</p>
      </Motion.div>

      {/* Action Status */}
      {actionStatus.type !== "idle" && (
        <Motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}
          className={`p-3 rounded-lg text-sm font-medium flex items-center ${
            actionStatus.type === "success" ? "bg-emerald-50 text-emerald-700 border border-emerald-200" :
            actionStatus.type === "error" ? "bg-red-50 text-red-700 border border-red-200" :
            "bg-blue-50 text-blue-700 border border-blue-200"
          }`}>
          {actionStatus.type === "success" && <CheckCircle2 className="w-4 h-4 mr-2" />}
          {actionStatus.type === "error" && <XCircle className="w-4 h-4 mr-2" />}
          {actionStatus.message}
        </Motion.div>
      )}

      <div className="grid lg:grid-cols-5 gap-6">
        {/* LEFT: Invoice List */}
        <div className="lg:col-span-2">
          <Card className="overflow-hidden border-slate-200 shadow-sm">
            <CardHeader className="bg-amber-50/50 border-b border-amber-100">
              <CardTitle className="text-sm flex items-center">
                <AlertTriangle className="w-4 h-4 mr-2 text-amber-500" />
                Pending Review ({invoices.length})
              </CardTitle>
            </CardHeader>
            <div className="divide-y divide-slate-100 max-h-[600px] overflow-y-auto">
              {isLoading ? (
                <div className="p-8 text-center text-slate-500">
                  <div className="w-6 h-6 border-2 border-amber-500 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
                  Loading...
                </div>
              ) : invoices.length === 0 ? (
                <div className="p-8 text-center text-slate-500">
                  <CheckCircle2 className="w-10 h-10 mx-auto text-emerald-300 mb-3" />
                  All clear! No invoices need review.
                </div>
              ) : (
                invoices.map((inv) => (
                  <button key={inv.id} onClick={() => openReview(inv)}
                    className={`w-full text-left p-4 hover:bg-amber-50/50 transition-colors cursor-pointer ${
                      selectedInvoice?.id === inv.id ? "bg-amber-50 border-l-4 border-amber-500" : ""
                    }`}>
                    <div className="flex items-center justify-between mb-1">
                      <span className="font-medium text-slate-900 text-sm truncate">{inv.supplierName || inv.originalFileName}</span>
                      {getConfidenceBadge(inv.aiConfidenceScore)}
                    </div>
                    <div className="flex items-center justify-between text-xs text-slate-500">
                      <span>{inv.invoiceNumber || "No number"}</span>
                      <span>{inv.invoiceDate || "No date"}</span>
                    </div>
                    {inv.totalAmount && (
                      <div className="text-sm font-semibold text-slate-800 mt-1">{formatCurrency(inv.totalAmount)}</div>
                    )}
                  </button>
                ))
              )}
            </div>
          </Card>
        </div>

        {/* RIGHT: Review Panel */}
        <div className="lg:col-span-3">
          {selectedInvoice ? (
            <Motion.div key={selectedInvoice.id} initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }}>
              <Card className="border-slate-200 shadow-sm">
                <CardHeader className="bg-slate-50/50 border-b border-slate-100 flex flex-row items-center justify-between">
                  <CardTitle className="text-lg">Invoice Review</CardTitle>
                  <div className="flex gap-2">
                    <Button variant="outline" onClick={() => setEditMode(!editMode)}>
                      <Edit3 className="w-4 h-4 mr-1" /> {editMode ? "Cancel Edit" : "Edit"}
                    </Button>
                    {editMode ? (
                      <Button onClick={() => handleCorrectAndApprove(selectedInvoice.id)}>
                        <Save className="w-4 h-4 mr-1" /> Save & Approve
                      </Button>
                    ) : (
                      <Button onClick={() => handleApprove(selectedInvoice.id)}>
                        <CheckCircle2 className="w-4 h-4 mr-1" /> Approve
                      </Button>
                    )}
                  </div>
                </CardHeader>
                <CardContent className="pt-6 space-y-4">
                  {/* PDF Link */}
                  {selectedInvoice.s3FileUrl && (
                    <a href={selectedInvoice.s3FileUrl} target="_blank" rel="noopener noreferrer"
                      className="inline-flex items-center text-sm text-avenra-600 hover:text-avenra-700 font-medium">
                      <Eye className="w-4 h-4 mr-1" /> View Original PDF
                    </a>
                  )}

                  {/* Confidence Banner */}
                  <div className={`p-3 rounded-lg text-sm font-medium ${
                    (selectedInvoice.aiConfidenceScore || 0) < 85 || selectedInvoice.failureReason
                      ? "bg-amber-50 text-amber-800 border border-amber-200"
                      : "bg-emerald-50 text-emerald-800 border border-emerald-200"
                  }`}>
                    <div className="flex items-center mb-1">
                      <strong className="mr-2">AI Confidence: {selectedInvoice.aiConfidenceScore || "N/A"}%</strong>
                      {(selectedInvoice.aiConfidenceScore || 0) < 85 && " — Below 85% threshold."}
                    </div>
                    {selectedInvoice.failureReason && (
                      <div className="flex items-center text-red-700 font-semibold text-xs mt-2 p-2 bg-red-50 rounded border border-red-100">
                        <AlertTriangle className="w-4 h-4 mr-1" />
                        Failure Reason: {selectedInvoice.failureReason}
                      </div>
                    )}
                  </div>

                  {/* Data Fields */}
                  <div className="grid md:grid-cols-2 gap-4">
                    {[
                      { label: "Invoice Number", key: "invoiceNumber" },
                      { label: "Invoice Date", key: "invoiceDate" },
                      { label: "Supplier Name", key: "supplierName" },
                      { label: "Supplier GSTIN", key: "supplierGstin", highlight: true },
                      { label: "Buyer GSTIN", key: "buyerGstin", highlight: true },
                      { label: "HSN/SAC Code", key: "hsnSac" },
                      { label: "Ledger Account", key: "ledgerAccountName" },
                    ].map(field => (
                      <div key={field.key}>
                        <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">{field.label}</label>
                        {editMode ? (
                          field.key === "ledgerAccountName" && clientLedgers.length > 0 ? (
                            <select
                              className="flex h-10 w-full rounded-md border border-slate-300 bg-transparent px-3 py-2 text-sm placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-slate-400 focus:border-transparent disabled:cursor-not-allowed disabled:opacity-50"
                              value={editData[field.key] || ""}
                              onChange={(e) => setEditData(prev => ({ ...prev, [field.key]: e.target.value }))}
                            >
                              <option value="">-- Select Ledger --</option>
                              {clientLedgers.map(l => (
                                <option key={l.id} value={l.ledgerName}>{l.ledgerName}</option>
                              ))}
                            </select>
                          ) : (
                            <Input value={editData[field.key] || ""} onChange={(e) => setEditData(prev => ({ ...prev, [field.key]: e.target.value }))} />
                          )
                        ) : (
                          <p className={`text-sm font-medium p-2 rounded ${
                            field.highlight && selectedInvoice[field.key === "hsnSac" ? "hsnSacCode" : field.key] &&
                            !/^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/.test(selectedInvoice[field.key === "hsnSac" ? "hsnSacCode" : field.key])
                              ? "bg-red-50 text-red-800 border border-red-200" : "bg-slate-50 text-slate-900"
                          }`}>
                            {selectedInvoice[field.key === "hsnSac" ? "hsnSacCode" : field.key] || "—"}
                          </p>
                        )}
                      </div>
                    ))}
                  </div>

                  {/* Financial Fields */}
                  <div className="border-t border-slate-200 pt-4">
                    <h3 className="text-sm font-semibold text-slate-700 mb-3">Financial Breakdown</h3>
                    <div className="grid grid-cols-5 gap-3">
                      {[
                        { label: "Base Amount", key: "baseAmount", entityKey: "baseTaxableAmount" },
                        { label: "CGST", key: "cgst", entityKey: "cgst" },
                        { label: "SGST", key: "sgst", entityKey: "sgst" },
                        { label: "IGST", key: "igst", entityKey: "igst" },
                        { label: "Total", key: "totalAmount", entityKey: "totalAmount" },
                      ].map(field => {
                        const value = selectedInvoice[field.entityKey];
                        // Check tax math mismatch
                        const base = Number(selectedInvoice.baseTaxableAmount) || 0;
                        const c = Number(selectedInvoice.cgst) || 0;
                        const s = Number(selectedInvoice.sgst) || 0;
                        const i = Number(selectedInvoice.igst) || 0;
                        const t = Number(selectedInvoice.totalAmount) || 0;
                        const mathMismatch = field.key === "totalAmount" && Math.abs((base + c + s + i) - t) > 0.01;

                        return (
                          <div key={field.key}>
                            <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">{field.label}</label>
                            {editMode ? (
                              <Input type="number" step="0.01" value={editData[field.key] || 0}
                                onChange={(e) => setEditData(prev => ({ ...prev, [field.key]: parseFloat(e.target.value) || 0 }))} />
                            ) : (
                              <p className={`text-sm font-semibold p-2 rounded ${
                                mathMismatch ? "bg-red-100 text-red-800 border border-red-300" : "bg-slate-50 text-slate-900"
                              }`}>
                                {formatCurrency(value)}
                              </p>
                            )}
                          </div>
                        );
                      })}
                    </div>
                    {/* Math check message */}
                    {(() => {
                      const base = Number(selectedInvoice.baseTaxableAmount) || 0;
                      const c = Number(selectedInvoice.cgst) || 0;
                      const s = Number(selectedInvoice.sgst) || 0;
                      const ig = Number(selectedInvoice.igst) || 0;
                      const t = Number(selectedInvoice.totalAmount) || 0;
                      const diff = (base + c + s + ig) - t;
                      if (Math.abs(diff) > 0.01) {
                        return (
                          <div className="mt-3 p-2 bg-red-50 text-red-700 text-xs rounded border border-red-200 flex items-center">
                            <AlertTriangle className="w-4 h-4 mr-1.5 text-red-500" />
                            Tax math mismatch: Base + CGST + SGST + IGST ≠ Total. Difference: {formatCurrency(Math.abs(diff))}
                          </div>
                        );
                      }
                      return null;
                    })()}
                  </div>

                  {/* Audit History Timeline */}
                  {auditLogs.length > 0 && (
                    <div className="border-t border-slate-200 pt-4">
                      <h3 className="text-sm font-semibold text-slate-700 mb-3 flex items-center">
                        <ClipboardCheck className="w-4 h-4 mr-2" /> Audit Trail (Change History)
                      </h3>
                      <div className="space-y-3">
                        {auditLogs.map((log) => (
                          <div key={log.id} className="relative pl-4 border-l-2 border-slate-200">
                            <div className="absolute -left-1.5 top-1.5 w-3 h-3 rounded-full bg-avenra-500 border-2 border-white"></div>
                            <div className="text-xs text-slate-500 mb-0.5">
                              {new Date(log.timestamp).toLocaleString()}
                            </div>
                            <div className="text-sm">
                              Changed <span className="font-semibold">{log.fieldName}</span> from{" "}
                              <span className="line-through text-red-500 bg-red-50 px-1 rounded mx-1">{log.oldValue || "empty"}</span>{" "}
                              to <span className="text-emerald-600 bg-emerald-50 px-1 rounded mx-1 font-medium">{log.newValue || "empty"}</span>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>
            </Motion.div>
          ) : (
            <Card className="border-slate-200 shadow-sm h-full flex items-center justify-center min-h-[400px]">
              <div className="text-center text-slate-400">
                <ClipboardCheck className="w-12 h-12 mx-auto mb-3 text-slate-300" />
                <p className="font-medium">Select an invoice to review</p>
                <p className="text-sm mt-1">Click an invoice from the list to see details</p>
              </div>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
