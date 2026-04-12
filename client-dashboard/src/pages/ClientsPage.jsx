import { useState, useEffect } from "react";
import { motion as Motion } from "framer-motion";
import api from "../api/axiosInterceptor";
import { Card, CardHeader, CardTitle, CardContent } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { Input } from "../components/ui/Input";
import { Badge } from "../components/ui/Badge";
import { Users, Plus, Building2, Calendar, AlertCircle, CheckCircle2, List } from "lucide-react";
import LedgerModal from "../components/shared/LedgerModal";

const GSTIN_REGEX = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/;

export default function ClientsPage() {
  const [clients, setClients] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({ clientName: "", clientGstin: "", financialYear: "" });
  const [formError, setFormError] = useState("");
  const [formSuccess, setFormSuccess] = useState("");
  const [gstinValid, setGstinValid] = useState(null);
  const [selectedClientForLedger, setSelectedClientForLedger] = useState(null);

  useEffect(() => {
    fetchClients();
  }, []);

  const fetchClients = async () => {
    try {
      const res = await api.get("/clients");
      setClients(res.data);
    } catch (err) {
      console.error("Failed to fetch clients:", err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleGstinChange = (value) => {
    const upper = value.toUpperCase().trim();
    setFormData(prev => ({ ...prev, clientGstin: upper }));
    if (upper.length === 15) {
      setGstinValid(GSTIN_REGEX.test(upper));
    } else {
      setGstinValid(null);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError("");
    setFormSuccess("");

    if (!formData.clientName.trim()) { setFormError("Client name is required."); return; }
    if (!GSTIN_REGEX.test(formData.clientGstin)) { setFormError("Invalid GSTIN format."); return; }
    if (!/^\d{4}-\d{2}$/.test(formData.financialYear)) { setFormError("Financial year must be YYYY-YY format."); return; }

    try {
      const res = await api.post("/clients", formData);
      setFormSuccess(`Client created: ${res.data.clientName} (${res.data.clientGstin})`);
      setFormData({ clientName: "", clientGstin: "", financialYear: "" });
      setGstinValid(null);
      setShowForm(false);
      fetchClients();
    } catch (err) {
      setFormError(err.response?.data?.error || "Failed to create client.");
    }
  };

  return (
    <div className="max-w-6xl mx-auto space-y-8">
      <Motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Client Management</h1>
          <p className="text-slate-500 mt-1 text-sm">Manage your CA firm's clients and their GSTIN records.</p>
        </div>
        <Button onClick={() => setShowForm(!showForm)}>
          <Plus className="w-4 h-4 mr-2" /> Add Client
        </Button>
      </Motion.div>

      {/* Create Client Form */}
      {showForm && (
        <Motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }}>
          <Card className="border-avenra-200 shadow-md">
            <CardHeader className="bg-avenra-50/30 border-b border-avenra-100">
              <CardTitle className="flex items-center text-lg"><Building2 className="w-5 h-5 mr-2 text-avenra-500"/>New Client</CardTitle>
            </CardHeader>
            <CardContent className="pt-6">
              <form onSubmit={handleSubmit} className="grid md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1.5">Client Name</label>
                  <Input placeholder="e.g., Reliance Industries" value={formData.clientName}
                    onChange={(e) => setFormData(prev => ({ ...prev, clientName: e.target.value }))} />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1.5">
                    GSTIN
                    {gstinValid === true && <CheckCircle2 className="w-4 h-4 text-emerald-500 inline ml-1.5" />}
                    {gstinValid === false && <AlertCircle className="w-4 h-4 text-red-500 inline ml-1.5" />}
                  </label>
                  <Input placeholder="e.g., 27AAPFU0939F1ZV" maxLength={15} value={formData.clientGstin}
                    onChange={(e) => handleGstinChange(e.target.value)}
                    className={gstinValid === false ? "border-red-300 focus:ring-red-500" : gstinValid === true ? "border-emerald-300 focus:ring-emerald-500" : ""} />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1.5">Financial Year</label>
                  <Input placeholder="e.g., 2025-26" maxLength={7} value={formData.financialYear}
                    onChange={(e) => setFormData(prev => ({ ...prev, financialYear: e.target.value }))} />
                </div>
                <div className="md:col-span-3 flex items-center gap-3">
                  <Button type="submit">Create Client</Button>
                  <Button type="button" variant="outline" onClick={() => { setShowForm(false); setFormError(""); }}>Cancel</Button>
                </div>
              </form>
              {formError && (
                <div className="mt-3 flex items-center text-red-600 text-sm">
                  <AlertCircle className="w-4 h-4 mr-1.5" />{formError}
                </div>
              )}
              {formSuccess && (
                <div className="mt-3 flex items-center text-emerald-600 text-sm">
                  <CheckCircle2 className="w-4 h-4 mr-1.5" />{formSuccess}
                </div>
              )}
            </CardContent>
          </Card>
        </Motion.div>
      )}

      {/* Client List */}
      <Motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
        <Card className="overflow-hidden border-slate-200 shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="text-xs text-slate-500 uppercase bg-slate-50/80 border-b border-slate-200">
                <tr>
                  <th className="px-6 py-4 font-semibold">Client Name</th>
                  <th className="px-6 py-4 font-semibold">GSTIN</th>
                  <th className="px-6 py-4 font-semibold">Financial Year</th>
                  <th className="px-6 py-4 font-semibold text-center">Ledger Status</th>
                  <th className="px-6 py-4 font-semibold">Created</th>
                  <th className="px-6 py-4 font-semibold text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr>
                    <td colSpan="6" className="px-6 py-12 text-center text-slate-500">
                      <div className="flex justify-center mb-2">
                        <div className="w-6 h-6 border-2 border-avenra-500 border-t-transparent rounded-full animate-spin"></div>
                      </div>
                      Loading clients...
                    </td>
                  </tr>
                ) : clients.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="px-6 py-12 text-center text-slate-500">
                      <Users className="w-10 h-10 mx-auto text-slate-300 mb-3" />
                      No clients yet. Add your first client to get started.
                    </td>
                  </tr>
                ) : (
                  clients.map((client, i) => (
                    <Motion.tr key={client.id} initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: i * 0.05 }}
                      className="bg-white hover:bg-slate-50 transition-colors">
                      <td className="px-6 py-4 font-medium text-slate-900">{client.clientName}</td>
                      <td className="px-6 py-4"><Badge variant="default" className="font-mono text-xs">{client.clientGstin}</Badge></td>
                      <td className="px-6 py-4 text-slate-600"><Calendar className="w-3.5 h-3.5 inline mr-1.5 text-slate-400"/>{client.financialYear}</td>
                      <td className="px-6 py-4 text-center">
                         {client.clientLedgers?.length > 0 ? (
                           <Badge variant="success" className="bg-emerald-50 text-emerald-700 border-emerald-200">
                             {client.clientLedgers.length} Ledgers
                           </Badge>
                         ) : (
                           <Badge variant="warning" className="bg-amber-50 text-amber-700 border-amber-200">
                             Unassigned
                           </Badge>
                         )}
                       </td>
                      <td className="px-6 py-4 text-slate-500 text-xs">{new Date(client.createdAt).toLocaleDateString()}</td>
                      <td className="px-6 py-4 text-right">
                        <Button variant="outline" size="sm" onClick={() => setSelectedClientForLedger(client)}>
                          <List className="w-3 h-3 mr-1" /> Ledgers
                        </Button>
                      </td>
                    </Motion.tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </Card>
      </Motion.div>

      <LedgerModal 
        isOpen={!!selectedClientForLedger} 
        onClose={() => setSelectedClientForLedger(null)} 
        onUpdate={fetchClients}
        client={selectedClientForLedger} 
      />
    </div>
  );
}
