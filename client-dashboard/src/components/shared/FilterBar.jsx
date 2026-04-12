import { useState } from "react";
import { motion as Motion, AnimatePresence } from "framer-motion";
import { Input } from "../ui/Input";
import { Select } from "../ui/Select";
import { Button } from "../ui/Button";
import { Search, Filter, X } from "lucide-react";

export default function FilterBar({ onFilterChange }) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [filters, setFilters] = useState({
    supplierName: "",
    ledgerAccountName: "",
    status: "",
    startDate: "",
    endDate: "",
  });

  // When a user types or selects, we update local state AND tell the Dashboard
  const handleChange = (key, value) => {
    const newFilters = { ...filters, [key]: value };
    setFilters(newFilters);
    onFilterChange(newFilters); // Fire the API call
  };

  const clearFilters = () => {
    const reset = { supplierName: "", ledgerAccountName: "", status: "", startDate: "", endDate: "" };
    setFilters(reset);
    onFilterChange(reset);
  };

  const activeFilterCount = Object.values(filters).filter(val => val !== "").length;

  return (
    <div className="bg-slate-50 border-b border-slate-200 p-4 flex flex-col overflow-hidden">
      {/* Top Row: Global Search & Toggle */}
      <div className="flex flex-col sm:flex-row gap-4 justify-between items-center relative z-10">
        <div className="relative w-full sm:max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <Input 
            placeholder="Search supplier names..." 
            className="pl-9 bg-white"
            value={filters.supplierName}
            onChange={(e) => handleChange("supplierName", e.target.value)}
          />
        </div>
        
        <div className="flex items-center space-x-2 w-full sm:w-auto">
          <AnimatePresence>
            {activeFilterCount > 1 && (
              <Motion.div
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.9 }}
                transition={{ duration: 0.2 }}
              >
                <Button variant="ghost" onClick={clearFilters} className="text-slate-500 h-10 px-3">
                  <X className="w-4 h-4 mr-2" /> Clear
                </Button>
              </Motion.div>
            )}
          </AnimatePresence>
          <Button 
            variant={isExpanded ? "default" : "outline"} 
            onClick={() => setIsExpanded(!isExpanded)}
            className="w-full sm:w-auto bg-white transition-all duration-300"
          >
            <Motion.span
              animate={{ rotate: isExpanded ? 180 : 0 }}
              transition={{ type: "spring", stiffness: 300, damping: 20 }}
              className="mr-2"
            >
              <Filter className="w-4 h-4" />
            </Motion.span>
            Filters {activeFilterCount > 0 && `(${activeFilterCount})`}
          </Button>
        </div>
      </div>

      {/* Expanded Row: Deep Search Parameters */}
      <AnimatePresence>
        {isExpanded && (
          <Motion.div 
            initial={{ height: 0, opacity: 0, marginTop: 0 }}
            animate={{ height: "auto", opacity: 1, marginTop: 16 }}
            exit={{ height: 0, opacity: 0, marginTop: 0 }}
            transition={{ type: "spring", stiffness: 200, damping: 25 }}
            className="overflow-hidden"
          >
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 pt-4 border-t border-slate-200">
              <Motion.div 
                initial={{ y: -10, opacity: 0 }}
                animate={{ y: 0, opacity: 1 }}
                transition={{ delay: 0.1 }}
              >
                <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1 px-1">Ledger Account</label>
                <Input 
                  placeholder="e.g. Software Expense" 
                  value={filters.ledgerAccountName}
                  onChange={(e) => handleChange("ledgerAccountName", e.target.value)}
                />
              </Motion.div>

              <Motion.div 
                initial={{ y: -10, opacity: 0 }}
                animate={{ y: 0, opacity: 1 }}
                transition={{ delay: 0.15 }}
              >
                <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1 px-1">Processing Status</label>
                <Select value={filters.status} onChange={(e) => handleChange("status", e.target.value)}>
                  <option value="">All Statuses</option>
                  <option value="COMPLETED">Completed</option>
                  <option value="PENDING">Pending</option>
                  <option value="PROCESSING">Processing</option>
                  <option value="FAILED">Failed</option>
                </Select>
              </Motion.div>

              <Motion.div 
                initial={{ y: -10, opacity: 0 }}
                animate={{ y: 0, opacity: 1 }}
                transition={{ delay: 0.2 }}
              >
                <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1 px-1">Start Date</label>
                <Input 
                  type="date" 
                  value={filters.startDate} 
                  onChange={(e) => handleChange("startDate", e.target.value)} 
                />
              </Motion.div>

              <Motion.div 
                initial={{ y: -10, opacity: 0 }}
                animate={{ y: 0, opacity: 1 }}
                transition={{ delay: 0.25 }}
              >
                <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1 px-1">End Date</label>
                <Input 
                  type="date" 
                  value={filters.endDate} 
                  onChange={(e) => handleChange("endDate", e.target.value)} 
                />
              </Motion.div>
            </div>
          </Motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
