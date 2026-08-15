import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Place, VoteType } from '../../core/models/place.model';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-place-detail',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="modal-backdrop" (click)="onBackdropClick($event)">
      <div class="modal-card detail-modal">
        <!-- Close button -->
        <button class="close-btn" (click)="close.emit()">✕</button>

        <!-- Gallery / Image header -->
        <div class="gallery-section">
          <div *ngIf="place.images && place.images.length > 0; else noImages" class="gallery-main">
            <img [src]="activeImageUrl() || place.images[0].url" [alt]="place.title" class="main-photo" />
            
            <div *ngIf="place.images.length > 1" class="thumbnails-bar">
              <img
                *ngFor="let img of place.images"
                [src]="img.url"
                [class.active]="(activeImageUrl() || place.images[0].url) === img.url"
                (click)="activeImageUrl.set(img.url)"
                class="thumb-img"
              />
            </div>
          </div>

          <ng-template #noImages>
            <div class="gallery-placeholder">
              <span class="placeholder-emoji">🛍️</span>
              <span class="placeholder-text">{{ place.categoryLabel }}</span>
            </div>
          </ng-template>
        </div>

        <div class="detail-body">
          <!-- Badges header -->
          <div class="badges-row">
            <span class="badge badge-lg" [ngClass]="getCategoryBadgeClass(place.category)">
              {{ place.categoryLabel }}
            </span>
            <span class="badge-price">
              Cenová hladina: <strong>{{ place.priceLevelLabel }}</strong>
            </span>
            <span class="badge-discount">
              {{ place.discountTypeLabel }}
            </span>
          </div>

          <h2 class="detail-title">{{ place.title }}</h2>

          <!-- Location & Navigation -->
          <div class="info-block location-block">
            <div class="info-icon">📍</div>
            <div class="info-text">
              <p class="address-main">{{ place.address }}, {{ place.city }}</p>
              <p *ngIf="place.postalCode" class="address-sub">PSČ: {{ place.postalCode }}</p>
            </div>
            <a
              [href]="getMapyCzUrl()"
              target="_blank"
              rel="noopener noreferrer"
              class="btn btn-secondary nav-link-btn"
            >
              <span>Navigovat na Mapy.cz</span>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path>
                <polyline points="15 3 21 3 21 9"></polyline>
                <line x1="10" y1="14" x2="21" y2="3"></line>
              </svg>
            </a>
          </div>

          <!-- Opening hours -->
          <div *ngIf="place.openingHours" class="info-block">
            <div class="info-icon">🕒</div>
            <div class="info-text">
              <p class="info-label">Otevírací doba</p>
              <p class="info-value">{{ place.openingHours }}</p>
            </div>
          </div>

          <!-- Description -->
          <div class="description-section">
            <h4 class="section-heading">O místě a slevách</h4>
            <p class="description-text">
              {{ place.description || 'K tomuto místu zatím nebyl vložen podrobnější popis.' }}
            </p>
          </div>

          <!-- Community Verification Box -->
          <div class="verification-box">
            <div class="ver-header">
              <h4 class="ver-title">Ověření aktuálnosti komunitou</h4>
              <span class="ver-stats">
                👍 {{ place.votesActive }} potvrdilo fungování / ⚠️ {{ place.votesClosed }} hlásí problém
              </span>
            </div>

            <p class="ver-subtext">Navštívili jste toto místo? Pomozte ostatním udržovat mapu přesnou!</p>

            <div class="ver-actions">
              <button
                class="btn btn-success ver-btn"
                [class.voted]="place.userVote === 'STILL_OPEN'"
                (click)="vote('STILL_OPEN')"
                [disabled]="isVoting()"
              >
                👍 Stále funguje a platí
              </button>

              <button
                class="btn btn-danger ver-btn"
                [class.voted]="place.userVote === 'CLOSED'"
                (click)="vote('CLOSED')"
                [disabled]="isVoting()"
              >
                ⚠️ Nahlásit zavřeno / neaktuální
              </button>
            </div>

            <div *ngIf="voteFeedbackMessage()" class="vote-feedback">
              {{ voteFeedbackMessage() }}
            </div>
          </div>
        </div>

        <!-- Footer / Meta -->
        <div class="detail-footer">
          <div class="author-info">
            <span *ngIf="place.author">Vložil: <strong>{{ place.author.nickname }}</strong></span>
            <span class="created-date">{{ place.createdAt | date:'d. M. yyyy' }}</span>
          </div>

          <div *ngIf="canEdit()" class="admin-actions">
            <button class="btn btn-secondary" (click)="editPlace.emit(place)">
              Upravit místo
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .detail-modal {
      max-width: 680px;
      position: relative;
    }
    .close-btn {
      position: absolute;
      top: 1rem;
      right: 1rem;
      z-index: 10;
      background: rgba(15, 23, 42, 0.6);
      backdrop-filter: blur(4px);
      color: #ffffff;
      border: none;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      font-size: 1rem;
      transition: all var(--transition-fast);
    }
    .close-btn:hover {
      background: rgba(15, 23, 42, 0.9);
      transform: scale(1.05);
    }
    .gallery-section {
      width: 100%;
      height: 280px;
      background: #f1f5f9;
      position: relative;
      overflow: hidden;
      border-radius: var(--radius-lg) var(--radius-lg) 0 0;
    }
    .main-photo {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .gallery-placeholder {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #f8fafc, #e2e8f0);
      color: var(--text-muted);
    }
    .placeholder-emoji {
      font-size: 3.5rem;
      margin-bottom: 0.5rem;
    }
    .placeholder-text {
      font-weight: 600;
      font-size: 1rem;
    }
    .thumbnails-bar {
      position: absolute;
      bottom: 10px;
      left: 10px;
      right: 10px;
      display: flex;
      gap: 6px;
      overflow-x: auto;
      padding: 4px;
      background: rgba(0, 0, 0, 0.4);
      backdrop-filter: blur(4px);
      border-radius: var(--radius-md);
    }
    .thumb-img {
      width: 50px;
      height: 50px;
      object-fit: cover;
      border-radius: var(--radius-sm);
      cursor: pointer;
      border: 2px solid transparent;
      opacity: 0.8;
      transition: all var(--transition-fast);
    }
    .thumb-img:hover, .thumb-img.active {
      opacity: 1;
      border-color: #ffffff;
      transform: scale(1.05);
    }
    .detail-body {
      padding: 1.5rem;
    }
    .badges-row {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      flex-wrap: wrap;
      margin-bottom: 0.75rem;
    }
    .badge-lg {
      font-size: 0.8125rem;
      padding: 0.3rem 0.75rem;
    }
    .badge-price {
      font-size: 0.8125rem;
      background: #f1f5f9;
      padding: 0.3rem 0.75rem;
      border-radius: var(--radius-full);
      color: var(--text-main);
    }
    .badge-discount {
      font-size: 0.8125rem;
      background: var(--primary-50);
      color: var(--primary-700);
      font-weight: 600;
      padding: 0.3rem 0.75rem;
      border-radius: var(--radius-full);
    }
    .detail-title {
      font-size: 1.5rem;
      font-weight: 800;
      letter-spacing: -0.02em;
      color: var(--text-main);
      margin-bottom: 1rem;
    }
    .info-block {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.875rem 1rem;
      background: var(--bg-app);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      margin-bottom: 0.75rem;
    }
    .location-block {
      justify-content: space-between;
    }
    .info-icon {
      font-size: 1.25rem;
    }
    .info-text {
      flex: 1;
    }
    .address-main {
      font-weight: 600;
      font-size: 0.9375rem;
    }
    .address-sub {
      font-size: 0.8125rem;
      color: var(--text-muted);
    }
    .nav-link-btn {
      font-size: 0.8125rem;
      padding: 0.45rem 0.85rem;
    }
    .info-label {
      font-size: 0.75rem;
      color: var(--text-muted);
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    .info-value {
      font-size: 0.875rem;
      font-weight: 500;
    }
    .description-section {
      margin: 1.25rem 0;
    }
    .section-heading {
      font-size: 0.9375rem;
      font-weight: 700;
      margin-bottom: 0.5rem;
      color: var(--text-main);
    }
    .description-text {
      font-size: 0.9375rem;
      line-height: 1.6;
      color: #334155;
      white-space: pre-line;
    }
    .verification-box {
      background: #f8fafc;
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 1.25rem;
      margin-top: 1.5rem;
    }
    .ver-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 0.35rem;
      flex-wrap: wrap;
      gap: 0.5rem;
    }
    .ver-title {
      font-size: 0.9375rem;
      font-weight: 700;
    }
    .ver-stats {
      font-size: 0.8125rem;
      font-weight: 600;
      color: var(--text-muted);
    }
    .ver-subtext {
      font-size: 0.8125rem;
      color: var(--text-muted);
      margin-bottom: 1rem;
    }
    .ver-actions {
      display: flex;
      gap: 0.75rem;
      flex-wrap: wrap;
    }
    .ver-btn {
      flex: 1;
      min-width: 200px;
    }
    .ver-btn.voted {
      box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.4);
      font-weight: 800;
    }
    .vote-feedback {
      margin-top: 0.75rem;
      font-size: 0.8125rem;
      color: #15803d;
      font-weight: 600;
      text-align: center;
    }
    .detail-footer {
      padding: 1rem 1.5rem;
      background: var(--bg-app);
      border-top: 1px solid var(--border-color);
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-radius: 0 0 var(--radius-lg) var(--radius-lg);
    }
    .author-info {
      font-size: 0.8125rem;
      color: var(--text-muted);
      display: flex;
      gap: 0.75rem;
    }
    @media (max-width: 640px) {
      .gallery-section {
        height: 200px;
      }
      .location-block {
        flex-direction: column;
        align-items: flex-start;
      }
      .nav-link-btn {
        width: 100%;
        margin-top: 0.5rem;
      }
    }
  `],
})
export class PlaceDetailComponent {
  private apiService = inject(ApiService);
  private authService = inject(AuthService);

  @Input({ required: true }) place!: Place;
  @Output() close = new EventEmitter<void>();
  @Output() editPlace = new EventEmitter<Place>();
  @Output() updated = new EventEmitter<void>();

  readonly activeImageUrl = signal<string | null>(null);
  readonly isVoting = signal(false);
  readonly voteFeedbackMessage = signal<string | null>(null);

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.close.emit();
    }
  }

  getMapyCzUrl(): string {
    return `https://mapy.cz/zakladni?q=${encodeURIComponent(
      this.place.address + ', ' + this.place.city
    )}&y=${this.place.latitude}&x=${this.place.longitude}&z=17`;
  }

  vote(type: VoteType): void {
    this.isVoting.set(true);
    this.apiService.verifyPlace(this.place.id, type).subscribe({
      next: (res) => {
        this.isVoting.set(false);
        this.place.votesActive = res.votesActive;
        this.place.votesClosed = res.votesClosed;
        this.place.userVote = res.userVote;
        this.voteFeedbackMessage.set(res.message);
        this.updated.emit();
      },
      error: () => {
        this.isVoting.set(false);
        this.voteFeedbackMessage.set('Hlasování se nezdařilo. Zkuste to prosím později.');
      },
    });
  }

  canEdit(): boolean {
    const user = this.authService.currentUser();
    if (!user) return false;
    if (user.role === 'ROLE_ADMIN') return true;
    return this.place.author?.id === user.id;
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
