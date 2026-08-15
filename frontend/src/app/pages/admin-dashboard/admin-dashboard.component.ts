import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { Place } from '../../core/models/place.model';

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
            ⏳ Čekající na schválení ({{ pendingPlaces().length }})
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
        </div>

        <!-- Notification Message -->
        <div *ngIf="statusMessage()" class="alert-success">
          {{ statusMessage() }}
        </div>

        <!-- Pending Queue / Place List -->
        <div *ngIf="isLoading()" class="loading-state">
          Načítám záznamy...
        </div>

        <div *ngIf="!isLoading() && displayPlaces().length === 0" class="empty-state">
          <p>Žádná místa v této kategorii.</p>
        </div>

        <div *ngIf="!isLoading() && displayPlaces().length > 0" class="places-grid">
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
    .reject-modal {
      max-width: 480px;
    }
  `],
})
export class AdminDashboardComponent implements OnInit {
  private apiService = inject(ApiService);
  readonly authService = inject(AuthService);

  readonly activeTab = signal<'PENDING' | 'APPROVED' | 'REJECTED'>('PENDING');
  readonly pendingPlaces = signal<Place[]>([]);
  readonly displayPlaces = signal<Place[]>([]);
  readonly isLoading = signal(false);
  readonly statusMessage = signal<string | null>(null);

  readonly rejectingPlace = signal<Place | null>(null);
  rejectionReasonText = '';

  ngOnInit(): void {
    this.loadData();
  }

  switchTab(tab: 'PENDING' | 'APPROVED' | 'REJECTED'): void {
    this.activeTab.set(tab);
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);

    // Načíst pending pro badge
    this.apiService.getPendingPlaces().subscribe({
      next: (res) => this.pendingPlaces.set(res),
    });

    this.apiService.getAllPlacesAdmin(this.activeTab()).subscribe({
      next: (res) => {
        this.displayPlaces.set(res);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  approve(place: Place): void {
    this.apiService.approvePlace(place.id).subscribe({
      next: () => {
        this.statusMessage.set(`Místo "${place.title}" bylo úspěšně schváleno a publikováno.`);
        this.loadData();
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
        this.statusMessage.set(`Místo "${place.title}" bylo zamítnuto.`);
        this.loadData();
      },
    });
  }

  deletePlace(place: Place): void {
    if (confirm(`Opravdu chcete trvale smazat místo "${place.title}"?`)) {
      this.apiService.deletePlace(place.id).subscribe({
        next: () => {
          this.statusMessage.set(`Místo "${place.title}" bylo smazáno.`);
          this.loadData();
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
