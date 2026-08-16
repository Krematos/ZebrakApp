import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Toast, ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container" aria-live="polite" aria-atomic="true">
      <div
        *ngFor="let toast of toastService.toasts()"
        class="toast-item"
        [ngClass]="'toast-' + toast.type"
        role="alert"
      >
        <div class="toast-icon">
          <span *ngIf="toast.type === 'error'">❌</span>
          <span *ngIf="toast.type === 'success'">✅</span>
          <span *ngIf="toast.type === 'warning'">⚠️</span>
          <span *ngIf="toast.type === 'info'">ℹ️</span>
        </div>

        <div class="toast-content">
          <strong *ngIf="toast.title" class="toast-title">{{ toast.title }}</strong>
          <p class="toast-message">{{ toast.message }}</p>
        </div>

        <button
          type="button"
          class="toast-close-btn"
          (click)="toastService.remove(toast.id)"
          aria-label="Zavřít oznámení"
        >
          ✕
        </button>
      </div>
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      top: 1.25rem;
      right: 1.25rem;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      max-width: 420px;
      width: calc(100% - 2.5rem);
      pointer-events: none;
    }

    .toast-item {
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
      padding: 0.875rem 1rem;
      border-radius: var(--radius-md, 8px);
      box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.15), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
      background: #ffffff;
      border-left: 4px solid;
      pointer-events: auto;
      animation: slideIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
      transition: all 0.2s ease;
    }

    @keyframes slideIn {
      from {
        opacity: 0;
        transform: translateY(-12px) scale(0.95);
      }
      to {
        opacity: 1;
        transform: translateY(0) scale(1);
      }
    }

    .toast-error {
      border-left-color: #dc2626;
      background-color: #ffffff;
    }
    .toast-error .toast-title {
      color: #991b1b;
    }

    .toast-success {
      border-left-color: #16a34a;
      background-color: #ffffff;
    }
    .toast-success .toast-title {
      color: #166534;
    }

    .toast-warning {
      border-left-color: #d97706;
      background-color: #ffffff;
    }
    .toast-warning .toast-title {
      color: #92400e;
    }

    .toast-info {
      border-left-color: #2563eb;
      background-color: #ffffff;
    }
    .toast-info .toast-title {
      color: #1e40af;
    }

    .toast-icon {
      font-size: 1.125rem;
      flex-shrink: 0;
      margin-top: 1px;
    }

    .toast-content {
      flex: 1;
      min-width: 0;
    }

    .toast-title {
      display: block;
      font-size: 0.875rem;
      font-weight: 700;
      margin-bottom: 0.2rem;
    }

    .toast-message {
      font-size: 0.8125rem;
      color: #334155;
      line-height: 1.4;
      margin: 0;
      word-break: break-word;
    }

    .toast-close-btn {
      background: transparent;
      border: none;
      font-size: 0.875rem;
      color: #94a3b8;
      cursor: pointer;
      padding: 0.2rem;
      line-height: 1;
      border-radius: 4px;
      flex-shrink: 0;
      transition: color 0.15s ease;
    }

    .toast-close-btn:hover {
      color: #334155;
    }

    @media (max-width: 640px) {
      .toast-container {
        top: 0.75rem;
        left: 0.75rem;
        right: 0.75rem;
        width: calc(100% - 1.5rem);
      }
    }
  `],
})
export class ToastContainerComponent {
  readonly toastService = inject(ToastService);
}
