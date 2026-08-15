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
import { CategoryInfo, CategoryType, DiscountType, Place, PriceLevelType } from '../../core/models/place.model';

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
              Nalezená místa ({{ places().length }})
            </h2>
            <span class="sidebar-subtitle">Kliknutím zobrazíte detail a polohu</span>
          </div>

          <!-- Loading state -->
          <div *ngIf="isLoading()" class="loading-state">
            <div class="spinner"></div>
            <p>Načítám levná místa v okolí...</p>
          </div>

          <!-- Empty state -->
          <div *ngIf="!isLoading() && places().length === 0" class="empty-state">
            <span class="empty-icon">🔍</span>
            <h3>Žádná místa neodpovídají filtrům</h3>
            <p>Zkuste upravit vyhledávací dotaz nebo vybrat jinou kategorii.</p>
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
        </aside>

        <!-- Map Container -->
        <section class="map-section" [class.mobile-hidden]="mobileTab() === 'list'">
          <app-map-view
            [places]="places()"
            [selectedPlace]="selectedPlace()"
            (placeClicked)="onSelectPlace($event)"
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
          📋 Seznam míst ({{ places().length }})
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

  readonly places = signal<Place[]>([]);
  readonly categories = signal<CategoryInfo[]>([]);
  readonly isLoading = signal(false);
  readonly selectedPlace = signal<Place | null>(null);
  readonly detailPlace = signal<Place | null>(null);
  readonly editPlaceData = signal<Place | null>(null);

  readonly showAuthModal = signal(false);
  readonly showAddPlaceModal = signal(false);
  readonly mobileTab = signal<'list' | 'map'>('list');

  private currentFilters: any = {};

  ngOnInit(): void {
    this.loadMetadata();
    this.loadPlaces();
  }

  loadMetadata(): void {
    this.apiService.getCategories().subscribe({
      next: (cats) => this.categories.set(cats),
    });
  }

  loadPlaces(): void {
    this.isLoading.set(true);
    this.apiService.searchPlaces(this.currentFilters).subscribe({
      next: (res) => {
        this.places.set(res);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  onFilterChange(filters: {
    category: CategoryType | null;
    priceLevel: PriceLevelType | null;
    discountType: DiscountType | null;
    query: string;
  }): void {
    this.currentFilters = {
      category: filters.category || undefined,
      priceLevel: filters.priceLevel || undefined,
      discountType: filters.discountType || undefined,
      q: filters.query || undefined,
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
