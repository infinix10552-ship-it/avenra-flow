import { useState } from "react";
import { Input } from "../ui/Input";
import { Select } from "../ui/Select";
import { Button } from "../ui/Button";
import { Search, Filter, X } from "lucide-react";

export default function FilterBar({ onFilterChange }) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [filters, setFilters] = useState({
    vendorName: "",
    category: "",
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
    const reset = { vendorName: "", category: "", status: "", startDate: "", endDate: "" };
    setFilters(reset);
    onFilterChange(reset);
  };

  const activeFilterCount = Object.values(filters).filter(val => val !== "").length;

  return (
    <div className="bg-slate-50 border-b border-slate-200 p-4 flex flex-col gap-4">
      {/* Top Row: Global Search & Toggle */}
      <div className="flex flex-col sm:flex-row gap-4 justify-between items-center">
        <div className="relative w-full sm:max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <Input 
            placeholder="Search vendor names..." 
            className="pl-9 bg-white"
            value={filters.vendorName}
            onChange={(e) => handleChange("vendorName", e.target.value)}
          />
        </div>
        
        <div className="flex items-center space-x-2 w-full sm:w-auto">
          {activeFilterCount > 1 && (
            <Button variant="ghost" onClick={clearFilters} className="text-slate-500 h-10 px-3">
              <X className="w-4 h-4 mr-2" /> Clear
            </Button>
          )}
          <Button 
            variant={isExpanded ? "default" : "outline"} 
            onClick={() => setIsExpanded(!isExpanded)}
            className="w-full sm:w-auto bg-white"
          >
            <Filter className="w-4 h-4 mr-2" /> 
            Filters {activeFilterCount > 0 && `(${activeFilterCount})`}
          </Button>
        </div>
      </div>

      {/* Expanded Row: Deep Search Parameters */}
      {isExpanded && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 pt-4 border-t border-slate-200 animate-in slide-in-from-top-2 fade-in duration-200">
          
          <div>
            <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Category</label>
            <Select value={filters.category} onChange={(e) => handleChange("category", e.target.value)}>
              <option value="">All Categories</option>
              <option value="SOFTWARE">Software & SaaS</option>
              <option value="HARDWARE">Hardware & IT</option>
              <option value="TRAVEL">Travel & Logistics</option>
              <option value="OFFICE_SUPPLIES">Office Supplies</option>
              <option value="SERVICES">Professional Services</option>
            </Select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Processing Status</label>
            <Select value={filters.status} onChange={(e) => handleChange("status", e.target.value)}>
              <option value="">All Statuses</option>
              <option value="COMPLETED">Completed</option>
              <option value="PENDING">Pending</option>
              <option value="PROCESSING">Processing</option>
              <option value="FAILED">Failed</option>
            </Select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Start Date</label>
            <Input 
              type="date" 
              value={filters.startDate} 
              onChange={(e) => handleChange("startDate", e.target.value)} 
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">End Date</label>
            <Input 
              type="date" 
              value={filters.endDate} 
              onChange={(e) => handleChange("endDate", e.target.value)} 
            />
          </div>

        </div>
      )}
    </div>
  );
}
