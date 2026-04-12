import { useState, useEffect } from "react";
import { motion as Motion, AnimatePresence } from "framer-motion";
import api from "../../api/axiosInterceptor";
import { Card, CardHeader, CardTitle, CardContent } from "../ui/Card";
import { Button } from "../ui/Button";
import { Input } from "../ui/Input";
import { X, Plus, Trash2, List, FileSpreadsheet, RefreshCw } from "lucide-react";

export default function LedgerModal({ isOpen, onClose, onUpdate, client }) {
  const [ledgers, setLedgers] = useState([]);
  const [newLedgerName, setNewLedgerName] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [bulkText, setBulkText] = useState("");
  const [activeTab, setActiveTab] = useState("list"); // 'list' | 'bulk'

  useEffect(() => {
    if (isOpen && client) {
      fetchLedgers();
    }
  }, [isOpen, client]);

  const fetchLedgers = async () => {
    setIsLoading(true);
    try {
      const res = await api.get(`/clients/${client.id}/ledgers`);
      setLedgers(res.data.ledgers || []);
    } catch (err) {
      console.error("Failed to fetch ledgers:", err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleAddSingle = async () => {
    if (!newLedgerName.trim()) return;
    try {
      await api.post(`/clients/${client.id}/ledgers`, { ledgerName: newLedgerName });
      setNewLedgerName("");
      fetchLedgers();
      if (onUpdate) onUpdate();
    } catch (err) {
      console.error("Add failed:", err);
      alert(err.response?.data?.error || "Failed to add ledger");
    }
  };

  const handleDelete = async (ledgerId) => {
    try {
      await api.delete(`/clients/${client.id}/ledgers/${ledgerId}`);
      fetchLedgers();
      if (onUpdate) onUpdate();
    } catch (err) {
      console.error("Delete failed:", err);
    }
  };

  const handleBulkReplace = async () => {
    if (!bulkText.trim()) return;
    const items = bulkText.split(/[\n,]/).map(s => s.trim()).filter(s => s.length > 0);
    try {
      await api.put(`/clients/${client.id}/ledgers`, { ledgerNames: items });
      setBulkText("");
      setActiveTab("list");
      fetchLedgers();
      if (onUpdate) onUpdate();
    } catch (err) {
      console.error("Bulk replace failed:", err);
      alert(err.response?.data?.error || "Failed to replace ledgers");
    }
  };

  if (!isOpen || !client) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm">
        <Motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.95 }}
          className="bg-white rounded-xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col"
        >
          <div className="flex items-center justify-between p-4 border-b border-slate-100 bg-slate-50/50">
            <div>
              <h2 className="text-lg font-bold text-slate-900">Chart of Accounts: {client.clientName}</h2>
              <p className="text-xs text-slate-500 mt-0.5">Define approved ledger categories for AI mapping.</p>
            </div>
            <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-colors">
              <X className="w-5 h-5" />
            </button>
          </div>

          <div className="flex border-b border-slate-200">
            <button
              onClick={() => setActiveTab("list")}
              className={`flex-1 py-3 text-sm font-medium flex items-center justify-center border-b-2 transition-colors ${
                activeTab === "list" ? "border-avenra-500 text-avenra-600" : "border-transparent text-slate-500 hover:text-slate-700"
              }`}
            >
              <List className="w-4 h-4 mr-2" /> Current Ledgers
            </button>
            <button
              onClick={() => setActiveTab("bulk")}
              className={`flex-1 py-3 text-sm font-medium flex items-center justify-center border-b-2 transition-colors ${
                activeTab === "bulk" ? "border-amber-500 text-amber-600" : "border-transparent text-slate-500 hover:text-slate-700"
              }`}
            >
              <FileSpreadsheet className="w-4 h-4 mr-2" /> Bulk Replace
            </button>
          </div>

          <div className="p-6 overflow-y-auto flex-1">
            {activeTab === "list" ? (
              <div className="space-y-4">
                <div className="flex gap-2">
                  <Input 
                    placeholder="E.g., Office Supplies, Software Subscriptions..." 
                    value={newLedgerName}
                    onChange={(e) => setNewLedgerName(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleAddSingle()}
                  />
                  <Button onClick={handleAddSingle} className="whitespace-nowrap">
                    <Plus className="w-4 h-4 mr-1" /> Add 
                  </Button>
                </div>

                {isLoading ? (
                  <div className="py-8 text-center text-slate-400">Loading ledgers...</div>
                ) : ledgers.length === 0 ? (
                  <div className="py-8 text-center text-slate-500 border-2 border-dashed border-slate-200 rounded-lg">
                    No ledgers mapped yet. The AI cannot map expenses without an allowed list.
                  </div>
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mt-4">
                    {ledgers.map(l => (
                      <div key={l.id} className="flex items-center justify-between p-3 bg-slate-50 border border-slate-100 rounded-lg group hover:border-slate-300 transition-colors">
                        <span className="text-sm font-medium text-slate-700">{l.ledgerName}</span>
                        <button 
                          onClick={() => handleDelete(l.id)}
                          className="text-slate-400 opacity-0 group-hover:opacity-100 hover:text-red-500 transition-all p-1 hover:bg-red-50 rounded"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ) : (
              <div className="space-y-4">
                <div className="bg-amber-50 border border-amber-200 text-amber-800 p-3 rounded-lg text-sm mb-2">
                  <strong className="font-semibold block mb-1">Warning</strong>
                  This will completely overwrite the existing Chart of Accounts for this client. 
                  Paste ledgers separated by commas or newlines.
                </div>
                <textarea
                  className="flex w-full rounded-md border border-slate-300 bg-transparent px-3 py-2 text-sm placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent min-h-[200px]"
                  placeholder="Office Supplies&#10;Software Subscriptions&#10;Travel Expenses&#10;Legal & Professional Fees"
                  value={bulkText}
                  onChange={(e) => setBulkText(e.target.value)}
                ></textarea>
                <Button onClick={handleBulkReplace} className="w-full bg-amber-500 hover:bg-amber-600 text-white">
                  <RefreshCw className="w-4 h-4 mr-2" /> Execute Bulk Replace
                </Button>
              </div>
            )}
          </div>
        </Motion.div>
      </div>
    </AnimatePresence>
  );
}
