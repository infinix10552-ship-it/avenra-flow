import { useState, useRef, useEffect } from "react";
import { motion as Motion } from "framer-motion";
import api from "../api/axiosInterceptor";
import { Card, CardHeader, CardTitle, CardContent } from "../components/ui/Card";
import { UploadCloud, FileArchive, CheckCircle2, AlertCircle, Loader2, FileText, Users } from "lucide-react";

const StatusMessage = ({ status }) => {
  if (status.type === "idle") return null;
  if (status.type === "loading") return (
    <Motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex items-center text-avenra-500 text-sm font-medium mt-4">
      <Loader2 className="w-4 h-4 mr-2 animate-spin" /> {status.message}
    </Motion.div>
  );
  if (status.type === "success") return (
    <Motion.div initial={{ opacity: 0, y: 5 }} animate={{ opacity: 1, y: 0 }} className="flex flex-col mt-4">
      <div className="flex items-center text-emerald-600 text-sm font-medium">
        <CheckCircle2 className="w-4 h-4 mr-2" /> {status.message}
      </div>
      {status.report && (
        <div className="mt-3 bg-emerald-50/50 border border-emerald-100 rounded-lg p-4 text-xs grid grid-cols-3 gap-2 text-center shadow-sm">
           <div className="text-emerald-700 font-bold text-lg">{status.report.successful} <br/><span className="text-emerald-600 font-medium text-xs uppercase tracking-wider">Success</span></div>
           <div className="text-amber-600 font-bold text-lg">{status.report.duplicates_skipped} <br/><span className="text-amber-600/80 font-medium text-xs uppercase tracking-wider">Duplicates</span></div>
           <div className="text-red-600 font-bold text-lg">{status.report.failed} <br/><span className="text-red-600/80 font-medium text-xs uppercase tracking-wider">Failed</span></div>
        </div>
      )}
    </Motion.div>
  );
  return (
    <Motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex items-center text-red-600 text-sm font-medium mt-4 bg-red-50 p-3 rounded-lg border border-red-100">
      <AlertCircle className="w-4 h-4 mr-2" /> {status.message}
    </Motion.div>
  );
};

export default function UploadHub() {
  const [singleDragActive, setSingleDragActive] = useState(false);
  const [singleStatus, setSingleStatus] = useState({ type: "idle", message: "" });
  const [bulkDragActive, setBulkDragActive] = useState(false);
  const [bulkStatus, setBulkStatus] = useState({ type: "idle", message: "", report: null });
  const [clients, setClients] = useState([]);
  const [selectedClientId, setSelectedClientId] = useState("");

  const singleInputRef = useRef(null);
  const bulkInputRef = useRef(null);

  useEffect(() => {
    const fetchClients = async () => {
      try {
        const res = await api.get("/clients");
        setClients(res.data);
      } catch (err) { console.error("Failed to fetch clients:", err); }
    };
    fetchClients();
  }, []);

  const processUpload = async (file, isBulk = false) => {
    const setStatus = isBulk ? setBulkStatus : setSingleStatus;
    let endpoint = isBulk ? "/invoices/upload/bulk" : "/invoices/upload";
    
    if (!file) return;

    if (!isBulk && file.type !== "application/pdf") {
      setStatus({ type: "error", message: "Please upload a valid PDF file." });
      return;
    }
    if (isBulk && !file.name.endsWith('.zip')) {
      setStatus({ type: "error", message: "Please upload a valid ZIP file." });
      return;
    }

    // Append clientId if selected
    if (selectedClientId) {
      endpoint += `?clientId=${selectedClientId}`;
    }

    setStatus({ type: "loading", message: `Encrypting and transmitting ${file.name}...` });

    const formData = new FormData();
    formData.append("file", file);

    try {
      const response = await api.post(endpoint, formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });

      if (isBulk) {
        setStatus({ type: "success", message: "Bulk batch processed successfully.", report: response.data.report });
      } else {
        setStatus({ type: "success", message: `Invoice securely vaulted. ID: ${response.data.invoiceId.substring(0,8)}...` });
      }
    } catch (error) {
      setStatus({ type: "error", message: error.response?.data?.error || "A secure transmission error occurred." });
    }
  };

  const handleDrag = (e, setDragActive) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") setDragActive(true);
    else if (e.type === "dragleave") setDragActive(false);
  };

  const handleDrop = (e, setDragActive, isBulk) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      processUpload(e.dataTransfer.files[0], isBulk);
    }
  };

  return (
    <div className="max-w-5xl mx-auto space-y-8">
      <Motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }}>
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Data Ingestion Hub</h1>
        <p className="text-slate-500 mt-1">Upload financial documents for cognitive extraction and indexing.</p>
      </Motion.div>

      {/* Client Selector */}
      {clients.length > 0 && (
        <Motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex items-center gap-3 p-4 bg-slate-50 rounded-lg border border-slate-200">
          <Users className="w-5 h-5 text-avenra-500" />
          <label className="text-sm font-medium text-slate-700">Assign to Client:</label>
          <select
            value={selectedClientId}
            onChange={(e) => setSelectedClientId(e.target.value)}
            className="border border-slate-300 rounded-lg px-3 py-1.5 text-sm bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-avenra-500 focus:border-avenra-500 cursor-pointer"
          >
            <option value="">None (Unassigned)</option>
            {clients.map((c) => (
              <option key={c.id} value={c.id}>{c.clientName} — {c.clientGstin}</option>
            ))}
          </select>
        </Motion.div>
      )}

      <div className="grid md:grid-cols-2 gap-8">
        
        {/* SINGLE UPLOAD ZONE */}
        <Motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.1 }}>
          <Card className="h-full border-slate-200/60 shadow-sm hover:shadow-md transition-shadow">
            <CardHeader className="bg-slate-50/50 border-b border-slate-100">
              <CardTitle className="flex items-center text-lg"><FileText className="w-5 h-5 mr-2 text-avenra-500"/> Single Invoice</CardTitle>
            </CardHeader>
            <CardContent className="pt-6">
              <Motion.div 
                whileHover={{ scale: singleStatus.type === "loading" ? 1 : 1.01 }}
                whileTap={{ scale: singleStatus.type === "loading" ? 1 : 0.98 }}
                className={`relative border-2 border-dashed rounded-2xl p-10 text-center transition-all duration-300 ${
                  singleDragActive ? "border-avenra-500 bg-avenra-50/50 shadow-[inset_0_0_20px_rgba(48,91,163,0.05)]" : "border-slate-300 hover:border-avenra-300 hover:bg-slate-50/50 bg-white"
                } ${singleStatus.type === "loading" ? "opacity-50 pointer-events-none" : "cursor-pointer"}`}
                onDragEnter={(e) => handleDrag(e, setSingleDragActive)}
                onDragLeave={(e) => handleDrag(e, setSingleDragActive)}
                onDragOver={(e) => handleDrag(e, setSingleDragActive)}
                onDrop={(e) => handleDrop(e, setSingleDragActive, false)}
                onClick={() => singleInputRef.current?.click()}
              >
                <input ref={singleInputRef} type="file" accept="application/pdf" className="hidden" onChange={(e) => processUpload(e.target.files[0], false)}/>
                <UploadCloud className={`mx-auto h-12 w-12 mb-4 transition-colors duration-300 ${singleDragActive ? "text-avenra-500" : "text-slate-400"}`} />
                <p className="text-base font-semibold text-slate-700">Click to upload or drag & drop</p>
                <p className="text-sm text-slate-500 mt-2">Strictly PDF files up to 10MB</p>
              </Motion.div>
              <StatusMessage status={singleStatus} />
            </CardContent>
          </Card>
        </Motion.div>

        {/* BULK UPLOAD ZONE */}
        <Motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5, delay: 0.2 }}>
          <Card className="h-full border-slate-200/60 shadow-sm hover:shadow-md transition-shadow">
            <CardHeader className="bg-slate-50/50 border-b border-slate-100">
              <CardTitle className="flex items-center text-lg"><FileArchive className="w-5 h-5 mr-2 text-indigo-500"/> Bulk Batch (ZIP)</CardTitle>
            </CardHeader>
            <CardContent className="pt-6">
              <Motion.div 
                whileHover={{ scale: bulkStatus.type === "loading" ? 1 : 1.01 }}
                whileTap={{ scale: bulkStatus.type === "loading" ? 1 : 0.98 }}
                className={`relative border-2 border-dashed rounded-2xl p-10 text-center transition-all duration-300 ${
                  bulkDragActive ? "border-indigo-500 bg-indigo-50/50 shadow-[inset_0_0_20px_rgba(99,102,241,0.05)]" : "border-slate-300 hover:border-indigo-300 hover:bg-slate-50/50 bg-white"
                } ${bulkStatus.type === "loading" ? "opacity-50 pointer-events-none" : "cursor-pointer"}`}
                onDragEnter={(e) => handleDrag(e, setBulkDragActive)}
                onDragLeave={(e) => handleDrag(e, setBulkDragActive)}
                onDragOver={(e) => handleDrag(e, setBulkDragActive)}
                onDrop={(e) => handleDrop(e, setBulkDragActive, true)}
                onClick={() => bulkInputRef.current?.click()}
              >
                <input ref={bulkInputRef} type="file" accept=".zip" className="hidden" onChange={(e) => processUpload(e.target.files[0], true)}/>
                <FileArchive className={`mx-auto h-12 w-12 mb-4 transition-colors duration-300 ${bulkDragActive ? "text-indigo-500" : "text-slate-400"}`} />
                <p className="text-base font-semibold text-slate-700">Click to upload or drag & drop</p>
                <p className="text-sm text-slate-500 mt-2">ZIP archive containing multiple PDFs</p>
              </Motion.div>
              <StatusMessage status={bulkStatus} />
            </CardContent>
          </Card>
        </Motion.div>

      </div>
    </div>
  );
}
