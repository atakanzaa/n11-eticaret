export type ProductStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';

export interface ProductImageDto {
  id: string;
  url: string;
  altText?: string;
  displayOrder: number;
  primary: boolean;
}

export interface ProductResponse {
  id: string;
  title: string;
  slug: string;
  description?: string;
  shortDescription?: string;
  brandId?: string;
  categoryId: string;
  attributes: Record<string, unknown>;
  status: ProductStatus;
  primaryImageUrl?: string;
  images: ProductImageDto[];
  version: number;
  averageRating: number;
  reviewCount: number;
}

export interface CreateProductRequest {
  title: string;
  description?: string;
  shortDescription?: string;
  brandId?: string;
  categoryId: string;
  attributes?: Record<string, unknown>;
}

export interface UpdateProductRequest {
  title?: string;
  description?: string;
  shortDescription?: string;
  attributes?: Record<string, unknown>;
  status?: ProductStatus;
}
