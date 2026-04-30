export interface CategoryResponse {
  id: string;
  parentId?: string;
  name: string;
  slug: string;
  description?: string;
  imageUrl?: string;
  displayOrder: number;
  productCount: number;
}
