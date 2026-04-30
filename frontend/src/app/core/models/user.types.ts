export type AddressType = 'HOME' | 'WORK' | 'OTHER';

export interface UserProfileDto {
  id: string;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: 'MALE' | 'FEMALE' | 'OTHER';
  avatarUrl?: string;
  preferredLanguage?: string;
  preferredCurrency?: string;
  marketingConsent: boolean;
  marketingConsentAt?: string;
  kvkkConsent: boolean;
  kvkkConsentAt?: string;
  version: number;
}

export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: 'MALE' | 'FEMALE' | 'OTHER';
  avatarUrl?: string;
  preferredLanguage?: string;
  preferredCurrency?: string;
  marketingConsent?: boolean;
}

export interface AddressDto {
  id: string;
  label: string;
  fullName: string;
  phone: string;
  country: string;
  city: string;
  district: string;
  neighborhood?: string;
  street: string;
  buildingNo?: string;
  apartmentNo?: string;
  postalCode?: string;
  fullAddress: string;
  defaultShipping: boolean;
  defaultBilling: boolean;
  addressType: AddressType;
}

export type CreateAddressRequest = Omit<AddressDto, 'id'>;
export type UpdateAddressRequest = Omit<AddressDto, 'id'>;
