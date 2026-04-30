/**
 * Cross-cutting types that show up in every API response shape:
 * Spring Data's pagination envelope, the project's standard error body,
 * and a few enums shared across modules.
 */

export interface Page<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
  sort?: { sorted: boolean; unsorted: boolean; empty: boolean };
}

export interface Pageable {
  page?: number;
  size?: number;
  sort?: string;
}

/**
 * Matches the backend's `ErrorResponse` shape (shared/common-errors).
 * Always wrapped in an `error` key.
 */
export interface ProblemDetail {
  code: string;
  message: string;
  details?: Array<{ field?: string; message: string }>;
  correlationId?: string;
  timestamp?: string;
}

export interface BackendErrorEnvelope {
  error: ProblemDetail;
}

export type SortDirection = 'asc' | 'desc';
