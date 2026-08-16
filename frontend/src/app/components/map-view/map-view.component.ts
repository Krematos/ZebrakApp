import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Place } from '../../core/models/place.model';
import { MapyService } from '../../core/services/mapy.service';
import * as L from 'leaflet';

/**
 * Bezpečné escapování HTML speciálních znaků pro ochranu před XSS útoky
 */
export function escapeHtml(str: string | undefined | null): string {
  if (!str) return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

@Component({
  selector: 'app-map-view',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="map-wrapper">
      <div #mapContainer id="main-map" class="map-container"></div>

      <!-- Floating Controls -->
      <div class="map-floating-controls">
        <button
          class="btn btn-secondary map-ctrl-btn"
          (click)="locateUser()"
          title="Moje aktuální poloha"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"></polygon>
          </svg>
        </button>

        <button
          class="btn btn-secondary map-ctrl-btn"
          (click)="fitAllMarkers()"
          title="Zobrazit celou ČR"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 3 21 3 21 9"></polyline>
            <polyline points="9 21 3 21 3 15"></polyline>
            <line x1="21" y1="3" x2="14" y2="10"></line>
            <line x1="3" y1="21" x2="10" y2="14"></line>
          </svg>
        </button>
      </div>

      <!-- Map legend -->
      <div class="map-legend">
        <span class="legend-item"><span class="legend-dot" style="background:#16a34a"></span> Potraviny</span>
        <span class="legend-item"><span class="legend-dot" style="background:#ec4899"></span> Second-hand</span>
        <span class="legend-item"><span class="legend-dot" style="background:#f97316"></span> Outlety</span>
        <span class="legend-item"><span class="legend-dot" style="background:#8b5cf6"></span> Palety/Vratky</span>
        <span class="legend-item"><span class="legend-dot" style="background:#0284c7"></span> Podnikové</span>
      </div>
    </div>
  `,
  styles: [`
    .map-wrapper {
      position: relative;
      width: 100%;
      height: 100%;
    }
    .map-container {
      width: 100%;
      height: 100%;
      z-index: 1;
    }
    .map-floating-controls {
      position: absolute;
      top: 16px;
      right: 16px;
      z-index: 500;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .map-ctrl-btn {
      width: 42px;
      height: 42px;
      padding: 0;
      border-radius: var(--radius-md);
      box-shadow: var(--shadow-md);
      background: #ffffff;
      color: var(--text-main);
    }
    .map-ctrl-btn:hover {
      background: var(--bg-surface-hover);
    }
    .map-legend {
      position: absolute;
      bottom: 24px;
      right: 16px;
      z-index: 500;
      background: rgba(255, 255, 255, 0.92);
      backdrop-filter: blur(8px);
      padding: 8px 14px;
      border-radius: var(--radius-full);
      box-shadow: var(--shadow-md);
      border: 1px solid var(--border-color);
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 0.75rem;
      font-weight: 600;
      color: var(--text-main);
      flex-wrap: wrap;
    }
    .legend-item {
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .legend-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
    }
    @media (max-width: 768px) {
      .map-legend {
        display: none;
      }
    }
  `],
})
export class MapViewComponent implements OnInit, OnChanges {
  @ViewChild('mapContainer', { static: true }) mapContainer!: ElementRef;

  @Input() places: Place[] = [];
  @Input() selectedPlace: Place | null = null;
  @Output() placeClicked = new EventEmitter<Place>();

  private mapyService = inject(MapyService);
  private map?: L.Map;
  private markersMap = new Map<number, L.Marker>();

  ngOnInit(): void {
    this.initMap();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['places'] && this.map) {
      this.renderMarkers();
    }
    if (changes['selectedPlace'] && this.selectedPlace && this.map) {
      this.centerOnPlace(this.selectedPlace);
    }
  }

  private initMap(): void {
    // Výchozí střed na ČR
    this.map = L.map(this.mapContainer.nativeElement, {
      center: [49.8175, 15.473],
      zoom: 8,
      zoomControl: true,
    });

    // Mapy.cz / CartoDB Voyager / OSM styl podkladu
    L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap contributors, © CartoDB, © Mapy.cz data',
    }).addTo(this.map);

    this.renderMarkers();
  }

  private renderMarkers(): void {
    if (!this.map) return;

    // Odstranit staré markery
    this.markersMap.forEach((marker) => marker.remove());
    this.markersMap.clear();

    const bounds = L.latLngBounds([]);

    this.places.forEach((place) => {
      const color = this.mapyService.getCategoryColor(place.category);
      const emoji = this.getCategoryEmoji(place.category);

      const customIcon = L.divIcon({
        className: `custom-map-pin pin-${place.id}`,
        html: `<div class="pin-content" style="background:${color};">${emoji}</div>`,
        iconSize: [38, 38],
        iconAnchor: [19, 38],
        popupAnchor: [0, -36],
      });

      const marker = L.marker([place.latitude, place.longitude], { icon: customIcon }).addTo(this.map!);

      // Bezpečné vytvoření popup elementu bez rizika XSS
      const popupEl = this.createPopupElement(place);
      marker.bindPopup(popupEl);

      marker.on('click', () => {
        this.placeClicked.emit(place);
      });

      this.markersMap.set(place.id, marker);
      bounds.extend([place.latitude, place.longitude]);
    });

    if (this.places.length > 0 && bounds.isValid()) {
      this.map.fitBounds(bounds, { padding: [50, 50], maxZoom: 14 });
    }
  }

  centerOnPlace(place: Place): void {
    if (!this.map) return;
    this.map.setView([place.latitude, place.longitude], 15, { animate: true });
    const marker = this.markersMap.get(place.id);
    if (marker) {
      marker.openPopup();
    }
  }

  locateUser(): void {
    if (navigator.geolocation && this.map) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const lat = pos.coords.latitude;
          const lng = pos.coords.longitude;
          this.map?.setView([lat, lng], 14, { animate: true });

          const userIcon = L.divIcon({
            className: 'custom-user-marker',
            html: '<div style="width: 18px; height: 18px; background: #2563eb; border: 3px solid #ffffff; border-radius: 50%; box-shadow: 0 0 10px rgba(37,99,235,0.6);"></div>',
            iconSize: [24, 24],
            iconAnchor: [12, 12],
          });

          L.marker([lat, lng], { icon: userIcon })
            .addTo(this.map!)
            .bindPopup('Vaše aktuální poloha')
            .openPopup();
        },
        () => {
          alert('Nepodařilo se zjistit vaši polohu. Zkontrolujte oprávnění prohlížeče.');
        }
      );
    }
  }

  fitAllMarkers(): void {
    if (!this.map) return;
    if (this.places.length > 0) {
      const bounds = L.latLngBounds(this.places.map((p) => [p.latitude, p.longitude]));
      this.map.fitBounds(bounds, { padding: [50, 50] });
    } else {
      this.map.setView([49.8175, 15.473], 8);
    }
  }

  private createPopupElement(place: Place): HTMLElement {
    const container = document.createElement('div');
    container.style.cssText = "font-family:'Plus Jakarta Sans',sans-serif; min-width: 180px; padding: 4px;";

    const title = document.createElement('strong');
    title.style.cssText = 'font-size: 14px; color: #0f172a; display: block; margin-bottom: 4px;';
    title.textContent = place.title;
    container.appendChild(title);

    const badgesDiv = document.createElement('div');
    badgesDiv.style.cssText = 'display: flex; gap: 4px; margin-bottom: 6px; flex-wrap: wrap;';

    const categorySpan = document.createElement('span');
    categorySpan.style.cssText =
      'font-size: 11px; background: #f1f5f9; padding: 2px 6px; border-radius: 99px; font-weight: 600; color: #475569;';
    categorySpan.textContent = place.categoryLabel || place.category;
    badgesDiv.appendChild(categorySpan);

    const priceSpan = document.createElement('span');
    priceSpan.style.cssText =
      'font-size: 11px; background: #eff6ff; color: #1d4ed8; padding: 2px 6px; border-radius: 99px; font-weight: 700;';
    priceSpan.textContent = place.priceLevelLabel || place.priceLevel;
    badgesDiv.appendChild(priceSpan);

    container.appendChild(badgesDiv);

    const addressP = document.createElement('p');
    addressP.style.cssText = 'font-size: 12px; color: #64748b; margin: 0 0 8px 0;';
    addressP.textContent = `📍 ${place.city}, ${place.address}`;
    container.appendChild(addressP);

    const btn = document.createElement('button');
    btn.style.cssText =
      'width: 100%; background: #2563eb; color: #fff; border: none; padding: 6px; border-radius: 8px; font-size: 12px; font-weight: 600; cursor: pointer; transition: background 0.2s;';
    btn.textContent = 'Zobrazit detail →';
    btn.onclick = (e) => {
      e.stopPropagation();
      this.placeClicked.emit(place);
    };
    container.appendChild(btn);

    return container;
  }

  private getCategoryEmoji(category: string): string {
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
