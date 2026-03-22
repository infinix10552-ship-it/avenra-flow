import { useState, useRef } from "react";
import api from "../api/axiosInterceptor";
import { Card, CardHeader, CardTitle, CardContent } from "../components/ui/Card";
import { UploadCloud, FileArchive, CheckCircle2, AlertCircle, Loader2, FileText } from "lucide-react";

// --- RENDER HELPERS ---
const StatusMessage = ({ status }) => {
  if (status.type === "idle") return null;
  if (status.type === "loading") return (
    <div className="flex items-center text-avenra-500 text-sm font-medium mt-4">
      <Loader2 className="w-4 h-4 mr-2 animate-spin" /> {status.message}
    </div>
  );
  if (status.type === "success") return (
    <div className="flex flex-col mt-4">
      <div className="flex items-center text-emerald-600 text-sm font-medium">
        <CheckCircle2 className="w-4 h-4 mr-2" /> {status.message}
      </div>
      {/* Render Bulk Report Card if it exists */}
      {status.report && (
        <div className="mt-3 bg-slate-50 border border-slate-200 rounded p-3 text-xs grid grid-cols-3 gap-2 text-center">
           <div className="text-emerald-600 font-bold">{status.report.successful} <br/><span className="text-slate-500 font-normal">Success</span></div>
           <div className="text-amber-600 font-bold">{status.report.duplicates_skipped} <br/><span className="text-slate-500 font-normal">Duplicates</span></div>
           <div className="text-red-600 font-bold">{status.report.failed} <br/><span className="text-slate-500 font-normal">Failed</span></div>
        </div>
      )}
    </div>
  );
  return (
    <div className="flex items-center text-red-600 text-sm font-medium mt-4">
      <AlertCircle className="w-4 h-4 mr-2" /> {status.message}
    </div>
  );
};

export default function UploadHub() {
  // State for Single Upload
  const [singleDragActive, setSingleDragActive] = useState(false);
  const [singleStatus, setSingleStatus] = useState({ type: "idle", message: "" });
  
  // State for Bulk Upload
  const [bulkDragActive, setBulkDragActive] = useState(false);
  const [bulkStatus, setBulkStatus] = useState({ type: "idle", message: "", report: null });

  // Refs to trigger hidden file inputs
  const singleInputRef = useRef(null);
  const bulkInputRef = useRef(null);

  // --- THE UPLOAD ENGINE ---
  // Notice we don't pass the Org ID or JWT here. The axiosInterceptor handles it!
  const processUpload = async (file, isBulk = false) => {
    const setStatus = isBulk ? setBulkStatus : setSingleStatus;
    const endpoint = isBulk ? "/invoices/upload/bulk" : "/invoices/upload";
    
    if (!file) return;

    // Validate file types before wasting bandwidth
    if (!isBulk && file.type !== "application/pdf") {
      setStatus({ type: "error", message: "Please upload a valid PDF file." });
      return;
    }
    if (isBulk && !file.name.endsWith('.zip')) {
      setStatus({ type: "error", message: "Please upload a valid ZIP file." });
      return;
    }

    setStatus({ type: "loading", message: `Encrypting and transmitting ${file.name}...` });

    // Pack the file for the HTTP request
    const formData = new FormData();
    formData.append("file", file);

    try {
      const response = await api.post(endpoint, formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });

      if (isBulk) {
        setStatus({ 
          type: "success", 
          message: "Bulk batch processed successfully.",
          report: response.data.report 
        });
      } else {
        setStatus({ type: "success", message: `Invoice securely vaulted. ID: ${response.data.invoiceId.substring(0,8)}...` });
      }
    } catch (error) {
      setStatus({ 
        type: "error", 
        message: error.response?.data?.error || "A secure transmission error occurred." 
      });
    }
  };

  // --- DRAG AND DROP HANDLERS ---
  const handleDrag = (e, setDragActive) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
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
    <div className="max-w-5xl mx-auto space-y-6">
      
      <div>
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Data Ingestion Hub</h1>
        <p className="text-slate-500 mt-1">Upload financial documents for cognitive extraction and indexing.</p>
      </div>

      <div className="grid md:grid-cols-2 gap-6">
        
        {/* === SINGLE UPLOAD ZONE === */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center"><FileText className="w-5 h-5 mr-2 text-avenra-500"/> Single Invoice</CardTitle>
          </CardHeader>
          <CardContent>
            <div 
              className={`relative border-2 border-dashed rounded-xl p-8 text-center transition-colors ${
                singleDragActive ? "border-avenra-500 bg-avenra-50" : "border-slate-300 hover:bg-slate-50 bg-white"
              } ${singleStatus.type === "loading" ? "opacity-50 pointer-events-none" : "cursor-pointer"}`}
              onDragEnter={(e) => handleDrag(e, setSingleDragActive)}
              onDragLeave={(e) => handleDrag(e, setSingleDragActive)}
              onDragOver={(e) => handleDrag(e, setSingleDragActive)}
              onDrop={(e) => handleDrop(e, setSingleDragActive, false)}
              onClick={() => singleInputRef.current?.click()}
            >
              <input 
                ref={singleInputRef}
                type="file" 
                accept="application/pdf" 
                className="hidden" 
                onChange={(e) => processUpload(e.target.files[0], false)}
              />
              <UploadCloud className={`mx-auto h-10 w-10 mb-3 ${singleDragActive ? "text-avenra-500" : "text-slate-400"}`} />
              <p className="text-sm font-medium text-slate-700">Click to upload or drag & drop</p>
              <p className="text-xs text-slate-500 mt-1">Strictly PDF files up to 10MB</p>
            </div>
            <StatusMessage status={singleStatus} />
          </CardContent>
        </Card>

        {/* === BULK UPLOAD ZONE === */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center"><FileArchive className="w-5 h-5 mr-2 text-indigo-500"/> Bulk Batch (ZIP)</CardTitle>
          </CardHeader>
          <CardContent>
            <div 
              className={`relative border-2 border-dashed rounded-xl p-8 text-center transition-colors ${
                bulkDragActive ? "border-indigo-500 bg-indigo-50" : "border-slate-300 hover:bg-slate-50 bg-white"
              } ${bulkStatus.type === "loading" ? "opacity-50 pointer-events-none" : "cursor-pointer"}`}
              onDragEnter={(e) => handleDrag(e, setBulkDragActive)}
              onDragLeave={(e) => handleDrag(e, setBulkDragActive)}
              onDragOver={(e) => handleDrag(e, setBulkDragActive)}
              onDrop={(e) => handleDrop(e, setBulkDragActive, true)}
              onClick={() => bulkInputRef.current?.click()}
            >
              <input 
                ref={bulkInputRef}
                type="file" 
                accept=".zip" 
                className="hidden" 
                onChange={(e) => processUpload(e.target.files[0], true)}
              />
              <FileArchive className={`mx-auto h-10 w-10 mb-3 ${bulkDragActive ? "text-indigo-500" : "text-slate-400"}`} />
              <p className="text-sm font-medium text-slate-700">Click to upload or drag & drop</p>
              <p className="text-xs text-slate-500 mt-1">ZIP archive containing multiple PDFs</p>
            </div>
            <StatusMessage status={bulkStatus} />
          </CardContent>
        </Card>

      </div>
    </div>
  );
}