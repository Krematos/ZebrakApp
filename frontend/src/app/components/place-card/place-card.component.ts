import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Place } from '../../core/models/place.model';

@Component({
  selector: 'app-place-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div
      class="place-card"
      [class.selected]="isSelected"
      (click)="selectPlace.emit(place)"
    >
      <!-- Image Thumbnail / Placeholder -->
      <div class="card-image-wrap">
        <img
          *ngIf="place.images && place.images.length > 0; else noImage"
          [src]="place.images[0].url"
          [alt]="place.title"
          class="card-img"
          loading="lazy"
        />
        <ng-template #noImage>
          <div class="no-img-placeholder" [ngClass]="getCategoryBadgeClass(place.category)">
            <span class="placeholder-emoji">{{ getCategoryEmoji(place.category) }}</span>
          </div>
        </ng-template>

        <!-- Price badge over image -->
        <span class="price-chip">
          {{ place.priceLevelLabel }}
        </span>
      </div>

      <!-- Card Details -->
      <div class="card-content">
        <div class="card-header">
          <span class="badge" [ngClass]="getCategoryBadgeClass(place.category)">
            {{ place.categoryLabel }}
          </span>
          <span class="discount-badge">
            {{ place.discountTypeLabel }}
          </span>
        </div>

        <h3 class="card-title">{{ place.title }}</h3>
        
        <p class="card-location">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
            <circle cx="12" cy="10" r="3"></circle>
          </svg>
          <span>{{ place.city }}, {{ place.address }}</span>
        </p>

        <p *ngIf="place.description" class="card-desc">
          {{ place.description }}
        </p>

        <div class="card-footer">
          <div class="status-indicator" [class.high-trust]="place.votesActive >= place.votesClosed">
            <span class="status-dot"></span>
            <span>{{ place.votesActive }} ověření funkčnosti</span>
          </div>

          <span class="details-hint">Zobrazit detail →</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .place-card {
      background: #ffffff;
      border: 1px solid var(--border-color);
      border-radius: var(--radius-lg);
      padding: 0.875rem;
      display: flex;
      gap: 1rem;
      cursor: pointer;
      transition: all var(--transition-fast);
      position: relative;
    }
    .place-card:hover {
      border-color: #cbd5e1;
      box-shadow: var(--shadow-md);
      transform: translateY(-2px);
    }
    .place-card.selected {
      border-color: var(--primary-500);
      box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.2);
      background: #f8fafc;
    }
    .card-image-wrap {
      width: 110px;
      height: 110px;
      border-radius: var(--radius-md);
      overflow: hidden;
      flex-shrink: 0;
      position: relative;
      background: #f1f5f9;
    }
    .card-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .no-img-placeholder {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .placeholder-emoji {
      font-size: 2rem;
    }
    .price-chip {
      position: absolute;
      bottom: 6px;
      left: 6px;
      background: rgba(15, 23, 42, 0.85);
      backdrop-filter: blur(4px);
      color: #ffffff;
      font-size: 0.6875rem;
      font-weight: 700;
      padding: 2px 6px;
      border-radius: var(--radius-sm);
    }
    .card-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      min-width: 0;
    }
    .card-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 0.35rem;
      flex-wrap: wrap;
    }
    .discount-badge {
      font-size: 0.6875rem;
      font-weight: 600;
      color: var(--text-muted);
      background: var(--bg-app);
      padding: 2px 6px;
      border-radius: var(--radius-sm);
    }
    .card-title {
      font-size: 1rem;
      font-weight: 700;
      color: var(--text-main);
      margin-bottom: 0.25rem;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .card-location {
      font-size: 0.8125rem;
      color: var(--text-muted);
      display: flex;
      align-items: center;
      gap: 0.35rem;
      margin-bottom: 0.35rem;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .card-desc {
      font-size: 0.8125rem;
      color: #475569;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      line-height: 1.35;
      margin-bottom: 0.5rem;
    }
    .card-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: auto;
      font-size: 0.75rem;
    }
    .status-indicator {
      display: flex;
      align-items: center;
      gap: 0.35rem;
      color: var(--text-muted);
      font-weight: 600;
    }
    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #22c55e;
    }
    .details-hint {
      color: var(--primary-600);
      font-weight: 600;
    }
    @media (max-width: 480px) {
      .card-image-wrap {
        width: 85px;
        height: 85px;
      }
      .card-desc {
        display: none;
      }
    }
  `],
})
export class PlaceCardComponent {
  @Input({ required: true }) place!: Place;
  @Input() isSelected = false;
  @Output() selectPlace = new EventEmitter<Place>();

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

  getCategoryEmoji(category: string): string {
    switch (category) {
      case 'FOOD': return '🥦';
      case 'SECOND_HAND': return '👗';
      case 'OUTLET': return '🏷️';
      case 'PALLET_GOODS': return '📦';
      case 'FACTORY_STORE': return '🏭';
      case 'FURNITURE_BAZAAR': return '🛋️';
      case 'DRUGSTORE': return '✨';
      default: return '📍';
    }
  }
}
