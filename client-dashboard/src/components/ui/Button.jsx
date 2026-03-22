import { forwardRef } from "react";
import { cn } from "../../lib/utils";
import { Loader2 } from "lucide-react"; // Loading spinner icon

export const Button = forwardRef(({ className, variant = "default", isLoading, children, ...props }, ref) => {
  
  // Define our FinTech design variants
  const variants = {
    default: "bg-avenra-600 text-white hover:bg-avenra-700 shadow-sm",
    outline: "border border-slate-300 bg-transparent hover:bg-slate-50 text-slate-700",
    ghost: "bg-transparent hover:bg-slate-100 text-slate-700",
  };

  return (
    <button
      ref={ref}
      disabled={isLoading}
      className={cn(
        "inline-flex items-center justify-center rounded-md text-sm font-medium transition-colors cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-avenra-500 disabled:pointer-events-none disabled:opacity-50 h-11 px-4 py-2",
        variants[variant],
        className
      )}
      {...props}
    >
      {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
      {children}
    </button>
  );
});
Button.displayName = "Button";