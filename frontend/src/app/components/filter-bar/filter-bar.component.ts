import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CategoryInfo, CategoryType, DiscountType, PriceLevelType } from '../../core/models/place.model';

@Component({
  selector: 'app-filter-bar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="filter-bar">
      <!-- Search Input -->
      <div class="search-row">
        <div class="search-box">
          <svg class="search-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input
            type="text"
            class="search-input"
            placeholder="Hledat levné místo, město, adresu nebo sortiment..."
            [(ngModel)]="searchQuery"
            (ngModelChange)="onSearchChange()"
          />
          <button *ngIf="searchQuery" class="clear-btn" (click)="clearSearch()">✕</button>
        </div>

        <!-- Filter toggles -->
        <div class="dropdown-filters">
          <!-- Price Level -->
          <select
            class="input-field select-field filter-select"
            [(ngModel)]="selectedPriceLevel"
            (ngModelChange)="onFilterChange()"
          >
            <option [ngValue]="null">Cenová hladina: Vše</option>
            <option value="LOW">Levné (€)</option>
            <option value="VERY_LOW">Velmi levné (€€)</option>
            <option value="EXTREME">Extrémní výprodej (€€€)</option>
          </select>

          <!-- Discount Type -->
          <select
            class="input-field select-field filter-select"
            [(ngModel)]="selectedDiscountType"
            (ngModelChange)="onFilterChange()"
          >
            <option [ngValue]="null">Typ slevy: Vše</option>
            <option value="PERMANENT">Trvalé nízké ceny</option>
            <option value="FLASH_SALES">Nárazové výprodeje / akce</option>
          </select>

          <button
            *ngIf="hasActiveFilters()"
            class="btn btn-secondary btn-reset"
            (click)="resetFilters()"
            title="Resetovat filtry"
          >
            Reset
          </button>
        </div>
      </div>

      <!-- Categories horizontal scroll chips -->
      <div class="categories-scroll">
        <button
          class="cat-chip"
          [class.active]="selectedCategory === null"
          (click)="selectCategory(null)"
        >
          <span class="cat-icon">🏷️</span>
          <span>Všechny kategorie</span>
        </button>

        <button
          *ngFor="let cat of categories"
          class="cat-chip"
          [class.active]="selectedCategory === cat.name"
          (click)="selectCategory(cat.name)"
        >
          <span class="cat-icon">{{ getCategoryEmoji(cat.name) }}</span>
          <span>{{ cat.label }}</span>
        </button>
      </div>
    </div>
  `,
  styles: [`
    .filter-bar {
      background: #ffffff;
      border-bottom: 1px solid var(--border-color);
      padding: 0.875rem 1.25rem;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }
    .search-row {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      flex-wrap: wrap;
    }
    .search-box {
      flex: 1;
      min-width: 260px;
      position: relative;
      display: flex;
      align-items: center;
    }
    .search-icon {
      position: absolute;
      left: 0.875rem;
      color: var(--text-light);
      pointer-events: none;
    }
    .search-input {
      width: 100%;
      padding: 0.65rem 2.25rem 0.65rem 2.5rem;
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      font-size: 0.875rem;
      font-family: inherit;
      outline: none;
      background: var(--bg-app);
      transition: all var(--transition-fast);
    }
    .search-input:focus {
      background: #ffffff;
      border-color: var(--primary-500);
      box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
    }
    .clear-btn {
      position: absolute;
      right: 0.75rem;
      background: none;
      border: none;
      color: var(--text-light);
      cursor: pointer;
      font-size: 0.875rem;
    }
    .dropdown-filters {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      flex-wrap: wrap;
    }
    .filter-select {
      width: auto;
      min-width: 160px;
      padding-top: 0.55rem;
      padding-bottom: 0.55rem;
      font-size: 0.8125rem;
      background-color: var(--bg-app);
    }
    .btn-reset {
      font-size: 0.75rem;
      padding: 0.55rem 0.75rem;
    }
    .categories-scroll {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      overflow-x: auto;
      padding-bottom: 4px;
    }
    .cat-chip {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.4rem 0.85rem;
      border-radius: var(--radius-full);
      background: var(--bg-app);
      border: 1px solid var(--border-color);
      font-size: 0.8125rem;
      font-weight: 600;
      color: var(--text-muted);
      cursor: pointer;
      white-space: nowrap;
      transition: all var(--transition-fast);
    }
    .cat-chip:hover {
      background: #e2e8f0;
      color: var(--text-main);
    }
    .cat-chip.active {
      background: var(--primary-50);
      border-color: var(--primary-500);
      color: var(--primary-700);
    }
    .cat-icon {
      font-size: 0.95rem;
    }
  `],
})
export class FilterBarComponent {
  @Input() categories: CategoryInfo[] = [];

  @Output() filterChange = new EventEmitter<{
    category: CategoryType | null;
    priceLevel: PriceLevelType | null;
    discountType: DiscountType | null;
    query: string;
  }>();

  searchQuery = '';
  selectedCategory: CategoryType | null = null;
  selectedPriceLevel: PriceLevelType | null = null;
  selectedDiscountType: DiscountType | null = null;

  selectCategory(category: CategoryType | null): void {
    this.selectedCategory = category;
    this.emitChange();
  }

  onSearchChange(): void {
    this.emitChange();
  }

  onFilterChange(): void {
    this.emitChange();
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.emitChange();
  }

  resetFilters(): void {
    this.searchQuery = '';
    this.selectedCategory = null;
    this.selectedPriceLevel = null;
    this.selectedDiscountType = null;
    this.emitChange();
  }

  hasActiveFilters(): boolean {
    return (
      !!this.searchQuery ||
      this.selectedCategory !== null ||
      this.selectedPriceLevel !== null ||
      this.selectedDiscountType !== null
    );
  }

  private emitChange(): void {
    this.filterChange.emit({
      category: this.selectedCategory,
      priceLevel: this.selectedPriceLevel,
      discountType: this.selectedDiscountType,
      query: this.searchQuery,
    });
  }

  getCategoryEmoji(category: CategoryType): string {
    switch (category) {
      case 'FOOD':
        return '🥦';
      case 'SECOND_HAND':
        return '👗';
      case 'OUTLET':
        return '🏷️';
      case 'PALLET_GOODS':
        return '📦';
      case 'FACTORY_STORE':
        return '🏭';
      case 'FURNITURE_BAZAAR':
        return '🛋️';
      case 'DRUGSTORE':
        return '✨';
      case 'OTHER':
      default:
        return '📍';
    }
  }
}
