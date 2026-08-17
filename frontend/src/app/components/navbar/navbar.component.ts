import { Component, EventEmitter, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ProfileModalComponent } from '../profile-modal/profile-modal.component';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, ProfileModalComponent],
  template: `
    <header class="navbar">
      <div class="nav-container">
        <!-- Logo & Title -->
        <a routerLink="/" class="logo-area">
          <div class="logo-badge">
            <span class="logo-icon">📍</span>
          </div>
          <div class="brand-text">
            <span class="brand-name">ŽEBRÁK</span>
            <span class="brand-tagline">Mapa levných nákupů</span>
          </div>
        </a>

        <!-- Actions -->
        <div class="nav-actions">
          <button class="btn btn-primary add-btn" (click)="onAddPlaceClick()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            <span>Přidat levné místo</span>
          </button>

          <!-- Admin badge/link if Admin -->
          <a *ngIf="authService.isAdmin()" routerLink="/admin" class="admin-link">
            <span class="admin-badge">Admin panel</span>
          </a>

          <!-- User Logged in -->
          <ng-container *ngIf="authService.currentUser() as user; else guestTpl">
            <div class="user-menu">
              <button class="user-pill-btn" (click)="showProfileModal.set(true)" title="Můj profil a správa účtu">
                <span class="user-avatar">{{ user.nickname.charAt(0).toUpperCase() }}</span>
                <span class="user-name">{{ user.nickname }}</span>
              </button>
              <button class="btn btn-secondary logout-btn" (click)="authService.logout()" title="Odhlásit se">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                  <polyline points="16 17 21 12 16 7"></polyline>
                  <line x1="21" y1="12" x2="9" y2="12"></line>
                </svg>
              </button>
            </div>
          </ng-container>

          <!-- Guest Tpl -->
          <ng-template #guestTpl>
            <button class="btn btn-secondary login-btn" (click)="openAuthModal.emit()">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
              <span>Přihlásit se</span>
            </button>
          </ng-template>
        </div>
      </div>
    </header>

    <!-- Profile & Account Management Modal -->
    <app-profile-modal
      *ngIf="showProfileModal()"
      (close)="showProfileModal.set(false)"
    ></app-profile-modal>
  `,
  styles: [`
    .navbar {
      height: 68px;
      background: #ffffff;
      border-bottom: 1px solid var(--border-color);
      display: flex;
      align-items: center;
      position: sticky;
      top: 0;
      z-index: 1000;
      box-shadow: var(--shadow-sm);
    }
    .nav-container {
      width: 100%;
      max-width: 1600px;
      margin: 0 auto;
      padding: 0 1.25rem;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .logo-area {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      text-decoration: none;
      color: inherit;
    }
    .logo-badge {
      width: 42px;
      height: 42px;
      border-radius: var(--radius-md);
      background: linear-gradient(135deg, #2563eb, #1d4ed8);
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4px 10px rgba(37, 99, 235, 0.25);
    }
    .logo-icon {
      font-size: 1.25rem;
    }
    .brand-text {
      display: flex;
      flex-direction: column;
    }
    .brand-name {
      font-size: 1.25rem;
      font-weight: 800;
      letter-spacing: -0.03em;
      color: #0f172a;
      line-height: 1.1;
    }
    .brand-tagline {
      font-size: 0.75rem;
      color: var(--text-muted);
      font-weight: 500;
    }
    .nav-actions {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .admin-link {
      text-decoration: none;
    }
    .admin-badge {
      background: #fef3c7;
      color: #b45309;
      font-weight: 700;
      font-size: 0.75rem;
      padding: 0.4rem 0.75rem;
      border-radius: var(--radius-full);
      border: 1px solid #fde68a;
      transition: all var(--transition-fast);
    }
    .admin-badge:hover {
      background: #fde68a;
    }
    .user-menu {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }
    .user-pill, .user-pill-btn {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.35rem 0.75rem 0.35rem 0.35rem;
      background: var(--bg-app);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-full);
      text-decoration: none;
      color: var(--text-main);
      font-size: 0.875rem;
      font-weight: 600;
      cursor: pointer;
      font-family: inherit;
      transition: all var(--transition-fast);
    }
    .user-pill:hover, .user-pill-btn:hover {
      background: #e2e8f0;
      border-color: #cbd5e1;
    }
    .user-avatar {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: var(--primary-600);
      color: #ffffff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.8rem;
      font-weight: 700;
    }
    .logout-btn {
      padding: 0.5rem;
      border-radius: 50%;
    }
    @media (max-width: 640px) {
      .brand-tagline {
        display: none;
      }
      .add-btn span {
        display: none;
      }
      .add-btn {
        padding: 0.6rem;
      }
      .user-name {
        display: none;
      }
    }
  `],
})
export class NavbarComponent {
  readonly authService = inject(AuthService);

  readonly showProfileModal = signal(false);

  @Output() openAuthModal = new EventEmitter<void>();
  @Output() openAddPlaceModal = new EventEmitter<void>();

  onAddPlaceClick(): void {
    if (!this.authService.isAuthenticated()) {
      this.openAuthModal.emit();
    } else {
      this.openAddPlaceModal.emit();
    }
  }
}
