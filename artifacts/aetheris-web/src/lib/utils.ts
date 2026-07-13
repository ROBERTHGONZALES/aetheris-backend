import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"
import { format } from 'date-fns';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatCurrency(amount: number, currency: string = 'USD') {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
  }).format(amount);
}

export function formatDate(dateStr: string, includeTime = false) {
  try {
    const d = new Date(dateStr);
    return format(d, includeTime ? 'MMM d, yyyy HH:mm' : 'MMM d, yyyy');
  } catch (e) {
    return dateStr;
  }
}
