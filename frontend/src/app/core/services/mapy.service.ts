import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { CategoryType } from '../models/place.model';

export interface GeocodeResult {
  label: string;
  name: string;
  city: string;
  street?: string;
  zip?: string;
  latitude: number;
  longitude: number;
}

@Injectable({
  providedIn: 'root',
})
export class MapyService {
  constructor(private http: HttpClient) {}

  /**
   * Vyhledání adresy (geokódování) s prioritou pro ČR
   */
  searchAddress(query: string): Observable<GeocodeResult[]> {
    if (!query || query.trim().length < 2) {
      return of([]);
    }

    // Photon / OpenStreetMap s biasem pro ČR & SR
    const url = `https://photon.komoot.io/api/?q=${encodeURIComponent(
      query
    )}&lang=default&limit=6&bbox=12.09,48.55,18.86,51.06`;

    return this.http.get<any>(url).pipe(
      map((res) => {
        if (!res || !res.features) return [];
        return res.features.map((f: any) => {
          const p = f.properties || {};
          const [lon, lat] = f.geometry.coordinates;

          const street = p.street ? (p.housenumber ? `${p.street} ${p.housenumber}` : p.street) : p.name;
          const city = p.city || p.town || p.village || p.county || '';
          const zip = p.postcode || '';

          const parts = [p.name, street, city].filter(Boolean);
          const uniqueParts = Array.from(new Set(parts));

          return {
            label: uniqueParts.join(', ') || 'Neznámé místo',
            name: p.name || street || '',
            city: city,
            street: street,
            zip: zip,
            latitude: lat,
            longitude: lon,
          };
        });
      }),
      catchError(() => of([]))
    );
  }

  /**
   * Zpětné geokódování (Reverse geocoding ze souřadnic)
   */
  reverseGeocode(lat: number, lon: number): Observable<GeocodeResult | null> {
    const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}&zoom=18&addressdetails=1&accept-language=cs`;

    return this.http.get<any>(url).pipe(
      map((res) => {
        if (!res || !res.address) return null;
        const addr = res.address;
        const street = addr.road ? (addr.house_number ? `${addr.road} ${addr.house_number}` : addr.road) : '';
        const city = addr.city || addr.town || addr.village || addr.municipality || '';
        const zip = addr.postcode || '';

        return {
          label: res.display_name,
          name: street || city || 'Vybraný bod na mapě',
          city: city,
          street: street,
          zip: zip,
          latitude: lat,
          longitude: lon,
        };
      }),
      catchError(() => of(null))
    );
  }

  /**
   * Barva markeru podle kategorie
   */
  getCategoryColor(category: CategoryType): string {
    switch (category) {
      case 'FOOD':
        return '#16a34a'; // zelená
      case 'SECOND_HAND':
        return '#ec4899'; // růžová
      case 'OUTLET':
        return '#f97316'; // oranžová
      case 'PALLET_GOODS':
        return '#8b5cf6'; // fialová
      case 'FACTORY_STORE':
        return '#0284c7'; // modrá
      case 'FURNITURE_BAZAAR':
        return '#d97706'; // jantarová
      case 'DRUGSTORE':
        return '#06b6d4'; // tyrkysová
      case 'OTHER':
      default:
        return '#64748b'; // šedá
    }
  }

  /**
   * Ikona podle kategorie (SVG)
   */
  getCategoryIconName(category: CategoryType): string {
    switch (category) {
      case 'FOOD':
        return 'shopping-bag';
      case 'SECOND_HAND':
        return 'shirt';
      case 'OUTLET':
        return 'tag';
      case 'PALLET_GOODS':
        return 'box';
      case 'FACTORY_STORE':
        return 'factory';
      case 'FURNITURE_BAZAAR':
        return 'armchair';
      case 'DRUGSTORE':
        return 'sparkles';
      case 'OTHER':
      default:
        return 'map-pin';
    }
  }
}
