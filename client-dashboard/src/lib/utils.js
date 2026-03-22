import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * Merges Tailwind classes safely. 
 * Essential for building reusable UI components (Buttons, Cards, Inputs).
 */
export function cn(...inputs) {
  return twMerge(clsx(inputs));
}