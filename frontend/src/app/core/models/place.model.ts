import { User } from './auth.model';

export type CategoryType =
  | 'FOOD'
  | 'SECOND_HAND'
  | 'OUTLET'
  | 'PALLET_GOODS'
  | 'FACTORY_STORE'
  | 'FURNITURE_BAZAAR'
  | 'DRUGSTORE'
  | 'OTHER';

export type PriceLevelType = 'LOW' | 'VERY_LOW' | 'EXTREME';

export type DiscountType = 'PERMANENT' | 'FLASH_SALES';

export type PlaceStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export type VoteType = 'STILL_OPEN' | 'CLOSED';

export interface PlaceImage {
  id: number;
  filename: string;
  url: string;
  isPrimary: boolean;
  createdAt: string;
}

export interface Place {
  id: number;
  title: string;
  description: string;
  category: CategoryType;
  categoryLabel: string;
  priceLevel: PriceLevelType;
  priceLevelLabel: string;
  discountType: DiscountType;
  discountTypeLabel: string;
  address: string;
  city: string;
  postalCode?: string;
  latitude: number;
  longitude: number;
  openingHours?: string;
  status: PlaceStatus;
  votesActive: number;
  votesClosed: number;
  userVote?: VoteType;
  rejectionReason?: string;
  author?: User;
  images: PlaceImage[];
  createdAt: string;
  updatedAt: string;
}

export interface PlaceCreatePayload {
  title: string;
  description?: string;
  category: CategoryType;
  priceLevel: PriceLevelType;
  discountType: DiscountType;
  address: string;
  city: string;
  postalCode?: string;
  latitude: number;
  longitude: number;
  openingHours?: string;
}

export interface VerificationResponse {
  placeId: number;
  votesActive: number;
  votesClosed: number;
  userVote: VoteType;
  message: string;
}

export interface CategoryInfo {
  name: CategoryType;
  label: string;
  description: string;
  icon?: string;
  color?: string;
}

export interface MapBounds {
  minLat: number;
  maxLat: number;
  minLng: number;
  maxLng: number;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}
