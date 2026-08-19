import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User } from '../models/auth.model';
import {
  CategoryInfo,
  CategoryType,
  DiscountType,
  PagedResponse,
  Place,
  PlaceCreatePayload,
  PriceLevelType,
  VerificationResponse,
  VoteType,
} from '../models/place.model';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private readonly baseUrl = '/api';

  constructor(private http: HttpClient) {}

  // Místa
  searchPlaces(params?: {
    category?: CategoryType;
    priceLevel?: PriceLevelType;
    discountType?: DiscountType;
    minLat?: number;
    maxLat?: number;
    minLng?: number;
    maxLng?: number;
    q?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }): Observable<PagedResponse<Place>> {
    let httpParams = new HttpParams();
    if (params) {
      if (params.category) httpParams = httpParams.set('category', params.category);
      if (params.priceLevel) httpParams = httpParams.set('priceLevel', params.priceLevel);
      if (params.discountType) httpParams = httpParams.set('discountType', params.discountType);
      if (params.minLat != null) httpParams = httpParams.set('minLat', params.minLat.toString());
      if (params.maxLat != null) httpParams = httpParams.set('maxLat', params.maxLat.toString());
      if (params.minLng != null) httpParams = httpParams.set('minLng', params.minLng.toString());
      if (params.maxLng != null) httpParams = httpParams.set('maxLng', params.maxLng.toString());
      if (params.q) httpParams = httpParams.set('q', params.q);
      if (params.page != null) httpParams = httpParams.set('page', params.page.toString());
      if (params.size != null) httpParams = httpParams.set('size', params.size.toString());
      if (params.sortBy) httpParams = httpParams.set('sortBy', params.sortBy);
      if (params.sortDir) httpParams = httpParams.set('sortDir', params.sortDir);
    }
    return this.http.get<PagedResponse<Place>>(`${this.baseUrl}/places`, { params: httpParams });
  }

  getPlaceById(id: number): Observable<Place> {
    return this.http.get<Place>(`${this.baseUrl}/places/${id}`);
  }

  createPlace(payload: PlaceCreatePayload): Observable<Place> {
    return this.http.post<Place>(`${this.baseUrl}/places`, payload);
  }

  updatePlace(id: number, payload: PlaceCreatePayload): Observable<Place> {
    return this.http.put<Place>(`${this.baseUrl}/places/${id}`, payload);
  }

  uploadImages(placeId: number, files: File[]): Observable<any[]> {
    const formData = new FormData();
    files.forEach((file) => {
      formData.append('files', file);
    });
    return this.http.post<any[]>(`${this.baseUrl}/places/${placeId}/images`, formData);
  }

  deleteImage(placeId: number, imageId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/places/${placeId}/images/${imageId}`);
  }

  verifyPlace(placeId: number, vote: VoteType): Observable<VerificationResponse> {
    return this.http.post<VerificationResponse>(`${this.baseUrl}/places/${placeId}/verify`, { vote });
  }

  getMyPlaces(page = 0, size = 20): Observable<PagedResponse<Place>> {
    let httpParams = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PagedResponse<Place>>(`${this.baseUrl}/users/my-places`, { params: httpParams });
  }

  deleteMyAccount(password: string): Observable<void> {
    return this.http.request<void>('delete', `${this.baseUrl}/users/me`, {
      body: { password }
    });
  }

  // Administrace
  getPendingPlaces(page = 0, size = 20): Observable<PagedResponse<Place>> {
    let httpParams = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PagedResponse<Place>>(`${this.baseUrl}/admin/places/pending`, { params: httpParams });
  }

  getAllPlacesAdmin(status?: string, page = 0, size = 20): Observable<PagedResponse<Place>> {
    let httpParams = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (status) httpParams = httpParams.set('status', status);
    return this.http.get<PagedResponse<Place>>(`${this.baseUrl}/admin/places`, { params: httpParams });
  }

  approvePlace(id: number): Observable<Place> {
    return this.http.post<Place>(`${this.baseUrl}/admin/places/${id}/approve`, {});
  }

  rejectPlace(id: number, reason: string): Observable<Place> {
    return this.http.post<Place>(`${this.baseUrl}/admin/places/${id}/reject`, { reason });
  }

  deletePlace(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/admin/places/${id}`);
  }

  getAdminUsers(page = 0, size = 20): Observable<PagedResponse<User>> {
    let httpParams = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PagedResponse<User>>(`${this.baseUrl}/admin/users`, { params: httpParams });
  }

  deleteUserByAdmin(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/admin/users/${userId}`);
  }

  // Metadata
  getCategories(): Observable<CategoryInfo[]> {
    return this.http.get<CategoryInfo[]>(`${this.baseUrl}/metadata/categories`);
  }

  getPriceLevels(): Observable<{ name: PriceLevelType; label: string }[]> {
    return this.http.get<{ name: PriceLevelType; label: string }[]>(`${this.baseUrl}/metadata/price-levels`);
  }

  getDiscountTypes(): Observable<{ name: DiscountType; label: string }[]> {
    return this.http.get<{ name: DiscountType; label: string }[]>(`${this.baseUrl}/metadata/discount-types`);
  }
}
