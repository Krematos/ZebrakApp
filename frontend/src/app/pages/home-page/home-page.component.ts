import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { FilterBarComponent } from '../../components/filter-bar/filter-bar.component';
import { MapViewComponent } from '../../components/map-view/map-view.component';
import { PlaceCardComponent } from '../../components/place-card/place-card.component';
import { PlaceDetailComponent } from '../../components/place-detail/place-detail.component';
import { AddPlaceModalComponent } from '../../components/add-place-modal/add-place-modal.component';
import { AuthModalComponent } from '../../components/auth-modal/auth-modal.component';
import { ApiService } from '../../core/services/api.service';
import { ToastService } from '../../core/services/toast.service';
import { CategoryInfo, CategoryType, DiscountType, MapBounds, Place, PriceLevelType } from '../../core/models/place.model';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [
    CommonModule,
    NavbarComponent,
    FilterBarComponent,
    MapViewComponent,
    PlaceCardComponent,
    PlaceDetailComponent,
    AddPlaceModalComponent,
    AuthModalComponent,
  ],
  template: `
    <div class="app-layout">
      <!-- Top Navbar -->
      <app-navbar
        (openAuthModal)="showAuthModal.set(true)"
        (openAddPlaceModal)="openAddPlace()"
      ></app-navbar>

      <!-- Filter bar -->
      <app-filter-bar
        [categories]="categories()"
        (filterChange)="onFilterChange($event)"
      ></app-filter-bar>

      <!-- Main Split Content -->
      <main class="main-content">
        <!-- Sidebar with Place Cards -->
        <aside class="places-sidebar" [class.mobile-hidden]="mobileTab() === 'map'">
          <div class="sidebar-header">
            <h2 class="sidebar-title">
              Nalezená místa ({{ totalElements() }})
            </h2>
            <span class="sidebar-subtitle">Kliknutím zobrazíte detail a polohu</span>
          </div>

          <!-- Error Alert Banner (if error occurred during refresh or load) -->
          <div *ngIf="hasError()" class="error-banner">
            <div class="error-banner-content">
              <span class="error-banner-icon">⚠️</span>
              <div class="error-banner-text">
                <strong>Nepodařilo se načíst data</strong>
                <span>Zkontrolujte připojení k serveru a zkuste to znovu.</span>
              </div>
            </div>
            <button class="btn btn-sm btn-retry-sm" (click)="loadPlaces()">
              🔄 Zkusit znovu
            </button>
          </div>

          <!-- Loading state -->
          <div *ngIf="isLoading()" class="loading-state">
            <div class="spinner"></div>
            <p>Načítám levná místa v okolí...</p>
          </div>

          <!-- Error Full State (when list is empty and error happened) -->
          <div *ngIf="!isLoading() && hasError() && places().length === 0" class="empty-state error-full-state">
            <span class="empty-icon error-icon">❌</span>
            <h3>Nepodařilo se načíst data</h3>
            <p>Při komunikaci se serverem došlo k chybě. Zkontrolujte prosím své připojení a zkuste to znovu.</p>
            <button class="btn btn-primary retry-btn" (click)="loadPlaces()">
              🔄 Zkusit znovu
            </button>
          </div>

          <!-- Empty state -->
          <div *ngIf="!isLoading() && !hasError() && places().length === 0" class="empty-state">
            <span class="empty-icon">🔍</span>
            <h3>Žádná místa neodpovídají filtrům</h3>
            <p>Zkuste upravit vyhledávací dotaz nebo posunout mapu do jiné oblasti.</p>
            <button class="btn btn-primary" (click)="openAddPlace()">
              + Přidat první místo sem
            </button>
          </div>

          <!-- Cards list -->
          <div *ngIf="!isLoading() && places().length > 0" class="cards-list">
            <app-place-card
              *ngFor="let place of places()"
              [place]="place"
              [isSelected]="selectedPlace()?.id === place.id"
              (selectPlace)="onSelectPlace(place)"
            ></app-place-card>
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
              Strana <strong>{{ currentPage() + 1 }}</strong> z <strong>{{ totalPages() }}</strong>
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
        </aside>

        <!-- Map Container -->
        <section class="map-section" [class.mobile-hidden]="mobileTab() === 'list'">
          <app-map-view
            [places]="places()"
            [selectedPlace]="selectedPlace()"
            (placeClicked)="onSelectPlace($event)"
            (boundsChanged)="onBoundsChanged($event)"
          ></app-map-view>
        </section>
      </main>

      <!-- Mobile Bottom Tab Switcher -->
      <div class="mobile-switcher">
        <button
          class="switch-btn"
          [class.active]="mobileTab() === 'list'"
          (click)="mobileTab.set('list')"
        >
          📋 Seznam míst ({{ totalElements() }})
        </button>
        <button
          class="switch-btn"
          [class.active]="mobileTab() === 'map'"
          (click)="mobileTab.set('map')"
        >
          🗺️ Mapa
        </button>
      </div>

      <!-- Modals -->
      <app-auth-modal
        *ngIf="showAuthModal()"
        (close)="showAuthModal.set(false)"
        (loggedIn)="onUserLoggedIn()"
      ></app-auth-modal>

      <app-place-detail
        *ngIf="detailPlace()"
        [place]="detailPlace()!"
        (close)="detailPlace.set(null)"
        (editPlace)="openEditPlace($event)"
        (updated)="loadPlaces()"
      ></app-place-detail>

      <app-add-place-modal
        *ngIf="showAddPlaceModal()"
        [editPlaceData]="editPlaceData()"
        [categories]="categories()"
        (close)="showAddPlaceModal.set(false); editPlaceData.set(null)"
        (saved)="onPlaceSaved($event)"
      ></app-add-place-modal>
    </div>
  `,
  styles: [`
    .app-layout {
      display: flex;
      flex-direction: column;
      height: 100vh;
      overflow: hidden;
    }
    .main-content {
      flex: 1;
      display: flex;
      overflow: hidden;
      position: relative;
    }
    .places-sidebar {
      width: 440px;
      height: 100%;
      overflow-y: auto;
      background: #ffffff;
      border-right: 1px solid var(--border-color);
      display: flex;
      flex-direction: column;
      flex-shrink: 0;
      z-index: 10;
    }
    .sidebar-header {
      padding: 1rem 1.25rem 0.5rem 1.25rem;
    }
    .sidebar-title {
      font-size: 1.125rem;
      font-weight: 800;
      color: var(--text-main);
    }
    .sidebar-subtitle {
      font-size: 0.75rem;
      color: var(--text-muted);
    }
    .cards-list {
      padding: 0.875rem 1.25rem;
      display: flex;
      flex-direction: column;
      gap: 0.875rem;
      flex: 1;
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
    .empty-icon {
      font-size: 3rem;
    }
    .empty-state h3 {
      font-size: 1.1rem;
      color: var(--text-main);
    }

    .error-banner {
      margin: 0.5rem 1.25rem 0.75rem 1.25rem;
      padding: 0.75rem 1rem;
      background-color: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: var(--radius-md, 8px);
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      animation: fadeIn 0.2s ease;
    }
    .error-banner-content {
      display: flex;
      align-items: center;
      gap: 0.6rem;
    }
    .error-banner-icon {
      font-size: 1.25rem;
      flex-shrink: 0;
    }
    .error-banner-text {
      display: flex;
      flex-direction: column;
    }
    .error-banner-text strong {
      font-size: 0.8125rem;
      color: #991b1b;
    }
    .error-banner-text span {
      font-size: 0.75rem;
      color: #b91c1c;
    }
    .btn-retry-sm {
      background: #dc2626;
      color: #ffffff;
      border: none;
      padding: 0.4rem 0.75rem;
      font-size: 0.75rem;
      font-weight: 700;
      border-radius: var(--radius-sm, 6px);
      cursor: pointer;
      white-space: nowrap;
      transition: background 0.15s ease;
    }
    .btn-retry-sm:hover {
      background: #b91c1c;
    }

    .error-full-state {
      background-color: #fffafb;
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

    .pagination-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0.875rem 1.25rem;
      border-top: 1px solid var(--border-color);
      background: #ffffff;
      margin-top: auto;
      position: sticky;
      bottom: 0;
      z-index: 5;
    }
    .pagination-btn {
      background: var(--bg-app);
      border: 1px solid var(--border-color);
      color: var(--text-main);
      padding: 0.45rem 0.85rem;
      border-radius: var(--radius-md, 8px);
      font-size: 0.8125rem;
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
      font-size: 0.8125rem;
      color: var(--text-muted);
    }
    .pagination-info strong {
      color: var(--text-main);
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-4px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .map-section {
      flex: 1;
      height: 100%;
      position: relative;
    }
    .mobile-switcher {
      display: none;
    }
    @media (max-width: 900px) {
      .places-sidebar {
        width: 100%;
        position: absolute;
        inset: 0;
        bottom: 56px;
      }
      .map-section {
        width: 100%;
        position: absolute;
        inset: 0;
        bottom: 56px;
      }
      .mobile-hidden {
        display: none !important;
      }
      .mobile-switcher {
        display: flex;
        position: fixed;
        bottom: 0;
        left: 0;
        right: 0;
        height: 56px;
        background: #ffffff;
        border-top: 1px solid var(--border-color);
        z-index: 1000;
      }
      .switch-btn {
        flex: 1;
        border: none;
        background: transparent;
        font-family: inherit;
        font-size: 0.875rem;
        font-weight: 700;
        color: var(--text-muted);
        cursor: pointer;
      }
      .switch-btn.active {
        color: var(--primary-600);
        background: var(--primary-50);
        border-top: 2px solid var(--primary-600);
      }
    }
  `],
})
export class HomePageComponent implements OnInit {
  private apiService = inject(ApiService);
  private toastService = inject(ToastService);

  readonly places = signal<Place[]>([]);
  readonly categories = signal<CategoryInfo[]>([]);
  readonly isLoading = signal(false);
  readonly hasError = signal(false);
  readonly selectedPlace = signal<Place | null>(null);
  readonly detailPlace = signal<Place | null>(null);
  readonly editPlaceData = signal<Place | null>(null);

  readonly currentPage = signal(0);
  readonly pageSize = 20;
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);
  readonly hasPrevious = signal(false);
  readonly hasNext = signal(false);

  readonly showAuthModal = signal(false);
  readonly showAddPlaceModal = signal(false);
  readonly mobileTab = signal<'list' | 'map'>('list');

  private currentBounds: MapBounds | null = null;
  private activeFilters: {
    category?: CategoryType;
    priceLevel?: PriceLevelType;
    discountType?: DiscountType;
    q?: string;
  } = {};

  private currentFilters: any = {};

  ngOnInit(): void {
    this.loadMetadata();
    this.loadPlaces();
  }

  loadMetadata(): void {
    this.apiService.getCategories().subscribe({
      next: (cats) => this.categories.set(cats),
      error: () => {
        // Categories can fail silently or retry, non-blocking
      },
    });
  }

  loadPlaces(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    const queryParams = {
      ...this.currentFilters,
      page: this.currentPage(),
      size: this.pageSize,
    };

    this.apiService.searchPlaces(queryParams).subscribe({
      next: (res) => {
        this.places.set(res.content);
        this.totalElements.set(res.totalElements);
        this.totalPages.set(res.totalPages);
        this.hasPrevious.set(res.hasPrevious);
        this.hasNext.set(res.hasNext);
        this.isLoading.set(false);
        this.hasError.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.hasError.set(true);
        this.toastService.error(
          'Nepodařilo se načíst data, zkuste to znovu.',
          'Chyba načítání míst'
        );
      },
    });
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadPlaces();
    }
  }

  onBoundsChanged(bounds: MapBounds | null): void {
    this.currentBounds = bounds;
    this.applyFiltersAndLoad(true);
  }

  onFilterChange(filters: {
    category: CategoryType | null;
    priceLevel: PriceLevelType | null;
    discountType: DiscountType | null;
    query: string;
  }): void {
    this.activeFilters = {
      category: filters.category || undefined,
      priceLevel: filters.priceLevel || undefined,
      discountType: filters.discountType || undefined,
      q: filters.query || undefined,
    };
    this.applyFiltersAndLoad(true);
  }

  private applyFiltersAndLoad(resetPage = false): void {
    if (resetPage) {
      this.currentPage.set(0);
    }
    this.currentFilters = {
      ...this.activeFilters,
      ...(this.currentBounds
        ? {
            minLat: this.currentBounds.minLat,
            maxLat: this.currentBounds.maxLat,
            minLng: this.currentBounds.minLng,
            maxLng: this.currentBounds.maxLng,
          }
        : {}),
    };
    this.loadPlaces();
  }

  onSelectPlace(place: Place): void {
    this.selectedPlace.set(place);
    this.detailPlace.set(place);
  }

  openAddPlace(): void {
    this.editPlaceData.set(null);
    this.showAddPlaceModal.set(true);
  }

  openEditPlace(place: Place): void {
    this.detailPlace.set(null);
    this.editPlaceData.set(place);
    this.showAddPlaceModal.set(true);
  }

  onPlaceSaved(savedPlace: Place): void {
    this.loadPlaces();
    this.onSelectPlace(savedPlace);
  }

  onUserLoggedIn(): void {
    this.loadPlaces();
  }
}
