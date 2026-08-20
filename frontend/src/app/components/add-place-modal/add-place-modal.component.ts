import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { ToastService } from '../../core/services/toast.service';
import { GeocodeResult, MapyService } from '../../core/services/mapy.service';
import { CategoryInfo, CategoryType, DiscountType, Place, PriceLevelType } from '../../core/models/place.model';
import * as L from 'leaflet';

@Component({
  selector: 'app-add-place-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="modal-backdrop" (click)="onBackdropClick($event)">
      <div class="modal-card add-modal">
        <div class="modal-header">
          <h3 class="modal-title">
            {{ editPlaceData ? 'Upravit levné místo' : 'Přidat nové levné místo' }}
          </h3>
          <button class="close-btn" (click)="close.emit()">✕</button>
        </div>

        <form [formGroup]="placeForm" (ngSubmit)="submitForm()">
          <div class="modal-body">
            <!-- Alert error -->
            <div *ngIf="errorMessage()" class="alert-error">
              {{ errorMessage() }}
            </div>

            <!-- Basic Info -->
            <div class="form-group">
              <label class="form-label">Název prodejny / místa *</label>
              <input
                type="text"
                class="input-field"
                placeholder="např. Levné potraviny Beroun, Outlet Arena, Vratky e-shopů"
                formControlName="title"
              />
              <div *ngIf="placeForm.get('title')?.touched && placeForm.get('title')?.invalid" class="field-error">
                Zadejte název místa
              </div>
            </div>

            <!-- Category & Price Level Row -->
            <div class="form-row">
              <div class="form-group flex-1">
                <label class="form-label">Kategorie *</label>
                <select class="input-field select-field" formControlName="category">
                  <option *ngFor="let cat of categories" [value]="cat.name">
                    {{ cat.label }}
                  </option>
                </select>
              </div>

              <div class="form-group flex-1">
                <label class="form-label">Cenová hladina *</label>
                <select class="input-field select-field" formControlName="priceLevel">
                  <option value="LOW">Levné (€)</option>
                  <option value="VERY_LOW">Velmi levné (€€)</option>
                  <option value="EXTREME">Extrémní výprodej (€€€)</option>
                </select>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">Typ slevy / nákupu *</label>
              <select class="input-field select-field" formControlName="discountType">
                <option value="PERMANENT">Trvalé nízké ceny (stálý sortiment)</option>
                <option value="FLASH_SALES">Nárazové výprodeje / akční dny / výprodej skladů</option>
              </select>
            </div>

            <!-- Address Autocomplete Search -->
            <div class="form-group">
              <label class="form-label">Adresa a poloha (hledat adresu v ČR) *</label>
              <div class="address-search-box">
                <input
                  type="text"
                  class="input-field"
                  placeholder="Zadejte adresu nebo město pro vyhledání na mapě..."
                  [(ngModel)]="addressSearchText"
                  [ngModelOptions]="{standalone: true}"
                  (input)="onAddressSearchInput()"
                />
                
                <!-- Autocomplete suggestions dropdown -->
                <div *ngIf="addressSuggestions.length > 0" class="suggestions-list">
                  <div
                    *ngFor="let item of addressSuggestions"
                    class="suggestion-item"
                    (click)="selectAddressSuggestion(item)"
                  >
                    📍 {{ item.label }}
                  </div>
                </div>
              </div>
            </div>

            <!-- City & Address Inputs -->
            <div class="form-row">
              <div class="form-group flex-1">
                <label class="form-label">Ulice a č.p. *</label>
                <input
                  type="text"
                  class="input-field"
                  placeholder="např. Plzeňská 45"
                  formControlName="address"
                />
              </div>

              <div class="form-group flex-1">
                <label class="form-label">Město / Obec *</label>
                <input
                  type="text"
                  class="input-field"
                  placeholder="např. Praha / Žebrák"
                  formControlName="city"
                />
              </div>

              <div class="form-group" style="width: 100px;">
                <label class="form-label">PSČ</label>
                <input
                  type="text"
                  class="input-field"
                  placeholder="15000"
                  formControlName="postalCode"
                />
              </div>
            </div>

            <!-- Mini Map for coordinate verification -->
            <div class="form-group">
              <label class="form-label">
                Poloha na mapě (kliknutím do mapy upravíte polohu pinu)
              </label>
              <div id="mini-map" class="mini-map-container"></div>
              <div class="coords-hint">
                Vybrané souřadnice: {{ placeForm.get('latitude')?.value | number:'1.4-4' }}, {{ placeForm.get('longitude')?.value | number:'1.4-4' }}
              </div>
            </div>

            <!-- Opening hours -->
            <div class="form-group">
              <label class="form-label">Otevírací doba</label>
              <input
                type="text"
                class="input-field"
                placeholder="např. Po-Pá: 8:00 - 18:00, So: 9:00 - 13:00"
                formControlName="openingHours"
              />
            </div>

            <!-- Description -->
            <div class="form-group">
              <label class="form-label">Podrobný popis, tipy co koupit a proč se to vyplatí</label>
              <textarea
                class="input-field textarea-field"
                rows="3"
                placeholder="Popište co přesně tam mají (např. jogurty za 5 Kč, značková trička od 99 Kč, vrácené zboží z Amazonu na váhu)..."
                formControlName="description"
              ></textarea>
            </div>

            <!-- Photo upload -->
            <div class="form-group">
              <label class="form-label">Fotografie místa / cenovek</label>
              <div class="file-upload-area" (click)="fileInput.click()">
                <input
                  #fileInput
                  type="file"
                  multiple
                  accept="image/jpeg,image/png,image/webp"
                  (change)="onFilesSelected($event)"
                  style="display: none;"
                />
                <div class="upload-prompt">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                    <circle cx="8.5" cy="8.5" r="1.5"></circle>
                    <polyline points="21 15 16 10 5 21"></polyline>
                  </svg>
                  <span>Klikněte pro výběr fotek (JPG, PNG, WEBP, max. 10 MB/soubor)</span>
                </div>
              </div>

              <!-- Selected files preview -->
              <div *ngIf="selectedFiles.length > 0" class="selected-files-list">
                <div *ngFor="let file of selectedFiles; let i = index" class="file-item">
                  <span>📷 {{ file.name }}</span>
                  <button type="button" class="remove-file-btn" (click)="removeFile(i)">✕</button>
                </div>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" (click)="close.emit()">
              Zrušit
            </button>
            <button
              type="submit"
              class="btn btn-primary"
              [disabled]="placeForm.invalid || isSubmitting()"
            >
              {{ isSubmitting() ? 'Ukládám...' : (editPlaceData ? 'Uložit změny' : 'Odeslat ke schválení') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .add-modal {
      max-width: 650px;
    }
    .modal-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1.25rem 1.5rem;
      border-bottom: 1px solid var(--border-color);
    }
    .modal-title {
      font-size: 1.2rem;
      font-weight: 700;
    }
    .close-btn {
      background: none;
      border: none;
      font-size: 1.25rem;
      color: var(--text-muted);
      cursor: pointer;
    }
    .modal-body {
      padding: 1.5rem;
      max-height: 70vh;
      overflow-y: auto;
    }
    .form-row {
      display: flex;
      gap: 0.75rem;
      flex-wrap: wrap;
    }
    .flex-1 {
      flex: 1;
      min-width: 140px;
    }
    .form-group {
      margin-bottom: 1rem;
    }
    .form-label {
      display: block;
      font-size: 0.8125rem;
      font-weight: 600;
      color: var(--text-main);
      margin-bottom: 0.35rem;
    }
    .textarea-field {
      resize: vertical;
      min-height: 80px;
    }
    .address-search-box {
      position: relative;
    }
    .suggestions-list {
      position: absolute;
      top: 100%;
      left: 0;
      right: 0;
      background: #ffffff;
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      box-shadow: var(--shadow-lg);
      z-index: 100;
      max-height: 180px;
      overflow-y: auto;
      margin-top: 4px;
    }
    .suggestion-item {
      padding: 0.6rem 0.85rem;
      font-size: 0.8125rem;
      cursor: pointer;
      border-bottom: 1px solid var(--border-color-subtle);
    }
    .suggestion-item:hover {
      background: var(--primary-50);
      color: var(--primary-700);
    }
    .mini-map-container {
      width: 100%;
      height: 180px;
      border-radius: var(--radius-md);
      border: 1px solid var(--border-color);
      z-index: 1;
    }
    .coords-hint {
      font-size: 0.75rem;
      color: var(--text-muted);
      margin-top: 0.25rem;
    }
    .file-upload-area {
      border: 2px dashed var(--border-color);
      border-radius: var(--radius-md);
      padding: 1.25rem;
      text-align: center;
      background: var(--bg-app);
      cursor: pointer;
      transition: all var(--transition-fast);
    }
    .file-upload-area:hover {
      border-color: var(--primary-500);
      background: var(--primary-50);
    }
    .upload-prompt {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.8125rem;
      color: var(--text-muted);
    }
    .selected-files-list {
      margin-top: 0.5rem;
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
    }
    .file-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      background: var(--bg-app);
      padding: 0.4rem 0.75rem;
      border-radius: var(--radius-sm);
      font-size: 0.8125rem;
    }
    .remove-file-btn {
      background: none;
      border: none;
      color: #dc2626;
      cursor: pointer;
    }
    .modal-footer {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 0.75rem;
      padding: 1rem 1.5rem;
      border-top: 1px solid var(--border-color);
      background: var(--bg-app);
      border-radius: 0 0 var(--radius-lg) var(--radius-lg);
    }
    .field-error {
      color: #dc2626;
      font-size: 0.75rem;
      margin-top: 0.25rem;
    }
    .alert-error {
      background: #fee2e2;
      color: #991b1b;
      padding: 0.75rem 1rem;
      border-radius: var(--radius-md);
      font-size: 0.875rem;
      margin-bottom: 1rem;
    }
  `],
})
export class AddPlaceModalComponent implements OnInit {
  private fb = inject(FormBuilder);
  private apiService = inject(ApiService);
  private mapyService = inject(MapyService);

  @Input() editPlaceData: Place | null = null;
  @Input() categories: CategoryInfo[] = [];
  @Output() close = new EventEmitter<void>();
  @Output() saved = new EventEmitter<Place>();

  private toastService = inject(ToastService);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  addressSearchText = '';
  addressSuggestions: GeocodeResult[] = [];
  selectedFiles: File[] = [];

  private miniMap?: L.Map;
  private miniMarker?: L.Marker;

  private readonly MAX_SINGLE_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
  private readonly MAX_TOTAL_FILES_SIZE = 30 * 1024 * 1024; // 30 MB
  private readonly ALLOWED_FILE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

  placeForm = this.fb.group({
    title: ['', [Validators.required]],
    category: ['FOOD' as CategoryType, [Validators.required]],
    priceLevel: ['LOW' as PriceLevelType, [Validators.required]],
    discountType: ['PERMANENT' as DiscountType, [Validators.required]],
    address: ['', [Validators.required]],
    city: ['', [Validators.required]],
    postalCode: [''],
    latitude: [50.0755, [Validators.required]],
    longitude: [14.4378, [Validators.required]],
    openingHours: [''],
    description: [''],
  });

  ngOnInit(): void {
    if (this.editPlaceData) {
      this.placeForm.patchValue({
        title: this.editPlaceData.title,
        category: this.editPlaceData.category,
        priceLevel: this.editPlaceData.priceLevel,
        discountType: this.editPlaceData.discountType,
        address: this.editPlaceData.address,
        city: this.editPlaceData.city,
        postalCode: this.editPlaceData.postalCode || '',
        latitude: this.editPlaceData.latitude,
        longitude: this.editPlaceData.longitude,
        openingHours: this.editPlaceData.openingHours || '',
        description: this.editPlaceData.description || '',
      });
    }

    setTimeout(() => {
      this.initMiniMap();
    }, 150);
  }

  private initMiniMap(): void {
    const lat = this.placeForm.get('latitude')?.value || 50.0755;
    const lng = this.placeForm.get('longitude')?.value || 14.4378;

    this.miniMap = L.map('mini-map', {
      center: [lat, lng],
      zoom: 13,
      zoomControl: false,
    });

    // Mapy.cz / OpenStreetMap podklad
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap contributors',
    }).addTo(this.miniMap);

    const icon = L.divIcon({
      className: 'custom-map-pin',
      html: '<div class="pin-content">📍</div>',
      iconSize: [34, 34],
      iconAnchor: [17, 34],
    });

    this.miniMarker = L.marker([lat, lng], { icon, draggable: true }).addTo(this.miniMap);

    this.miniMarker.on('dragend', (e: any) => {
      const position = e.target.getLatLng();
      this.updateCoords(position.lat, position.lng);
    });

    this.miniMap.on('click', (e: L.LeafletMouseEvent) => {
      this.updateCoords(e.latlng.lat, e.latlng.lng);
    });
  }

  private updateCoords(lat: number, lng: number): void {
    this.placeForm.patchValue({ latitude: lat, longitude: lng });
    if (this.miniMarker) {
      this.miniMarker.setLatLng([lat, lng]);
    }
    if (this.miniMap) {
      this.miniMap.panTo([lat, lng]);
    }

    // Automatické dohledání adresy pokud je prázdná
    this.mapyService.reverseGeocode(lat, lng).subscribe((res) => {
      if (res) {
        if (!this.placeForm.get('city')?.value && res.city) {
          this.placeForm.patchValue({ city: res.city });
        }
        if (!this.placeForm.get('address')?.value && res.street) {
          this.placeForm.patchValue({ address: res.street });
        }
        if (!this.placeForm.get('postalCode')?.value && res.zip) {
          this.placeForm.patchValue({ postalCode: res.zip });
        }
      }
    });
  }

  onAddressSearchInput(): void {
    if (this.addressSearchText.length < 2) {
      this.addressSuggestions = [];
      return;
    }

    this.mapyService.searchAddress(this.addressSearchText).subscribe((results) => {
      this.addressSuggestions = results;
    });
  }

  selectAddressSuggestion(item: GeocodeResult): void {
    this.addressSearchText = item.label;
    this.addressSuggestions = [];

    this.placeForm.patchValue({
      address: item.street || item.name,
      city: item.city,
      postalCode: item.zip || '',
      latitude: item.latitude,
      longitude: item.longitude,
    });

    if (this.miniMarker) {
      this.miniMarker.setLatLng([item.latitude, item.longitude]);
    }
    if (this.miniMap) {
      this.miniMap.setView([item.latitude, item.longitude], 15);
    }
  }

  onFilesSelected(event: any): void {
    const files: FileList = event.target.files;
    if (!files || files.length === 0) return;

    let currentTotalSize = this.selectedFiles.reduce((acc, f) => acc + f.size, 0);

    for (let i = 0; i < files.length; i++) {
      const file = files[i];

      if (!this.ALLOWED_FILE_TYPES.includes(file.type.toLowerCase())) {
        this.toastService.warning(`Soubor "${file.name}" nemá podporovaný formát (JPG, PNG, WEBP).`, 'Neplatný soubor');
        continue;
      }

      if (file.size > this.MAX_SINGLE_FILE_SIZE) {
        this.toastService.warning(
          `Soubor "${file.name}" přesahuje povolený limit 10 MB (${(file.size / (1024 * 1024)).toFixed(1)} MB).`,
          'Příliš velký soubor'
        );
        continue;
      }

      if (currentTotalSize + file.size > this.MAX_TOTAL_FILES_SIZE) {
        this.toastService.warning(
          'Celková velikost vybraných fotografií překračuje limit 30 MB.',
          'Překročen limit velikosti'
        );
        break;
      }

      this.selectedFiles.push(file);
      currentTotalSize += file.size;
    }

    event.target.value = '';
  }

  removeFile(index: number): void {
    this.selectedFiles.splice(index, 1);
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.close.emit();
    }
  }

  submitForm(): void {
    if (this.placeForm.invalid) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const val = this.placeForm.value;
    const payload = {
      title: val.title!,
      category: val.category!,
      priceLevel: val.priceLevel!,
      discountType: val.discountType!,
      address: val.address!,
      city: val.city!,
      postalCode: val.postalCode || '',
      latitude: val.latitude!,
      longitude: val.longitude!,
      openingHours: val.openingHours || '',
      description: val.description || '',
    };

    const action$ = this.editPlaceData
      ? this.apiService.updatePlace(this.editPlaceData.id, payload)
      : this.apiService.createPlace(payload);

    action$.subscribe({
      next: (savedPlace) => {
        // Upload fotek pokud byly vybrány
        if (this.selectedFiles.length > 0) {
          this.apiService.uploadImages(savedPlace.id, this.selectedFiles).subscribe({
            next: () => {
              this.isSubmitting.set(false);
              this.toastService.success(
                this.editPlaceData ? 'Místo bylo úspěšně upraveno.' : 'Místo bylo úspěšně přidáno.'
              );
              this.saved.emit(savedPlace);
              this.close.emit();
            },
            error: (uploadErr) => {
              this.isSubmitting.set(false);
              const uploadMsg = uploadErr.error?.message || 'Místo bylo uloženo, ale fotografie se nepodařilo nahrát z důvodu překročení velikosti nebo nepodporovaného formátu.';
              this.toastService.warning(uploadMsg, 'Upozornění k fotografiím');
              this.saved.emit(savedPlace);
              this.close.emit();
            },
          });
        } else {
          this.isSubmitting.set(false);
          this.toastService.success(
            this.editPlaceData ? 'Místo bylo úspěšně upraveno.' : 'Místo bylo úspěšně přidáno.'
          );
          this.saved.emit(savedPlace);
          this.close.emit();
        }
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(
          err.error?.message || 'Uložení místa se nezdařilo. Zkontrolujte vyplněné údaje.'
        );
      },
    });
  }
}
