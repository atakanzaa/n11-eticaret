export interface CategoryResponse {
  id: string;
  parentId?: string;
  name: string;
  slug: string;
  description?: string;
  imageUrl?: string;
  displayOrder: number;
  productCount: number;
  active?: boolean;
}

export interface CreateCategoryRequest {
  parentId?: string;
  name: string;
  slug: string;
  description?: string;
  imageUrl?: string;
  displayOrder: number;
}

export interface UpdateCategoryRequest {
  parentId?: string;
  name?: string;
  slug?: string;
  description?: string;
  imageUrl?: string;
  displayOrder?: number;
  active?: boolean;
}
