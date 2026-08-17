import { Component, EventEmitter, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ApiService } from '../../core/services/api.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-profile-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="modal-backdrop" (click)="onBackdropClick($event)">
      <div class="modal-card">
        <!-- Header -->
        <div class="modal-header">
          <h2 class="modal-title">Uživatelský profil</h2>
          <button class="close-btn" (click)="close.emit()" title="Zavřít">✕</button>
        </div>

        <div class="modal-body" *ngIf="authService.currentUser() as user">
          <!-- User info card -->
          <div class="user-summary">
            <div class="user-avatar-lg">
              {{ user.nickname.charAt(0).toUpperCase() }}
            </div>
            <div class="user-details">
              <h3 class="user-nickname">{{ user.nickname }}</h3>
              <p class="user-email">✉️ {{ user.email }}</p>
              <span class="user-role-badge" [class.admin]="authService.isAdmin()">
                {{ authService.isAdmin() ? 'Administrátor' : 'Uživatel' }}
              </span>
            </div>
          </div>

          <!-- Quick Navigation Links -->
          <div class="quick-links">
            <a routerLink="/my-places" (click)="close.emit()" class="quick-link-item">
              <span>📍 Moje vložená místa</span>
              <span class="link-arrow">→</span>
            </a>
          </div>

          <!-- Danger Zone -->
          <div class="danger-zone">
            <div class="danger-header">
              <span class="danger-icon">⚠️</span>
              <h4>Nebezpečná zóna</h4>
            </div>
            <p class="danger-desc">
              Smazáním účtu dojde k jeho okamžité deaktivaci a odhlášení. Vaše osobní údaje budou po uplynutí 30denní lhůty trvale vymazány. Vaše vložená místa zůstanou v komunitní mapě zachována anonymně.
            </p>

            <ng-container *ngIf="!showConfirm()">
              <button class="btn btn-outline-danger delete-init-btn" (click)="showConfirm.set(true)">
                Smazat můj uživatelský účet
              </button>
            </ng-container>

            <!-- Password Confirmation Form -->
            <div *ngIf="showConfirm()" class="delete-confirm-box">
              <p class="confirm-prompt">Pro potvrzení smazání zadejte své současné heslo:</p>

              <div class="input-group">
                <input
                  type="password"
                  class="input-field"
                  placeholder="Vaše stávající heslo"
                  [(ngModel)]="password"
                  [disabled]="isDeleting()"
                  (keyup.enter)="handleDeleteAccount()"
                  autofocus
                />
              </div>

              <div *ngIf="errorMessage()" class="field-error">
                {{ errorMessage() }}
              </div>

              <div class="confirm-actions">
                <button
                  class="btn btn-secondary"
                  (click)="showConfirm.set(false); errorMessage.set(''); password = ''"
                  [disabled]="isDeleting()"
                >
                  Zrušit
                </button>
                <button
                  class="btn btn-danger"
                  (click)="handleDeleteAccount()"
                  [disabled]="isDeleting() || !password.trim()"
                >
                  <span *ngIf="isDeleting()" class="spinner-sm"></span>
                  {{ isDeleting() ? 'Mažu účet...' : 'Trvale potvrdit smazání' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .modal-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1.25rem 1.5rem;
      border-bottom: 1px solid var(--border-color);
    }
    .modal-title {
      font-size: 1.15rem;
      font-weight: 800;
      color: var(--text-main);
      margin: 0;
    }
    .close-btn {
      background: transparent;
      border: none;
      font-size: 1.25rem;
      color: var(--text-muted);
      cursor: pointer;
      padding: 0.25rem;
      line-height: 1;
    }
    .close-btn:hover {
      color: var(--text-main);
    }
    .modal-body {
      padding: 1.5rem;
    }
    .user-summary {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding-bottom: 1.25rem;
      border-bottom: 1px solid var(--border-color);
      margin-bottom: 1.25rem;
    }
    .user-avatar-lg {
      width: 56px;
      height: 56px;
      border-radius: 50%;
      background: linear-gradient(135deg, #2563eb, #1d4ed8);
      color: #ffffff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.5rem;
      font-weight: 800;
      flex-shrink: 0;
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
    }
    .user-details {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }
    .user-nickname {
      font-size: 1.15rem;
      font-weight: 700;
      color: var(--text-main);
      margin: 0;
    }
    .user-email {
      font-size: 0.85rem;
      color: var(--text-muted);
      margin: 0;
    }
    .user-role-badge {
      display: inline-block;
      align-self: flex-start;
      font-size: 0.7rem;
      font-weight: 700;
      padding: 0.2rem 0.6rem;
      border-radius: var(--radius-full);
      background: #e2e8f0;
      color: #475569;
      margin-top: 0.25rem;
    }
    .user-role-badge.admin {
      background: #fef3c7;
      color: #b45309;
    }
    .quick-links {
      margin-bottom: 1.5rem;
    }
    .quick-link-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0.75rem 1rem;
      background: var(--bg-app);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      text-decoration: none;
      color: var(--text-main);
      font-weight: 600;
      font-size: 0.875rem;
      transition: all var(--transition-fast);
    }
    .quick-link-item:hover {
      background: #e2e8f0;
      border-color: #cbd5e1;
    }
    .link-arrow {
      color: var(--text-muted);
      font-size: 1.1rem;
    }
    .danger-zone {
      border: 1px solid #fecaca;
      background: #fff5f5;
      border-radius: var(--radius-md);
      padding: 1.25rem;
    }
    .danger-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 0.5rem;
    }
    .danger-header h4 {
      font-size: 0.95rem;
      font-weight: 700;
      color: #991b1b;
      margin: 0;
    }
    .danger-icon {
      font-size: 1.1rem;
    }
    .danger-desc {
      font-size: 0.8125rem;
      color: #7f1d1d;
      line-height: 1.45;
      margin-bottom: 1rem;
    }
    .delete-init-btn {
      width: 100%;
      padding: 0.65rem;
      font-size: 0.875rem;
      font-weight: 700;
    }
    .btn-outline-danger {
      background: #ffffff;
      color: #dc2626;
      border: 1px solid #f87171;
      border-radius: var(--radius-md);
      cursor: pointer;
      transition: all var(--transition-fast);
    }
    .btn-outline-danger:hover {
      background: #fee2e2;
      border-color: #dc2626;
    }
    .delete-confirm-box {
      background: #ffffff;
      border: 1px solid #fca5a5;
      border-radius: var(--radius-md);
      padding: 1rem;
      margin-top: 0.75rem;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }
    .confirm-prompt {
      font-size: 0.8125rem;
      font-weight: 600;
      color: #991b1b;
      margin: 0;
    }
    .field-error {
      color: #dc2626;
      font-size: 0.8rem;
      font-weight: 600;
    }
    .confirm-actions {
      display: flex;
      justify-content: flex-end;
      gap: 0.5rem;
      margin-top: 0.25rem;
    }
    .btn-danger {
      background: #dc2626;
      color: #ffffff;
      border: none;
      padding: 0.5rem 1rem;
      font-size: 0.875rem;
      font-weight: 700;
      border-radius: var(--radius-md);
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      transition: background var(--transition-fast);
    }
    .btn-danger:hover:not(:disabled) {
      background: #b91c1c;
    }
    .btn-danger:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    .spinner-sm {
      width: 14px;
      height: 14px;
      border: 2px solid #ffffff;
      border-top-color: transparent;
      border-radius: 50%;
      animation: spin 0.6s linear infinite;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `],
})
export class ProfileModalComponent {
  readonly authService = inject(AuthService);
  private apiService = inject(ApiService);
  private toastService = inject(ToastService);
  private router = inject(Router);

  @Output() close = new EventEmitter<void>();

  password = '';
  readonly showConfirm = signal(false);
  readonly isDeleting = signal(false);
  readonly errorMessage = signal('');

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.close.emit();
    }
  }

  handleDeleteAccount(): void {
    if (!this.password.trim()) {
      this.errorMessage.set('Zadejte své současné heslo.');
      return;
    }

    this.isDeleting.set(true);
    this.errorMessage.set('');

    this.apiService.deleteMyAccount(this.password).subscribe({
      next: () => {
        this.isDeleting.set(false);
        this.close.emit();
        this.authService.logout();
        this.router.navigate(['/']);
        this.toastService.success(
          'Váš účet byl úspěšně deaktivován. Do 30 dnů bude kompletně vymazán.',
          'Účet smazán'
        );
      },
      error: (err) => {
        this.isDeleting.set(false);
        if (err.status === 400) {
          this.errorMessage.set(err.error?.message || 'Zadané heslo není správné.');
        } else if (err.status === 401) {
          this.errorMessage.set('Relace vypršela. Přihlaste se prosím znovu.');
        } else {
          this.errorMessage.set('Nepodařilo se smazat účet. Zkontrolujte připojení.');
        }
      },
    });
  }
}
