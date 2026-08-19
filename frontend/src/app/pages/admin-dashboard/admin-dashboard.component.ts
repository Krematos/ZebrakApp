import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { Place } from '../../core/models/place.model';
import { User } from '../../core/models/auth.model';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="admin-page">
      <!-- Admin Top Header -->
      <header class="admin-header">
        <div class="admin-header-container">
          <div class="admin-brand">
            <a routerLink="/" class="back-link">← Zpět na mapu</a>
            <h1>Administrátorský panel ŽEBRÁK</h1>
          </div>

          <div class="admin-user-info">
            <span>Přihlášen jako: <strong>{{ authService.currentUser()?.nickname }}</strong> (ADMIN)</span>
          </div>
        </div>
      </header>

      <main class="admin-main">
        <!-- Tabs -->
        <div class="admin-tabs">
          <button
            class="tab-btn"
            [class.active]="activeTab() === 'PENDING'"
            (click)="switchTab('PENDING')"
          >
            ⏳ Čekající na schválení ({{ pendingTotal() }})
          </button>
          <button
            class="tab-btn"
            [class.active]="activeTab() === 'APPROVED'"
            (click)="switchTab('APPROVED')"
          >
            ✅ Schválená místa na mapě
          </button>
          <button
            class="tab-btn"
            [class.active]="activeTab() === 'REJECTED'"
            (click)="switchTab('REJECTED')"
          >
            ❌ Zamítnutá místa
          </button>
          <button
            class="tab-btn"
            [class.active]="activeTab() === 'USERS'"
            (click)="switchTab('USERS')"
          >
            👥 Správa uživatelů
          </button>
        </div>

        <!-- Notification Message -->
        <div *ngIf="statusMessage()" class="alert-success">
          {{ statusMessage() }}
        </div>

        <!-- Error Banner -->
        <div *ngIf="hasError()" class="error-banner">
          <div class="error-banner-content">
            <span class="error-banner-icon">⚠️</span>
            <div class="error-banner-text">
              <strong>Nepodařilo se načíst administrátorská data</strong>
              <span>Zkontrolujte připojení k serveru a zkuste to znovu.</span>
            </div>
          </div>
          <button class="btn btn-sm btn-retry-sm" (click)="loadData()">
            🔄 Zkusit znovu
          </button>
        </div>

        <!-- Loading State -->
        <div *ngIf="isLoading()" class="loading-state">
          <div class="spinner"></div>
          <p>Načítám záznamy...</p>
        </div>

        <!-- USERS View -->
        <div *ngIf="!isLoading() && activeTab() === 'USERS'" class="users-section">
          <div *ngIf="users().length === 0 && !hasError()" class="empty-state">
            <p>Žádní registrovaní uživatelé.</p>
          </div>

          <div *ngIf="users().length > 0" class="users-list">
            <div *ngFor="let u of users()" class="user-row-card">
              <div class="user-main-info">
                <div class="user-avatar-sm">{{ u.nickname.charAt(0).toUpperCase() }}</div>
                <div class="user-text">
                  <span class="u-name">{{ u.nickname }}</span>
                  <span class="u-email">{{ u.email }}</span>
                </div>
                <span class="badge" [class.badge-admin]="u.role === 'ROLE_ADMIN'" [class.badge-user]="u.role === 'ROLE_USER'">
                  {{ u.role === 'ROLE_ADMIN' ? 'Administrátor' : 'Uživatel' }}
                </span>
                <span class="u-date">Registrován: {{ u.createdAt | date:'d. M. yyyy' }}</span>
              </div>

              <div class="user-actions">
                <button
                  *ngIf="u.id !== authService.currentUser()?.id"
                  class="btn btn-sm btn-danger"
                  (click)="deleteUser(u)"
                  title="Smazat uživatele (Soft Delete)"
                >
                  🗑️ Smazat účet
                </button>
                <span *ngIf="u.id === authService.currentUser()?.id" class="current-user-badge">
                  (Váš účet)
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Places Queue / Place List -->
        <div *ngIf="!isLoading() && activeTab() !== 'USERS' && !hasError() && displayPlaces().length === 0" class="empty-state">
          <p>Žádná místa v této kategorii.</p>
        </div>

        <div *ngIf="!isLoading() && activeTab() !== 'USERS' && displayPlaces().length > 0" class="places-grid">
          <div *ngFor="let place of displayPlaces()" class="admin-card">
            <div class="admin-card-header">
              <div class="header-badges">
                <span class="badge" [ngClass]="getCategoryBadgeClass(place.category)">
                  {{ place.categoryLabel }}
                </span>
                <span class="badge" [ngClass]="'badge-status-' + place.status.toLowerCase()">
                  {{ getStatusLabel(place.status) }}
                </span>
              </div>
              <span class="card-date">{{ place.createdAt | date:'d. M. yyyy HH:mm' }}</span>
            </div>

            <div class="admin-card-body">
              <div *ngIf="place.images && place.images.length > 0" class="card-thumb-wrap">
                <img [src]="place.images[0].url" [alt]="place.title" class="card-thumb" />
              </div>

              <div class="card-main-info">
                <h3 class="card-title">{{ place.title }}</h3>
                <p class="card-addr">📍 {{ place.address }}, {{ place.city }} (PSČ: {{ place.postalCode }})</p>
                <p class="card-tags">
                  <span>Cena: <strong>{{ place.priceLevelLabel }}</strong></span>
                  <span>Typ: <strong>{{ place.discountTypeLabel }}</strong></span>
                  <span *ngIf="place.openingHours">Otevírací: {{ place.openingHours }}</span>
                </p>
                <p *ngIf="place.description" class="card-description">
                  {{ place.description }}
                </p>
                <p *ngIf="place.author" class="card-author">
                  Zadal uživatel: <strong>{{ place.author.nickname }}</strong> ({{ place.author.email }})
                </p>
                <p *ngIf="place.rejectionReason" class="card-rejection">
                  Důvod zamítnutí: <em>{{ place.rejectionReason }}</em>
                </p>
              </div>
            </div>

            <div class="admin-card-actions">
              <!-- If PENDING or REJECTED: Show Approve -->
              <button
                *ngIf="place.status !== 'APPROVED'"
                class="btn btn-success"
                (click)="approve(place)"
              >
                ✓ Schválit a publikovat
              </button>

              <!-- If PENDING or APPROVED: Show Reject -->
              <button
                *ngIf="place.status !== 'REJECTED'"
                class="btn btn-danger"
                (click)="openRejectModal(place)"
              >
                ✕ Zamítnout
              </button>

              <button
                class="btn btn-secondary delete-btn"
                (click)="deletePlace(place)"
                title="Trvale smazat"
              >
                🗑️ Smazat
              </button>
            </div>
          </div>
        </div>

        <!-- Pagination Bar -->
        <div *ngIf="!isLoading() && totalPages() > 1" class="pagination-bar">
          <button
            class="pagination-btn"
            [disabled]="!hasPrevious()"
            (click)="goToPage(currentPage() - 1)"
            aria-label="Předchozí stránka"
          >
            « Předchozí
          </button>
          <span class="pagination-info">
            Strana <strong>{{ currentPage() + 1 }}</strong> z <strong>{{ totalPages() }}</strong> (celkem {{ totalElements() }} položek)
          </span>
          <button
            class="pagination-btn"
            [disabled]="!hasNext()"
            (click)="goToPage(currentPage() + 1)"
            aria-label="Další stránka"
          >
            Další »
          </button>
        </div>
      </main>

      <!-- Reject Reason Modal -->
      <div *ngIf="rejectingPlace()" class="modal-backdrop">
        <div class="modal-card reject-modal">
          <div class="modal-header">
            <h3>Důvod zamítnutí místa</h3>
            <button class="close-btn" (click)="rejectingPlace.set(null)">✕</button>
          </div>
          <div class="modal-body">
            <p>Zadejte odůvodnění pro uživatele, proč toto místo nebylo schváleno:</p>
            <textarea
              class="input-field textarea-field"
              rows="3"
              placeholder="např. Neplatná adresa, prodejna již neexistuje, nejedná se o levné nákupy..."
              [(ngModel)]="rejectionReasonText"
            ></textarea>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" (click)="rejectingPlace.set(null)">Zrušit</button>
            <button class="btn btn-danger" (click)="confirmReject()">Potvrdit zamítnutí</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .admin-page {
      min-height: 100vh;
      background: var(--bg-app);
      display: flex;
      flex-direction: column;
    }
    .admin-header {
      background: #ffffff;
      border-bottom: 1px solid var(--border-color);
      padding: 1rem 0;
    }
    .admin-header-container {
      max-width: 1400px;
      margin: 0 auto;
      padding: 0 1.5rem;
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 1rem;
    }
    .admin-brand h1 {
      font-size: 1.25rem;
      font-weight: 800;
      color: var(--text-main);
    }
    .back-link {
      font-size: 0.8125rem;
      font-weight: 600;
      color: var(--primary-600);
      text-decoration: none;
      display: block;
      margin-bottom: 0.25rem;
    }
    .admin-main {
      max-width: 1400px;
      width: 100%;
      margin: 0 auto;
      padding: 1.5rem;
      flex: 1;
      display: flex;
      flex-direction: column;
    }
    .admin-tabs {
      display: flex;
      gap: 0.5rem;
      margin-bottom: 1.5rem;
      flex-wrap: wrap;
    }
    .tab-btn {
      padding: 0.65rem 1.25rem;
      border-radius: var(--radius-md);
      border: 1px solid var(--border-color);
      background: #ffffff;
      font-family: inherit;
      font-size: 0.875rem;
      font-weight: 600;
      color: var(--text-muted);
      cursor: pointer;
      transition: all var(--transition-fast);
    }
    .tab-btn.active {
      background: var(--primary-600);
      color: #ffffff;
      border-color: var(--primary-600);
      box-shadow: var(--shadow-sm);
    }
    .places-grid {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      margin-bottom: 1.5rem;
    }
    .admin-card {
      background: #ffffff;
      border: 1px solid var(--border-color);
      border-radius: var(--radius-lg);
      padding: 1.25rem;
      box-shadow: var(--shadow-sm);
    }
    .admin-card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 0.75rem;
    }
    .header-badges {
      display: flex;
      gap: 0.5rem;
    }
    .card-date {
      font-size: 0.75rem;
      color: var(--text-muted);
    }
    .admin-card-body {
      display: flex;
      gap: 1rem;
      margin-bottom: 1rem;
    }
    .card-thumb-wrap {
      width: 120px;
      height: 90px;
      border-radius: var(--radius-md);
      overflow: hidden;
      flex-shrink: 0;
    }
    .card-thumb {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .card-main-info {
      flex: 1;
    }
    .card-title {
      font-size: 1.125rem;
      font-weight: 700;
      margin-bottom: 0.25rem;
    }
    .card-addr {
      font-size: 0.875rem;
      color: var(--text-muted);
      margin-bottom: 0.35rem;
    }
    .card-tags {
      display: flex;
      gap: 1rem;
      font-size: 0.8125rem;
      color: #475569;
      margin-bottom: 0.5rem;
      flex-wrap: wrap;
    }
    .card-description {
      font-size: 0.875rem;
      line-height: 1.5;
      color: #334155;
      margin-bottom: 0.5rem;
      background: var(--bg-app);
      padding: 0.5rem 0.75rem;
      border-radius: var(--radius-sm);
    }
    .card-author {
      font-size: 0.75rem;
      color: var(--text-muted);
    }
    .card-rejection {
      font-size: 0.8125rem;
      color: #991b1b;
      margin-top: 0.35rem;
    }
    .admin-card-actions {
      display: flex;
      gap: 0.75rem;
      border-top: 1px solid var(--border-color);
      padding-top: 0.75rem;
    }
    .alert-success {
      background: #dcfce7;
      color: #15803d;
      padding: 0.75rem 1rem;
      border-radius: var(--radius-md);
      margin-bottom: 1.25rem;
      border: 1px solid #bbf7d0;
      font-weight: 600;
    }
    .error-banner {
      margin-bottom: 1.25rem;
      padding: 0.875rem 1.25rem;
      background-color: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: var(--radius-md, 8px);
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      animation: fadeIn 0.2s ease;
    }
    .error-banner-content {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .error-banner-icon {
      font-size: 1.5rem;
      flex-shrink: 0;
    }
    .error-banner-text {
      display: flex;
      flex-direction: column;
    }
    .error-banner-text strong {
      font-size: 0.875rem;
      color: #991b1b;
    }
    .error-banner-text span {
      font-size: 0.8125rem;
      color: #b91c1c;
    }
    .btn-retry-sm {
      background: #dc2626;
      color: #ffffff;
      border: none;
      padding: 0.45rem 0.85rem;
      font-size: 0.8125rem;
      font-weight: 700;
      border-radius: var(--radius-sm, 6px);
      cursor: pointer;
      white-space: nowrap;
      transition: background 0.15s ease;
    }
    .btn-retry-sm:hover {
      background: #b91c1c;
    }

    .loading-state, .empty-state {
      padding: 3rem 1.5rem;
      text-align: center;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.75rem;
      color: var(--text-muted);
    }
    .spinner {
      width: 36px;
      height: 36px;
      border: 3px solid #e2e8f0;
      border-top-color: var(--primary-600);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    .reject-modal {
      max-width: 480px;
    }

    .pagination-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1rem 1.5rem;
      background: #ffffff;
      border: 1px solid var(--border-color);
      border-radius: var(--radius-lg, 12px);
      margin-top: auto;
    }
    .pagination-btn {
      background: var(--bg-app);
      border: 1px solid var(--border-color);
      color: var(--text-main);
      padding: 0.5rem 1rem;
      border-radius: var(--radius-md, 8px);
      font-size: 0.875rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.15s ease;
    }
    .pagination-btn:hover:not(:disabled) {
      background: var(--primary-50);
      color: var(--primary-600);
      border-color: var(--primary-200);
    }
    .pagination-btn:disabled {
      opacity: 0.45;
      cursor: not-allowed;
    }
    .pagination-info {
      font-size: 0.875rem;
      color: var(--text-muted);
    }
    .pagination-info strong {
      color: var(--text-main);
    }

    /* Users list styles */
    .users-list {
      display: flex;
      flex-direction: column;
      gap: 0.875rem;
      margin-bottom: 1.5rem;
    }
    .user-row-card {
      background: #ffffff;
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md, 8px);
      padding: 1rem 1.25rem;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      flex-wrap: wrap;
    }
    .user-main-info {
      display: flex;
      align-items: center;
      gap: 1rem;
      flex-wrap: wrap;
    }
    .user-avatar-sm {
      width: 38px;
      height: 38px;
      background: var(--primary-100);
      color: var(--primary-700);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 1rem;
    }
    .user-text {
      display: flex;
      flex-direction: column;
    }
    .u-name {
      font-weight: 700;
      font-size: 0.95rem;
      color: var(--text-main);
    }
    .u-email {
      font-size: 0.8125rem;
      color: var(--text-muted);
    }
    .badge-admin {
      background: #fef3c7;
      color: #b45309;
    }
    .badge-user {
      background: #e2e8f0;
      color: #475569;
    }
    .u-date {
      font-size: 0.8125rem;
      color: var(--text-muted);
    }
    .current-user-badge {
      font-size: 0.8125rem;
      font-weight: 600;
      color: var(--text-muted);
      font-style: italic;
    }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-4px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `],
})
export class AdminDashboardComponent implements OnInit {
  private apiService = inject(ApiService);
  private toastService = inject(ToastService);
  readonly authService = inject(AuthService);

  readonly activeTab = signal<'PENDING' | 'APPROVED' | 'REJECTED' | 'USERS'>('PENDING');
  readonly pendingTotal = signal(0);
  readonly displayPlaces = signal<Place[]>([]);
  readonly users = signal<User[]>([]);
  readonly isLoading = signal(false);
  readonly hasError = signal(false);
  readonly statusMessage = signal<string | null>(null);

  readonly currentPage = signal(0);
  readonly pageSize = 20;
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);
  readonly hasPrevious = signal(false);
  readonly hasNext = signal(false);

  readonly rejectingPlace = signal<Place | null>(null);
  rejectionReasonText = '';

  ngOnInit(): void {
    this.loadData();
  }

  switchTab(tab: 'PENDING' | 'APPROVED' | 'REJECTED' | 'USERS'): void {
    this.activeTab.set(tab);
    this.currentPage.set(0);
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    // Načíst celkový počet pending pro badge
    this.apiService.getPendingPlaces(0, 1).subscribe({
      next: (res) => this.pendingTotal.set(res.totalElements),
      error: () => {},
    });

    if (this.activeTab() === 'USERS') {
      this.apiService.getAdminUsers(this.currentPage(), this.pageSize).subscribe({
        next: (res) => {
          this.users.set(res.content);
          this.totalElements.set(res.totalElements);
          this.totalPages.set(res.totalPages);
          this.hasPrevious.set(res.hasPrevious);
          this.hasNext.set(res.hasNext);
          this.isLoading.set(false);
          this.hasError.set(false);
        },
        error: () => {
          this.isLoading.set(false);
          this.hasError.set(true);
          this.toastService.error('Nepodařilo se načíst uživatele.', 'Chyba serveru');
        },
      });
    } else {
      const statusParam = this.activeTab() === 'PENDING' ? 'PENDING' : this.activeTab() === 'APPROVED' ? 'APPROVED' : 'REJECTED';
      this.apiService.getAllPlacesAdmin(statusParam, this.currentPage(), this.pageSize).subscribe({
        next: (res) => {
          this.displayPlaces.set(res.content);
          this.totalElements.set(res.totalElements);
          this.totalPages.set(res.totalPages);
          this.hasPrevious.set(res.hasPrevious);
          this.hasNext.set(res.hasNext);
          this.isLoading.set(false);
          this.hasError.set(false);
        },
        error: () => {
          this.isLoading.set(false);
          this.hasError.set(true);
          this.toastService.error('Nepodařilo se načíst data administrace.', 'Chyba serveru');
        },
      });
    }
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadData();
    }
  }

  deleteUser(u: User): void {
    if (confirm(`Opravdu chcete smazat (soft delete) uživatelský účet "${u.nickname}" (${u.email})?`)) {
      this.apiService.deleteUserByAdmin(u.id).subscribe({
        next: () => {
          this.toastService.success(`Uživatel "${u.nickname}" byl označen jako smazaný.`);
          this.loadData();
        },
        error: (err) => {
          this.toastService.error(err.error?.message || 'Smazání uživatele se nezdařilo.');
        },
      });
    }
  }

  approve(place: Place): void {
    this.apiService.approvePlace(place.id).subscribe({
      next: () => {
        this.toastService.success(`Místo "${place.title}" bylo schváleno.`);
        this.loadData();
      },
      error: () => {
        this.toastService.error(`Schválení místa "${place.title}" se nezdařilo.`);
      },
    });
  }

  openRejectModal(place: Place): void {
    this.rejectingPlace.set(place);
    this.rejectionReasonText = '';
  }

  confirmReject(): void {
    const place = this.rejectingPlace();
    if (!place) return;

    this.apiService.rejectPlace(place.id, this.rejectionReasonText).subscribe({
      next: () => {
        this.rejectingPlace.set(null);
        this.toastService.success(`Místo "${place.title}" bylo zamítnuto.`);
        this.loadData();
      },
      error: () => {
        this.toastService.error(`Zamítnutí místa se nezdařilo.`);
      },
    });
  }

  deletePlace(place: Place): void {
    if (confirm(`Opravdu chcete trvale smazat místo "${place.title}"?`)) {
      this.apiService.deletePlace(place.id).subscribe({
        next: () => {
          this.toastService.success(`Místo "${place.title}" bylo smazáno.`);
          this.loadData();
        },
        error: () => {
          this.toastService.error(`Smazání místa se nezdařilo.`);
        },
      });
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'APPROVED': return 'Schváleno';
      case 'PENDING': return 'Čeká na schválení';
      case 'REJECTED': return 'Zamítnuto';
      default: return status;
    }
  }

  getCategoryBadgeClass(category: string): string {
    switch (category) {
      case 'FOOD': return 'badge-food';
      case 'SECOND_HAND': return 'badge-second-hand';
      case 'OUTLET': return 'badge-outlet';
      case 'PALLET_GOODS': return 'badge-pallet';
      case 'FACTORY_STORE': return 'badge-factory';
      case 'FURNITURE_BAZAAR': return 'badge-furniture';
      case 'DRUGSTORE': return 'badge-drugstore';
      default: return 'badge-other';
    }
  }
}
