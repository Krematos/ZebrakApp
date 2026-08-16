import { Injectable, signal } from '@angular/core';

export type ToastType = 'error' | 'success' | 'warning' | 'info';

export interface Toast {
  id: string;
  type: ToastType;
  message: string;
  title?: string;
  duration?: number;
}

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  readonly toasts = signal<Toast[]>([]);

  show(type: ToastType, message: string, title?: string, duration = 5000): void {
    const id = `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
    const toast: Toast = { id, type, message, title, duration };

    this.toasts.update((current) => [...current, toast]);

    if (duration > 0) {
      setTimeout(() => {
        this.remove(id);
      }, duration);
    }
  }

  error(message: string, title = 'Chyba', duration = 6000): void {
    this.show('error', message, title, duration);
  }

  success(message: string, title = 'Úspěch', duration = 4000): void {
    this.show('success', message, title, duration);
  }

  warning(message: string, title = 'Upozornění', duration = 5000): void {
    this.show('warning', message, title, duration);
  }

  info(message: string, title = 'Informace', duration = 4000): void {
    this.show('info', message, title, duration);
  }

  remove(id: string): void {
    this.toasts.update((current) => current.filter((t) => t.id !== id));
  }

  clear(): void {
    this.toasts.set([]);
  }
}
