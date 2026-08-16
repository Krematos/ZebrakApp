import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { Place } from '../../core/models/place.model';
import { AddPlaceModalComponent } from '../../components/add-place-modal/add-place-modal.component';

@Component({
  selector: 'app-my-places',
  standalone: true,
  imports: [CommonModule, RouterModule, AddPlaceModalComponent],
  template: `
    <div class="my-places-page">
      <header class="page-header">
        <div class="header-container">
          <div>
            <a routerLink="/" class="back-link">← Zpět na mapu</a>
            <h1>Moje přidaná místa</h1>
          </div>
          <button class="btn btn-primary" (click)="openAddModal()">
            + Přidat další levné místo
          </button>
        </div>
      </header>

      <main class="page-main">
        <!-- Error Banner (if error occurred during refresh) -->
        <div *ngIf="hasError() && places().length > 0" class="error-banner">
          <div class="error-banner-content">
            <span class="error-banner-icon">⚠️</span>
            <div class="error-banner-text">
              <strong>Nepodařilo se načíst data</strong>
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
          <p>Načítám vaše místa...</p>
        </div>

        <!-- Error Full State -->
        <div *ngIf="!isLoading() && hasError() && places().length === 0" class="empty-state error-full-state">
          <span class="empty-emoji error-emoji">❌</span>
          <h3>Nepodařilo se načíst vaše místa</h3>
          <p>Při komunikaci se serverem došlo k chybě. Zkontrolujte připojení a zkuste to znovu.</p>
          <button class="btn btn-primary retry-btn" (click)="loadData()">
            🔄 Zkusit znovu
          </button>
        </div>

        <!-- Empty State -->
        <div *ngIf="!isLoading() && !hasError() && places().length === 0" class="empty-state">
          <span class="empty-emoji">📍</span>
          <h3>Zatím jste nepřidali žádné místo</h3>
          <p>Znáte skvělý sekáč, levné potraviny nebo podnikový outlet? Přidejte ho na mapu!</p>
          <button class="btn btn-primary" (click)="openAddModal()">
            Přidat první místo
          </button>
        </div>

        <!-- Places Grid -->
        <div *ngIf="!isLoading() && places().length > 0" class="places-grid">
          <div *ngFor="let place of places()" class="user-place-card">
            <div class="card-top">
              <div class="badges">
                <span class="badge" [ngClass]="getCategoryBadgeClass(place.category)">
                  {{ place.categoryLabel }}
                </span>
                <span class="badge" [ngClass]="'badge-status-' + place.status.toLowerCase()">
                  {{ getStatusLabel(place.status) }}
                </span>
              </div>
              <span class="date">{{ place.createdAt | date:'d. M. yyyy' }}</span>
            </div>

            <h3 class="title">{{ place.title }}</h3>
            <p class="address">📍 {{ place.address }}, {{ place.city }}</p>
            <p *ngIf="place.description" class="desc">{{ place.description }}</p>

            <div *ngIf="place.status === 'REJECTED' && place.rejectionReason" class="rejection-box">
              ⚠️ Důvod zamítnutí administrátorem: {{ place.rejectionReason }}
            </div>

            <div class="card-bottom">
              <span class="votes-info">
                👍 {{ place.votesActive }} aktivních ověření
              </span>

              <button class="btn btn-secondary btn-edit" (click)="openEditModal(place)">
                Upravit
              </button>
            </div>
          </div>
        </div>
      </main>

      <!-- Add / Edit Modal -->
      <app-add-place-modal
        *ngIf="showAddModal()"
        [editPlaceData]="editPlaceData()"
        [categories]="categories()"
        (close)="showAddModal.set(false); editPlaceData.set(null)"
        (saved)="loadData()"
      ></app-add-place-modal>
    </div>
  `,
  styles: [`
    .my-places-page {
      min-height: 100vh;
      background: var(--bg-app);
      display: flex;
      flex-direction: column;
    }
    .page-header {
      background: #ffffff;
      border-bottom: 1px solid var(--border-color);
      padding: 1rem 0;
    }
    .header-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 1.5rem;
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 1rem;
    }
    .back-link {
      font-size: 0.8125rem;
      font-weight: 600;
      color: var(--primary-600);
      text-decoration: none;
      display: block;
      margin-bottom: 0.25rem;
    }
    .header-container h1 {
      font-size: 1.25rem;
      font-weight: 800;
    }
    .page-main {
      max-width: 1200px;
      width: 100%;
      margin: 0 auto;
      padding: 1.5rem;
      flex: 1;
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

    .loading-state {
      padding: 4rem 1.5rem;
      text-align: center;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
      color: var(--text-muted);
    }
    .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid #e2e8f0;
      border-top-color: var(--primary-600);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .places-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
      gap: 1.25rem;
    }
    .user-place-card {
      background: #ffffff;
      border: 1px solid var(--border-color);
      border-radius: var(--radius-lg);
      padding: 1.25rem;
      display: flex;
      flex-direction: column;
    }
    .card-top {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 0.75rem;
    }
    .badges {
      display: flex;
      gap: 0.5rem;
    }
    .date {
      font-size: 0.75rem;
      color: var(--text-muted);
    }
    .title {
      font-size: 1.125rem;
      font-weight: 700;
      margin-bottom: 0.25rem;
    }
    .address {
      font-size: 0.875rem;
      color: var(--text-muted);
      margin-bottom: 0.5rem;
    }
    .desc {
      font-size: 0.8125rem;
      color: #475569;
      line-height: 1.4;
      margin-bottom: 1rem;
      flex: 1;
    }
    .rejection-box {
      background: #fee2e2;
      color: #991b1b;
      font-size: 0.8125rem;
      padding: 0.5rem 0.75rem;
      border-radius: var(--radius-sm);
      margin-bottom: 0.75rem;
    }
    .card-bottom {
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-top: 1px solid var(--border-color);
      padding-top: 0.75rem;
      margin-top: auto;
    }
    .votes-info {
      font-size: 0.75rem;
      font-weight: 600;
      color: var(--text-muted);
    }
    .btn-edit {
      font-size: 0.8125rem;
      padding: 0.4rem 0.85rem;
    }
    .empty-state {
      text-align: center;
      padding: 4rem 1.5rem;
      background: #ffffff;
      border-radius: var(--radius-lg);
      border: 1px solid var(--border-color);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.75rem;
    }
    .empty-emoji {
      font-size: 3.5rem;
    }
    .error-full-state {
      background: #fffafb;
    }
    .error-full-state h3 {
      color: #991b1b;
    }
    .retry-btn {
      margin-top: 0.5rem;
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-4px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `],
})
export class MyPlacesComponent implements OnInit {
  private apiService = inject(ApiService);
  private toastService = inject(ToastService);
  readonly authService = inject(AuthService);

  readonly places = signal<Place[]>([]);
  readonly categories = signal<any[]>([]);
  readonly isLoading = signal(false);
  readonly hasError = signal(false);
  readonly showAddModal = signal(false);
  readonly editPlaceData = signal<Place | null>(null);

  ngOnInit(): void {
    this.loadData();
    this.apiService.getCategories().subscribe({
      next: (cats) => this.categories.set(cats),
      error: () => {},
    });
  }

  loadData(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    this.apiService.getMyPlaces().subscribe({
      next: (res) => {
        this.places.set(res);
        this.isLoading.set(false);
        this.hasError.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.hasError.set(true);
        this.toastService.error(
          'Nepodařilo se načíst vaše místa, zkuste to znovu.',
          'Chyba načítání'
        );
      },
    });
  }

  openAddModal(): void {
    this.editPlaceData.set(null);
    this.showAddModal.set(true);
  }

  openEditModal(place: Place): void {
    this.editPlaceData.set(place);
    this.showAddModal.set(true);
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'APPROVED': return 'Schváleno a viditelné';
      case 'PENDING': return 'Čeká na schválení adminem';
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
